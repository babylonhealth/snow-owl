/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.model;

import java.lang.reflect.Field;
import java.util.Comparator;

public class FieldOrderComparator implements Comparator<Field> {

	@Override
	public int compare(Field o1, Field o2) {
		Order or1 = o1.getAnnotation(Order.class);
		Order or2 = o2.getAnnotation(Order.class);
		if (or1 != null && or2 != null) {
			return or1.value() - or2.value();
		} else if (or1 != null && or2 == null) {
			return -1;
		} else if (or1 == null && or2 != null) {
			return 1;
		}
		return o1.getName().compareTo(o2.getName());
	}

}