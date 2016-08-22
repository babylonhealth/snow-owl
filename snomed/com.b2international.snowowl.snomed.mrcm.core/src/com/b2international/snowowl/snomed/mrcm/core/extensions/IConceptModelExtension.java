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

import com.b2international.snowowl.snomed.mrcm.core.widget.model.ConceptWidgetModel;

/**
 * Allows extending the {@link ConceptWidgetModel concept model}.
 * 
 */
public interface IConceptModelExtension extends IConceptExtension {

	/**
	 * Modifies the specified concept widget model. The supplied widget model might already contain items based on
	 * applicable MRCM rules or other concept model extensions.
	 * 
	 * @param widgetModel the model to modify (may not be {@code null})
	 */
	public void modifyWidgetModel(ConceptWidgetModel widgetModel);

}