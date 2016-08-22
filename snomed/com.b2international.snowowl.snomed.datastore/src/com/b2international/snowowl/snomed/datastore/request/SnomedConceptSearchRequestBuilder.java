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

import java.util.Collection;
import java.util.Set;

import com.b2international.snowowl.datastore.request.RevisionSearchRequest;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;

/**
 * @since 4.5
 */
public final class SnomedConceptSearchRequestBuilder extends SnomedSearchRequestBuilder<SnomedConceptSearchRequestBuilder, SnomedConcepts> {

	SnomedConceptSearchRequestBuilder(String repositoryId) {
		super(repositoryId);
	}

	public final SnomedConceptSearchRequestBuilder withDoi() {
		return addOption(SnomedConceptSearchRequest.OptionKey.USE_DOI, true);
	}

	public final SnomedConceptSearchRequestBuilder withSearchProfile(final String userId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.SEARCH_PROFILE, userId);
	}

	public final SnomedConceptSearchRequestBuilder withFuzzySearch() {
		return addOption(SnomedConceptSearchRequest.OptionKey.USE_FUZZY, true);
	}

	public final SnomedConceptSearchRequestBuilder withParsedTerm() {
		return addOption(SnomedConceptSearchRequest.OptionKey.PARSED_TERM, true);
	}

	public final SnomedConceptSearchRequestBuilder filterByTerm(String term) {
		return addOption(SnomedConceptSearchRequest.OptionKey.TERM, term);
	}

	public final SnomedConceptSearchRequestBuilder filterByDescriptionType(String type) {
		return addOption(SnomedConceptSearchRequest.OptionKey.DESCRIPTION_TYPE, type);
	}

	public final SnomedConceptSearchRequestBuilder filterByEscg(String expression) {
		return addOption(SnomedConceptSearchRequest.OptionKey.ESCG, expression);
	}

	/**
	 * Filter matches to have the specified parent identifier amongst the direct inferred super types.
	 * 
	 * @param parentId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByParent(String parentId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.PARENT, parentId);
	}
	
	/**
	 * Filter matches to have any of the specified parent identifier amongst the direct inferred super types.
	 * 
	 * @param parentId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByParents(Set<String> parentIds) {
		return addOption(SnomedConceptSearchRequest.OptionKey.PARENT, parentIds);
	}

	/**
	 * Filter matches to have the specified parent identifier amongst the direct stated super types.
	 * 
	 * @param parentId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByStatedParent(String parentId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.STATED_PARENT, parentId);
	}
	
	/**
	 * Filter matches to have the specified parent identifier amongst the direct stated super types.
	 * 
	 * @param parentId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByStatedParents(Set<String> parentIds) {
		return addOption(SnomedConceptSearchRequest.OptionKey.STATED_PARENT, parentIds);
	}

	/**
	 * Filter matches to have the specified ancestor identifier amongst the inferred super types (including direct as well).
	 * 
	 * @param ancestorId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByAncestor(String ancestorId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.ANCESTOR, ancestorId);
	}
	
	/**
	 * Filter matches to have any of the specified ancestor identifier amongst the inferred super types (including direct as well).
	 * 
	 * @param ancestorIds
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByAncestors(Collection<String> ancestorIds) {
		return addOption(SnomedConceptSearchRequest.OptionKey.ANCESTOR, ancestorIds);
	}

	/**
	 * Filter matches to have the specified ancestor identifier amongst the stated super types (including direct as well).
	 * 
	 * @param ancestorId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByStatedAncestor(String ancestorId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.STATED_ANCESTOR, ancestorId);
	}
	
	/**
	 * Filter matches to have any of the specified ancestor identifier amongst the stated super types (including direct as well).
	 * 
	 * @param ancestorId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByStatedAncestors(Set<String> ancestorIds) {
		return addOption(SnomedConceptSearchRequest.OptionKey.STATED_ANCESTOR, ancestorIds);
	}

	/**
	 * Filter matches to have the specified definition status set.
	 * 
	 * @param definitionStatusId
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByDefinitionStatus(String definitionStatusId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.DEFINITION_STATUS, definitionStatusId);
	}

	/**
	 * Filter matches to have their ID from the specified namespace.
	 * 
	 * @param namespaceId
	 *            - the namespace concept identifier
	 * @return
	 */
	public final SnomedConceptSearchRequestBuilder filterByNamespace(String namespaceId) {
		return addOption(SnomedConceptSearchRequest.OptionKey.NAMESPACE, namespaceId);
	}

	@Override
	protected RevisionSearchRequest<SnomedConcepts> createSearch() {
		return new SnomedConceptSearchRequest();
	}

}
