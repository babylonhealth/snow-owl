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
package com.b2international.snowowl.snomed.mrcm.core.extensions;

import java.util.Collection;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.snomed.Concept;

/**
 */
public interface IConceptModelExtensionProvider {

	/**
	 * Returns the applicable concept model extensions for the specified concept.
	 * 
	 * @param concept
	 * @return the applicable concept model extensions
	 */
	Collection<IConceptModelExtension> getModelExtensions(Concept concept);

	/**
	 * Returns the applicable concept model extensions for the concept with the specified identifier.
	 * 
	 * @param branchPath the branch path to use when determining extension applicability (may not be {@code null})
	 * @param conceptId the identifier of the concept to collect extensions for
	 * @return the applicable concept model extensions, or an empty collection if no extension could be collected
	 */
	Collection<IConceptModelExtension> getModelExtensions(IBranchPath branchPath, long conceptId);
}