/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.reindex;

import java.util.Collection;

import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.server.InternalSession;

import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchManager;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.core.ft.FeatureToggles;
import com.b2international.snowowl.datastore.server.internal.InternalRepository;
import com.b2international.snowowl.datastore.server.internal.branch.InternalCDOBasedBranch;

/**
 * @since 4.7
 */
@SuppressWarnings("restriction")
public class ReindexRequest extends BaseRequest<RepositoryContext, ReindexResult> {
	
	private long failedCommitTimestamp = 1;

	ReindexRequest() {}
	
	void setFailedCommitTimestamp(final long failedCommitTimestamp) {
		this.failedCommitTimestamp = failedCommitTimestamp;
	}
	
	@Override
	public ReindexResult execute(RepositoryContext context) {
		final InternalRepository repository = (InternalRepository) context.service(Repository.class);
		final FeatureToggles features = context.service(FeatureToggles.class);
		
		int maxCdoBranchId = -1;
		final BranchManager branchManager = context.service(BranchManager.class);
		final Collection<? extends Branch> branches = branchManager.getBranches();
		
		for (final Branch branch : branches) {
			final InternalCDOBasedBranch cdoBranch = (InternalCDOBasedBranch) branch;
			if (cdoBranch.cdoBranchId() > maxCdoBranchId) {
				maxCdoBranchId = cdoBranch.cdoBranchId();
			}
		}
		
		final org.eclipse.emf.cdo.internal.server.Repository cdoRepository = (org.eclipse.emf.cdo.internal.server.Repository) repository.getCdoRepository().getRepository();
		final InternalSession session = cdoRepository.getSessionManager().openSession(null);
		
		try {
			features.enable(featureFor(context.id()));
			//set the session on the StoreThreadlocal for later access
			StoreThreadLocal.setSession(session);
			//for partial replication get the last branch id and commit time from the index
			//right now index is fully recreated
			final IndexMigrationReplicationContext replicationContext = new IndexMigrationReplicationContext(context, maxCdoBranchId, failedCommitTimestamp - 1, session);
			cdoRepository.replicate(replicationContext);
			return new ReindexResult(replicationContext.getFailedCommitTimestamp(),
					replicationContext.getProcessedCommits(), replicationContext.getSkippedCommits(), replicationContext.getException());
		} finally {
			features.disable(featureFor(context.id()));
			StoreThreadLocal.release();
			session.close();
		}
	}

	@Override
	protected Class<ReindexResult> getReturnType() {
		return ReindexResult.class;
	}

	public static ReindexRequestBuilder builder(String repositoryId) {
		return new ReindexRequestBuilder(repositoryId);
	}

	public static String featureFor(String repositoryId) {
		return String.format("%s.reindex", repositoryId);
	}
	
}
