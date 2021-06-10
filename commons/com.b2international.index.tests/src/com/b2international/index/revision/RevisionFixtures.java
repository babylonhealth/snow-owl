/*
 * Copyright 2011-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.revision;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.b2international.commons.collections.Collections3;
import com.b2international.index.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * @since 4.7
 */
public class RevisionFixtures {

	private RevisionFixtures() {
	}
	
	@Doc(revisionHash = { "field1", "field2", "terms" })
	public static class RevisionData extends Revision {
		
		public static class Builder extends Revision.Builder<RevisionData.Builder, RevisionData> {

			private String id;
			private String field1;
			private String field2;
			private List<String> terms;
			private String derivedField;
			
			public Builder(RevisionData revisionData) {
				this.id = revisionData.getId();
				this.field1 = revisionData.field1;
				this.field2 = revisionData.field2;
				this.terms = revisionData.terms;
				this.derivedField = revisionData.derivedField;
			}

			public Builder id(String id) {
				this.id = id;
				return getSelf();
			}
			
			public Builder field1(String field1) {
				this.field1 = field1;
				return getSelf();
			}
			
			public Builder field2(String field2) {
				this.field2 = field2;
				return getSelf();
			}
			
			public Builder terms(Iterable<String> terms) {
				this.terms = terms != null ? Collections3.toImmutableList(terms) : null;
				return getSelf();
			}
			
			public Builder derivedField(String derivedField) {
				this.derivedField = derivedField;
				return getSelf();
			}
			
			@Override
			protected Builder getSelf() {
				return this;
			}

			@Override
			public RevisionData build() {
				return new RevisionData(id, field1, field2, terms, derivedField);
			}
		}
		
		@Text(analyzer=Analyzers.TOKENIZED)
		private final String field1;
		private final String field2;
		private final List<String> terms;
		private final String derivedField;

		public RevisionData(final String id, final String field1, final String field2) {
			this(id, field1, field2, null, null);
		}
		
		@JsonCreator
		public RevisionData(
				@JsonProperty(Revision.Fields.ID) final String id, 
				@JsonProperty("field1") final String field1, 
				@JsonProperty("field2") final String field2,
				@JsonProperty("terms") final List<String> terms,
				@JsonProperty("derivedField") final String derivedField) {
			super(id);
			this.field1 = field1;
			this.field2 = field2;
			this.terms = terms;
			this.derivedField = derivedField;
		}
		
		public String getField1() {
			return field1;
		}
		
		public String getField2() {
			return field2;
		}
		
		public List<String> getTerms() {
			return terms;
		}
		
		public String getDerivedField() {
			return derivedField;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			RevisionData other = (RevisionData) obj;
			return Objects.equals(field1, other.field1) 
					&& Objects.equals(field2, other.field2)
					&& Objects.equals(terms, other.terms); 
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(field1, field2, terms);
		}
		
		public Builder toBuilder() {
			return new Builder(this);
		}
	}
	
	@Doc
	public static final class AnalyzedData extends Revision {
		
		@Text
		private final String field;
		
		@JsonCreator
		public AnalyzedData(
				@JsonProperty(Revision.Fields.ID) final String id, 
				@JsonProperty("field") final String field) {
			super(id);
			this.field = field;
		}
		
		public String getField() {
			return field;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			AnalyzedData other = (AnalyzedData) obj;
			return Objects.equals(field, other.field); 
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(field);
		}
		
	}
	
	@Doc
	@Script(name="doi", script="return doc.doi.value")
	@Script(name="doiFactor", script="return doc.doi.value * params.factor")
	public static final class ScoredData extends RevisionData implements WithScore {
		
		private float score = 0.0f;
		private final float doi;

		@JsonCreator
		public ScoredData(
				@JsonProperty(Revision.Fields.ID) final String id,
				@JsonProperty("field1") final String field1, 
				@JsonProperty("field2") final String field2,
				@JsonProperty("terms") final List<String> terms,
				@JsonProperty("doi") final float doi) {
			super(id, field1, field2, terms, null);
			this.doi = doi;
		}
		
		@Override
		public float getScore() {
			return score;
		}

		@Override
		public void setScore(float score) {
			this.score = score;
		}
		
		public float getDoi() {
			return doi;
		}

		@Override
		protected ToStringHelper doToString() {
			return super.doToString()
					.add("doi", doi)
					.add("score", score);
		}
		
	}
	
	@Doc
	public static final class BooleanData extends RevisionData {

		private final boolean active;

		@JsonCreator
		public BooleanData(
				@JsonProperty(Revision.Fields.ID) final String id,
				@JsonProperty("field1") final String field1, 
				@JsonProperty("field2") final String field2,
				@JsonProperty("terms") final List<String> terms,
				@JsonProperty("value") final boolean active) {
			super(id, field1, field2, terms, null);
			this.active = active;
		}
		
		public boolean isActive() {
			return active;
		}
		
	}
	
	@Doc
	public static final class RangeData extends RevisionData {
		
		private final int from;
		private final int to;

		@JsonCreator
		public RangeData(
				@JsonProperty(Revision.Fields.ID) final String id,
				@JsonProperty("field1") final String field1, 
				@JsonProperty("field2") final String field2, 
				@JsonProperty("terms") final List<String> terms,
				@JsonProperty("from") final int from,
				@JsonProperty("to") final int to) {
			super(id, field1, field2, terms, null);
			this.from = from;
			this.to = to;
		}
		
		public int getFrom() {
			return from;
		}
		
		public int getTo() {
			return to;
		}
		
	}
	
