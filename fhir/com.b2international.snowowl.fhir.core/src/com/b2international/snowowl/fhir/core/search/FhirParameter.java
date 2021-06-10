/*
 * Copyright 2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.search;

import java.util.Collection;
import java.util.Objects;

import com.b2international.snowowl.fhir.core.model.ValidatingBuilder;
import com.b2international.snowowl.fhir.core.search.FhirUriParameterDefinition.FhirRequestParameterType;
import com.b2international.snowowl.fhir.core.search.FhirUriSearchParameterDefinition.SearchRequestParameterValuePrefix;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * FHIR URI request parameter
 * <br>
 * parameter[:modifier]=[prefix]value
 * <br>
 * parameter[:modifier]=[prefix]system|code for tokens
 * 
 * <br>
 * where the modifiers are:<ul>
 * 		<li>missing
 * 		<li>exact
 * 		<li>contains
 * 		<li>text
 * 		<li>in
 * 		<li>below
 * 		<li>above
 * 		<li>not-in
 * 		<li>type
 * </ul>		
 * where the value types are:<ul>
 * 		<li>number (missing)
 * 		<li>date (missing)
 * 		<li>string (missing, exact, contains)
 * 		<li>token (missing, text, in, below, above, not-in)
 * 		<li>reference (missing, type)
 * 		<li>composite (missing)
 * 		<li>quantity (missing)
 * 		<li>uri (missing, below, above)
 * </ul>
 * prefixes are:<ul>
 * 		<li>eq - equal (default if no prefix is present)
 * 		<li>ne
 * 		<li>gt
 * 		<li>lt
 * 		<li>ge
 * 		<li>le
 * 		<li>sa
 * 		<li>eb
 * 		<li>ap
 * 
 * https://www.hl7.org/fhir/searchparameter-registry.html
 * @since 7.17.0
 */
public abstract class FhirParameter {
	
	public static class PrefixedValue {
		
		private SearchRequestParameterValuePrefix prefix;
		
		private String value;
		
		private PrefixedValue(SearchRequestParameterValuePrefix prefix, String value) {
			this.prefix = prefix;
			this.value = value;
		}
		
		public static PrefixedValue of(final String valueString) {
			if (valueString == null) {
				return new PrefixedValue(null, null);
			} else if (valueString.length() < 2) {
				return new PrefixedValue(null, valueString);
			} else {
				String prefixCandidate = valueString.substring(0, 2);
				String valuePart = valueString.substring(2);
				try {
					SearchRequestParameterValuePrefix valueOf = SearchRequestParameterValuePrefix.valueOf(prefixCandidate);
					return new PrefixedValue(valueOf, valuePart);
				} catch(IllegalArgumentException iae) {
					return new PrefixedValue(null, valueString);
				}
			}
		}
		
		public static PrefixedValue witoutPrefix(final String valueString) {
			return new PrefixedValue(null, valueString);
		}
		
		public SearchRequestParameterValuePrefix getPrefix() {
			return prefix;
		}
		
		public String getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			return "PrefixedValue [prefix=" + prefix + ", value=" + value + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(prefix, value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PrefixedValue other = (PrefixedValue) obj;
			return prefix == other.prefix && Objects.equals(value, other.value);
		}
		
	}
	
	private final FhirUriParameterDefinition parameterDefinition;
	
	private final Collection<PrefixedValue> values;
	
	FhirParameter(final FhirUriParameterDefinition parameterDefinition, Collection<PrefixedValue> values) {
		this.parameterDefinition = parameterDefinition;
		this.values = values;
	}

	public String getName() {
		return parameterDefinition.getName();
	}
	
	public FhirRequestParameterType getType() {
		return parameterDefinition.getType();
	}
	
	public Collection<PrefixedValue> getValues() {
		return values;
	}
	
	public FhirUriParameterDefinition getParameterDefinition() {
		return parameterDefinition;
	}

	public static abstract class Builder<B extends Builder<B, T>, T extends FhirParameter> extends ValidatingBuilder<T> {
		
		protected Collection<PrefixedValue> values = ImmutableSet.of();
		
		public B values(final Collection<PrefixedValue> values) {
			this.values = values;
			return getSelf();
		}
		
		public B value(final PrefixedValue value) {
			this.values = ImmutableSet.of(value);
			return getSelf();
		}
		
		protected abstract B getSelf();
	}

}
