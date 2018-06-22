/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.internal;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.b2international.commons.platform.Extensions;
import com.b2international.index.DefaultIndex;
import com.b2international.index.Index;
import com.b2international.index.IndexClient;
import com.b2international.index.IndexClientFactory;
import com.b2international.index.Indexes;
import com.b2international.index.mapping.Mappings;
import com.b2international.index.revision.BaseRevisionBranching;
import com.b2international.index.revision.Commit;
import com.b2international.index.revision.DefaultRevisionIndex;
import com.b2international.index.revision.RevisionIndex;
import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.commit.CommitInfo;
import com.b2international.snowowl.core.commit.CommitInfos;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.domain.DelegatingContext;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.RepositoryEvent;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.merge.MergeService;
import com.b2international.snowowl.core.setup.Environment;
import com.b2international.snowowl.datastore.CodeSystemEntry;
import com.b2international.snowowl.datastore.CodeSystemVersionEntry;
import com.b2international.snowowl.datastore.cdo.CDOCommitInfoUtils;
import com.b2international.snowowl.datastore.cdo.CDOCommitInfoUtils.ConsumeAllCDOBranchesHandler;
import com.b2international.snowowl.datastore.cdo.CDOCommitInfoUtils.ConsumeAllCommitInfoHandler;
import com.b2international.snowowl.datastore.config.IndexConfiguration;
import com.b2international.snowowl.datastore.config.IndexSettings;
import com.b2international.snowowl.datastore.config.RepositoryConfiguration;
import com.b2international.snowowl.datastore.events.RepositoryCommitNotification;
import com.b2international.snowowl.datastore.index.MappingProvider;
import com.b2international.snowowl.datastore.internal.InternalRepository;
import com.b2international.snowowl.datastore.internal.branch.CDOBranchManagerImpl;
import com.b2international.snowowl.datastore.replicate.BranchReplicator;
import com.b2international.snowowl.datastore.request.IndexReadRequest;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.datastore.review.ConceptChanges;
import com.b2international.snowowl.datastore.review.Review;
import com.b2international.snowowl.datastore.review.ReviewManager;
import com.b2international.snowowl.datastore.server.RepositoryClassLoaderProviderRegistry;
import com.b2international.snowowl.datastore.server.RepositoryInitializerRegistry;
import com.b2international.snowowl.datastore.server.ReviewConfiguration;
import com.b2international.snowowl.datastore.server.internal.merge.MergeServiceImpl;
import com.b2international.snowowl.datastore.server.internal.review.ReviewManagerImpl;
import com.b2international.snowowl.eventbus.IEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.MapMaker;

/**
 * @since 4.1
 */
public final class CDOBasedRepository extends DelegatingContext implements InternalRepository {

	private static final String REINDEX_DIAGNOSIS_TEMPLATE = "Run reindex to synchronize index with the database. Command: 'snowowl reindex %s%s'.";
	private static final String RESTORE_DIAGNOSIS = "Inconsistent database and index. Shutdown and restore database and indexes from a backup.";

	private final String toolingId;
	private final String repositoryId;
	private final Map<Long, RepositoryCommitNotification> commitNotifications = new MapMaker().makeMap();
	private Health health = Health.RED;
	private String diagnosis;
	
	CDOBasedRepository(String repositoryId, String toolingId, int mergeMaxResults, Environment env) {
		super(env);
		this.toolingId = toolingId;
		this.repositoryId = repositoryId;
		final ObjectMapper mapper = service(ObjectMapper.class);
		BaseRevisionBranching branching = initializeBranchingSupport(mergeMaxResults);
		RevisionIndex index = initIndex(mapper, branching);
		bind(Repository.class, this);
		bind(ClassLoader.class, env.service(RepositoryClassLoaderProviderRegistry.class).getClassLoader());
		// initialize the index
		index.admin().create();
		checkHealth();
		if (health == Health.GREEN) {
			RepositoryInitializerRegistry.INSTANCE.getInitializer(repositoryId);
		}
	}

	@Override
	public void checkHealth() {
		
		final List<CDOCommitInfo> cdoCommits = getAllCDOCommitInfos();
		final CommitInfos indexCommits = getAllIndexCommits();
		
		boolean emptyDatabase = cdoCommits.isEmpty();
		boolean emptyIndex = indexCommits.isEmpty();

		if (emptyDatabase && emptyIndex) {
			// empty dataset, OK
			setHealth(Health.GREEN, null); 
		} else if (emptyDatabase && !emptyIndex) {
			setHealth(Health.RED, RESTORE_DIAGNOSIS);
		} else if (!emptyDatabase && emptyIndex) {
			setHealth(Health.RED, String.format(REINDEX_DIAGNOSIS_TEMPLATE, id(), ""));
		} else if (!emptyDatabase && !emptyIndex) {
			final String diagnosis = validateCommitConsistency(cdoCommits, indexCommits);
			if (Strings.isNullOrEmpty(diagnosis)) {
				setHealth(Health.GREEN, null);	
			} else {
				setHealth(Health.RED, diagnosis);
			}
		} else {
			throw new IllegalStateException("Should not happen");
		}
	}

