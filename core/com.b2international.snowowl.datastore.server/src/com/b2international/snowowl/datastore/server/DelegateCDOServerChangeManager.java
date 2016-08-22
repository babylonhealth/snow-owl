/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.datastore.server;

import static com.google.common.collect.Sets.newConcurrentHashSet;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.server.InternalSession;
import org.eclipse.emf.cdo.view.CDOView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.concurrent.equinox.ForkJoinUtils;
import com.b2international.commons.status.Statuses;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.LogUtils;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.core.events.metrics.Metrics;
import com.b2international.snowowl.core.events.metrics.MetricsThreadLocal;
import com.b2international.snowowl.core.exceptions.ApiException;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.ICDOChangeProcessor;
import com.b2international.snowowl.datastore.ICDOCommitChangeSet;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.exception.RepositoryLockException;
import com.b2international.snowowl.datastore.oplock.IOperationLockManager;
import com.b2international.snowowl.datastore.oplock.IOperationLockTarget;
import com.b2international.snowowl.datastore.oplock.OperationLockException;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContext;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreOperationLockException;
import com.b2international.snowowl.datastore.oplock.impl.IDatastoreOperationLockManager;
import com.b2international.snowowl.datastore.oplock.impl.SingleRepositoryAndBranchLockTarget;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Change processor implementation to process changes and persist it into lightweight stores.
 * @see CDOServerChangeManager
 */
public class DelegateCDOServerChangeManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DelegateCDOServerChangeManager.class);

	private final Collection<CDOChangeProcessorFactory> factories;
	private final ICDOCommitChangeSet commitChangeSet;
	private final IBranchPath branchPath;
	private final String repositoryUuid;
	
	private @Nullable IOperationLockTarget lockTarget;
	private @Nullable Collection<ICDOChangeProcessor> changeProcessors;
	
	public DelegateCDOServerChangeManager(final ICDOCommitChangeSet commitChangeSet, final Collection<CDOChangeProcessorFactory> factories, final boolean copySession) {
		this.commitChangeSet = Preconditions.checkNotNull(commitChangeSet, "Commit change set data argument cannot be null.");
		final CDOView view = commitChangeSet.getView();
		this.repositoryUuid = ApplicationContext.getInstance().getService(ICDOConnectionManager.class).get(view).getUuid();
		this.branchPath = BranchPathUtils.createPath(view);
		this.factories = Preconditions.checkNotNull(factories, "CDO change processor factories argument cannot be null.");
	}
	
	public ICDOCommitChangeSet getCommitChangeSet() {
		return commitChangeSet;
	}
	
	/**
	 * Provides a way to handle transactions that are to be committed to the lightweight store.
	 * @throws RuntimeException to indicate that the commit operation must not be executed against the index store.
	 */
	public void handleTransactionBeforeCommitting() throws RuntimeException {
		
		try {
			
			lockBranch();
			createProcessors(branchPath);
			
			final Collection<Job> changeProcessingJobs = Sets.newHashSetWithExpectedSize(changeProcessors.size());
			final InternalSession session = StoreThreadLocal.getSession();
			final Metrics metrics = MetricsThreadLocal.get();
			
			for (final ICDOChangeProcessor processor : changeProcessors) {
				
				changeProcessingJobs.add(new Job("Processing commit information with " + processor.getName()) {
					@Override 
					public IStatus run(final IProgressMonitor monitor) {
						
						try {
							StoreThreadLocal.setSession(session);
							MetricsThreadLocal.set(metrics);
							
							processor.process(commitChangeSet);
							return Statuses.ok();
						} catch (final Exception e) {
							return Statuses.error(DatastoreServerActivator.PLUGIN_ID, "Error while processing changes with " + processor.getName() + " for branch: " + branchPath, e);
						} finally {
							//release session for all threads
							StoreThreadLocal.release();
							MetricsThreadLocal.release();
						}
					}
				});
			}

			ForkJoinUtils.runJobsInParallelWithErrorHandling(changeProcessingJobs, null);
			
		} catch (final Exception e) {
			
			try {
				/* 
				 * XXX (apeteri): we don't know if we got here via applyChanges or a CDO commit, so handleTransactionRollback() may be called 
				 * once from here and then once again, separately.
				 */
				handleTransactionRollback();
			} catch (final Exception e2) {
				e.addSuppressed(e2);
			}
			
			if (e instanceof RuntimeException) {
				if (e.getCause() instanceof ApiException) {
					throw (ApiException) e.getCause();
				} else {
					throw new SnowowlRuntimeException("Error when executing change processors on branch: " + branchPath, e);
				}
			} else {
				throw new SnowowlRuntimeException("Error when executing change processors on branch: " + branchPath, e);
			}
		}
	}

	public void handleTransactionRollback() {
		
		if (null == changeProcessors) {
			return;
		}
		
		RuntimeException caughtException = null;
		
		try {
			rollbackAll(changeProcessors);
			changeProcessors = null;
		} catch (final Exception e) {
			caughtException = new SnowowlRuntimeException("Error when rolling back change processors on branch: " + branchPath, e);
		} finally {
			unlockBranch(caughtException);
		}
	}
	
	/**
	 * Provides a way to handle transactions after they have been committed to the lightweight store.
	 * @param monitor
	 */
	public void handleTransactionAfterCommitted() {
		
		if (null == changeProcessors) {
			throw new IllegalStateException("Change processor collection is null for branch: " + branchPath);
		}
		
		RuntimeException caughtException = null;
		final Collection<ICDOChangeProcessor> committedChangeProcessors = newConcurrentHashSet();
		
		try {
			final Metrics metrics = MetricsThreadLocal.get();
			
			final Collection<Job> commitJobs = Sets.newHashSetWithExpectedSize(changeProcessors.size());
			
			for (final ICDOChangeProcessor processor : changeProcessors) {
				commitJobs.add(new Job("Committing " + processor.getName()) {
					
					@Override protected IStatus run(final IProgressMonitor monitor) {
						try {
							MetricsThreadLocal.set(metrics);
							
							//log if anything had changed
							if (processor.hadChangesToProcess()) {

								//XXX if the change set does not contain any changes to the specific terminology
								//there is no need to initialize tracking index writer at IndexServerService
								//and also *NO* need to reopen reader triggered by #commit on change processor.
								//according to profile results getting tracking index writer reference also triggers
								//directory cache loader initialization and snapshotting the current state of the index directory
								processor.commit();

								//log changes
								logUserActivity(processor);
								
								// Add to set of change processors that committed changes successfully
								committedChangeProcessors.add(processor);
							}
							
							return Status.OK_STATUS;
						} catch (final SnowowlServiceException e) {
							try {
								processor.rollback();
							} catch (final SnowowlServiceException ee) {
								return new Status(IStatus.ERROR, DatastoreServerActivator.PLUGIN_ID, "Error while rolling back changes in " + processor.getName() + " for branch: " + branchPath, ee);
							}
							return new Status(IStatus.ERROR, DatastoreServerActivator.PLUGIN_ID, "Error while committing changes with " + processor.getName() + " for branch: " + branchPath, e);
						} finally {
							MetricsThreadLocal.release();
						}
					}
				});
			}
			
			ForkJoinUtils.runJobsInParallelWithErrorHandling(commitJobs, null);
			
		} catch (final Exception e) {
			caughtException = new SnowowlRuntimeException("Error when committing change processors on branch: " + branchPath, e);
		} finally {
			cleanupAfterCommit(caughtException, committedChangeProcessors);
		}
	}

	private void cleanupAfterCommit(RuntimeException caughtException, Collection<ICDOChangeProcessor> committedChangeProcessors) {
		
		try {
			
			final Collection<Job> cleanupJobs = Sets.newHashSetWithExpectedSize(committedChangeProcessors.size());
			
			for (final ICDOChangeProcessor processor : committedChangeProcessors) {
				cleanupJobs.add(new Job("Cleaning up " + processor.getName()) {
					@Override 
					protected IStatus run(final IProgressMonitor monitor) {
						try {
							processor.afterCommit();
							return Status.OK_STATUS;
						} catch (final RuntimeException e) {
							return new Status(IStatus.ERROR, DatastoreServerActivator.PLUGIN_ID, "Error while cleaning up change processor with " + processor.getName() + " for branch: " + branchPath, e);
						}
					}
				});
			}
			
			ForkJoinUtils.runJobsInParallelWithErrorHandling(cleanupJobs, null);
			
		} catch (final Exception e) {
			if (caughtException == null) {
				caughtException = new SnowowlRuntimeException("Error when cleaning up change processors on branch: " + branchPath, e);
			} else {
				caughtException.addSuppressed(new SnowowlRuntimeException("Error when cleaning up change processors on branch: " + branchPath, e));
			}
		} finally {
			unlockBranch(caughtException);
		}
	}

	/**
	 * Logs the change processor activity for audit purposes.
	 * @param processor
	 */
	protected void logUserActivity(final ICDOChangeProcessor processor) {
		LogUtils.logUserEvent(LOGGER, processor.getUserId(), processor.getBranchPath(), processor.getChangeDescription());
	}

	/*performs a rollback in the lightweight stores held by the CDO change processor instances.*/
	private void rollbackAll(final Collection<ICDOChangeProcessor> processors) throws SnowowlServiceException {
		final List<Exception> exceptions = Lists.newArrayList();
		for (final ICDOChangeProcessor processor : processors) {
			try {
				processor.rollback();
			} catch (final Exception e) {
				final SnowowlServiceException exception = new SnowowlServiceException("Error while rolling back changes in " + processor.getName() + ".", e);
				exceptions.add(exception);
			}
		}
		if (exceptions.size() == 1) {
			throw new SnowowlServiceException("Error while rolling back changes.", exceptions.get(0));
		} else if (exceptions.size() > 1) {
			for (final Exception exception : exceptions) {
				LOGGER.error("Error while rolling back changes.", exception);
			}
			throw new SnowowlServiceException("Multiple errors occurred while rolling back changes. See log for details.");
		}
	}
	
	/*initialize all the change processors created via the registered change processor factories.*/
	private void createProcessors(final IBranchPath branchPath) throws Exception {
		
		changeProcessors = null;
		final List<ICDOChangeProcessor> processors = new CopyOnWriteArrayList<ICDOChangeProcessor>();
		final Collection<Job> processorInitializerJobs = Sets.newHashSetWithExpectedSize(factories.size());
		
		for (final CDOChangeProcessorFactory factory : factories) {
			
			processorInitializerJobs.add(new Job("Creating change processor with " + factory.getFactoryName()) {
				
				@Override protected IStatus run(final IProgressMonitor monitor) {
					try {
						processors.add(factory.createChangeProcessor(branchPath));
						return Status.OK_STATUS;
					} catch (final SnowowlServiceException e) {
						final StringBuilder messageBuilder = new StringBuilder("Error while creating change processor for ").append(factory.getFactoryName()).append(" on ").append(branchPath).append(".");
						return new Status(IStatus.ERROR, DatastoreServerActivator.PLUGIN_ID, messageBuilder.toString(), e);
					}
				}
			});
			
		}
		
		ForkJoinUtils.runJobsInParallelWithErrorHandling(processorInitializerJobs, null);
		changeProcessors = processors;
	}
	
	private void lockBranch() {

		final IOperationLockTarget target = createLockTarget();
		final DatastoreLockContext lockContext = createLockContext();
		
		try {
			lockTarget = null;
			getLockManager().lock(lockContext, IOperationLockManager.IMMEDIATE, target);
		} catch (final DatastoreOperationLockException dle) {
			throw createRepositoryLockException(target, dle.getContext(target));
		} catch (final OperationLockException le) {
			throw createRepositoryLockException(target);
		} catch (final InterruptedException e) {
			throw SnowowlRuntimeException.wrap(e);
		}
		
		lockTarget = target;
	}

	private SingleRepositoryAndBranchLockTarget createLockTarget() {
		return new SingleRepositoryAndBranchLockTarget(repositoryUuid, branchPath);
	}

	private DatastoreLockContext createLockContext() {
		return new DatastoreLockContext(commitChangeSet.getUserId(), DatastoreLockContextDescriptions.PROCESS_CHANGES, DatastoreLockContextDescriptions.COMMIT);
	}

	private RepositoryLockException createRepositoryLockException(final IOperationLockTarget lockTarget) {
		return new RepositoryLockException("Write access to " + lockTarget + " was denied; please try again later.");
	}
	
	private RepositoryLockException createRepositoryLockException(final IOperationLockTarget lockTarget, final DatastoreLockContext context) {
		if (null == context) {
			return createRepositoryLockException(lockTarget);
		}
		
		return new RepositoryLockException("Write access to " + lockTarget + " was denied because " + context.getUserId() + " is " + context.getDescription() + ". Please try again later.");
	}

	private void unlockBranch(final RuntimeException caughtException) {

		// Check first if we even managed to get the lock
		if (null == lockTarget) {
			return;
		}
		
		try {
			final DatastoreLockContext lockContext = createLockContext();
			getLockManager().unlock(lockContext, lockTarget);
			lockTarget = null;
		} catch (final OperationLockException le) {
			if (null != caughtException) {
				caughtException.addSuppressed(createUnlockException());
				throw caughtException;
			} else {
				throw createUnlockException();
			}
		}
	}

	private RepositoryLockException createUnlockException() {
		return new RepositoryLockException("Could not unlock " + lockTarget + ".");
	}

	private IDatastoreOperationLockManager getLockManager() {
		return getApplicationContext().getServiceChecked(IDatastoreOperationLockManager.class);
	}
	
	private ApplicationContext getApplicationContext() {
		return ApplicationContext.getInstance();
	}
}