/*
 * Copyright 2018-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.index.taxonomy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import com.b2international.collections.longs.LongKeyMap;
import com.b2international.collections.longs.LongList;
import com.b2international.collections.longs.LongSet;
import com.b2international.snowowl.snomed.datastore.ConcreteDomainFragment;
import com.b2international.snowowl.snomed.datastore.StatementFragment;

/**
 * Represents a snapshot of the ontology for reasoner input and normal form generation.
 * 
 * @since 7.0
 */
public final class ReasonerTaxonomy implements IReasonerTaxonomy {

	private final InternalIdMap conceptMap;
	private final LongKeyMap<String> fullySpecifiedNames;

	private final InternalIdEdges statedAncestors;
	private final InternalIdEdges statedDescendants;

	private final InternalSctIdSet definingConcepts;
	private final InternalSctIdSet exhaustiveConcepts;

	private final InternalIdMultimap<StatementFragment> statedRelationships;
	private final InternalIdMultimap<StatementFragment> axiomNonIsARelationships;
	private final InternalIdMultimap<StatementFragment> existingInferredRelationships;
	private final InternalIdMultimap<StatementFragment> additionalGroupedRelationships;
	
	private final InternalIdMultimap<String> axioms;
	private final LongSet neverGroupedTypeIds;
	private final Set<PropertyChain> propertyChains;
	
	private final InternalIdMultimap<ConcreteDomainFragment> statedConcreteDomainMembers;
	private final InternalIdMultimap<ConcreteDomainFragment> inferredConcreteDomainMembers;
	private final InternalIdMultimap<ConcreteDomainFragment> additionalGroupedConcreteDomainMembers;

	private final InternalIdEdges inferredAncestors;
	private final InternalSctIdSet unsatisfiableConcepts;
	private final InternalSctIdMultimap equivalentConcepts;
	private final LongList iterationOrder;

	/*package*/ ReasonerTaxonomy(
			final InternalIdMap conceptMap, 
			final LongKeyMap<String> fullySpecifiedNames,
			
			final InternalIdEdges statedAncestors,
			final InternalIdEdges statedDescendants, 
			
			final InternalSctIdSet definingConcepts, 
			final InternalSctIdSet exhaustiveConcepts, 
			
			final InternalIdMultimap<StatementFragment> statedRelationships,
			final InternalIdMultimap<StatementFragment> axiomNonIsARelationships,
			final InternalIdMultimap<StatementFragment> existingInferredRelationships,
			final InternalIdMultimap<StatementFragment> additionalGroupedRelationships, 
			
			final InternalIdMultimap<String> axioms,
			final LongSet neverGroupedTypeIds,
			final Set<PropertyChain> propertyChains, 
			
			final InternalIdMultimap<ConcreteDomainFragment> statedConcreteDomainMembers,
			final InternalIdMultimap<ConcreteDomainFragment> inferredConcreteDomainMembers,
			final InternalIdMultimap<ConcreteDomainFragment> additionalGroupedConcreteDomainMembers, 
			
			final InternalIdEdges inferredAncestors,
			final InternalSctIdSet unsatisfiableConcepts,
			final InternalSctIdMultimap equivalentConcepts, 
			final LongList iterationOrder) {

		this.conceptMap = conceptMap;
		this.fullySpecifiedNames = fullySpecifiedNames;
		
		this.statedAncestors = statedAncestors;
		this.statedDescendants = statedDescendants;

		this.definingConcepts = definingConcepts;
		this.exhaustiveConcepts = exhaustiveConcepts;
		
		this.statedRelationships = statedRelationships;
		this.axiomNonIsARelationships = axiomNonIsARelationships;
		this.existingInferredRelationships = existingInferredRelationships;
		this.additionalGroupedRelationships = additionalGroupedRelationships;
		
		this.axioms = axioms;
		this.neverGroupedTypeIds = neverGroupedTypeIds;
		this.propertyChains = propertyChains;
		
		this.statedConcreteDomainMembers = statedConcreteDomainMembers;
		this.inferredConcreteDomainMembers = inferredConcreteDomainMembers;
		this.additionalGroupedConcreteDomainMembers = additionalGroupedConcreteDomainMembers;
		
		this.inferredAncestors = inferredAncestors;
		this.unsatisfiableConcepts = unsatisfiableConcepts;
		this.equivalentConcepts = equivalentConcepts;
		this.iterationOrder = iterationOrder;
	}

