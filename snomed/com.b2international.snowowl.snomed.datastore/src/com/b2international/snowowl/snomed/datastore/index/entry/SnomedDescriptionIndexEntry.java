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
package com.b2international.snowowl.snomed.datastore.index.entry;

import static com.b2international.index.query.Expressions.exactMatch;
import static com.b2international.index.query.Expressions.matchAny;
import static com.b2international.index.query.Expressions.matchTextAll;
import static com.b2international.index.query.Expressions.matchTextAllPrefix;
import static com.b2international.index.query.Expressions.matchTextFuzzy;
import static com.b2international.index.query.Expressions.matchTextParsed;
import static com.b2international.index.query.Expressions.matchTextPhrase;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.b2international.index.Analyzed;
import com.b2international.index.Analyzers;
import com.b2international.index.Doc;
import com.b2international.index.compat.TextConstants;
import com.b2international.index.query.Expression;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.datastore.cdo.CDOIDUtils;
import com.b2international.snowowl.snomed.Description;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A transfer object representing a SNOMED CT description.
 */
@Doc
@JsonDeserialize(builder = SnomedDescriptionIndexEntry.Builder.class)
public final class SnomedDescriptionIndexEntry extends SnomedDocument {

	private static final long serialVersionUID = 301681633674309020L;

	public static Builder builder() {
		return new Builder();
	}
	
	public static Builder builder(final ISnomedDescription input) {
		final Builder builder = builder()
				.storageKey(input.getStorageKey())
				.id(input.getId())
				.term(input.getTerm()) 
				.moduleId(input.getModuleId())
				.languageCode(input.getLanguageCode())
				.released(input.isReleased())
				.active(input.isActive())
				.typeId(input.getTypeId())
				.conceptId(input.getConceptId())
				.caseSignificanceId(input.getCaseSignificance().getConceptId())
				.effectiveTime(EffectiveTimes.getEffectiveTime(input.getEffectiveTime()));
		
		// TODO add back scoring
//		if (input.getScore() != null) {
//			builder.score(input.getScore());
//		}
		
		if (input.getType() != null && input.getType().getPt() != null) {
			builder.typeLabel(input.getType().getPt().getTerm());
		}
		
		for (final String refSetId : input.getAcceptabilityMap().keySet()) {
			builder.acceptability(refSetId, input.getAcceptabilityMap().get(refSetId));
		}
	
		return builder;
	}
	
	public static Builder builder(Description description) {
		return builder()
				.storageKey(CDOIDUtils.asLong(description.cdoID()))
				.id(description.getId()) 
				.term(description.getTerm())
				.moduleId(description.getModule().getId())
				.released(description.isReleased()) 
				.active(description.isActive()) 
				.typeId(description.getType().getId()) 
				.caseSignificanceId(description.getCaseSignificance().getId()) 
				.conceptId(description.getConcept().getId())
				.languageCode(description.getLanguageCode())
				.effectiveTime(description.isSetEffectiveTime() ? description.getEffectiveTime().getTime() : EffectiveTimes.UNSET_EFFECTIVE_TIME);
	}
	
	/**
	 * Creates a new {@link Builder} from the given {@link SnomedDescriptionIndexEntry}. The acceptability map is not copied over to the
	 * {@link Builder} instance, if you need that, manually modify the returned {@link Builder} to represent the desired acceptability state.
	 * 
	 * @param doc
	 * @return
	 */
	public static Builder builder(SnomedDescriptionIndexEntry doc) {
		return builder()
				.storageKey(doc.getStorageKey())
				.id(doc.getId())
				.term(doc.getTerm())
				.moduleId(doc.getModuleId())
				.released(doc.isReleased())
				.active(doc.isActive())
				.typeId(doc.getTypeId())
				.caseSignificanceId(doc.getCaseSignificanceId())
				.conceptId(doc.getConceptId())
				.languageCode(doc.getLanguageCode())
				.effectiveTime(doc.getEffectiveTime());
	}
	
	public static List<SnomedDescriptionIndexEntry> fromDescriptions(Iterable<ISnomedDescription> descriptions) {
		return FluentIterable.from(descriptions).transform(new Function<ISnomedDescription, SnomedDescriptionIndexEntry>() {
			@Override
			public SnomedDescriptionIndexEntry apply(ISnomedDescription input) {
				return builder(input).build();
			}
		}).toList();
	}

	public final static class Fields extends SnomedDocument.Fields {
		public static final String CONCEPT_ID = SnomedRf2Headers.FIELD_CONCEPT_ID;
		public static final String TYPE_ID = SnomedRf2Headers.FIELD_TYPE_ID;
		public static final String CASE_SIGNIFICANCE_ID = SnomedRf2Headers.FIELD_CASE_SIGNIFICANCE_ID;
		public static final String TERM = SnomedRf2Headers.FIELD_TERM;
		public static final String LANGUAGE_CODE = SnomedRf2Headers.FIELD_LANGUAGE_CODE;
		public static final String PREFERRED_IN = "preferredIn";
		public static final String ACCEPTABLE_IN = "acceptableIn";
	}
	
