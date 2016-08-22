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
package com.b2international.snowowl.snomed.datastore.index.change;

import static com.b2international.snowowl.snomed.datastore.id.RandomSnomedIdentiferGenerator.generateConceptId;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.junit.Ignore;
import org.junit.Test;

import com.b2international.collections.PrimitiveSets;
import com.b2international.collections.longs.LongSet;
import com.b2international.index.revision.Revision;
import com.b2international.index.revision.RevisionBranch;
import com.b2international.index.revision.RevisionIndexRead;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.datastore.ICDOCommitChangeSet;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Relationship;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.SnomedFactory;
import com.b2international.snowowl.snomed.SnomedPackage;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Builder;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.datastore.taxonomy.Taxonomies;
import com.b2international.snowowl.snomed.datastore.taxonomy.Taxonomy;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetPackage;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * @since 4.7
 */
public class ConceptChangeProcessorTest extends BaseChangeProcessorTest {

	private Collection<String> availableImages = ImmutableSet.of(Concepts.ROOT_CONCEPT);
	private LongSet statedChangedConceptIds = PrimitiveSets.newLongOpenHashSet();
	private LongSet inferredChangedConceptIds = PrimitiveSets.newLongOpenHashSet();
	
	private ConceptChangeProcessor process() {
		return index().read(RevisionBranch.MAIN_PATH, new RevisionIndexRead<ConceptChangeProcessor>() {
			@Override
			public ConceptChangeProcessor execute(RevisionSearcher searcher) throws IOException {
				final ICDOCommitChangeSet commitChangeSet = createChangeSet();
				final Taxonomy inferredTaxonomy = Taxonomies.inferred(searcher, commitChangeSet, inferredChangedConceptIds, true);
				final Taxonomy statedTaxonomy = Taxonomies.stated(searcher, commitChangeSet, statedChangedConceptIds, true);
				final ConceptChangeProcessor processor = new ConceptChangeProcessor(availableImages, statedTaxonomy, inferredTaxonomy);
				processor.process(commitChangeSet, searcher);
				return processor;
			}
		});
	}
	
