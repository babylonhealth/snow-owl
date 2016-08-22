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

import com.b2international.index.revision.Purge;
import com.b2international.index.revision.RevisionIndex;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequest;

/**
 * @since 5.0
 */
public class PurgeRequest extends BaseRequest<RepositoryContext, Boolean> {

	private String branchPath;
	private Purge purge;
	
	PurgeRequest() {}
	
	void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}
	
	void setPurge(Purge purge) {
		this.purge = purge;
	}
	
	@Override
	public Boolean execute(RepositoryContext context) {
		context.service(RevisionIndex.class).purge(branchPath, purge);
		return Boolean.TRUE;
	}

	@Override
	protected Class<Boolean> getReturnType() {
		return Boolean.class;
	}

	public static PurgeRequestBuilder builder(String repositoryId) {
		return new PurgeRequestBuilder(repositoryId);
	}

}
