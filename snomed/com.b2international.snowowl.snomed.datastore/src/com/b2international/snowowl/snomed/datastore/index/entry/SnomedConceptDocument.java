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
import static com.b2international.index.query.Expressions.match;
import static com.b2international.index.query.Expressions.matchAny;
import static com.b2international.index.query.Expressions.matchAnyInt;
import static com.b2international.index.query.Expressions.matchAnyLong;
import static com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants.getValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.cdo.common.id.CDOIDUtil;

import com.b2international.collections.PrimitiveSets;
import com.b2international.collections.longs.LongSet;
import com.b2international.commons.StringUtils;
import com.b2international.commons.collections.Collections3;
import com.b2international.commons.functions.StringToLongFunction;
import com.b2international.index.Doc;
import com.b2international.index.query.Expression;
import com.b2international.snowowl.core.CoreTerminologyBroker;
import com.b2international.snowowl.core.api.ITreeComponent;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.datastore.cdo.CDOUtils;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSet;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetUtil;
import com.b2international.snowowl.snomed.snomedrefset.SnomedMappingRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Function;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.FluentIterable;

/**
 * A transfer object representing a SNOMED CT concept.
 */
@Doc
@JsonDeserialize(builder=SnomedConceptDocument.Builder.class)
public class SnomedConceptDocument extends SnomedComponentDocument implements ITreeComponent {

	public static final Long ROOT_ID = -1L;
	public static final float DEFAULT_DOI = 1.0f;
	private static final long serialVersionUID = -824286402410205210L;

	public static Builder builder() {
		return new Builder();
	}
	
	public final static class Expressions extends SnomedComponentDocument.Expressions {
		
		private Expressions() {
		}

		public static Expression parents(Collection<String> parentIds) {
			return matchAnyLong(Fields.PARENTS, StringToLongFunction.copyOf(parentIds));
		}

		public static Expression ancestors(Collection<String> ancestorIds) {
			return matchAnyLong(Fields.ANCESTORS, StringToLongFunction.copyOf(ancestorIds));
		}

		public static Expression statedParents(Collection<String> statedParentIds) {
			return matchAnyLong(Fields.STATED_PARENTS, StringToLongFunction.copyOf(statedParentIds));
		}
		
		public static Expression statedAncestors(Collection<String> statedAncestorIds) {
			return matchAnyLong(Fields.STATED_ANCESTORS, StringToLongFunction.copyOf(statedAncestorIds));
		}
		
		public static Expression primitive() {
			return match(Fields.PRIMITIVE, true);
		}
		
		public static Expression defining() {
			return match(Fields.PRIMITIVE, false);
		}
		
		public static Expression exhaustive() {
			return match(Fields.EXHAUSTIVE, true);
		}
		
		public static Expression refSetStorageKey(long storageKey) {
			return exactMatch(Fields.REFSET_STORAGEKEY, storageKey);
		}
		
		public static Expression refSetType(SnomedRefSetType type) {
			return refSetTypes(Collections.singleton(type));
		}
		
		public static Expression refSetTypes(Collection<SnomedRefSetType> types) {
			return matchAny(Fields.REFSET_TYPE, FluentIterable.from(types).transform(new Function<SnomedRefSetType, String>() {
				@Override
				public String apply(SnomedRefSetType input) {
					return input.name();
				}
			}).toSet());
		}
		
		public static Expression referencedComponentType(int referencedComponentType) {
			return match(Fields.REFERENCED_COMPONENT_TYPE, referencedComponentType);
		}
		
		public static Expression referencedComponentTypes(Collection<Integer> referencedComponentTypes) {
			return matchAnyInt(Fields.REFERENCED_COMPONENT_TYPE, referencedComponentTypes);
		}
		
		public static Expression mapTargetComponentType(int mapTargetComponentType) {
			return match(Fields.MAP_TARGET_COMPONENT_TYPE, mapTargetComponentType);
		}
		
		public static Expression mapTargetComponentTypes(Collection<Integer> mapTargetComponentTypes) {
			return matchAnyInt(Fields.MAP_TARGET_COMPONENT_TYPE, mapTargetComponentTypes);
		}
		
		public static Expression structuralRefSet() {
			return match(Fields.STRUCTURAL, true);
		}
		
