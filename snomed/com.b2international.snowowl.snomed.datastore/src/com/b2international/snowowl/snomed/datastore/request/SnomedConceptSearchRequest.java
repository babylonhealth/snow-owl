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

import static com.b2international.snowowl.datastore.index.RevisionDocument.Expressions.ids;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedComponentDocument.Expressions.namespace;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Expressions.ancestors;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Expressions.defining;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Expressions.parents;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Expressions.primitive;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Expressions.statedAncestors;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Expressions.statedParents;
import static com.google.common.collect.Maps.newHashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.b2international.collections.longs.LongCollection;
import com.b2international.commons.collect.LongSets;
import com.b2international.index.Hits;
import com.b2international.index.query.DualScoreFunction;
import com.b2international.index.query.Expression;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.query.FieldScoreFunction;
import com.b2international.index.query.Query;
import com.b2international.index.query.ScoreFunction;
import com.b2international.index.query.SortBy;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.exceptions.IllegalQueryParameterException;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.datastore.converter.SnomedConverters;
import com.b2international.snowowl.snomed.datastore.escg.ConceptIdQueryEvaluator2;
import com.b2international.snowowl.snomed.datastore.escg.EscgParseFailedException;
import com.b2international.snowowl.snomed.datastore.escg.EscgRewriter;
import com.b2international.snowowl.snomed.datastore.escg.IndexQueryQueryEvaluator;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.index.SearchProfileQueryProvider;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Fields;
import com.b2international.snowowl.snomed.dsl.query.RValue;
import com.b2international.snowowl.snomed.dsl.query.SyntaxErrorException;

/**
 * @since 4.5
 */
final class SnomedConceptSearchRequest extends SnomedSearchRequest<SnomedConcepts> {

	private static final float MIN_DOI_VALUE = 1.05f;
	private static final float MAX_DOI_VALUE = 10288.383f;
	
	enum OptionKey {

		/**
		 * Description term to (smart) match
		 */
		TERM,
		
		/**
		 * Parse the term for query syntax search
		 */
		PARSED_TERM,
		
		/**
		 * Description type to match
		 */
		DESCRIPTION_TYPE,

		/**
		 * ESCG expression to match
		 */
		ESCG,

		/**
		 * Namespace part of concept ID to match (?)
		 */
		NAMESPACE,
		
		/**
		 * The definition status to match
		 */
		DEFINITION_STATUS,
		
		/**
		 * Parent concept ID that can be found in the inferred direct super type hierarchy
		 */
		PARENT,
		
		/**
		 * Ancestor concept ID that can be found in the inferred super type hierarchy (includes direct parents)
		 */
		ANCESTOR, 
		
		/**
		 * Parent concept ID that can be found in the stated direct super type hierarchy
		 */
		STATED_PARENT,
		
		/**
		 * Ancestor concept ID that can be found in the stated super type hierarchy (includes direct stated parents as well)
		 */
		STATED_ANCESTOR,
		
		/**
		 * Enable score boosting using DOI field
		 */
		USE_DOI,
		
		/**
		 * Use search profile of the user
		 */
		SEARCH_PROFILE,
		
		/**
		 * Use fuzzy query in the search
		 */
		USE_FUZZY

	}
	
	SnomedConceptSearchRequest() {}

	@Override
	protected SnomedConcepts doExecute(BranchContext context) throws IOException {
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);

		ExpressionBuilder queryBuilder = Expressions.builder();
		
		addActiveClause(queryBuilder);
		addModuleClause(queryBuilder);
		addComponentIdFilter(queryBuilder);
		addEffectiveTimeClause(queryBuilder);
		
		if (containsKey(OptionKey.NAMESPACE)) {
			queryBuilder.must(namespace(getString(OptionKey.NAMESPACE)));
		}
		
		if (containsKey(OptionKey.DEFINITION_STATUS)) {
			if (Concepts.PRIMITIVE.equals(getString(OptionKey.DEFINITION_STATUS))) {
				queryBuilder.must(primitive());
			} else if (Concepts.FULLY_DEFINED.equals(getString(OptionKey.DEFINITION_STATUS))) {
				queryBuilder.must(defining());
			}
		}
		
		if (containsKey(OptionKey.PARENT)) {
			queryBuilder.must(parents(getCollection(OptionKey.PARENT, String.class)));
		}
		
		if (containsKey(OptionKey.STATED_PARENT)) {
			queryBuilder.must(statedParents(getCollection(OptionKey.STATED_PARENT, String.class)));
		}
		
		if (containsKey(OptionKey.ANCESTOR)) {
			final Collection<String> ancestorIds = getCollection(OptionKey.ANCESTOR, String.class);
			queryBuilder.must(Expressions.builder()
					.should(parents(ancestorIds))
					.should(ancestors(ancestorIds))
					.build());
		}
		
		if (containsKey(OptionKey.STATED_ANCESTOR)) {
			final Collection<String> ancestorIds = getCollection(OptionKey.STATED_ANCESTOR, String.class);
			queryBuilder.must(Expressions.builder()
					.should(statedParents(ancestorIds))
					.should(statedAncestors(ancestorIds))
					.build());
		}

