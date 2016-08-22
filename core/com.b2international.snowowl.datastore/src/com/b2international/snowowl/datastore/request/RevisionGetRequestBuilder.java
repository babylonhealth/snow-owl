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

import com.b2international.snowowl.core.domain.BranchContext;

/**
 * @since 4.5
 */
public abstract class RevisionGetRequestBuilder<B extends RevisionGetRequestBuilder<B, R>, R> extends BaseResourceRequestBuilder<B, R> {

	private String componentId;

	protected RevisionGetRequestBuilder(String repositoryId) {
		super(repositoryId);
	}
	
	public final B setComponentId(String componentId) {
		this.componentId = componentId;
		return getSelf();
	}

	@Override
	protected final BaseResourceRequest<BranchContext, R> create() {
		final RevisionGetRequest<R> req = createGet();
		req.setComponentId(componentId);
		return req;
	}
	
	protected abstract RevisionGetRequest<R> createGet();
	
}
