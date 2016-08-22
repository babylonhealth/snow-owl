/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.taxonomy;

import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.ecore.EClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.datastore.CDOCommitChangeSet;
import com.b2international.snowowl.datastore.ICDOCommitChangeSet;
import com.b2international.snowowl.datastore.cdo.CDOIDUtils;
import com.b2international.snowowl.snomed.Component;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Relationship;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.SnomedPackage;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.datastore.taxonomy.ISnomedTaxonomyBuilder.TaxonomyBuilderEdge;
import com.b2international.snowowl.snomed.datastore.taxonomy.ISnomedTaxonomyBuilder.TaxonomyBuilderNode;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * @since 4.7
 */
public class SnomedTaxonomyUpdateRunnable implements Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedTaxonomyUpdateRunnable.class);

	private static final Function<CDOObject, EClass> GET_ECLASS_FUNCTION = new Function<CDOObject, EClass>() {
		@Override public EClass apply(CDOObject input) {
			return input.eClass();
		}
	};
	
	private static Function<Component, String> GET_SCT_ID_FUNCTION = new Function<Component, String>() {
		@Override public String apply(final Component component) {
			return Preconditions.checkNotNull(component, "Component argument cannot be null.").getId();
		}
	};

	private final ICDOCommitChangeSet commitChangeSet;
	private final ISnomedTaxonomyBuilder taxonomyBuilder;
	private final String characteristicTypeId;
	private final RevisionSearcher searcher;
	
	private SnomedTaxonomyBuilderResult taxonomyBuilderResult;

	public SnomedTaxonomyUpdateRunnable(RevisionSearcher searcher, CDOTransaction transaction,
			ISnomedTaxonomyBuilder taxonomyBuilder, 
			String characteristicTypeId) {
		
		this(searcher, new CDOCommitChangeSet(transaction, 
						transaction.getSession().getUserID(), 
						transaction.getCommitComment(), 
						transaction.getNewObjects().values(), 
						transaction.getDirtyObjects().values(), 
						Maps.transformValues(transaction.getDetachedObjects(), GET_ECLASS_FUNCTION), 
						transaction.getRevisionDeltas(), 
						-1L),
				taxonomyBuilder,
				characteristicTypeId);
	}
			
	public SnomedTaxonomyUpdateRunnable(RevisionSearcher searcher, 
			ICDOCommitChangeSet commitChangeSet, 
			ISnomedTaxonomyBuilder taxonomyBuilder, 
			String characteristicTypeId) {

		this.searcher = searcher;
		this.commitChangeSet = commitChangeSet;
		this.taxonomyBuilder = taxonomyBuilder;
		this.characteristicTypeId = characteristicTypeId;
	}
	
	@Override 
	public void run() {
		
		LOGGER.info("Processing changes taxonomic information.");
		
		//here we have to consider changes triggered by repository state revert
		//this point the following might happen:
		//SNOMED CT concept and/or relationship will be contained by both deleted and new collections
		//with same business (SCT ID) but different primary ID (CDO ID) [this is the way how we handle object resurrection]
		//we decided, to order changes by primary keys. as primary IDs are provided in sequence, one could assume
		//that the larger primary ID happens later, and that is the truth
		
		//but as deletion always happens later than addition, we only have to take care of deletion
		//so if the deletion is about to erase something that has the same SCT ID but more recent (larger) 
		//primary key, we just ignore it when building the taxonomy.
		
		final Iterable<Concept> newConcepts = commitChangeSet.getNewComponents(Concept.class);
		final Iterable<Concept> dirtyConcepts = commitChangeSet.getDirtyComponents(Concept.class);
		final Iterable<CDOID> deletedConceptStorageKeys = commitChangeSet.getDetachedComponents(SnomedPackage.Literals.CONCEPT);
		final Iterable<Relationship> newRelationships = commitChangeSet.getNewComponents(Relationship.class);
		final Iterable<Relationship> dirtyRelationships = commitChangeSet.getDirtyComponents(Relationship.class);
		final Iterable<CDOID> deletedRelationships = commitChangeSet.getDetachedComponents(SnomedPackage.Literals.RELATIONSHIP);
		
		//SCT ID - relationships
		final Map<String, Relationship> _newRelationships = Maps.newHashMap(Maps.uniqueIndex(newRelationships, GET_SCT_ID_FUNCTION));
		
		//SCT ID - concepts
		final Map<String, Concept> _newConcepts = Maps.newHashMap(Maps.uniqueIndex(newConcepts, GET_SCT_ID_FUNCTION));
		
		for (final Relationship newRelationship : newRelationships) {
			taxonomyBuilder.addEdge(createEdge(newRelationship));
		}
		
		for (final Relationship dirtyRelationship : dirtyRelationships) {
			taxonomyBuilder.addEdge(createEdge(dirtyRelationship));
		}
		
		// lookup all deleted relationship documents
		final Iterable<SnomedRelationshipIndexEntry> deletedRelationshipEntries;
		try {
			deletedRelationshipEntries = searcher.get(SnomedRelationshipIndexEntry.class, CDOIDUtils.createCdoIdToLong(deletedRelationships));
		} catch (IOException e) {
			throw new SnowowlRuntimeException(e);
		}
		
		for (final SnomedRelationshipIndexEntry relationship : deletedRelationshipEntries) {
			final String relationshipId = relationship.getId();
			//same relationship as new and detached
			if (_newRelationships.containsKey(relationshipId)) {
				final Relationship newRelationship = _newRelationships.get(relationshipId);
				final String typeId = newRelationship.getType().getId();
				//ignore everything but IS_As
				if (Concepts.IS_A.equals(typeId)) {
					//check source and destination as well
					if (relationship.getSourceId().equals(newRelationship.getSource().getId())
							&& relationship.getDestinationId().equals(newRelationship.getDestination().getId())) {
						
						//and if the new relationship has more recent (larger CDO ID), ignore deletion
						if (CDOIDUtils.asLong(newRelationship.cdoID()) > relationship.getStorageKey()) {
							continue;
						}
					}
				}
			}
			taxonomyBuilder.removeEdge(createEdge(relationship));
		}
		for (final Concept newConcept : newConcepts) {
			taxonomyBuilder.addNode(createNode(newConcept));
		}
		
		try {
			final Iterable<SnomedConceptDocument> deletedConcepts = searcher.get(SnomedConceptDocument.class, CDOIDUtils.createCdoIdToLong(deletedConceptStorageKeys));
			for (final SnomedConceptDocument concept : deletedConcepts) {
				//consider the same as for relationship
				//we have to decide if deletion is the 'stronger' modification or not
				final String conceptId = concept.getId();
				
				//same concept as addition and deletion
				if (_newConcepts.containsKey(conceptId)) {
					final Concept newConcept = _newConcepts.get(conceptId);
					//check whether new concept has more recent (larger CDO ID) or not, ignore deletion
					if (CDOIDUtils.asLong(newConcept.cdoID()) > concept.getStorageKey()) {
						continue;
					}
				}
				//else delete it
				taxonomyBuilder.removeNode(createDeletedNode(conceptId));
			}
		} catch (IOException e) {
			throw new SnowowlRuntimeException(e);
		}
		
		for (final Concept dirtyConcept : dirtyConcepts) {
			final CDORevisionDelta revisionDelta = commitChangeSet.getRevisionDeltas().get(dirtyConcept.cdoID());
			if (revisionDelta == null) {
				continue;
			}
			final boolean statusChange = revisionDelta.getFeatureDelta(SnomedPackage.Literals.COMPONENT__ACTIVE) != null;
			if (statusChange) {
				if (!dirtyConcept.isActive()) { //we do not need this concept. either it was deactivated now or sometime earlier.
					//nothing can be dirty and new at the same time
					taxonomyBuilder.removeNode(createNode(dirtyConcept.getId(), true));
				} else { //consider reverting inactivation
					if (!taxonomyBuilder.containsNode(dirtyConcept.getId())) {
						taxonomyBuilder.addNode(createNode(dirtyConcept));
					}
				}
			}
		}
		LOGGER.info("Rebuilding taxonomic information based on the changes.");
		this.taxonomyBuilderResult = taxonomyBuilder.build();
	}
	
	/*creates a taxonomy edge instance based on the given SNOMED CT relationship*/
	private TaxonomyBuilderEdge createEdge(final Relationship relationship) {
		return new TaxonomyBuilderEdge() {
			@Override public boolean isCurrent() {
				return relationship.isActive();
			}
			@Override public String getId() {
				return relationship.getId();
			}
			@Override public boolean isValid() {
				return Concepts.IS_A.equals(relationship.getType().getId()) && characteristicTypeId.equals(relationship.getCharacteristicType().getId());
			}
			@Override public String getSoureId() {
				return relationship.getSource().getId();
			}
			@Override public String getDestinationId() {
				return relationship.getDestination().getId();
			}
		};
	}
	
	public SnomedTaxonomyBuilderResult getTaxonomyBuilderResult() {
		return taxonomyBuilderResult;
	}
	
	/*creates a taxonomy edge instance based on the given SNOMED CT relationship*/
	private TaxonomyBuilderEdge createEdge(final SnomedRelationshipIndexEntry relationship) {
		return new TaxonomyBuilderEdge() {
			@Override public boolean isCurrent() {
				return relationship.isActive();
			}
			@Override public String getId() {
				return relationship.getId();
			}
			@Override public boolean isValid() {
				return Concepts.IS_A.equals(relationship.getTypeId()) && characteristicTypeId.equals(relationship.getCharacteristicTypeId());
			}
			@Override public String getSoureId() {
				return relationship.getSourceId();
			}
			@Override public String getDestinationId() {
				return relationship.getDestinationId();
			}
		};
	}
	
	/*creates and returns with a new taxonomy node instance based on the given SNOMED CT concept*/
	private TaxonomyBuilderNode createNode(final Concept concept) {
		return new TaxonomyBuilderNode() {
			@Override public boolean isCurrent() {
				return concept.isActive();
			}
			@Override public String getId() {
				return concept.getId();
			}
		};
	}

	/*creates and returns with a new taxonomy node instance based on the given SNOMED CT concept*/
	private TaxonomyBuilderNode createNode(final String id, final boolean active) {
		return new TaxonomyBuilderNode() {
			@Override public boolean isCurrent() {
				return active;
			}
			@Override public String getId() {
				return id;
			}
		};
	}
	
	private TaxonomyBuilderNode createDeletedNode(final String id) {
		return new TaxonomyBuilderNode() {
			@Override public boolean isCurrent() {
				throw new UnsupportedOperationException("This method should not be called when removing taxonomy nodes.");
			}
			@Override public String getId() {
				return id;
			}
		};
	}
}