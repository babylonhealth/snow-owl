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
package com.b2international.collections.longs;

/**
 * @since 4.7
 */
public class LongSetWrapper extends LongCollectionWrapper implements LongSet {

	protected LongSetWrapper(it.unimi.dsi.fastutil.longs.LongSet delegate) {
		super(delegate);
	}
	
	@Override
	protected final it.unimi.dsi.fastutil.longs.LongSet delegate() {
		return (it.unimi.dsi.fastutil.longs.LongSet) super.delegate();
	}
	
	public static LongSet wrap(it.unimi.dsi.fastutil.longs.LongSet collection) {
		return new LongSetWrapper(collection);
	}
	
	@Override
	public boolean equals(Object obj) {
		return AbstractLongSet.equals(this, obj);
	}
	
	@Override
	public int hashCode() {
        return AbstractLongSet.hashCode(this);
	}
}
