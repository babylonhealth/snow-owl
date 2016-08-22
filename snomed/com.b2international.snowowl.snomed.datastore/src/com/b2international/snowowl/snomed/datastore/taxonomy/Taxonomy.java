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

import com.b2international.collections.longs.LongSet;
import com.b2international.commons.Pair;
import com.google.common.base.Preconditions;

/**
 * @since 4.6
 */
public class Taxonomy {
	
	private final ISnomedTaxonomyBuilder newTaxonomy;
	private final ISnomedTaxonomyBuilder oldTaxonomy;
	private final Pair<LongSet, LongSet> diff;

	public Taxonomy(ISnomedTaxonomyBuilder newTaxonomy, ISnomedTaxonomyBuilder oldTaxonomy, Pair<LongSet, LongSet> diff) {
		this.newTaxonomy = newTaxonomy;
		Preconditions.checkState(!newTaxonomy.isDirty(), "Builder for representing the new state of the taxonomy has dirty state.");
		this.oldTaxonomy = oldTaxonomy;
		this.diff = diff;
	}
	
	public ISnomedTaxonomyBuilder getNewTaxonomy() {
		return newTaxonomy;
	}
	
	public ISnomedTaxonomyBuilder getOldTaxonomy() {
		return oldTaxonomy;
	}
	
	public Pair<LongSet, LongSet> getDifference() {
		return diff;
	}

}
