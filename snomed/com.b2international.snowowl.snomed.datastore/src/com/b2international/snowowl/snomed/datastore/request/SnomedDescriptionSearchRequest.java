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

import static com.b2international.snowowl.datastore.index.RevisionDocument.Expressions.id;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.acceptableIn;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.allTermPrefixesPresent;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.allTermsPresent;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.concepts;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.exactTerm;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.fuzzy;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.languageCodes;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.parsedTerm;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.preferredIn;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry.Expressions.types;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;

import com.b2international.collections.longs.LongSet;
import com.b2international.commons.collect.LongSets;
import com.b2international.index.Hits;
import com.b2international.index.query.Expression;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.query.Query;
import com.b2international.index.query.SortBy;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.IllegalQueryParameterException;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.datastore.converter.SnomedConverters;
import com.b2international.snowowl.snomed.datastore.escg.ConceptIdQueryEvaluator2;
import com.b2international.snowowl.snomed.datastore.escg.EscgRewriter;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry;
import com.b2international.snowowl.snomed.dsl.query.RValue;
import com.b2international.snowowl.snomed.dsl.query.SyntaxErrorException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMultimap;

/**
 * @since 4.5
 */
final class SnomedDescriptionSearchRequest extends SnomedSearchRequest<SnomedDescriptions> {

	enum OptionKey {
		TERM,
		CONCEPT_ESCG,
		CONCEPT_ID,
		TYPE,
		ACCEPTABILITY,
		LANGUAGE,
		USE_FUZZY,
		PARSED_TERM;
	}
	
	SnomedDescriptionSearchRequest() {}

	@Override
	protected SnomedDescriptions doExecute(BranchContext context) throws IOException {
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);
		if (containsKey(OptionKey.TERM) && getString(OptionKey.TERM).length() < 2) {
			throw new BadRequestException("Description term must be at least 2 characters long.");
		}
		
