/*
 * Copyright 2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.codesystem;

import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequestBuilder;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.request.RepositoryRequestBuilder;
import com.b2international.snowowl.core.uri.CodeSystemURI;

/**
 * @since 7.17
 */
public final class CodeSystemUpgradeSynchronizationRequestBuilder
		extends BaseRequestBuilder<CodeSystemUpgradeSynchronizationRequestBuilder, RepositoryContext, Boolean>
		implements RepositoryRequestBuilder<Boolean> {

	private final CodeSystemURI codeSystemId;
	private final CodeSystemURI source;

	public CodeSystemUpgradeSynchronizationRequestBuilder(CodeSystemURI codeSystemId, CodeSystemURI source) {
		this.codeSystemId = codeSystemId;
		this.source = source;
	}

	@Override
	protected Request<RepositoryContext, Boolean> doBuild() {
		return new CodeSystemUpgradeSynchronizationRequest(codeSystemId, source);
	}

}
