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
package com.b2international.snowowl.snomed.reasoner.server.diff.concretedomain;

import com.b2international.snowowl.core.ComponentIdentifierPair;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.datastore.ConcreteDomainFragment;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetEditingContext;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetUtil;
import com.b2international.snowowl.snomed.datastore.model.SnomedModelExtensions;
import com.b2international.snowowl.snomed.reasoner.server.diff.OntologyChange.Nature;
import com.b2international.snowowl.snomed.reasoner.server.diff.OntologyChangeProcessor;
import com.b2international.snowowl.snomed.snomedrefset.SnomedConcreteDataTypeRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedConcreteDataTypeRefSetMember;

/**
 * Applies changes related to concrete domain elements using the specified SNOMED CT editing context.
 */
public class ConcreteDomainPersister extends OntologyChangeProcessor<ConcreteDomainFragment> {

	private final SnomedRefSetEditingContext refSetEditingContext;
	private final Concept moduleConcept;
	private final Nature nature;
	
	public ConcreteDomainPersister(final SnomedEditingContext context, final Nature nature) {
		this.nature = nature;
		this.refSetEditingContext = context.getRefSetEditingContext();
		this.moduleConcept = context.getDefaultModuleConcept();
	}

	@Override
	protected void handleRemovedSubject(final long conceptId, final ConcreteDomainFragment removedEntry) {
		
		if (!Nature.REMOVE.equals(nature)) {
			return;
		}
		
		final SnomedConcreteDataTypeRefSetMember existingMember = (SnomedConcreteDataTypeRefSetMember) refSetEditingContext.lookup(removedEntry.getStorageKey());
		SnomedModelExtensions.removeOrDeactivate(existingMember);
	}
	
	@Override
	protected void handleAddedSubject(final long conceptId, final ConcreteDomainFragment addedEntry) {
		
		if (!Nature.ADD.equals(nature)) {
			return;
		}

		final SnomedConcreteDataTypeRefSet concreteDataTypeRefSet = refSetEditingContext.lookup(Long.toString(addedEntry.getRefSetId()), SnomedConcreteDataTypeRefSet.class);
		
		final ComponentIdentifierPair<String> componentPair = ComponentIdentifierPair.create(SnomedTerminologyComponentConstants.CONCEPT, Long.toString(conceptId));
		final SnomedConcreteDataTypeRefSetMember refSetMember = refSetEditingContext.createConcreteDataTypeRefSetMember(
				componentPair,
				nullIfUnset(addedEntry.getUomId()),
				Concepts.CD_EQUAL,
				SnomedRefSetUtil.deserializeValue(addedEntry.getDataType(), addedEntry.getValue()), 
				Concepts.INFERRED_RELATIONSHIP, 
				addedEntry.getLabel(), 
				moduleConcept.getId(), 
				concreteDataTypeRefSet);
		
		final Concept referencedComponent = refSetEditingContext.lookup(componentPair.getComponentId(), Concept.class);
		referencedComponent.getConcreteDomainRefSetMembers().add(refSetMember);
	}

	private String nullIfUnset(final long uomId) {
		return (uomId == ConcreteDomainFragment.UNSET_UOM_ID) ? null : Long.toString(uomId);
	}
}