		if (containsKey(OptionKey.ACCEPTABILITY) || !languageRefSetIds().isEmpty()) {
			
			if (containsKey(OptionKey.ACCEPTABILITY) && languageRefSetIds().isEmpty()) {
				throw new BadRequestException("A list of language reference set identifiers must be specified if acceptability is set.");
			}
			
			final ImmutableMultimap.Builder<String, ISnomedDescription> buckets = ImmutableMultimap.builder();
			
			int position = 0;
			int total = 0;
			
			for (final String languageRefSetId : languageRefSetIds()) {
				// Do a hitcount-only run for this language reference set first
				SnomedDescriptions subResults = search(context, searcher, languageRefSetId, 0, 0);
				int subTotal = subResults.getTotal();

				// Run actual search only if it is within the required range
				if (position + subTotal > offset() && position < offset() + limit()) {
					// Relative offset may not become negative
					int subOffset = Math.max(0, offset() - position);
					// Relative limit may not go over subTotal, or the number of remaining items to collect
					int subLimit = Math.min(subTotal - subOffset, offset() + limit() - position);
					
					subResults = search(context, searcher, languageRefSetId, subOffset, subLimit);
					buckets.putAll(languageRefSetId, subResults.getItems());
				}
				
				total += subTotal;
				position += subTotal;
			}
			
			List<ISnomedDescription> concatenatedList = buckets.build().values().asList();
			return new SnomedDescriptions(concatenatedList, offset(), limit(), total);
			
		} else {
			return search(context, searcher, "-1", offset(), limit());
		}
	}

	private SnomedDescriptions search(BranchContext context, final RevisionSearcher searcher, String languageRefSetId, int offset, int limit) throws IOException {
		final ExpressionBuilder queryBuilder = Expressions.builder();
		// Add (presumably) most selective filters first
		addActiveClause(queryBuilder);
		addEffectiveTimeClause(queryBuilder);
		addModuleClause(queryBuilder);
		addConceptIdsFilter(queryBuilder);
		addComponentIdFilter(queryBuilder);
		addLocaleFilter(context, queryBuilder, languageRefSetId);
		addLanguageFilter(queryBuilder);
		addEscgFilter(context, queryBuilder, OptionKey.CONCEPT_ESCG, new Function<LongSet, Expression>() {
			@Override
			public Expression apply(LongSet input) {
				return concepts(LongSets.toStringSet(input));
			}
		});
		addEscgFilter(context, queryBuilder, OptionKey.TYPE, new Function<LongSet, Expression>() {
			@Override
			public Expression apply(LongSet input) {
				return types(LongSets.toStringSet(input));
			}
		});
		
		SortBy sortBy = SortBy.NONE;
		
		if (containsKey(OptionKey.TERM)) {
			final String searchTerm = getString(OptionKey.TERM);
			if (!containsKey(OptionKey.USE_FUZZY)) {
				queryBuilder.must(toDescriptionTermQuery(searchTerm));
			} else {
				queryBuilder.must(fuzzy(searchTerm));
			}
			sortBy = SortBy.SCORE;
		}

		final Hits<SnomedDescriptionIndexEntry> hits = searcher.search(Query.select(SnomedDescriptionIndexEntry.class)
				.where(queryBuilder.build())
				.offset(offset)
				.limit(limit)
				.sortBy(sortBy)
				.withScores(containsKey(OptionKey.TERM))
				.build());
		if (limit < 1 || hits.getTotal() < 1) {
			return new SnomedDescriptions(offset, limit, hits.getTotal());
		} else {
			return SnomedConverters.newDescriptionConverter(context, expand(), locales()).convert(hits.getHits(), offset, limit, hits.getTotal());
		}
	}
	
	private Expression toDescriptionTermQuery(final String searchTerm) {
		final ExpressionBuilder qb = Expressions.builder();
		qb.should(createTermDisjunctionQuery(searchTerm));
		
		if (containsKey(OptionKey.PARSED_TERM)) {
			qb.should(parsedTerm(searchTerm));
		}
		
		if (isComponentId(searchTerm, ComponentCategory.DESCRIPTION)) {
			qb.should(Expressions.boost(id(searchTerm), 1000f));
		}
		
		return qb.build();
	}
	
	private boolean isComponentId(String value, ComponentCategory type) {
		try {
			return SnomedIdentifiers.getComponentCategory(value) == type;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private Expression createTermDisjunctionQuery(final String searchTerm) {
		final List<Expression> disjuncts = newArrayList();
		disjuncts.add(exactTerm(searchTerm));
		disjuncts.add(allTermsPresent(searchTerm));
		disjuncts.add(allTermPrefixesPresent(searchTerm));
		return Expressions.dismax(disjuncts);
	}

	private void addConceptIdsFilter(ExpressionBuilder queryBuilder) {
		if (containsKey(OptionKey.CONCEPT_ID)) {
			queryBuilder.must(concepts(getCollection(OptionKey.CONCEPT_ID, String.class)));
		}
	}

	private void addEscgFilter(BranchContext context, final ExpressionBuilder queryBuilder, OptionKey key, Function<LongSet, Expression> expressionProvider) {
		if (containsKey(key)) {
			try {
				final String escg = getString(key);
				final RValue expression = context.service(EscgRewriter.class).parseRewrite(escg);
				final LongSet conceptIds = new ConceptIdQueryEvaluator2(context.service(RevisionSearcher.class)).evaluate(expression);
				final Expression conceptFilter = expressionProvider.apply(conceptIds);
				queryBuilder.must(conceptFilter);
			} catch (SyntaxErrorException e) {
				throw new IllegalQueryParameterException(e.getMessage());
			}
		}
	}
	
	private void addLanguageFilter(ExpressionBuilder queryBuilder) {
		if (containsKey(OptionKey.LANGUAGE)) {
			queryBuilder.must(languageCodes(getCollection(OptionKey.LANGUAGE, String.class)));
		}
	}

	private void addLocaleFilter(BranchContext context, ExpressionBuilder queryBuilder, String positiveRefSetId) {
		for (String languageRefSetId : languageRefSetIds()) {
			final Expression acceptabilityFilter; 
			if (containsKey(OptionKey.ACCEPTABILITY)) {
				acceptabilityFilter = Acceptability.PREFERRED.equals(get(OptionKey.ACCEPTABILITY, Acceptability.class)) ?
						preferredIn(languageRefSetId) :
							SnomedDescriptionIndexEntry.Expressions.acceptableIn(languageRefSetId);
			} else {
				acceptabilityFilter = Expressions.builder()
						.should(preferredIn(languageRefSetId))
						.should(acceptableIn(languageRefSetId))
						.build();
			}

			if (languageRefSetId.equals(positiveRefSetId)) {
				queryBuilder.must(acceptabilityFilter);
				break;
			} else {
				queryBuilder.mustNot(acceptabilityFilter);
			}
		}
	}

	@Override
	protected Class<SnomedDescriptions> getReturnType() {
		return SnomedDescriptions.class;
	}
	
}
