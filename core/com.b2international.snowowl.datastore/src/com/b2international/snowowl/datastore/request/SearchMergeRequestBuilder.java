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
package com.b2international.snowowl.datastore.request;

import com.b2international.commons.CompareUtils;
import com.b2international.commons.options.OptionsBuilder;
import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.core.merge.MergeCollection;

/**
 * @since 4.6
 */
public class SearchMergeRequestBuilder {

	private final String repositoryId;
	private final OptionsBuilder optionsBuilder = OptionsBuilder.newBuilder();
	
	public SearchMergeRequestBuilder(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public SearchMergeRequestBuilder withSource(String source) {
		if (!CompareUtils.isEmpty(source)) {
			optionsBuilder.put("source", source);
		}
		return this;
	}

	public SearchMergeRequestBuilder withTarget(String target) {
		if (!CompareUtils.isEmpty(target)) {
			optionsBuilder.put("target", target);
		}
		return this;
	}

	public SearchMergeRequestBuilder withStatus(Merge.Status status) {
		if (!CompareUtils.isEmpty(status)) {
			optionsBuilder.put("status", status);
		}
		return this;
	}

	public Request<ServiceProvider, MergeCollection> build() {
		return RepositoryRequests.wrap(repositoryId, new SearchMergeRequest(optionsBuilder.build()));
	}
}