	public final static class Expressions extends SnomedDocument.Expressions {
		
		private Expressions() {
		}

		public static Expression fuzzy(String term) {
			final Splitter tokenSplitter = Splitter.on(TextConstants.WHITESPACE_OR_DELIMITER_MATCHER).omitEmptyStrings();
			final ExpressionBuilder fuzzyQuery = com.b2international.index.query.Expressions.builder();
			int tokenCount = 0;

			for (final String token : tokenSplitter.split(term)) {
				fuzzyQuery.should(matchTextFuzzy(Fields.TERM, token));
				++tokenCount;
			}

			final int minShouldMatch = Math.max(1, tokenCount - 2);
			fuzzyQuery.setMinimumNumberShouldMatch(minShouldMatch);
			
			return fuzzyQuery.build();
		}
		
		public static Expression exactTerm(String term) {
			return matchTextPhrase(Fields.TERM, term);
		}
		
		public static Expression allTermPrefixesPresent(String term) {
			return matchTextAllPrefix(Fields.TERM, term);
		}
		
		public static Expression allTermsPresent(String term) {
			return matchTextAll(Fields.TERM, term, Analyzers.NON_BOOKEND);
		}
		
		public static Expression parsedTerm(String term) {
			return matchTextParsed(Fields.TERM, term);
		}
		
		public static Expression concept(String conceptId) {
			return concepts(Collections.singleton(conceptId));
		}
		
		public static Expression concepts(Collection<String> conceptIds) {
			return matchAny(Fields.CONCEPT_ID, conceptIds);
		}
		
		public static Expression type(String typeId) {
			return types(Collections.singleton(typeId));
		}
		
		public static Expression types(Collection<String> typeIds) {
			return matchAny(Fields.TYPE_ID, typeIds);
		}
		
		public static Expression caseSignificance(String caseSignificanceId) {
			return caseSignificances(Collections.singleton(caseSignificanceId));
		}
		
		public static Expression caseSignificances(Collection<String> caseSignificanceIds) {
			return matchAny(Fields.CASE_SIGNIFICANCE_ID, caseSignificanceIds);
		}
		
		public static Expression acceptableIn(String languageReferenceSetId) {
			return acceptableIn(Collections.singleton(languageReferenceSetId));
		}
		
		public static Expression preferredIn(String languageReferenceSetId) {
			return preferredIn(Collections.singleton(languageReferenceSetId));
		}
		
		public static Expression acceptableIn(Collection<String> languageReferenceSetIds) {
			return matchAny(Fields.ACCEPTABLE_IN, languageReferenceSetIds);
		}
		
		public static Expression preferredIn(Collection<String> languageReferenceSetIds) {
			return matchAny(Fields.PREFERRED_IN, languageReferenceSetIds);
		}
		
		public static Expression languageCode(String languageCode) {
			return exactMatch(Fields.LANGUAGE_CODE, languageCode);
		}
		
		public static Expression languageCodes(Collection<String> languageCodes) {
			return matchAny(Fields.LANGUAGE_CODE, languageCodes);
		}
		
	}
	
	@JsonPOJOBuilder(withPrefix="")
	public static class Builder extends SnomedDocumentBuilder<Builder> {

		private String term;
		private String conceptId;
		private String languageCode;
		private String typeId;
		private String typeLabel;
		private String caseSignificanceId;
		private Set<String> acceptableIn = newHashSet();
		private Set<String> preferredIn = newHashSet();

		@JsonCreator
		private Builder() {
			// Disallow instantiation outside static method
		}
		
		@Override
		protected Builder getSelf() {
			return this;
		}

		public Builder term(final String term) {
			this.term = term;
			return getSelf();
		}

		public Builder conceptId(final String conceptId) {
			this.conceptId = conceptId;
			return getSelf();
		}

		public Builder languageCode(final String languageCode) {
			this.languageCode = languageCode;
			return getSelf();
		}

		public Builder typeId(final String typeId) {
			this.typeId = typeId;
			return getSelf();
		}
		
		public Builder typeLabel(final String typeLabel) {
			this.typeLabel = typeLabel;
			return getSelf();
		}

		public Builder caseSignificanceId(final String caseSignificanceId) {
			this.caseSignificanceId = caseSignificanceId;
			return getSelf();
		}
		
		public Builder acceptableIn(final Set<String> acceptableIn) {
			this.acceptableIn = acceptableIn;
			return getSelf();
		}
		
		public Builder preferredIn(final Set<String> preferredIn) {
			this.preferredIn = preferredIn;
			return getSelf();
		}
		
		public Builder acceptability(final String languageRefSetId, final Acceptability acceptability) {
			switch (acceptability) {
			case ACCEPTABLE:
				this.acceptableIn.add(languageRefSetId);
				break;
			case PREFERRED:
				this.preferredIn.add(languageRefSetId);
				break;
			default: throw new UnsupportedOperationException("Not implemented: " + acceptability);
			}
			return getSelf();
		}
		
