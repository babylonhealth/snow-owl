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
package com.b2international.snowowl.snomed.validation.constraints.component;

import static com.b2international.commons.CompareUtils.isEmpty;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.index.IndexException;
import com.b2international.snowowl.core.validation.ComponentValidationConstraint;
import com.b2international.snowowl.core.validation.ComponentValidationDiagnostic;
import com.b2international.snowowl.core.validation.ComponentValidationDiagnosticImpl;
import com.b2international.snowowl.datastore.index.DocIdCollector;
import com.b2international.snowowl.datastore.index.DocIdCollector.DocIdsIterator;
import com.b2international.snowowl.datastore.index.IndexQueryBuilder;
import com.b2international.snowowl.datastore.index.IndexUtils;
import com.b2international.snowowl.datastore.index.field.ComponentIdLongField;
import com.b2international.snowowl.datastore.server.snomed.index.SnomedIndexServerService;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.datastore.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants;
import com.b2international.snowowl.snomed.datastore.browser.SnomedIndexQueries;
import com.b2international.snowowl.snomed.datastore.index.SnomedIndexService;
import com.b2international.snowowl.snomed.datastore.services.SnomedConceptNameProvider;
import com.b2international.snowowl.snomed.datastore.services.SnomedRelationshipNameProvider;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Active relationships of the following types must be ungrouped:
 * <ul>
 * <li>Is a (116680003)</li>
 * <li>Part of (123005000)</li>
 * <li>Laterality (272741003)</li>
 * <li>Has active ingredient (127489000)</li>
 * <li>Has dose form (411116001)</li>
 * </ul>
 */
public class SnomedConceptUngroupedRelationshipConstraint extends ComponentValidationConstraint<SnomedConceptIndexEntry> {

	public static final String ID = "com.b2international.snowowl.snomed.validation.constraints.component.SnomedConceptUngroupedRelationshipConstraint";
	
	private static final Set<Long> UNGROUPED_RELATIONSHIP_TYPES = ImmutableSet.of(
			116680003L, 123005000L, 272741003L, 127489000L, 411116001L);
	
	@Override
	public ComponentValidationDiagnostic validate(IBranchPath branchPath, SnomedConceptIndexEntry component) {
		SnomedIndexServerService indexService = (SnomedIndexServerService) ApplicationContext.getInstance().getService(SnomedIndexService.class);
		DocIdCollector collector = DocIdCollector.create(indexService.maxDoc(branchPath));
		IndexQueryBuilder relationshipTypeIndexQueryBuilder = new IndexQueryBuilder();
		for (Long ungroupedRelationshipTypeId : UNGROUPED_RELATIONSHIP_TYPES) {
			relationshipTypeIndexQueryBuilder.matchExactTerm(SnomedIndexBrowserConstants.RELATIONSHIP_ATTRIBUTE_ID, 
					IndexUtils.longToPrefixCoded(ungroupedRelationshipTypeId));
		}
		Query query = new IndexQueryBuilder()
			.require(SnomedIndexQueries.ACTIVE_COMPONENT_QUERY)
			.requireExactTerm(SnomedIndexBrowserConstants.RELATIONSHIP_OBJECT_ID, IndexUtils.longToPrefixCoded(component.getId()))
			.require(relationshipTypeIndexQueryBuilder)
			.toQuery();
		indexService.search(branchPath, query, collector);
		try {
			List<ComponentValidationDiagnostic> diagnostics = Lists.newArrayList();
			DocIdsIterator iterator = collector.getDocIDs().iterator();
			while (iterator.next()) {
				Document doc = indexService.document(branchPath, iterator.getDocID(), ImmutableSet.of(SnomedIndexBrowserConstants.RELATIONSHIP_GROUP,
						SnomedIndexBrowserConstants.RELATIONSHIP_ATTRIBUTE_ID, ComponentIdLongField.COMPONENT_ID));
				int relationshipGroup = doc.getField(SnomedIndexBrowserConstants.RELATIONSHIP_GROUP).numericValue().intValue();
				if (relationshipGroup != 0) {
					long relationshipTypeId = doc.getField(SnomedIndexBrowserConstants.RELATIONSHIP_ATTRIBUTE_ID).numericValue().longValue();
					String relationshipTypeLabel = SnomedConceptNameProvider.INSTANCE.getText(String.valueOf(relationshipTypeId));
					String relationshipLabel = SnomedRelationshipNameProvider.INSTANCE.getText(ComponentIdLongField.getString(doc));
					String errorMessage = "'" + component.getLabel() + "' has a grouped relationship '" + relationshipLabel 
							+ "' of the type '" + relationshipTypeLabel + "' that must always be ungrouped.";
					diagnostics.add(new ComponentValidationDiagnosticImpl(component.getId(), errorMessage, ID, SnomedTerminologyComponentConstants.CONCEPT_NUMBER, error()));
				}
			}
			
			if (isEmpty(diagnostics)) {
				return createOk(component.getId(), ID, SnomedTerminologyComponentConstants.CONCEPT_NUMBER);
			} else {
				return new ComponentValidationDiagnosticImpl(component.getId(), ID, SnomedTerminologyComponentConstants.CONCEPT_NUMBER, diagnostics);
			}
			
		} catch (IOException e) {
			throw new IndexException("Error when evaluating validation constraint.", e);
		}
	}

}