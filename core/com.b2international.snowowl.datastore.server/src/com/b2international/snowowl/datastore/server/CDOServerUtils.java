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

import static com.b2international.commons.CompareUtils.isEmpty;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.commit.CDOChangeSetData;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfoHandler;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.internal.server.Repository;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.IStoreAccessor;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.server.db.IDBStore;
import org.eclipse.emf.cdo.server.db.IDBStoreAccessor;
import org.eclipse.emf.cdo.spi.common.commit.CDORevisionAvailabilityInfo;
import org.eclipse.emf.cdo.spi.common.commit.InternalCDOCommitInfoManager.CommitInfoLoader;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.spi.server.InternalCommitContext;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSession;
import org.eclipse.emf.cdo.spi.server.InternalSessionManager;
import org.eclipse.emf.cdo.spi.server.InternalStore;
import org.eclipse.emf.cdo.spi.server.InternalTransaction;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.spi.cdo.InternalCDOSession;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.monitor.EclipseMonitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.collections.PrimitiveLists;
import com.b2international.collections.longs.LongList;
import com.b2international.commons.CompareUtils;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.datastore.CDOEditingContext;
import com.b2international.snowowl.datastore.cdo.CDOCommitInfoUtils;
import com.b2international.snowowl.datastore.cdo.CDOTransactionAggregator;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.cdo.ICDORepositoryManager;
import com.b2international.snowowl.datastore.cdo.ICDOTransactionAggregator;
import com.b2international.snowowl.datastore.server.internal.ImpersonatingSessionProtocol;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Sugar for server side CDO usage.
 * <p>
 * <b>NOTE:&nbsp;</b>This class is heavily depends on server side services 
 * registered to the application context. Be smart when moving this class to other plug-in.
 */