	@Override
	public String id() {
		return repositoryId;
	}
	
	@Override
	public IEventBus events() {
		return getDelegate().service(IEventBus.class);
	}
	
	@Override
	public void sendNotification(RepositoryEvent event) {
		if (event instanceof RepositoryCommitNotification) {
			final RepositoryCommitNotification notification = (RepositoryCommitNotification) event;
			// enqueue and wait until the actual CDO commit notification arrives
			commitNotifications.put(notification.getCommitTimestamp(), notification);
		} else {
			event.publish(events());
		}
	}
	
	private BaseRevisionBranching initializeBranchingSupport(int mergeMaxResults) {
		final CDOBranchManagerImpl branchManager = new CDOBranchManagerImpl(this, service(ObjectMapper.class));
		bind(BaseRevisionBranching.class, branchManager);
		bind(BranchReplicator.class, branchManager);
		
		final ReviewConfiguration reviewConfiguration = getDelegate().service(SnowOwlConfiguration.class).getModuleConfig(ReviewConfiguration.class);
		final ReviewManagerImpl reviewManager = new ReviewManagerImpl(this, reviewConfiguration);
		bind(ReviewManager.class, reviewManager);

		final MergeServiceImpl mergeService = new MergeServiceImpl(this, mergeMaxResults);
		bind(MergeService.class, mergeService);
		
		return branchManager;
	}

	private RevisionIndex initIndex(final ObjectMapper mapper, BaseRevisionBranching branching) {
		final Collection<Class<?>> types = newArrayList();
		types.add(Review.class);
		types.add(ConceptChanges.class);
		types.add(CodeSystemEntry.class);
		types.add(CodeSystemVersionEntry.class);
		types.addAll(getToolingTypes(toolingId));
		types.add(Commit.class);
		
		final Map<String, Object> indexSettings = newHashMap(getDelegate().service(IndexSettings.class));
		final IndexConfiguration repositoryIndexConfiguration = getDelegate().service(SnowOwlConfiguration.class).getModuleConfig(RepositoryConfiguration.class).getIndexConfiguration();
		indexSettings.put(IndexClientFactory.NUMBER_OF_SHARDS, repositoryIndexConfiguration.getNumberOfShards());
		final IndexClient indexClient = Indexes.createIndexClient(repositoryId, mapper, new Mappings(types), indexSettings);
		final Index index = new DefaultIndex(indexClient);
		final RevisionIndex revisionIndex = new DefaultRevisionIndex(index, branching, mapper);
		// register index and revision index access, the underlying index is the same
		bind(Index.class, index);
		bind(RevisionIndex.class, revisionIndex);
		return revisionIndex;
	}

	private Collection<Class<?>> getToolingTypes(String toolingId) {
		final Collection<Class<?>> types = newHashSet();
		final Collection<MappingProvider> providers = Extensions.getExtensions("com.b2international.snowowl.datastore.mappingProvider", MappingProvider.class);
		for (MappingProvider provider : providers) {
			if (provider.getToolingId().equals(toolingId)) {
				types.addAll(provider.getMappings());
			}
		}
		return types;
	}

	@Override
	public void doDispose() {
		service(RevisionIndex.class).admin().close();
	}
	
	
	@Override
	protected Environment getDelegate() {
		return (Environment) super.getDelegate();
	}
	
	
	@Override
	@SuppressWarnings("restriction")
	public void handleCommitInfo(CDOCommitInfo commitInfo) {
		if (!(commitInfo instanceof org.eclipse.emf.cdo.internal.common.commit.FailureCommitInfo)) {
			final CDOBranch branch = commitInfo.getBranch();
			final long commitTimestamp = commitInfo.getTimeStamp();
			// send out the currently enqueued commit notification, if there is any (import might skip sending commit notifications until a certain point)
			RepositoryCommitNotification notification = commitNotifications.remove(commitTimestamp);
			if (notification == null) {
				// make sure we always send out commit notification
				// required in case of manual commit notifications via CDO API
				CommitInfo indexCommit = executeLocalSearch(RepositoryRequests.commitInfos()
					.prepareSearchCommitInfo()
					.one()
					.filterByTimestamp(commitTimestamp)
					.filterByAuthor(commitInfo.getUserID())
					.filterByBranch(branch.getPathName())
					.build())
					.first()
					.get();
				
				notification = new RepositoryCommitNotification(id(),
					indexCommit.getId(),						
					CDOCommitInfoUtils.getUuid(commitInfo),
					branch.getPathName(),
					commitTimestamp,
					commitInfo.getUserID(),
					commitInfo.getComment(),
					Collections.emptyList(),
					Collections.emptyList(),
					Collections.emptyList());
			}
			notification.publish(events());
        }
	}