		public Builder acceptabilityMap(final Map<String, Acceptability> acceptabilityMap) {
			for (Entry<String, Acceptability> entry : acceptabilityMap.entrySet()) {
				acceptability(entry.getKey(), entry.getValue());
			}
			return getSelf();
		}
		
		@Override
		public Builder label(String label) {
			throw new IllegalStateException("Use term() builder method instead to set the label property");
		}

		public SnomedDescriptionIndexEntry build() {
			final SnomedDescriptionIndexEntry doc = new SnomedDescriptionIndexEntry(id,
					term,
					moduleId,
					released, 
					active, 
					effectiveTime, 
					conceptId, 
					languageCode,
					term,
					typeId,
					typeLabel == null ? typeId : typeLabel,
					caseSignificanceId,
					preferredIn, acceptableIn);
			doc.setScore(score);
			doc.setBranchPath(branchPath);
			doc.setCommitTimestamp(commitTimestamp);
			doc.setStorageKey(storageKey);
			doc.setReplacedIns(replacedIns);
			doc.setSegmentId(segmentId);
			return doc;
		}
	}

	private final String conceptId;
	private final String languageCode;
	
	@Analyzed
	private final String term;
	
	private final String typeId;
	private final String caseSignificanceId;
	private final Set<String> acceptableIn;
	private final Set<String> preferredIn;
	private final String typeLabel;

	private SnomedDescriptionIndexEntry(final String id,
			final String label,
			final String moduleId, 
			final boolean released, 
			final boolean active, 
			final long effectiveTime, 
			final String conceptId,
			final String languageCode,
			final String term,
			final String typeId,
			final String typeLabel,
			final String caseSignificanceId,
			final Set<String> preferredIn, final Set<String> acceptableIn) {

		super(id, label, typeId /* XXX: iconId is the same as typeId*/, moduleId, released, active, effectiveTime);
		this.conceptId = checkNotNull(conceptId, "Description concept identifier may not be null.");
		this.languageCode = checkNotNull(languageCode, "Description language code may not be null.");
		this.term = checkNotNull(term, "Description term may not be null.");
		this.typeId = checkNotNull(typeId, "Description type identifier may not be null.");
		this.typeLabel = typeLabel;
		this.caseSignificanceId = checkNotNull(caseSignificanceId, "Description case significance identifier may not be null.");
		this.preferredIn = preferredIn == null ? Collections.<String>emptySet() : preferredIn;
		this.acceptableIn = acceptableIn == null ? Collections.<String>emptySet() : acceptableIn;
	}
	
	@Override
	public String getContainerId() {
		return getConceptId();
	}
	
	@Override
	@JsonIgnore
	public String getIconId() {
		return super.getIconId();
	}

	/**
	 * @return the parent concept identifier
	 */
	public String getConceptId() {
		return conceptId;
	}

	/**
	 * @return the description Locale (of which only the ISO-639 language code should be populated) 
	 */
	public String getLanguageCode() {
		return languageCode;
	}
	
	/**
	 * @return the description term
	 */
	public String getTerm() {
		return term;
	}

	/**
	 * @return the description type concept identifier
	 */
	public String getTypeId() {
		return typeId;
	}
	
	/**
	 * @return the label of the description type concept
	 */
	@JsonIgnore
	public String getTypeLabel() {
		return typeLabel;
	}

	/**
	 * @return the case significance concept identifier
	 */
	public String getCaseSignificanceId() {
		return caseSignificanceId;
	}
	
	/**
	 * Returns the language reference set identifiers where this description is preferred.
	 * @return
	 */
	public Set<String> getPreferredIn() {
		return preferredIn;
	}
	
	/**
	 * Returns the language reference set identifiers where this description is acceptable.
	 * @return
	 */
	public Set<String> getAcceptableIn() {
		return acceptableIn;
	}
	
	/**
	 * @return the map of active acceptability values for the description, keyed by language reference set identifier
	 */
	@JsonIgnore
	public Map<String, Acceptability> getAcceptabilityMap() {
		// TODO check reindex vm argument
		final Map<String, Acceptability> result = Maps.newHashMap();
		for (String acceptableIn : this.acceptableIn) {
			result.put(acceptableIn, Acceptability.ACCEPTABLE);
		}
		for (String preferredIn : this.preferredIn) {
			result.put(preferredIn, Acceptability.PREFERRED);
		}
		return result;
	}
	
	/**
	 * @return <code>true</code> if this description is a fully specified name, <code>false</code> otherwise.
	 */
	@JsonIgnore
	public boolean isFsn() {
		return Concepts.FULLY_SPECIFIED_NAME.equals(getTypeId());
	}
	
	@Override
	protected ToStringHelper doToString() {
		return super.doToString()
				.add("conceptId", conceptId)
				.add("languageCode", languageCode)
				.add("term", term)
				.add("typeId", typeId)
				.add("caseSignificanceId", caseSignificanceId)
				.add("acceptableIn", acceptableIn)
				.add("preferredIn", preferredIn)
				.add("typeLabel", typeLabel);
	}

}