		if (containsKey(OptionKey.ESCG)) {
			final String escg = getString(OptionKey.ESCG);
			try {
				final IndexQueryQueryEvaluator queryEvaluator = new IndexQueryQueryEvaluator();
				final Expression escgQuery = queryEvaluator.evaluate(context.service(EscgRewriter.class).parseRewrite(escg));
				queryBuilder.must(escgQuery);
			} catch (final SyntaxErrorException e) {
				throw new IllegalQueryParameterException(e.getMessage());
			} catch (EscgParseFailedException e) {
				final RValue expression = context.service(EscgRewriter.class).parseRewrite(escg);
				final LongCollection matchingConceptIds = new ConceptIdQueryEvaluator2(searcher).evaluate(expression);
				queryBuilder.must(ids(LongSets.toStringSet(matchingConceptIds)));
			}
		}
		
		Expression searchProfileQuery = null;
		if (containsKey(OptionKey.SEARCH_PROFILE)) {
			final String userId = getString(OptionKey.SEARCH_PROFILE);
			searchProfileQuery = SearchProfileQueryProvider.provideQuery(userId);
		}

		
		final Expression queryExpression;
		final SortBy sortBy;
		
		if (containsKey(OptionKey.TERM)) {
			final ExpressionBuilder bq = Expressions.builder();
			// nest current query
			bq.must(queryBuilder.build());
			queryBuilder = bq;
			
			final String term = getString(OptionKey.TERM);
			final Map<String, Float> conceptScoreMap = executeDescriptionSearch(context, term);
			
			try {
				final ComponentCategory category = SnomedIdentifiers.getComponentCategory(term);
				if (category == ComponentCategory.CONCEPT) {
					conceptScoreMap.put(term, Float.MAX_VALUE);
				}
			} catch (IllegalArgumentException e) {
				// ignored
			}
			
			if (conceptScoreMap.isEmpty()) {
				return new SnomedConcepts(offset(), limit(), 0);
			}
			
			queryBuilder.must(ids(conceptScoreMap.keySet()));
			
			final ScoreFunction func = new DualScoreFunction<String, Float>("ConceptScoreMap", Fields.ID, Fields.DOI) {
				@Override
				protected float compute(String idValue, Float interestValue) {
					float interest = containsKey(OptionKey.USE_DOI) ? interestValue : 0.0f;
					
					// TODO move this normalization to index initializer.
					if (interest != 0.0f) {
						interest = (interest - MIN_DOI_VALUE) / (MAX_DOI_VALUE - MIN_DOI_VALUE);
					}
					
					if (conceptScoreMap.containsKey(idValue)) {
						return conceptScoreMap.get(idValue) + interest;
					} else {
						return 0.0f;
					}
				}
			};
			
			
			final Expression q = addSearchProfile(searchProfileQuery, queryBuilder.build());
			queryExpression = Expressions.customScore(q, func);
			sortBy = SortBy.SCORE;
		} else if (containsKey(OptionKey.USE_DOI)) {
			final Expression q = addSearchProfile(searchProfileQuery, queryBuilder.build());
			queryExpression = Expressions.customScore(q, new FieldScoreFunction(Fields.DOI));
			sortBy = SortBy.SCORE;
		} else {
			queryExpression = addSearchProfile(searchProfileQuery, queryBuilder.build());
			sortBy = SortBy.NONE;
		}
		
		
		final Hits<SnomedConceptDocument> hits = searcher.search(Query.select(SnomedConceptDocument.class)
				.where(queryExpression)
				.offset(offset())
				.limit(limit())
				.withScores(SortBy.SCORE == sortBy)
				.build());
		if (limit() < 1 || hits.getTotal() < 1) {
			return new SnomedConcepts(offset(), limit(), hits.getTotal());
		} else {
			return SnomedConverters.newConceptConverter(context, expand(), locales()).convert(hits.getHits(), offset(), limit(), hits.getTotal());
		}
	}

	private Expression addSearchProfile(final Expression searchProfileQuery, final Expression query) {
		if (searchProfileQuery == null) {
			return query;
		} else {
			return Expressions.builder()
					.must(searchProfileQuery)
					.must(query)
					.build();
		}
	}
	
	private Map<String, Float> executeDescriptionSearch(BranchContext context, String term) {
		final SnomedDescriptionSearchRequestBuilder requestBuilder = SnomedRequests.prepareSearchDescription()
			.all()
			.filterByActive(true)
			.filterByTerm(term)
			.filterByLanguageRefSetIds(languageRefSetIds())
			.filterByConceptId(componentIds());
		
		if (containsKey(OptionKey.DESCRIPTION_TYPE)) {
			final String type = getString(OptionKey.DESCRIPTION_TYPE);
			requestBuilder.filterByType(type);
		}
		
		if (containsKey(OptionKey.USE_FUZZY)) {
			requestBuilder.withFuzzySearch();
		}
		
		if (containsKey(OptionKey.PARSED_TERM)) {
			requestBuilder.withParsedTerm();
		}
		
		final Collection<ISnomedDescription> items = requestBuilder
			.build()
			.execute(context)
			.getItems();
		
		final Map<String, Float> conceptMap = newHashMap();
		
		for (ISnomedDescription description : items) {
			if (!conceptMap.containsKey(description.getConceptId())) {
				conceptMap.put(description.getConceptId(), description.getScore());
			}
		}
		
		return conceptMap;
	}

	@Override
	protected Class<SnomedConcepts> getReturnType() {
		return SnomedConcepts.class;
	}
}
