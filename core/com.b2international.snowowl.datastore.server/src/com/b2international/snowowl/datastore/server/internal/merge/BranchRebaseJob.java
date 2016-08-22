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
package com.b2international.snowowl.datastore.server.internal.merge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchMergeException;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.ConflictException;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.datastore.oplock.OperationLockException;
import com.b2international.snowowl.datastore.request.AbstractBranchChangeRequest;
import com.b2international.snowowl.datastore.request.Locks;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.google.common.base.Strings;

/**
 * @since 4.6
 */
public class BranchRebaseJob extends AbstractBranchChangeRemoteJob {

	private static class SyncRebaseRequest extends AbstractBranchChangeRequest<Branch> {

		private static final Logger LOG = LoggerFactory.getLogger(SyncRebaseRequest.class);
		
		SyncRebaseRequest(final Merge merge, final String commitMessage, String reviewId) {
			super(Branch.class, merge.getSource(), merge.getTarget(), commitMessage, reviewId);
		}

		@Override
		protected Branch execute(final RepositoryContext context, final Branch source, final Branch target) {

			if (!target.parent().equals(source)) {
				throw new BadRequestException("Cannot rebase target '%s' on source '%s'; source is not the direct parent of target.", target.path(), source.path());
			}

			try (Locks locks = new Locks(context, source, target)) {
				return target.rebase(source, commitMessage, new Runnable() {
					@Override
					public void run() {
						try {
							locks.unlock(source.path());
						} catch (OperationLockException e) {
							LOG.warn("Failed to unlock source branch in SyncRebaseRequest; continuing.", e);
						}
					}
				});
			} catch (BranchMergeException e) {
				throw new ConflictException(Strings.isNullOrEmpty(e.getMessage()) ? "Cannot rebase target '%s' on source '%s'." : e.getMessage(), target.path(), source.path(), e);
			} catch (OperationLockException e) {
				throw new ConflictException("Lock exception caught while rebasing target '%s' on source '%s'. %s", target.path(), source.path(), e.getMessage());
			} catch (InterruptedException e) {
				throw new ConflictException("Lock obtaining process was interrupted while rebasing target '%s' on source '%s'.", target.path(), source.path());
			}
		}
	}
	
	public BranchRebaseJob(Repository repository, String source, String target, String commitMessage, String reviewId) {
		super(repository, source, target, commitMessage, reviewId);
	}

	@Override
	protected void applyChanges() {
		RepositoryRequests
			.wrap(repository.id(), new SyncRebaseRequest(merge, commitComment, reviewId))
			.executeSync(repository.events());
	}
}