		public static Expression regularRefSet() {
			return match(Fields.STRUCTURAL, false);
		}
		
		public static Expression referringRefSet(String referringRefSet) {
			return exactMatch(Fields.REFERRING_REFSETS, referringRefSet);
		}
		
		public static Expression referringMappingRefSet(String referringMappingRefSet) {
			return exactMatch(Fields.REFERRING_MAPPING_REFSETS, referringMappingRefSet);
		}
		
		public static Expression referringPredicate(String referringPredicate) {
			return exactMatch(Fields.REFERRING_PREDICATES, referringPredicate);
		}
		
	}

	public static class Fields extends SnomedComponentDocument.Fields {
		public static final String REFSET_STORAGEKEY = "refSetStorageKey";
		public static final String REFERRING_PREDICATES = "referringPredicates";
		public static final String PRIMITIVE = "primitive";
		public static final String EXHAUSTIVE = "exhaustive";
		public static final String ANCESTORS = "ancestors";
		public static final String STATED_ANCESTORS = "statedAncestors";
		public static final String PARENTS = "parents";
		public static final String STATED_PARENTS = "statedParents";
		public static final String PREDICATES = "predicates";
		public static final String REFSET_TYPE = "refSetType";
		public static final String REFERENCED_COMPONENT_TYPE = "referencedComponentType";
		public static final String MAP_TARGET_COMPONENT_TYPE = "mapTargetComponentType";
		public static final String STRUCTURAL = "structural";
		public static final String REFERRING_REFSETS = "referringRefSets";
		public static final String REFERRING_MAPPING_REFSETS = "referringMappingRefSets";
		public static final String DOI = "doi";
	}
	
	public static Builder builder(final SnomedConceptDocument input) {
		return builder()
				.storageKey(input.getStorageKey())
				.id(input.getId())
//				.score(input.getScore())
				.moduleId(input.getModuleId())
				.active(input.isActive())
				.released(input.isReleased())
				.effectiveTime(input.getEffectiveTime())
				.iconId(input.getIconId())
				.primitive(input.isPrimitive())
				.exhaustive(input.isExhaustive())
				.parents(input.getParents())
				.ancestors(input.getAncestors())
				.statedParents(input.getStatedParents())
				.statedAncestors(input.getStatedAncestors())
				.refSetStorageKey(input.getRefSetStorageKey())
				.referencedComponentType(input.getReferencedComponentType())
				.mapTargetComponentType(input.getMapTargetComponentType())
				.refSetType(input.getRefSetType())
				.structural(input.isStructural())
				.doi(input.getDoi());
	}
	
	public static Builder builder(ISnomedConcept input) {
		final Builder builder = builder()
				.storageKey(input.getStorageKey())
				.id(input.getId())
				.moduleId(input.getModuleId())
				.active(input.isActive())
				.released(input.isReleased())
				.effectiveTime(EffectiveTimes.getEffectiveTime(input.getEffectiveTime()))
				.iconId(input.getIconId())
				.primitive(input.getDefinitionStatus().isPrimitive())
				.exhaustive(input.getSubclassDefinitionStatus().isExhaustive())
				.parents(PrimitiveSets.newLongOpenHashSet(input.getParentIds()))
				.ancestors(PrimitiveSets.newLongOpenHashSet(input.getAncestorIds()))
				.statedParents(PrimitiveSets.newLongOpenHashSet(input.getStatedParentIds()))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(input.getStatedAncestorIds()));
		
//		if (input.getScore() != null) {
//			builder.score(input.getScore());
//		}
		
