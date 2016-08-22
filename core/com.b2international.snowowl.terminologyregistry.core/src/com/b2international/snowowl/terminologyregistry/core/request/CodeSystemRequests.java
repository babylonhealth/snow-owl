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
package com.b2international.snowowl.terminologyregistry.core.request;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 4.7
 */
public class CodeSystemRequests {

	private final String repositoryId;

	public CodeSystemRequests(final String repositoryId) {
		this.repositoryId = checkNotNull(repositoryId, "repositoryId");
	}
	
	public CodeSystemCreateRequestBuilder prepareNewCodeSystem() {
		return new CodeSystemCreateRequestBuilder(repositoryId);
	}

	public CodeSystemUpdateRequestBuilder prepareUpdateCodeSystem(final String uniqueId) {
		return new CodeSystemUpdateRequestBuilder(repositoryId, uniqueId);
	}

	public CodeSystemGetRequestBuilder prepareGetCodeSystem() {
		return new CodeSystemGetRequestBuilder(repositoryId);
	}

	public CodeSystemSearchRequestBuilder prepareSearchCodeSystem() {
		return new CodeSystemSearchRequestBuilder(repositoryId);
	}

	public CodeSystemVersionSearchRequestBuilder prepareSearchCodeSystemVersion() {
		return new CodeSystemVersionSearchRequestBuilder(repositoryId);
	}
	
	public String getRepositoryId() {
		return repositoryId;
	}

}
