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

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.RelationshipModifier;

/**
 * @since 4.5
 */
public final class SnomedRelationshipCreateRequestBuilder extends SnomedComponentCreateRequestBuilder<SnomedRelationshipCreateRequestBuilder> {

	private Boolean active = Boolean.TRUE;
	private CharacteristicType characteristicType = CharacteristicType.STATED_RELATIONSHIP;
	private String destinationId;
	private String sourceId;
	private boolean destinationNegated;
	private int group = 0;
	private RelationshipModifier modifier = RelationshipModifier.EXISTENTIAL;
	private int unionGroup = 0;
	private String typeId;

	SnomedRelationshipCreateRequestBuilder(String repositoryId) {
		super(repositoryId, ComponentCategory.RELATIONSHIP);
	}
	
	public SnomedRelationshipCreateRequestBuilder setActive(Boolean active) {
		this.active = active;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setDestinationId(String destinationId) {
		this.destinationId = destinationId;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setSourceId(String sourceId) {
		this.sourceId = sourceId;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setCharacteristicType(CharacteristicType characteristicType) {
		this.characteristicType = characteristicType;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setDestinationNegated(boolean destinationNegated) {
		this.destinationNegated = destinationNegated;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setGroup(int group) {
		this.group = group;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setModifier(RelationshipModifier modifier) {
		this.modifier = modifier;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setTypeId(String typeId) {
		this.typeId = typeId;
		return getSelf();
	}
	
	public SnomedRelationshipCreateRequestBuilder setUnionGroup(int unionGroup) {
		this.unionGroup = unionGroup;
		return getSelf();
	}
	
	@Override
	protected void init(BaseSnomedComponentCreateRequest request) {
		final SnomedRelationshipCreateRequest req = (SnomedRelationshipCreateRequest) request;
		req.setActive(active);
		req.setCharacteristicType(characteristicType);
		req.setDestinationId(destinationId);
		req.setSourceId(sourceId);
		req.setDestinationNegated(destinationNegated);
		req.setGroup(group);
		req.setModifier(modifier);
		req.setUnionGroup(unionGroup);
		req.setTypeId(typeId);
	}

	@Override
	protected BaseSnomedComponentCreateRequest createRequest() {
		return new SnomedRelationshipCreateRequest();
	}

}