		return builder;
	}
	
	public static List<SnomedConceptDocument> fromConcepts(Iterable<ISnomedConcept> concepts) {
		return FluentIterable.from(concepts).transform(new Function<ISnomedConcept, SnomedConceptDocument>() {
			@Override
			public SnomedConceptDocument apply(ISnomedConcept input) {
				final ISnomedDescription pt = input.getPt();
				final String preferredTerm = pt == null ? input.getId() : pt.getTerm();
				return SnomedConceptDocument.builder(input).label(preferredTerm).build();
			}
		}).toList();
	}

	@JsonPOJOBuilder(withPrefix="")
	public static class Builder extends SnomedComponentDocumentBuilder<Builder> {

		private boolean primitive;
		private boolean exhaustive;
		private LongSet parents;
		private LongSet ancestors;
		private LongSet statedParents;
		private LongSet statedAncestors;
		private SnomedRefSetType refSetType;
		private int referencedComponentType;
		private int mapTargetComponentType;
		private float doi = DEFAULT_DOI;
		private Collection<String> referringRefSets;
		private Collection<String> referringMappingRefSets;
		private long refSetStorageKey = CDOUtils.NO_STORAGE_KEY;
		private boolean structural = false;

		@JsonCreator
		private Builder() {
			// Disallow instantiation outside static method
		}
		
		@Override
		protected Builder getSelf() {
			return this;
		}
		
		public Builder primitive(final boolean primitive) {
			this.primitive = primitive;
			return getSelf();
		}

		public Builder exhaustive(final boolean exhaustive) {
			this.exhaustive = exhaustive;
			return getSelf();
		}
		
		public Builder parents(final LongSet parents) {
			this.parents = parents;
			return getSelf();
		}
		
		public Builder statedParents(final LongSet statedParents) {
			this.statedParents = statedParents;
			return getSelf();
		}
		
		public Builder ancestors(final LongSet ancestors) {
			this.ancestors = ancestors;
			return getSelf();
		}
		
		public Builder statedAncestors(final LongSet statedAncestors) {
			this.statedAncestors = statedAncestors;
			return getSelf();
		}
		
		@JsonIgnore
		public Builder refSet(final SnomedRefSet refSet) {
			if (refSet instanceof SnomedMappingRefSet) {
				mapTargetComponentType(((SnomedMappingRefSet) refSet).getMapTargetComponentType());
			}
			return structural(SnomedRefSetUtil.isStructural(refSet.getIdentifierId(), refSet.getType()))
					.refSetType(refSet.getType())
					.referencedComponentType(refSet.getReferencedComponentType())
					.refSetStorageKey(CDOIDUtil.getLong(refSet.cdoID()));
		}
		
		@JsonIgnore
		public Builder refSet(final SnomedReferenceSet refSet) {
			if (!StringUtils.isEmpty(refSet.getMapTargetComponentType())) {
				final int componentType = CoreTerminologyBroker.getInstance()
						.getTerminologyComponentIdAsInt(refSet.getMapTargetComponentType());
				mapTargetComponentType(componentType);
			}
			
			return structural(SnomedRefSetUtil.isStructural(refSet.getId(), refSet.getType()))
					.refSetType(refSet.getType())
					.referencedComponentType(getValue(refSet.getReferencedComponentType()))
					.refSetStorageKey(refSet.getStorageKey());
		}
		
		Builder mapTargetComponentType(int mapTargetComponentType) {
			this.mapTargetComponentType = mapTargetComponentType;
			return getSelf();
		}
		
		Builder refSetStorageKey(long refSetStorageKey) {
			this.refSetStorageKey = refSetStorageKey;
			return getSelf();
		}

		Builder referencedComponentType(int referencedComponentType) {
			this.referencedComponentType = referencedComponentType;
			return getSelf();
		}

		Builder refSetType(SnomedRefSetType refSetType) {
			this.refSetType = refSetType;
			return getSelf();
		}

		Builder structural(boolean structural) {
			this.structural = structural;
			return getSelf();
		}
		
		public Builder doi(float doi) {
			this.doi = doi;
			return getSelf();
		}
		
		public Builder referringRefSets(Collection<String> referringRefSets) {
			this.referringRefSets = referringRefSets;
			return getSelf();
		}
		
		public Builder referringMappingRefSets(Collection<String> referringMappingRefSets) {
			this.referringMappingRefSets = referringMappingRefSets;
			return getSelf();
		}
		
		public SnomedConceptDocument build() {
			final SnomedConceptDocument entry = new SnomedConceptDocument(id,
					label,
					iconId, 
					moduleId, 
					released, 
					active, 
					effectiveTime, 
					namespace,
					primitive, 
					exhaustive,
					refSetType, 
					referencedComponentType,
					mapTargetComponentType,
					refSetStorageKey,
					structural);
			
			entry.doi = doi;
			entry.setScore(score);
			entry.setBranchPath(branchPath);
			entry.setCommitTimestamp(commitTimestamp);
			entry.setStorageKey(storageKey);
			entry.setReplacedIns(replacedIns);
			entry.setSegmentId(segmentId);
			
			if (parents != null) {
				entry.parents = parents;
			}
			
			if (statedParents != null) {
				entry.statedParents = statedParents;
			}
			
			if (ancestors != null) {
				entry.ancestors = ancestors;
			}
			
			if (statedAncestors != null) {
				entry.statedAncestors = statedAncestors;
			}
			
			entry.referringRefSets = Collections3.toImmutableSet(referringRefSets);
			entry.referringMappingRefSets = Collections3.toImmutableSet(referringMappingRefSets);
			
			return entry;
		}

	}

	private final boolean primitive;
	private final boolean exhaustive;
	private final SnomedRefSetType refSetType;
	private final int referencedComponentType;
	private final int mapTargetComponentType;
	private final boolean structural;
	private final long refSetStorageKey;
	
	private LongSet parents;
	private LongSet ancestors;
	private LongSet statedParents;
	private LongSet statedAncestors;
	private float doi;
	private Collection<String> referringRefSets;
	private Collection<String> referringMappingRefSets;

	protected SnomedConceptDocument(final String id,
			final String label,
			final String iconId, 
			final String moduleId,
			final boolean released,
			final boolean active,
			final long effectiveTime,
			final String namespace,
			final boolean primitive,
			final boolean exhaustive, 
			final SnomedRefSetType refSetType, 
			final int referencedComponentType,
			final int mapTargetComponentType,
			final long refSetStorageKey,
			final boolean structural) {

		super(id, label, iconId, moduleId, released, active, effectiveTime, namespace);
		this.primitive = primitive;
		this.exhaustive = exhaustive;
		this.refSetType = refSetType;
		this.referencedComponentType = referencedComponentType;
		this.mapTargetComponentType = mapTargetComponentType;
		this.refSetStorageKey = refSetStorageKey;
		this.structural = structural;
	}
	
	@Override
	public String getContainerId() {
		return getId();
	}
	
	@Override
	public boolean isRoot() {
		return true;
	}
	
	public long getRefSetStorageKey() {
		return refSetStorageKey;
	}
	
	public float getDoi() {
		return doi;
	}
	
	public Collection<String> getReferringRefSets() {
		return referringRefSets;
	}
	
	public Collection<String> getReferringMappingRefSets() {
		return referringMappingRefSets;
	}
	
	/**
	 * @return {@code true} if the concept definition status is 900000000000074008 (primitive), {@code false} otherwise
	 */
	public boolean isPrimitive() {
		return primitive;
	}

	/**
	 * @return {@code true} if the concept subclass definition status is exhaustive, {@code false} otherwise
	 */
	public boolean isExhaustive() {
		return exhaustive;
	}
	
	@Override
	public LongSet getParents() {
		return parents;
	}
	
	public LongSet getStatedParents() {
		return statedParents;
	}
	
	@Override
	public LongSet getAncestors() {
		return ancestors;
	}
	
	public LongSet getStatedAncestors() {
		return statedAncestors;
	}
	
	public SnomedRefSetType getRefSetType() {
		return refSetType;
	}
	
	public int getReferencedComponentType() {
		return referencedComponentType;
	}
	
	public int getMapTargetComponentType() {
		return mapTargetComponentType;
	}
	
	public boolean isStructural() {
		return structural;
	}
	
	@Override
	protected ToStringHelper doToString() {
		return super.doToString()
				.add("primitive", primitive)
				.add("exhaustive", exhaustive)
				.add("refSetType", refSetType)
				.add("referencedComponentType", referencedComponentType)
				.add("mapTargetComponentType", mapTargetComponentType)
				.add("structural", structural)
				.add("refSetStorageKey", refSetStorageKey)
				.add("parents", parents)
				.add("ancestors", ancestors)
				.add("statedParents", statedParents)
				.add("statedAncestors", statedAncestors)
				.add("doi", doi)
				.add("referringRefSets", referringRefSets)
				.add("referringMappingRefSets", referringMappingRefSets);
	}

}

