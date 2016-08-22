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
package com.b2international.snowowl.datastore.index;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Providers high-level component identifier if this component is a subcomponent of the high-level component identifier. Should return own ID if this
 * is the high-level component.
 * 
 * @since 5.0
 */
public interface ContainerIdProvider {

	/**
	 * @return the container identifier
	 */
	@JsonIgnore
	String getContainerId();
	
	@JsonIgnore
	boolean isRoot();
	
}