	@Doc(
		revisionHash = {"field1", "data"}
	)
	public static final class NestedRevisionData extends Revision {
		
		private String field1;
		// using unversioned data not the revision based one here
		private com.b2international.index.Fixtures.Data data;
		
		@JsonCreator
		public NestedRevisionData(
				@JsonProperty(Revision.Fields.ID) final String id,
				@JsonProperty("field1") String field1, 
				@JsonProperty("data") com.b2international.index.Fixtures.Data data) {
			super(id);
			this.field1 = field1;
			this.data = data;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			NestedRevisionData other = (NestedRevisionData) obj;
			return Objects.equals(field1, other.field1) && Objects.equals(data, other.data); 
		}
		
		public String getField1() {
			return field1;
		}
		
		public com.b2international.index.Fixtures.Data getData() {
			return data;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(field1, data);
		}
		
	}
	
	@Doc
	public static final class DeeplyNestedData extends Revision {
		
		private com.b2international.index.Fixtures.ParentData parentData;
		
		@JsonCreator
		public DeeplyNestedData(
				@JsonProperty(Revision.Fields.ID) final String id,
				@JsonProperty("parentData") com.b2international.index.Fixtures.ParentData parentData) {
			super(id);
			this.parentData = parentData;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(parentData);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			DeeplyNestedData other = (DeeplyNestedData) obj;
			return Objects.equals(parentData, other.parentData); 
		}
		
	}
	
	@Doc
	public static final class ContainerRevisionData extends Revision {

		@JsonCreator
		public ContainerRevisionData(@JsonProperty("id") String id) {
			super(id);
		}
		
	}
	
	@Doc
	public static final class ComponentRevisionData extends Revision {

		private final String container;
		private final String property;

		@JsonCreator
		public ComponentRevisionData(@JsonProperty("id") String id, @JsonProperty("container") String container, @JsonProperty("property") String property) {
			super(id);
			this.container = container;
			this.property = property;
		}
		
		@Override
		protected ObjectId getContainerId() {
			return ObjectId.of(ContainerRevisionData.class, container);
		}
		
		public String getContainer() {
			return container;
		}
		
		public String getProperty() {
			return property;
		}
		
	}
	
	@Doc(revisionHash = { "nested" })
	public static final class ObjectPropertyData extends Revision {

		private final ObjectItem nested;

		@JsonCreator
		public ObjectPropertyData(@JsonProperty("id") String id, @JsonProperty("nested") ObjectItem nested) {
			super(id);
			this.nested = nested;
		}
		
		public ObjectItem getNested() {
			return nested;
		}
		
	}
	
	@Doc(revisionHash = { "items" })
	public static final class ObjectListPropertyData extends Revision {

		private final List<ObjectItem> items;

		@JsonCreator
		public ObjectListPropertyData(@JsonProperty("id") String id, @JsonProperty("items") List<ObjectItem> items) {
			super(id);
			this.items = items;
		}
		
		public List<ObjectItem> getItems() {
			return items;
		}
		
	}
	
	@Doc(revisionHash = { "items" })
	public static final class ObjectSetPropertyData extends Revision {

		private final Set<ObjectItem> items;

		@JsonCreator
		public ObjectSetPropertyData(@JsonProperty("id") String id, @JsonProperty("items") Set<ObjectItem> items) {
			super(id);
			this.items = items;
		}
		
		public Set<ObjectItem> getItems() {
			return items;
		}
		
	}
	
	@Doc
	public static final class ObjectItem {
		
		private final String field1;
		private final String field2;
		
		@JsonCreator
		public ObjectItem(@JsonProperty("field1") String field1, @JsonProperty("field2") String field2) {
			this.field1 = field1;
			this.field2 = field2;
		}
		
		public String getField1() {
			return field1;
		}
		
		public String getField2() {
			return field2;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(field1, field2);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ObjectItem other = (ObjectItem) obj;
			return Objects.equals(field1, other.field1) && Objects.equals(field2, other.field2);
		}
		
		@Override
		public String toString() {
			return String.format("{ \"field1\": %s, \"field2\": %s }", field1, field2);
		}
		
	}
	
	@Doc(revisionHash = { "items" })
	public static final class ObjectUniqueListPropertyData extends Revision {

		private final List<ObjectUniqueItem> items;

		@JsonCreator
		public ObjectUniqueListPropertyData(@JsonProperty("id") String id, @JsonProperty("items") List<ObjectUniqueItem> items) {
			super(id);
			this.items = items;
		}
		
		public List<ObjectUniqueItem> getItems() {
			return items;
		}
		
	}
	
	@Doc
	public static final class ObjectUniqueItem {
	
		@ID
		private final String id;
		
		private final String field1;
		private final String field2;
		
		@JsonCreator
		public ObjectUniqueItem(
				@JsonProperty("id") String id,
				@JsonProperty("field1") String field1, 
				@JsonProperty("field2") String field2) {
			this.id = id;
			this.field1 = field1;
			this.field2 = field2;
		}
		
		public String getId() {
			return id;
		}
		
		public String getField1() {
			return field1;
		}
		
		public String getField2() {
			return field2;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(getId(), getField1(), getField2());
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ObjectUniqueItem other = (ObjectUniqueItem) obj;
			return Objects.equals(getId(), other.getId()) 
					&& Objects.equals(getField1(), other.getField1()) 
					&& Objects.equals(getField2(), other.getField2());
		}
		
		@Override
		public String toString() {
			return String.format("{ \"id\": %s, \"field1\": %s, \"field2\": %s }", getId(), getField1(), getField2());
		}
		
	}
	
}
