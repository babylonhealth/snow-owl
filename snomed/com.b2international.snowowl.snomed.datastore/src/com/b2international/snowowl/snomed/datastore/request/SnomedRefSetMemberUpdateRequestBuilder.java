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
package com.b2international.snowowl.snomed.datastore.request;

import java.util.Map;

import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.datastore.request.BaseTransactionalRequestBuilder;

/**
 * @since 4.5
 */
public final class SnomedRefSetMemberUpdateRequestBuilder extends BaseTransactionalRequestBuilder<SnomedRefSetMemberUpdateRequestBuilder, Void> {

	private String memberId;
	private Map<String, Object> source;
	private Boolean force = Boolean.FALSE;
	
	SnomedRefSetMemberUpdateRequestBuilder(String repositoryId) {
		super(repositoryId);
	}
	
	public SnomedRefSetMemberUpdateRequestBuilder setMemberId(String memberId) {
		this.memberId = memberId;
		return getSelf();
	}
	
	public SnomedRefSetMemberUpdateRequestBuilder setSource(Map<String, Object> source) {
		// try to set memberId if not set
		if (memberId == null) {
			setMemberId((String) source.get("memberId"));
		}
		this.source = source;
		return getSelf();
	}
	
	public SnomedRefSetMemberUpdateRequestBuilder force(boolean force) {
		this.force = force;
		return getSelf();
	}
	
	@Override
	protected Request<TransactionContext, Void> doBuild() {
		return new SnomedRefSetMemberUpdateRequest(memberId, source, force);
	}

}