	@Test
	public void indexSingleConcept() throws Exception {
		final Concept concept = createConcept(generateConceptId());
		registerNew(concept);
		
		final ConceptChangeProcessor processor = process();
		
		final SnomedConceptDocument expected = doc(concept).build();
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void indexNewStatedChildConceptOfRoot() throws Exception {
		// index the ROOT concept as existing concept
		final long rootConceptId = Long.parseLong(Concepts.ROOT_CONCEPT);
		statedChangedConceptIds.add(rootConceptId);
		
		final Concept concept = createConcept(generateConceptId());
		registerNew(concept);
		
		final Relationship relationship = createStatedRelationship(concept.getId(), Concepts.IS_A, Concepts.ROOT_CONCEPT);
		concept.getOutboundRelationships().add(relationship);
		registerNew(relationship);
		
		final ConceptChangeProcessor processor = process();
		
		final SnomedConceptDocument expected = doc(concept)
				.statedParents(PrimitiveSets.newLongOpenHashSet(rootConceptId))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.build();
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void indexNewStatedAndInferredChildConceptOfRoot() throws Exception {
		// index the ROOT concept as existing concept
		final long rootConceptId = Long.parseLong(Concepts.ROOT_CONCEPT);
		statedChangedConceptIds.add(rootConceptId);
		inferredChangedConceptIds.add(rootConceptId);
		
		final Concept concept = createConcept(generateConceptId());
		registerNew(concept);
		
		final Relationship statedRelationship = createStatedRelationship(concept.getId(), Concepts.IS_A, Concepts.ROOT_CONCEPT);
		concept.getOutboundRelationships().add(statedRelationship);
		registerNew(statedRelationship);
		
		final Relationship inferredRelationship = createInferredRelationship(concept.getId(), Concepts.IS_A, Concepts.ROOT_CONCEPT);
		concept.getOutboundRelationships().add(inferredRelationship);
		registerNew(inferredRelationship);
		
		final ConceptChangeProcessor processor = process();
		
		final SnomedConceptDocument expected = doc(concept)
				.parents(PrimitiveSets.newLongOpenHashSet(rootConceptId))
				.ancestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.statedParents(PrimitiveSets.newLongOpenHashSet(rootConceptId))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.build();
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void deleteLeafConcept() throws Exception {
		final String conceptId = generateConceptId();
		final Concept concept = createConcept(conceptId);
		
		registerDetached(concept.cdoID(), SnomedPackage.Literals.CONCEPT);
		
		final ConceptChangeProcessor processor = process();
		
		assertEquals(0, processor.getMappings().size());
		assertEquals(1, processor.getDeletions().size());
	}
	
	@Test
	public void deleteConceptWithOneStatedChild() throws Exception {
		// given a parent concept and child concept
		final String parentId = generateConceptId();
		final String childId = generateConceptId();
		final Concept parentConcept = createConcept(parentId);
		final Concept childConcept = createConcept(childId);
		// and a stated relationship between the two
		final Relationship childToParentIsa = createStatedRelationship(childId, Concepts.IS_A, parentId);
		
		final long parentIdLong = Long.parseLong(parentId);
		final long childIdLong = Long.parseLong(childId);
		
		// index the child and parent concept documents as current state
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(parentConcept.cdoID()), doc(parentConcept).build());
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(childConcept.cdoID()), doc(childConcept)
				// child concept has stated parent and ROOT ancestor
				.statedParents(PrimitiveSets.newLongOpenHashSet(parentIdLong))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.build());
		
		// index a single relationship between the two IDs to indicate parent-child relations in the index
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(childToParentIsa.cdoID()), SnomedRelationshipIndexEntry.builder(childToParentIsa).build());
		
		// register child concept as existing concept in view, so it can be loaded via CDO
		registerExistingObject(childConcept);
		
		// delete parent concept and its single relationship
		registerDetached(parentConcept.cdoID(), SnomedPackage.Literals.CONCEPT);
		registerDetached(childToParentIsa.cdoID(), SnomedPackage.Literals.RELATIONSHIP);
		
		statedChangedConceptIds.add(parentIdLong);
		statedChangedConceptIds.add(childIdLong);
		
		final ConceptChangeProcessor processor = process();
		
		// the parent concept should be deleted
		assertEquals(1, processor.getDeletions().size());
		
		// and the child concept needs to be reindexed as child of the invisible ROOT ID
		assertEquals(1, processor.getMappings().size());
		final Revision newChildRevision = Iterables.getOnlyElement(processor.getMappings().values());
		final SnomedConceptDocument expectedChildRevision = doc(childConcept).build();
		assertDocEquals(expectedChildRevision, newChildRevision);
	}
	
