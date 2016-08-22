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
package com.b2international.index.lucene;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import com.b2international.collections.PrimitiveLists;
import com.b2international.collections.PrimitiveSets;
import com.b2international.collections.longs.LongCollection;
import com.b2international.collections.longs.LongIterator;
import com.b2international.collections.longs.LongList;
import com.b2international.collections.longs.LongSet;

/**
 * @since 4.3
 */
public class LongIndexField extends IndexFieldBase<Long> implements LongCollectionIndexField {

	public LongIndexField(String fieldName) {
		this(fieldName, true);
	}
	
	public LongIndexField(String fieldName, boolean stored) {
		super(fieldName, stored);
	}

	@Override
	protected IndexableField toField(Long value) {
		return new LongField(fieldName(), value, isStored());
	}

	@Override
	protected BytesRef toBytesRef(Long value) {
		return _toBytesRef(value);
	}
	
	@Override
	protected Type getSortFieldType() {
		return Type.LONG;
	}
	
	@Override
	public Long getValue(IndexableField field) {
		final Number num = field.numericValue();
		checkNotNull(num, "Cannot get numeric value from field '%s'");
		return num.longValue();
	}
	
	@Override
	public final LongSet getValueAsLongSet(Document doc) {
		final IndexableField[] fields = getFields(doc);
		final LongSet longIds = PrimitiveSets.newLongOpenHashSet(fields.length + 1);
		addIdsToLongCollection(fields, longIds);
		return longIds;
	}

	@Override
	public final LongList getValueAsLongList(Document doc) {
		final IndexableField[] fields = getFields(doc);
		final LongList longIds = PrimitiveLists.newLongArrayList(fields.length + 1);
		addIdsToLongCollection(fields, longIds);
		return longIds;
	}
	
	private void addIdsToLongCollection(final IndexableField[] fields, final LongCollection longIds) {
		for (final IndexableField field : fields) {
			longIds.add(getValue(field));
		}
	}
	
	public final Filter createTermsFilter(LongCollection values) {
		if (values == null || values.isEmpty()) {
			return new MatchNoDocsFilter(); 
		} else {
			// Converted BytesRef values should be unique, but TermsFilter requires a writable list for sorting
			final LongSet uniqueValues = PrimitiveSets.newLongOpenHashSet(values);

			final List<BytesRef> uniqueBytesRefs = newArrayList();
			final LongIterator iter = uniqueValues.iterator();
			while (iter.hasNext()) {
				uniqueBytesRefs.add(toBytesRef(iter.next()));
			}
			
			return new TermsFilter(fieldName(), uniqueBytesRefs);
		}
	}

	/**
	 * @param value
	 * @return
	 * @deprecated - if possible don't use this API, use {@link Fields} or {@link #LongIndexField(String) constructor} instead
	 */
	public static BytesRef _toBytesRef(Long value) {
		final BytesRefBuilder bytesRef = new BytesRefBuilder();
		NumericUtils.longToPrefixCoded(value, 0, bytesRef);
		return bytesRef.toBytesRef();
	}

}
