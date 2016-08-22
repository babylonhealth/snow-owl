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

import com.b2international.commons.options.Options;
import com.b2international.snowowl.core.api.IComponent;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.index.RevisionDocument;
import com.b2international.snowowl.datastore.request.RevisionGetRequest;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.datastore.converter.SnomedConverters;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;

/**
 * @since 4.5
 */
final class SnomedRelationshipGetRequest extends RevisionGetRequest<ISnomedRelationship> {

	protected SnomedRelationshipGetRequest() {
		super(ComponentCategory.RELATIONSHIP);
	}

	@Override
	protected ISnomedRelationship process(BranchContext context, IComponent<String> component, Options expand) {
		return SnomedConverters.newRelationshipConverter(context, expand, locales()).convert((SnomedRelationshipIndexEntry) component);
	}
	
	@Override
	protected Class<? extends RevisionDocument> getRevisionType() {
		return SnomedRelationshipIndexEntry.class;
	}
	
	@Override
	protected Class<ISnomedRelationship> getReturnType() {
		return ISnomedRelationship.class;
	}

}
