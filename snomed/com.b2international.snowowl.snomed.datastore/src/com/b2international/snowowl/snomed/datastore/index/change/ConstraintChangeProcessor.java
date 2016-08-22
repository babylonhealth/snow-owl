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
package com.b2international.snowowl.snomed.datastore.index.change;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;

import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.datastore.ICDOCommitChangeSet;
import com.b2international.snowowl.datastore.index.ChangeSetProcessorBase;
import com.b2international.snowowl.snomed.core.mrcm.ConceptModelUtils;
import com.b2international.snowowl.snomed.datastore.snor.SnomedConstraintDocument;
import com.b2international.snowowl.snomed.mrcm.AttributeConstraint;
import com.b2international.snowowl.snomed.mrcm.ConceptModelPredicate;
import com.b2international.snowowl.snomed.mrcm.ConceptSetDefinition;
import com.b2international.snowowl.snomed.mrcm.MrcmPackage;
import com.google.common.collect.Iterables;

/**
 * @since 4.3
 */
public class ConstraintChangeProcessor extends ChangeSetProcessorBase {

	public ConstraintChangeProcessor() {
		super("predicate changes");
	}

	@Override
	public void process(ICDOCommitChangeSet commitChangeSet, RevisionSearcher searcher) {
		final Collection<AttributeConstraint> newAndDirtyConstraints = newHashSet();

		for (ConceptModelPredicate predicate : Iterables.concat(commitChangeSet.getNewComponents(ConceptModelPredicate.class),
				commitChangeSet.getDirtyComponents(ConceptModelPredicate.class))) {
			newAndDirtyConstraints.add(ConceptModelUtils.getContainerConstraint(predicate));
		}

		for (ConceptSetDefinition definition : Iterables.concat(commitChangeSet.getNewComponents(ConceptSetDefinition.class),
				commitChangeSet.getDirtyComponents(ConceptSetDefinition.class))) {
			newAndDirtyConstraints.add(ConceptModelUtils.getContainerConstraint(definition));
		}

		// (re)index new/changed constraints
		for (AttributeConstraint constraint : newAndDirtyConstraints) {
			indexRevision(constraint.cdoID(), SnomedConstraintDocument.builder(constraint).build());
		}

		deleteRevisions(SnomedConstraintDocument.class, commitChangeSet.getDetachedComponents(MrcmPackage.Literals.ATTRIBUTE_CONSTRAINT));
	}
	
}