	@Test
	public void addIsaRelationshipToExistingConcepts() throws Exception {
		// given a parent concept and child concept
		final String parentId = generateConceptId();
		final String childId = generateConceptId();
		final Concept parentConcept = createConcept(parentId);
		final Concept childConcept = createConcept(childId);
		
		final long parentIdLong = Long.parseLong(parentId);
		final long childIdLong = Long.parseLong(childId);
		statedChangedConceptIds.add(parentIdLong);
		statedChangedConceptIds.add(childIdLong);

		// index the child and parent concept documents as current state
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(parentConcept.cdoID()), doc(parentConcept).build());
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(childConcept.cdoID()), doc(childConcept).build());
		
		registerExistingObject(childConcept);
		registerExistingObject(parentConcept);
		
		// add a new stated relationship between the two
		final Relationship childToParentIsa = createStatedRelationship(childId, Concepts.IS_A, parentId);
		registerNew(childToParentIsa);
		
		final ConceptChangeProcessor processor = process();
		
		// the child document should be reindexed with new parent information 
		assertEquals(1, processor.getMappings().size());
		final SnomedConceptDocument expectedDoc = doc(childConcept)
				.statedParents(PrimitiveSets.newLongOpenHashSet(parentIdLong))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.build();
		final Revision changedDoc = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expectedDoc, changedDoc);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void createNewRefSetWithIdentifierConcept() throws Exception {
		final String identifierId = generateConceptId();
		final Concept identifierConcept = createConcept(identifierId);
		registerNew(identifierConcept);
		final SnomedRefSet refSet = getRegularRefSet(identifierId, SnomedTerminologyComponentConstants.CONCEPT_NUMBER);
		registerNew(refSet);
		
		final ConceptChangeProcessor processor = process();
		
		final SnomedConceptDocument expected = doc(identifierConcept).refSet(refSet).build();
		assertEquals(1, processor.getMappings().size());
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Ignore("Unsupported")
	@Test
	public void createRefSetForExistingConcept() throws Exception {
		// TODO implement if required
	}
	
	@Test
	public void deleteRefSetButKeepIdentifierConcept() throws Exception {
		final SnomedRefSet refSet = getRegularRefSet(generateConceptId(), SnomedTerminologyComponentConstants.CONCEPT_NUMBER);
		registerDetached(refSet.cdoID(), SnomedRefSetPackage.Literals.SNOMED_REF_SET);
		
		final ConceptChangeProcessor processor = process();
		
		assertEquals(0, processor.getMappings().size());
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void addNewSimpleMemberToExistingConcept() throws Exception {
		final String conceptId = generateConceptId();
		final String referringReferenceSetId = generateConceptId();
		
		final Concept concept = createConcept(conceptId);
		final SnomedRefSetMember member = createSimpleMember(conceptId, referringReferenceSetId);
		
		// set current state
		registerExistingObject(concept);
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(concept.cdoID()), doc(concept).build());
		
		registerNew(member);
		
		final ConceptChangeProcessor processor = process();
		
		// the concept needs to be reindexed with the referring member value
		final SnomedConceptDocument expected = doc(concept)
				.referringRefSets(ImmutableSet.of(referringReferenceSetId))
				.build();
		assertEquals(1, processor.getMappings().size());
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void addNewSimpleMapMemberToExistingConcept() throws Exception {
		final String conceptId = generateConceptId();
		final String referringMappingReferenceSetId = generateConceptId();
		
		final Concept concept = createConcept(conceptId);
		final SnomedRefSetMember member = createSimpleMapMember(conceptId, "A00", referringMappingReferenceSetId);
		
		// set current state
		registerExistingObject(concept);
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(concept.cdoID()), doc(concept).build());
		
		registerNew(member);
		
		final ConceptChangeProcessor processor = process();
		
		// the concept needs to be reindexed with the referring member value
		final SnomedConceptDocument expected = doc(concept)
				.referringMappingRefSets(ImmutableSet.of(referringMappingReferenceSetId))
				.build();
		assertEquals(1, processor.getMappings().size());
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void deleteSimpleMemberOfConcept() throws Exception {
		final String conceptId = generateConceptId();
		final String referringReferenceSetId = generateConceptId();
		
		final Concept concept = createConcept(conceptId);
		final SnomedRefSetMember member = createSimpleMember(conceptId, referringReferenceSetId);
		
		// set current state
		registerExistingObject(concept);
		registerExistingObject(member);
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(concept.cdoID()), doc(concept)
				.referringRefSets(ImmutableSet.of(referringReferenceSetId))
				.build());
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(member.cdoID()), SnomedRefSetMemberIndexEntry.builder(member).build());
		
		registerDetached(member.cdoID(), SnomedRefSetPackage.Literals.SNOMED_REF_SET_MEMBER);
		
		final ConceptChangeProcessor processor = process();
		
		// the concept needs to be reindexed with the referring member value
		final SnomedConceptDocument expected = doc(concept).build();
		assertEquals(1, processor.getMappings().size());
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void deleteSimpleMapMemberOfConcept() throws Exception {
		final String conceptId = generateConceptId();
		final String referringMappingReferenceSetId = generateConceptId();
		
		final Concept concept = createConcept(conceptId);
		final SnomedRefSetMember member = createSimpleMapMember(conceptId, "A00", referringMappingReferenceSetId);
		
		// set current state
		registerExistingObject(concept);
		registerExistingObject(member);
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(concept.cdoID()), doc(concept)
				.referringMappingRefSets(ImmutableSet.of(referringMappingReferenceSetId))
				.build());
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(member.cdoID()), SnomedRefSetMemberIndexEntry.builder(member).build());
		
		registerDetached(member.cdoID(), SnomedRefSetPackage.Literals.SNOMED_REF_SET_MEMBER);
		
		final ConceptChangeProcessor processor = process();
		
		// the concept needs to be reindexed with the referring member value
		final SnomedConceptDocument expected = doc(concept).build();
		assertEquals(1, processor.getMappings().size());
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void inactivateConcept() throws Exception {
		// current state
		final String conceptId = generateConceptId();
		final Concept concept = createConcept(conceptId);
		final long conceptStorageKey = CDOIDUtil.getLong(concept.cdoID());
		registerExistingObject(concept);
		indexRevision(RevisionBranch.MAIN_PATH, conceptStorageKey, doc(concept).build());
		
		// change set
		// XXX intentionally not registering this object to the concept map
		final Concept dirtyConcept = SnomedFactory.eINSTANCE.createConcept();
		withCDOID(dirtyConcept, conceptStorageKey);
		dirtyConcept.setId(conceptId);
		dirtyConcept.setActive(false);
		dirtyConcept.setDefinitionStatus(getConcept(Concepts.FULLY_DEFINED));
		dirtyConcept.setModule(module());
		dirtyConcept.setExhaustive(false);
		registerDirty(dirtyConcept);
		registerSetRevisionDelta(dirtyConcept, SnomedPackage.Literals.COMPONENT__ACTIVE, false);
		
		final ConceptChangeProcessor processor = process();
		
		// expected index changes, concept should be inactive now
		assertEquals(1, processor.getMappings().size());
		final SnomedConceptDocument expected = doc(concept).active(false).build();
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	@Test
	public void newInferredIsaRelationshipDoesNotChangeStatedTaxonomy() throws Exception {
		// given a parent concept and child concept
		final String parentId = generateConceptId();
		final String childId = generateConceptId();
		final Concept parentConcept = createConcept(parentId);
		final Concept childConcept = createConcept(childId);
		final Relationship statedIsa = createStatedRelationship(childId, Concepts.IS_A, parentId);
		registerExistingObject(statedIsa);
		
		final long parentIdLong = Long.parseLong(parentId);
		final long childIdLong = Long.parseLong(childId);

		// index the child and parent concept documents as current state
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(parentConcept.cdoID()), doc(parentConcept).build());
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(childConcept.cdoID()), doc(childConcept)
				.statedParents(PrimitiveSets.newLongOpenHashSet(parentIdLong))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.build());
		indexRevision(RevisionBranch.MAIN_PATH, CDOIDUtil.getLong(statedIsa.cdoID()), SnomedRelationshipIndexEntry.builder(statedIsa).build());
		
		registerExistingObject(childConcept);
		registerExistingObject(parentConcept);
		
		// add a new stated relationship between the two
		inferredChangedConceptIds.add(parentIdLong);
		inferredChangedConceptIds.add(childIdLong);
		final Relationship childToParentIsa = createInferredRelationship(childId, Concepts.IS_A, parentId);
		registerNew(childToParentIsa);
		registerDirty(childConcept);
		
		final ConceptChangeProcessor processor = process();
		
		assertEquals(1, processor.getMappings().size());
		final SnomedConceptDocument expected = doc(childConcept)
				.statedParents(PrimitiveSets.newLongOpenHashSet(parentIdLong))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.parents(PrimitiveSets.newLongOpenHashSet(parentIdLong))
				.ancestors(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.build();
		final Revision actual = Iterables.getOnlyElement(processor.getMappings().values());
		assertDocEquals(expected, actual);
		assertEquals(0, processor.getDeletions().size());
	}
	
	private Builder doc(final Concept concept) {
		return SnomedConceptDocument.builder()
				.id(concept.getId())
				.iconId(Concepts.ROOT_CONCEPT)
				.active(concept.isActive())
				.released(concept.isReleased())
				.exhaustive(concept.isExhaustive())
				.moduleId(concept.getModule().getId())
				.effectiveTime(EffectiveTimes.getEffectiveTime(concept.getEffectiveTime()))
				.primitive(Concepts.PRIMITIVE.equals(concept.getDefinitionStatus().getId()))
				.parents(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.ancestors(PrimitiveSets.newLongOpenHashSet())
				.statedParents(PrimitiveSets.newLongOpenHashSet(SnomedConceptDocument.ROOT_ID))
				.statedAncestors(PrimitiveSets.newLongOpenHashSet())
				.referringRefSets(Collections.<String>emptySet())
				.referringMappingRefSets(Collections.<String>emptySet());
	}

	private Concept createConcept(final String id) {
		final Concept concept = getConcept(id);
		withCDOID(concept, nextStorageKey());
		concept.setActive(true);
		concept.setDefinitionStatus(getConcept(Concepts.FULLY_DEFINED));
		concept.setModule(module());
		concept.setExhaustive(false);
		return concept;
	}

}
