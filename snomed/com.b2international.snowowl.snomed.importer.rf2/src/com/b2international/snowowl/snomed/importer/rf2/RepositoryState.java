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
package com.b2international.snowowl.snomed.importer.rf2;

import java.util.Collection;

import com.b2international.collections.longs.LongCollection;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;

/**
 * Class which can hold certain parts of the current state of the SNOMED CT terminology in memory.
 * @since 4.7
 */
public final class RepositoryState {

	private final LongCollection conceptIds;
	private final Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> statedStatements;
	private final Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> inferredStatements;

	public RepositoryState(LongCollection conceptIds, Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> statedStatements, Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> inferredStatements) {
		this.conceptIds = conceptIds;
		this.statedStatements = statedStatements;
		this.inferredStatements = inferredStatements;
	}
	
	public LongCollection getConceptIds() {
		return conceptIds;
	}
	
	public Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> getInferredStatements() {
		return inferredStatements;
	}
	
	public Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> getStatedStatements() {
		return statedStatements;
	}
	
}