@SuppressWarnings("restriction")
public abstract class CDOServerUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CDOServerUtils.class);
	private static final int MAX_REVISION_LIMIT = 1_000_000;

	/**
	 * Commits the specified {@link CDOEditingContext}'s transaction with someone else user ID to the backend.
	 * @param context
	 * @param userId
	 * @param comment
	 * @param monitor
	 * @return
	 * @throws CommitException
	 */
	public static CDOCommitInfo commit(final CDOEditingContext context, final String userId, @Nullable final String comment, @Nullable final IProgressMonitor monitor) throws CommitException {
		return commit(context.getTransaction(), userId, comment, monitor);
	}
	
	/**
	 * Commits the specified transaction with someone else user ID to the backend.
	 * @param transaction the transaction to commit.
	 * @param userId the unique ID of the user.
	 * @param comment comment for the commit.
	 * @param monitor monitor for the progress. Can be {@code null}.
	 * @return the commit information containing the changes generated by persisting the change set wrapped by the current transaction.
	 * @throws CommitException if the commit failed.
	 */
	public static CDOCommitInfo commit(final CDOTransaction transaction, final String userId, @Nullable final String comment, 
			@Nullable final IProgressMonitor monitor) throws CommitException {
		
		return commit(transaction, userId, comment, true, monitor);
	}
	
	/**
	 * Commits the specified transaction with someone else user ID to the backend.
	 * @param transaction the transaction to commit.
	 * @param userId the unique ID of the user.
	 * @param comment comment for the commit.
	 * @param notifyWriteAccessHandlers {@code true} if all registered {@link WriteAccessHandler} registered to the {@link IRepository} instance
	 * should be notified after a successful backend commit, otherwise {@code false}. 
	 * @param monitor monitor for the progress. Can be {@code null}.
	 * @return the commit information containing the changes generated by persisting the change set wrapped by the current transaction.
	 * @throws CommitException if the commit failed.
	 */
	public static CDOCommitInfo commit(final CDOTransaction transaction, final String userId, @Nullable final String comment, 
			final boolean notifyWriteAccessHandlers, @Nullable final IProgressMonitor monitor) throws CommitException {
		
		return Iterables.getOnlyElement(commit(Collections.singleton(transaction), userId, comment, notifyWriteAccessHandlers, monitor));
	}
	
	/**
	 * Commits the specified transaction with someone else user ID to the backend.
	 * @param transactions the transactions to commit.
	 * @param userId the unique ID of the user.
	 * @param comment comment for the commit.
	 * @param notifyWriteAccessHandlers {@code true} if all registered {@link WriteAccessHandler} registered to the {@link IRepository} instance
	 * should be notified after a successful backend commit, otherwise {@code false}. 
	 * @param monitor monitor for the progress. Can be {@code null}.
	 * @return the commit information containing the changes generated by persisting the change set wrapped by the current transaction.
	 * @throws CommitException if the commit failed.
	 */
	public static Iterable<CDOCommitInfo> commit(final Iterable<CDOTransaction> transactions, final String userId, @Nullable final String comment, 
			final boolean notifyWriteAccessHandlers, @Nullable final IProgressMonitor monitor) throws CommitException {
		
		return commit(CDOTransactionAggregator.create(transactions), userId, comment, notifyWriteAccessHandlers, monitor);
	}

	/**
	 * Commits the specified transaction with someone else user ID to the backend.
	 * @param transactionAggregator an aggregator encapsulating an arbitrary number of CDO transactions.
	 * @param userId the unique ID of the user.
	 * @param comment comment for the commit.
	 * @param notifyWriteAccessHandlers {@code true} if all registered {@link WriteAccessHandler} registered to the {@link IRepository} instance
	 * should be notified after a successful backend commit, otherwise {@code false}.
	 * @param sendCommitNotification {@code true} is {@link InternalRepository#sendCommitNotification(InternalSession, CDOCommitInfo)} should be invoked
	 * after successful commit, otherwise {@code false}.
	 * @param monitor monitor for the progress. Can be {@code null}.
	 * @return the commit information containing the changes generated by persisting the change set wrapped by the current transaction.
	 * @throws CommitException if the commit failed.
	 */
	public static Iterable<CDOCommitInfo> commit(final ICDOTransactionAggregator transactionAggregator, final String userId, @Nullable final String comment, 
			final boolean notifyWriteAccessHandlers, @Nullable final IProgressMonitor monitor) throws CommitException {
		
		return commit(transactionAggregator, userId, comment, notifyWriteAccessHandlers, true, monitor);
	}
	
	/**
	 * Commits the specified transaction with someone else user ID to the backend.
	 * @param transactionAggregator an aggregator encapsulating an arbitrary number of CDO transactions.
	 * @param userId the unique ID of the user.
	 * @param comment comment for the commit.
	 * @param notifyWriteAccessHandlers {@code true} if all registered {@link WriteAccessHandler} registered to the {@link IRepository} instance
	 * should be notified after a successful backend commit, otherwise {@code false}.
	 * @param sendCommitNotification {@code true} is {@link InternalRepository#sendCommitNotification(InternalSession, CDOCommitInfo)} should be invoked
	 * after successful commit, otherwise {@code false}.
	 * @param monitor monitor for the progress. Can be {@code null}.
	 * @return the commit information containing the changes generated by persisting the change set wrapped by the current transaction.
	 * @throws CommitException if the commit failed.
	 */
	public static Iterable<CDOCommitInfo> commit(final ICDOTransactionAggregator transactionAggregator, final String userId, @Nullable final String comment, 
			final boolean notifyWriteAccessHandlers, final boolean sendCommitNotification, @Nullable IProgressMonitor monitor) throws CommitException {
		
		return new CDOServerCommitBuilder(userId, comment, transactionAggregator)
			.notifyWriteAccessHandlers(notifyWriteAccessHandlers)
			.sendCommitNotification(sendCommitNotification)
			.commit(monitor);
	}
	/**
	 * Performs a {@link InternalRepository#sendCommitNotification(InternalSession, CDOCommitInfo)} on the 
	 * {@link InternalRepository repository} associated with the {@link CDOCommitInfo commit info} argument.
	 * <br>This method does nothing if the {@link CDOCommitInfo commit info} argument is either {@code null} 
	 * or representing a failed commit.
	 * @param commitInfo the commit info to send.
	 */
	public static void sendCommitNotification(@Nullable final CDOCommitInfo commitInfo) {
		
		if (null != commitInfo && commitInfo.getBranch() instanceof CDOBranch) {
				
			InternalSession session = null;
			
			try {

				final ICDOConnection connection = getConnectionManager().get(commitInfo.getBranch());
				final String repositoryUuid = connection.getUuid();
				final InternalRepository repository = getRepositoryByUuid(repositoryUuid);
				
				session = openSession(commitInfo.getUserID(), repositoryUuid);
				
				final CDOCommitInfo delegateCommitInfo = CDOCommitInfoUtils.removeUuidFromComment(commitInfo);
				repository.sendCommitNotification(session, delegateCommitInfo);
				
			} finally {
				
				LifecycleUtil.deactivate(session);
				
			}
			
		}
			
		
	}
	
	/**
	 * Returns with the revisions for the given CDO IDs on a specified {@link CDOBranchPoint branch point}.
	 * @param branchPoint the branch point to get the revisions.
	 * @param ids the CDO IDs.
	 * @return a list of CDO revisions.
	 */
	public static List<CDORevision> getRevisions(final CDOBranchPoint branchPoint, final CDOID... ids) {
		return getRevisions(branchPoint, Arrays.asList(ids));
	}

	/**
	 * Returns with the revisions for the given CDO IDs on a specified {@link CDOBranchPoint branch point}.
	 * @param branchPoint the branch point to get the revisions.
	 * @param ids the CDO IDs.
	 * @return a list of CDO revisions.
	 */
	public static List<CDORevision> getRevisions(final CDOBranchPoint branchPoint, final Collection<CDOID> ids) {
		
		if (isEmpty(ids)) {
			return emptyList();
		}
		
		try {

			final CDOID cdoId = Iterables.get(ids, 0);
			StoreThreadLocal.setAccessor(getAccessor(cdoId));
			
			final List<CDORevision> revisions = newArrayList();
			final Iterator<List<CDOID>> itr = Iterators.partition(ids.iterator(), MAX_REVISION_LIMIT);
			final InternalCDORevisionManager revisionManager = getRevisionManager(cdoId);
			
			while (itr.hasNext()) {
				//get revisions at once but at most 1,000,000
				revisions.addAll(revisionManager.getRevisions(
						newArrayList(itr.next()), 
						branchPoint, 
						CDORevision.UNCHUNKED,
						CDORevision.DEPTH_NONE,
						true,
						null));
				
			}
			
			return revisions;
			
		} finally {
			
			//release resources
			StoreThreadLocal.release();
			
		}
		
	}

	/**
	 * Returns with the revision of an object given by its unique storage key from the {@link CDOBranchVersion branch version}.
	 * <br>May return with {@code null} if the object cannot be found.
	 * @param branchVersion the version of a CDO branch.
	 * @param id the unique CDO ID of an object.
	 * @return the revision of the object or {@code null}.
	 */
	@Nullable public static CDORevision getRevision(final CDOBranchVersion branchVersion, final CDOID id) {
		
		try {
			
			StoreThreadLocal.setAccessor(getAccessor(id));
			
			//get revisions at once
			return getRevisionManager(id).getRevisionByVersion(
					id, 
					branchVersion, 
					CDORevision.UNCHUNKED,
					true);
			
			
			
		} finally {
			
			//release resources
			StoreThreadLocal.release();
			
		}
		
	}
	
	/**
	 * Returns with a list of revisions representing the whole life of an object. Each {@link CDORevision revision}
	 * represents the actual state of an object. The list is ordered by time-wise, in other words, the first element 
	 * representing the latest state of the object, the last element is representing the birth of the object
	 * given by its unique CDO ID. May return with an empty list, if the object never existed on the specified 
	 * branch before (inclusive to the branch point timestamp) the branch point.
	 * <p>This method equivalent to {@link #getObjectRevisions(CDOBranchPoint, CDOID, Integer.MAX_VALUE)}.
	 * @param branchPoint the branch point representing the start point.
	 * @param cdoId the unique ID of the object.
	 * @return a list of revisions, representing lifecycle of an object. Could be empty list if the object
	 * cannot be found.
	 * @see #getObjectRevisions(CDOBranchPoint, CDOID, int)
	 */
	public static List<CDORevision> getObjectRevisions(final CDOBranchPoint branchPoint, final CDOID cdoId) {
		return getObjectRevisions(branchPoint, cdoId, Integer.MAX_VALUE);
	}

	/**
	 * This method behaves equivalently to {@link #getObjectRevisions(CDOBranchPoint, CDOID)} but stops 
	 * collecting the revisions (representing the object lifecycle states) after it reaches the limit.
	 * <p>
	 * This could be a more efficient than {@link #getObjectRevisions(CDOBranchPoint, CDOID)} when client
	 * has the CDO ID of a detached object but has no idea when was it deleted. And only the latest existing 
	 * state is required.
	 * <p>Or the client has an existing object but would like to compare the current state of that object 
	 * with the previous state. 
	 * @param branchPoint the branch point.
	 * @param cdoId the unique CDO ID of an object.
	 * @param limit the maximum number of CDO revisions to return.
	 * @return a list of CDO configuration. May return with an empty list.
	 */
	public static List<CDORevision> getObjectRevisions(final CDOBranchPoint branchPoint, final CDOID cdoId, final int limit) {
		
		Preconditions.checkNotNull(cdoId, "CDO ID argument cannot be null.");
		Preconditions.checkNotNull(branchPoint, "CDO branch point argument cannot be null.");
		Preconditions.checkArgument(limit > 0, "Limit argument should be a positive integer. Was: " + limit);
		
		final List<CDORevision> revisions = getRevisions(branchPoint, cdoId);
		
		//the revision representing the latest state of the object from the given branch point
		CDORevision latestRevision = null;
		
		//if revisions list is empty of the one and only element is null ->
		//the object was deleted or never existed
		//try to find the first revision
		if (emptyOrNullRevision(revisions)) {
	
			final LongList commitTimestamps = PrimitiveLists.newLongArrayList();
			
			((CommitInfoLoader) getAccessor(cdoId)).loadCommitInfos(
					branchPoint.getBranch(), //branch 
					branchPoint.getTimeStamp(),  //start (could be unspecified)
					CDOBranchPoint.UNSPECIFIED_DATE, // end
					new CDOCommitInfoHandler() {
				
				@Override public void handleCommitInfo(final CDOCommitInfo commitInfo) {
					commitTimestamps.add(commitInfo.getTimeStamp());
				}
			});
			
			for (int i = commitTimestamps.size(); i > 0; i--) {
				
				final long timestamp = commitTimestamps.get(i - 1);
				final CDOBranchPoint _branchPoint = branchPoint.getBranch().getPoint(timestamp);
				
				final List<CDORevision> _revisions = getRevisions(_branchPoint, cdoId);
				if (!emptyOrNullRevision(_revisions)) {
					
					latestRevision = Iterables.getOnlyElement(_revisions);
					break;
					
				}
				
				
			}
			
		} else {
			
			latestRevision = Iterables.getOnlyElement(revisions);
			
		}
		
		if (null == latestRevision) {
	
			return Collections.emptyList();
			
		}
		
		final List<CDORevision> $ = Lists.newArrayList();
		$.add(latestRevision);
		
		for (;;) {

			//sanity check. no more item has to processed.
			if ($.size() == limit) {
				break;
			}
			
			final int version = latestRevision.getVersion();
			
			if (version > CDOBranchVersion.FIRST_VERSION) {
				
				final CDOBranchVersion previous = latestRevision.getBranch().getVersion(version - 1);
				latestRevision = getRevision(previous, cdoId);
				
			} else {
				
				final CDOBranchPoint base = latestRevision.getBranch().getBase();
				if (null == base.getBranch()) {
					
					//reached repository creation time
					break;
					
				}
				
				final List<CDORevision> _revisions = getRevisions(base, cdoId);
				if (emptyOrNullRevision(_revisions)) {
				
					//XXX consider Snow Owl specific task synchronization
					//check branches with same name/path but different branch ID
					
					//reached branch where the object does not exist
					break;
					
				}
				
				latestRevision = Iterables.getOnlyElement(_revisions);
				
			}
			
			$.add(latestRevision);
			
		}
		
		return Collections.unmodifiableList($);
		
	}

	/**
	 * Compares the revisions between the given two {@link CDOBranchPoint branch points} and returns with the {@link CDOChangeSetData change set data}. 
	 * Clients may restrict the compare process to specific model packages by giving the unique namespace URIs to the model.
	 * <p>Since this method works on the {@link InternalRepository repository} directly, clients may use this 
	 * instead of {@link CDOView#compareRevisions(CDOBranchPoint, String...)} or 
	 * {@link CDONet4jSession#compareRevisions(CDOBranchPoint, CDOBranchPoint, String...)} as to avoid timeout exception due to the {@link OMMonitor monitor}. 
	 * @param sourceBranchPoint the source branch point.
	 * @param targetBranchPoint the target branch point.
	 * @param nsUris the unique namespace of the model packages.  
	 * @return a {@link CDOChangeSetData change set data} representing the change set between the two {@link CDOBranchPoint branch points}.  
	 */
	public static CDOChangeSetData compareRevisions(final CDOBranchPoint sourceBranchPoint, final CDOBranchPoint targetBranchPoint, final String... nsUris) {
		
		Preconditions.checkNotNull(sourceBranchPoint, "Source CDO branch point argument cannot be null.");
		Preconditions.checkNotNull(targetBranchPoint, "Target CDO branch point argument cannot be null.");
		
		CDOBranchPoint source = sourceBranchPoint;
		CDOBranchPoint target = targetBranchPoint;
		
		final ICDOConnection connection = getConnectionManager().get(source);
		final String uuid = connection.getUuid();
		final InternalCDOSession session = (InternalCDOSession) connection.getSession();
		
		final long now = session.getLastUpdateTime();
		
		if (CDOBranchPoint.UNSPECIFIED_DATE == target.getTimeStamp()) {
			target = target.getBranch().getPoint(now);
		}
		
		if (CDOBranchPoint.UNSPECIFIED_DATE == source.getTimeStamp()) {
			source = source.getBranch().getPoint(now);
		}
		
		final CDORevisionAvailabilityInfo targetInfo = session.createRevisionAvailabilityInfo(target);
		final CDORevisionAvailabilityInfo sourceInfo = session.createRevisionAvailabilityInfo(sourceBranchPoint);
		
		final EclipseMonitor monitor = getNullOmMonitor();
		monitor.begin();
		final Async async = monitor.forkAsync();
	
		
		try {
			
			//sets the accessor on the store thread local
			StoreThreadLocal.setAccessor(getAccessorByUuid(uuid));
			final Set<CDOID> ids = getRepositoryByUuid(uuid).getMergeData(targetInfo, sourceInfo, null, null, nsUris, monitor);
	
			return CDORevisionUtil.createChangeSetData(ids, sourceInfo, targetInfo);
			
		} finally {
			
			if (null != async) {
				
				async.stop();
				
			}
			
			if (null != monitor) {
				
				monitor.done();
				
			}
			
			StoreThreadLocal.release();
			
		}
		
	}

	/**
	 * Returns with a {@link Throwable} if a CDO resource given as a revision already exists. Otherwise returns with {@code null}.
	 * @param revision the revision to check.
	 * @return a {@link Throwable} if the CDO resource already exists.
	 */
	@Nullable public static Throwable checkDuplicateResources(final CDORevision revision, final CDOBranchPoint targetBranchPoint) {
		
		if (revision.isResource() || revision.isResourceFolder()) {

			try {

				StoreThreadLocal.setAccessor(getAccessor(revision.getID()));

				final CDOID folderId = (CDOID) revision.data().getContainerID();
				final String name = (String) revision.data().get(EresourcePackage.eINSTANCE.getCDOResourceNode_Name(), 0);
				final CDOID existingId = StoreThreadLocal.getAccessor().readResourceID(folderId, name, targetBranchPoint);
				
				if (null != existingId && !existingId.equals(revision.getID())) {
					LOGGER.warn("Duplicate resource or folder: " + name + " in folder " + folderId + ".");
					return new SnowowlServiceException("Duplicate resource or folder: " + name + " in folder " + folderId + ".");
				}

				return null;

			} finally {

				//release resources
				StoreThreadLocal.release();

			}

		}

		return null;

	}
	
	/**
	 * Returns with the last commit time made on the given branch.
	 * <br>This method will return with {@link Long#MIN_VALUE} if no modification has been
	 * made on the given branch, hence no commit time is associated with it.  
	 * @param branch
	 * @return
	 */
	public static long getLastCommitTime(final CDOBranch branch) {
		
		Preconditions.checkNotNull(branch, "CDO branch argument cannot be null.");
		final String uuid = getConnectionManager().get(branch).getUuid();
		
		try {
			
			IDBStoreAccessor accessor = ACCESSOR_CACHE.get(uuid);
			
			synchronized (accessor) {
				Connection connection = accessor.getConnection();
				connection.rollback();
		
				try (PreparedStatement statement = connection.prepareStatement("SELECT MAX(COMMIT_TIME) FROM CDO_COMMIT_INFOS WHERE BRANCH_ID=?")) {
					statement.setInt(1, branch.getID());
					ResultSet resultSet = statement.executeQuery();
					final long lastCommitTime = resultSet.next() ? resultSet.getLong(1) : Long.MIN_VALUE;
					return getDbStoreByUuid(uuid).getCreationTime() > lastCommitTime ? Long.MIN_VALUE : lastCommitTime;
				}
			}
			
		} catch (final ExecutionException e) {
			throw new RuntimeException("Failed to get last commit time for branch: " + branch, e);
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to get last commit time for branch: " + branch, e);
		}
	}

	/**
	 * Wraps the specified {@link Runnable} into another one that manages the thread-local store accessor, unsetting it when the wrapped runnable
	 * completes.
	 * 
	 * @param runnable
	 * @param accessor
	 * @return
	 */
	public static Runnable withAccessor(final Runnable runnable, final IStoreAccessor accessor) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					StoreThreadLocal.setAccessor(accessor);
					runnable.run();
				} finally {
					StoreThreadLocal.setAccessor(null);
				}
			}
		};
	}
	
	/**Returns with the store accessor for a given repository.<br>Never {@code null}.*/
	public static IStoreAccessor getAccessorByUuid(final String uuid) {
		return Preconditions.checkNotNull(getDbStoreByUuid(uuid).getWriter(null), "Store accessor was null for repository: " + uuid);
	}

	/**Returns with the store accessor for an object given as its CDO ID argument.<br>Never {@code null}.*/
	public static IDBStoreAccessor getAccessor(final long cdoId) {
		return checkNotNull(getDbStore(cdoId).getWriter(null), "Store accessor was null. CDO ID: " + cdoId);
	}

	/**Opens and returns with a new session with the given user ID for a repository.*/
	public static InternalSession openSession(final String userID, final String uuid) {
		return getSessionManager(uuid).openSession(new ImpersonatingSessionProtocol(userID));
	}

	/**Returns with the repository identified by its unique ID.*/
	public static InternalRepository getRepositoryByUuid(final String uuid) {
		return (InternalRepository) getRepositoryManager().getByUuid(uuid).getRepository();
	}

	/**Creates and initialize a {@link IRepository repository} instance with the given name, backing store, configuration and error logging strategy,*/
	public static IRepository createRepository(final String name, final IStore store, final Map<String, String> properties, final IErrorLoggingStrategy strategy) {
    
		Preconditions.checkNotNull(name, "name");
		Preconditions.checkNotNull(store, "store");
		Preconditions.checkNotNull(properties, "properties");
		Preconditions.checkNotNull(strategy, "strategy");
		
		final Repository repository = new Repository.Default() {
			@Override public InternalCommitContext createCommitContext(final InternalTransaction transaction) {
				return new CustomTransactionCommitContext(transaction, strategy);
			}
		};
    
    repository.setName(name);
    repository.setStore((InternalStore) store);
    repository.setProperties(properties);
    
    return repository;
  }
	
	/** Cache storing a dedicated {@link IDBStoreAccessor} for retrieving latest commit timestamps for each repository. */
	private static final LoadingCache<String, IDBStoreAccessor> ACCESSOR_CACHE = CacheBuilder.newBuilder().build(new CacheLoader<String, IDBStoreAccessor>() {
		@Override public IDBStoreAccessor load(final String uuid) throws Exception {
			Preconditions.checkNotNull(uuid, "Repository UUID argument cannot be null.");
			return (IDBStoreAccessor) getAccessorByUuid(uuid);
		}
	});
	
	/*returns with the connection manager.*/
	private static ICDOConnectionManager getConnectionManager() {
		return ApplicationContext.getInstance().getService(ICDOConnectionManager.class);
	}

	/*returns with a OM monitor instance wrapping a null progress monitor*/
	private static EclipseMonitor getNullOmMonitor() {
		return new EclipseMonitor(new NullProgressMonitor());
	}

	/*returns with the DB accessor*/
	private static IDBStoreAccessor getAccessor(final CDOID cdoId) {
		return getDbStore(cdoId).getWriter(null);
	}
	
	/*returns with the DB store*/
	private static IDBStore getDbStore(final CDOID cdoId) {
		return (IDBStore) getRepository(cdoId).getStore();
	}
	
	/*returns with the DB store*/
	private static IDBStore getDbStore(final long cdoId) {
		return (IDBStore) getRepository(cdoId).getStore();
	}
	
	/*returns with the repository*/
	private static IRepository getRepository(final CDOID cdoId) {
		return getRepositoryManager().get(cdoId).getRepository();
	}
	
	/*returns with the repository*/
	private static IRepository getRepository(final long cdoId) {
		return getRepositoryManager().get(cdoId).getRepository();
	}

	/*returns with the DB store*/
	private static IDBStore getDbStoreByUuid(final String uuid) {
		return (IDBStore) getRepositoryByUuid(uuid).getStore();
	}
	
	/*returns with the revision manager sticked with the underlying repository instance*/
	private static InternalCDORevisionManager getRevisionManager(final CDOID cdoId) {
		return (InternalCDORevisionManager) getRepository(cdoId).getRevisionManager();
	}
	
	/*returns true if the given list of revisions is null, empty or the only element is null. otherwise, false*/
	private static boolean emptyOrNullRevision(final List<CDORevision> revisions) {
		return CompareUtils.isEmpty(revisions) || null == Iterables.getOnlyElement(revisions, null);
	}
	
	/*returns with the session manager*/
	private static InternalSessionManager getSessionManager(final String uuid) {
		return getRepositoryByUuid(uuid).getSessionManager();
	}

	/*returns with the repository manager service*/
	private static ICDORepositoryManager getRepositoryManager() {
		return ApplicationContext.getInstance().getService(ICDORepositoryManager.class);
	}

	private CDOServerUtils() {
		//suppress instantiation
	}
	
}
