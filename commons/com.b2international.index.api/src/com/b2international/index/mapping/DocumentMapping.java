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
package com.b2international.index.mapping;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.b2international.index.Analyzed;
import com.b2international.index.Analyzers;
import com.b2international.index.Doc;
import com.b2international.index.query.Expression;
import com.b2international.index.query.Expressions;
import com.b2international.index.util.Reflections;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * @since 4.7
 */
public final class DocumentMapping {

	// type path delimiter to differentiate between same nested types in different contexts
	private static final String DELIMITER = ".";
	
	public static final String _ID = "_id";
	public static final String _UID = "_uid";
	public static final String _TYPE = "_type";

	private static final Function<? super Field, String> GET_NAME = new Function<Field, String>() {
		@Override
		public String apply(Field field) {
			return field.getName();
		}
	};
	
	private final Class<?> type;
	private final String typeAsString;
	private final Map<String, Field> fieldMap;
	private final Map<Class<?>, DocumentMapping> nestedTypes;
	private final Map<String, Analyzers> analyzedFields;
	private final DocumentMapping parent;

	DocumentMapping(Class<?> type) {
		this(null, type);
	}
		
	DocumentMapping(DocumentMapping parent, Class<?> type) {
		this.parent = parent;
		this.type = type;
		final String typeAsString = getType(type);
		this.typeAsString = parent == null ? typeAsString : parent.typeAsString() + DELIMITER + typeAsString;
		this.fieldMap = FluentIterable.from(Reflections.getFields(type))
			.filter(new Predicate<Field>() {
				@Override
				public boolean apply(Field field) {
					return !Modifier.isStatic(field.getModifiers());
				}
			}).uniqueIndex(GET_NAME);
		
		final Builder<String, Analyzers> analyzedFields = ImmutableMap.builder();

		for (Field field : getFields()) {
			if (field.isAnnotationPresent(Analyzed.class)) {
				final Analyzers analyzer = field.getAnnotation(Analyzed.class).analyzer();
				analyzedFields.put(field.getName(), analyzer);
			}
		}
		
		this.analyzedFields = analyzedFields.build();
				
		this.nestedTypes = FluentIterable.from(getFields())
			.transform(new Function<Field, Class<?>>() {
				@Override
				public Class<?> apply(Field field) {
					if (Reflections.isMapType(field)) {
						return Map.class;
					} else {
						return Reflections.getType(field);
					}
				}
			})
			.filter(new Predicate<Class<?>>() {
				@Override
				public boolean apply(Class<?> fieldType) {
					return isNestedDoc(fieldType);
				}
			})
			.toMap(new Function<Class<?>, DocumentMapping>() {
				@Override
				public DocumentMapping apply(Class<?> input) {
					return new DocumentMapping(DocumentMapping.this.parent == null ? DocumentMapping.this : DocumentMapping.this.parent, input);
				}
			});
	}
	
	public DocumentMapping getParent() {
		return parent;
	}
	
	public Collection<DocumentMapping> getNestedMappings() {
		return ImmutableList.copyOf(nestedTypes.values());
	}
	
	public DocumentMapping getNestedMapping(String field) {
		return nestedTypes.get(getNestedType(field));
	}
	
	public DocumentMapping getNestedMapping(Class<?> nestedType) {
		if (nestedTypes.containsKey(nestedType)) {
			return nestedTypes.get(nestedType);
		} else {
			for (DocumentMapping nestedMapping : nestedTypes.values()) {
				try {
					return nestedMapping.getNestedMapping(nestedType);
				} catch (IllegalArgumentException ignored) {
					continue;
				}
			}
			throw new IllegalArgumentException(String.format("Missing nested type '%s' on mapping of '%s'", nestedType, type));
		}
	}
	
	private Class<?> getNestedType(String field) {
		final Class<?> nestedType = Reflections.getType(getField(field));
		checkArgument(nestedTypes.containsKey(nestedType), "Missing nested type '%s' on mapping of '%s'", field, type);
		return nestedType;
	}
	
	public Field getField(String name) {
		checkArgument(fieldMap.containsKey(name), "Missing field '%s' on mapping of '%s'", name, type);
		return fieldMap.get(name);
	}
	
	public Collection<Field> getFields() {
		return ImmutableList.copyOf(fieldMap.values());
	}
	
	public boolean isAnalyzed(String field) {
		return analyzedFields.containsKey(field);
	}
	
	public Map<String, Analyzers> getAnalyzedFields() {
		return analyzedFields;
	}

	public Class<?> type() {
		return type;
	}
	
	public String typeAsString() {
		return typeAsString;
	}
	
	public Expression matchType() {
		return Expressions.exactMatch(_TYPE, typeAsString);
	}
	
	public String toUid(String key) {
		return String.format("%s#%s", typeAsString, key);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(type, parent);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final DocumentMapping other = (DocumentMapping) obj;
		return Objects.equals(type, other.type) && Objects.equals(parent, other.parent);
	}
	
	// static helpers
	
	public static Expression matchId(String id) {
		return Expressions.exactMatch(_ID, id);
	}
	
	public static String getType(Class<?> type) {
		final Doc annotation = getDocAnnotation(type);
		checkArgument(annotation != null, "Doc annotation must be present on type '%s' or on its class hierarchy", type);
		final String docType = Strings.isNullOrEmpty(annotation.type()) ? type.getSimpleName().toLowerCase() : annotation.type();
		checkArgument(!Strings.isNullOrEmpty(docType), "Document type should not be null or empty on class %s", type.getName());
		return docType;
	}
	
	private static Doc getDocAnnotation(Class<?> type) {
		if (type.isAnnotationPresent(Doc.class)) {
			return type.getAnnotation(Doc.class);
		} else {
			if (type.getSuperclass() != null) {
				final Doc doc = getDocAnnotation(type.getSuperclass());
				if (doc != null) {
					return doc;
				}
			}
			
			for (Class<?> iface : type.getInterfaces()) {
				final Doc doc = getDocAnnotation(iface);
				if (doc != null) {
					return doc;
				}
			}
			return null;
		}
	}

	public static boolean isNestedDoc(Class<?> fieldType) {
		final Doc doc = getDocAnnotation(fieldType);
		return doc == null ? false : doc.nested();
	}

}
