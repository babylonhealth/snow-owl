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
package com.b2international.snowowl.core;

import java.io.Closeable;

import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.eventbus.IEventBus;

/**
 * @since 4.5
 */
public interface Repository extends ServiceProvider, Closeable {

	/**
	 * Returns the ID of the repository.
	 * 
	 * @return
	 */
	String id();

	/**
	 * Returns the {@link Repository}s own event bus. It can be used to execute {@link Request}s targeting this {@link Repository}.
	 * 
	 * @return
	 */
	IEventBus handlers();

	/**
	 * Returns the global singleton {@link IEventBus} to send events to it.
	 * 
	 * @return
	 */
	IEventBus events();

}
