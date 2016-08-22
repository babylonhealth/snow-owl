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
/**
 * 
 */
package com.b2international.snowowl.semanticengine.simpleast.normalform;

import java.util.Collection;
import java.util.List;

import com.b2international.snowowl.semanticengine.simpleast.utils.QueryAstUtils;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.dsl.query.queryast.AttributeClause;
import com.b2international.snowowl.snomed.dsl.query.queryast.AttributeClauseGroup;
import com.b2international.snowowl.snomed.dsl.query.queryast.ConceptRef;
import com.b2international.snowowl.snomed.dsl.query.queryast.RValue;
import com.google.common.collect.Lists;

/**
 * Generates the normal form of an arbitrary ESCG expression.
 * 
 */
public class SimpleAstExpressionNormalFormGenerator {
	
	private final String branch;

	public SimpleAstExpressionNormalFormGenerator(String branch) {
		this.branch = branch;
	}

	/**
	 * @return the original expression in long normal form
	 */
	public RValue getLongNormalForm(RValue originalExpression) {
		// expression focus concepts	
		Collection<ConceptRef> focusConcepts = QueryAstUtils.getFocusConcepts(originalExpression);
		FocusConceptNormalizer focusConceptNormalizer = new FocusConceptNormalizer(branch);
		FocusConceptNormalizationResult normalizedFocusConcepts = focusConceptNormalizer.normalizeFocusConcepts(focusConcepts);
		
		// expression refinements
		List<AttributeClause> ungroupedExpressionAttributes = QueryAstUtils.getUngroupedAttributes(originalExpression);
		List<AttributeClauseGroup> attributeGroups = QueryAstUtils.getAttributeGroups(originalExpression);
		List<AttributeClauseList> expressionAttributeClauseLists = Lists.newArrayList();
		for (AttributeClauseGroup attributeClauseGroup : attributeGroups) {
			AttributeClauseList attributeClauseList = new AttributeClauseList();
			List<AttributeClause> attributes = QueryAstUtils.getUngroupedAttributes(attributeClauseGroup.getValue());
			attributeClauseList.getAttributeClauses().addAll(attributes);
			expressionAttributeClauseLists.add(attributeClauseList);
		}
		
		AttributeNormalizer attributeNormalizer = new AttributeNormalizer(branch);
		ConceptDefinition normalizedExpressionRefinements = attributeNormalizer.normalizeAttributes(expressionAttributeClauseLists, 
				ungroupedExpressionAttributes);
		
		// merge refinements
		RefinementsMerger refinementsMerger = new RefinementsMerger(branch);
		ConceptDefinition mergedRefinements = refinementsMerger.mergeRefinements(normalizedFocusConcepts, normalizedExpressionRefinements);
		
		// create expression
		List<String> focusConceptIds = Lists.newArrayList();
		for (SnomedConceptDocument conceptMini : normalizedFocusConcepts.filteredPrimitiveSuperTypes) {
			focusConceptIds.add(conceptMini.getId());
		}
		return QueryAstUtils.buildExpression(focusConceptIds, 
				mergedRefinements.getAttributeClauseLists(), 
				mergedRefinements.getUngroupedAttributes());
	}
	
	/**
	 * @return the original expression in short normal form
	 */
	public RValue getShortNormalForm(RValue originalExpression) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}