	public InternalIdMap getConceptMap() {
		return conceptMap;
	}

	public LongKeyMap<String> getFullySpecifiedNames() {
		return fullySpecifiedNames;
	}

	public InternalIdEdges getStatedAncestors() {
		return statedAncestors;
	}

	public InternalIdEdges getStatedDescendants() {
		return statedDescendants;
	}

	public InternalIdEdges getInferredAncestors() {
		return checkNotNull(inferredAncestors, "Inferred ancestors are unset on this taxonomy.");
	}

	@Override
	public InternalSctIdSet getUnsatisfiableConcepts() {
		return checkNotNull(unsatisfiableConcepts, "Unsatisfiable concept IDs are unset on this taxonomy.");
	}

	@Override
	public InternalSctIdMultimap getEquivalentConcepts() {
		return checkNotNull(equivalentConcepts, "Inferred equivalences are unset on this taxonomy.");
	}
	
	public InternalSctIdSet getDefiningConcepts() {
		return definingConcepts;
	}

	public InternalSctIdSet getExhaustiveConcepts() {
		return exhaustiveConcepts;
	}

	public InternalIdMultimap<StatementFragment> getStatedRelationships() {
		return statedRelationships;
	}
	
	public InternalIdMultimap<StatementFragment> getAxiomNonIsARelationships() {
		return axiomNonIsARelationships;
	}
	
	public InternalIdMultimap<StatementFragment> getExistingInferredRelationships() {
		return existingInferredRelationships;
	}
	
	public InternalIdMultimap<StatementFragment> getAdditionalGroupedRelationships() {
		return additionalGroupedRelationships;
	}

	public InternalIdMultimap<String> getAxioms() {
		return axioms;
	}
	
	public LongSet getNeverGroupedTypeIds() {
		return neverGroupedTypeIds;
	}
	
	public Set<PropertyChain> getPropertyChains() {
		return propertyChains;
	}

	public InternalIdMultimap<ConcreteDomainFragment> getStatedConcreteDomainMembers() {
		return statedConcreteDomainMembers;
	}
	
	public InternalIdMultimap<ConcreteDomainFragment> getInferredConcreteDomainMembers() {
		return inferredConcreteDomainMembers;
	}
	
	public InternalIdMultimap<ConcreteDomainFragment> getAdditionalGroupedConcreteDomainMembers() {
		return additionalGroupedConcreteDomainMembers;
	}
	
	public LongList getIterationOrder() {
		return iterationOrder;
	}

	public ReasonerTaxonomy withInferences(final InternalIdEdges newInferredAncestors, 
			final InternalSctIdSet newUnsatisfiableConcepts,
			final InternalSctIdMultimap newEquivalentConcepts, 
			final LongList iterationOrder) {

		checkNotNull(newInferredAncestors, "Inferred ancestors may not be null.");
		checkNotNull(newUnsatisfiableConcepts, "Inferred unsatisfiable concepts may not be null.");
		checkNotNull(newEquivalentConcepts, "Inferred equivalent concept sets may not be null.");
		checkNotNull(iterationOrder, "Inferred concept iteration order may not be null.");

		checkState(this.inferredAncestors == null, "Inferred ancestors are already present in this taxonomy.");
		checkState(this.unsatisfiableConcepts == null, "Inferred unsatisfiable concepts are already present in this taxonomy.");
		checkState(this.equivalentConcepts == null, "Inferred equivalent concept sets are already present in this taxonomy.");
		checkState(this.iterationOrder == null, "Inferred concept iteration order is already set in this taxonomy.");

		return new ReasonerTaxonomy(conceptMap, 
				fullySpecifiedNames,
				
				statedAncestors, 
				statedDescendants, 

				definingConcepts,
				exhaustiveConcepts,
				
				statedRelationships,
				axiomNonIsARelationships,
				existingInferredRelationships,
				additionalGroupedRelationships,
				
				axioms,
				neverGroupedTypeIds,
				propertyChains,
				
				statedConcreteDomainMembers, 
				inferredConcreteDomainMembers,
				additionalGroupedConcreteDomainMembers,
				
				newInferredAncestors, 
				newUnsatisfiableConcepts,
				newEquivalentConcepts,
				iterationOrder);
	}
}
