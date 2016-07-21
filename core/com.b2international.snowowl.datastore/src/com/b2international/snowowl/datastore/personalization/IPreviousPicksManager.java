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
package com.b2international.snowowl.datastore.personalization;

import com.b2international.index.compat.SingleDirectoryIndex;
import com.b2international.snowowl.core.ApplicationContext;

/**
 * Provides access to components previously selected from the global quick search control.
 * <p>
 * The interface only serves as an {@link ApplicationContext} service key.
 * 
 */
public interface IPreviousPicksManager extends IComponentSetManager, SingleDirectoryIndex { }