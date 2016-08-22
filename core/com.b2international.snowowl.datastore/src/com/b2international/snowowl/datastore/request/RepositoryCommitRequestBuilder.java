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
package com.b2international.snowowl.datastore.request;

import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.BaseRequestBuilder;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.events.RequestBuilder;
import com.b2international.snowowl.core.events.metrics.Metrics;

/**
 * @since 4.5
 */
public class RepositoryCommitRequestBuilder extends BaseRequestBuilder<RepositoryCommitRequestBuilder, ServiceProvider, CommitInfo> {
	
	private String userId;
	private String repositoryId;
	private String branch;
	private String commitComment = "";
	private Request<TransactionContext, ?> body;
	private long preparationTime = Metrics.SKIP;

	protected RepositoryCommitRequestBuilder(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public final RepositoryCommitRequestBuilder setBranch(String branch) {
		this.branch = branch;
		return getSelf();
	}
	
	public final RepositoryCommitRequestBuilder setUserId(String userId) {
		this.userId = userId;
		return getSelf();
	}
	
	public final RepositoryCommitRequestBuilder setBody(RequestBuilder<TransactionContext, ?> req) {
		return setBody(req.build());
	}
	
	public final RepositoryCommitRequestBuilder setBody(Request<TransactionContext, ?> req) {
		this.body = req;
		return getSelf();
	}
	
	public final RepositoryCommitRequestBuilder setCommitComment(String commitComment) {
		this.commitComment = commitComment;
		return getSelf();
	}
	
	/**
	 * Set additional preparation time for this commit. The caller is responsible for measuring the time properly before setting it in this builder and sending the request.  
	 * @param preparationTime
	 * @return
	 */
	public final RepositoryCommitRequestBuilder setPreparationTime(long preparationTime) {
		this.preparationTime = preparationTime;
		return getSelf();
	}

	@Override
	protected final Request<ServiceProvider, CommitInfo> doBuild() {
		return new RepositoryRequest<>(repositoryId, 
				new BranchRequest<>(branch,
					// additional functionality can be extended here after BranchRequest
					extend(new RevisionIndexReadRequest<>(new TransactionalRequest(userId, commitComment, body, preparationTime)))
				));
	}
	
	protected Request<BranchContext, CommitInfo> extend(Request<BranchContext, CommitInfo> req) {
		return req;
	}

}