	@Override
	public void setHealth(Health health, String diagnosis) {
		this.health = health;
		if (Health.GREEN != health) {
			checkState(!Strings.isNullOrEmpty(diagnosis), "Diagnosis required for health status %s", health);
		}
		this.diagnosis = diagnosis;
	}
	
	@Override
	public Health health() {
		return health;
	}
	
	@Override
	public String diagnosis() {
		return diagnosis;
	}
	
	private String validateCommitConsistency(List<CDOCommitInfo> cdoCommits, CommitInfos indexCommits) {
		
		final TreeMap<Long, CDOCommitInfo> cdoCommitsByTimestamp = new TreeMap<>();
		final TreeMap<Long, CommitInfo> indexCommitsByTimestamp = new TreeMap<>();
		
		cdoCommits.forEach(c -> cdoCommitsByTimestamp.put(c.getTimeStamp(), c));
		indexCommits.forEach(c -> indexCommitsByTimestamp.put(c.getTimestamp(), c));

		long firstMissingCdoCommitTimestamp = -1;
		
		for (Long nextCdoCommitTimestamp : cdoCommitsByTimestamp.navigableKeySet()) {
			
			final CDOCommitInfo nextCdoCommit = cdoCommitsByTimestamp.get(nextCdoCommitTimestamp);
			final Entry<Long, CommitInfo> firstIndexCommit = indexCommitsByTimestamp.firstEntry();
			
			if (firstIndexCommit != null) {
				
				final Long nextIndexCommitTimestamp = firstIndexCommit.getKey();
				if (nextIndexCommitTimestamp.equals(nextCdoCommitTimestamp)) {
					// cdo commit is present in the index remove index commit from the treemap
					indexCommitsByTimestamp.remove(nextIndexCommitTimestamp);
				} else if (nextIndexCommitTimestamp > nextCdoCommitTimestamp) {
					// importers can create batch commits using the same UUID in cdo which overrides subsequent index commits and results in a single
					// index commit at the end
					final String commitId = CDOCommitInfoUtils.getUuid(nextCdoCommit.getComment());

					boolean foundImporterIndexCommit = false;
					
					for (Long followingIndexCommitTimestamp : indexCommitsByTimestamp.navigableKeySet().tailSet(nextIndexCommitTimestamp)) {
						CommitInfo followingCommit = indexCommitsByTimestamp.get(followingIndexCommitTimestamp);
						if (Objects.equals(commitId, followingCommit.getGroupId())) {
							foundImporterIndexCommit = true;
							break;
						}
					}
					
					if (!foundImporterIndexCommit) {
						return RESTORE_DIAGNOSIS;
					}
					
				} else if (nextIndexCommitTimestamp < nextCdoCommitTimestamp) {
					return RESTORE_DIAGNOSIS;
				}
				
			} else {
				// first cdo commit from where reindex should be invoked  
				if (firstMissingCdoCommitTimestamp == -1) {
					firstMissingCdoCommitTimestamp = nextCdoCommitTimestamp;
				}
			}
		}
		
		if (firstMissingCdoCommitTimestamp != -1) {
			return String.format(REINDEX_DIAGNOSIS_TEMPLATE, id(), String.format(" %s", firstMissingCdoCommitTimestamp));
		}
		
		return null;
	}

	private List<CDOCommitInfo> getAllCDOCommitInfos() {
		final CDONet4jSession session = getConnection().getSession();
		
		Map<Integer, CDOBranch> existingCDOBranches = newHashMap();
		CDOBranch mainBranch = session.getBranchManager().getMainBranch();
		existingCDOBranches.put(mainBranch.getID(), mainBranch);
		
		ConsumeAllCDOBranchesHandler branchHandler = new ConsumeAllCDOBranchesHandler();
		session.getBranchManager().getBranches(0, Integer.MAX_VALUE, branchHandler);
		branchHandler.getBranches().forEach( b -> existingCDOBranches.put(b.getID(), b));
		
		final CDOCommitInfoManager commitInfoManager = session.getCommitInfoManager();
		final ConsumeAllCommitInfoHandler handler = new ConsumeAllCommitInfoHandler();
		commitInfoManager.getCommitInfos(null, CDOCommitInfo.UNSPECIFIED_DATE, CDOCommitInfo.UNSPECIFIED_DATE, handler);
		return handler.getInfos().stream()
				.filter(commitInfo -> !CDOCommitInfoUtils.CDOCommitInfoQuery.EXCLUDED_USERS.contains(commitInfo.getUserID())
						&& existingCDOBranches.containsKey(commitInfo.getBranch().getID()))
				.collect(Collectors.toList());
	}

	private CommitInfos getAllIndexCommits() {
		return executeLocalSearch(RepositoryRequests.commitInfos()
				.prepareSearchCommitInfo()
				.all()
				.build());
	}

	private <T> T executeLocalSearch(final Request<RepositoryContext, T> req) {
		return new IndexReadRequest<>(req).execute(new DefaultRepositoryContext(this, this));
	}
}
