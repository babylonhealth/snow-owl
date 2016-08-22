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
package com.b2international.index.query;

import com.b2international.index.query.Query.AfterWhereBuilder;
import com.b2international.index.query.Query.QueryBuilder;

/**
 * @since 4.7
 */
class DefaultQueryBuilder<T> implements QueryBuilder<T>, AfterWhereBuilder<T> {

	private static final int DEFAULT_LIMIT = 50;

	private final Class<T> select;
	private final Class<?> from;
	private final Class<?> scope;
	
	private int offset = 0;
	private int limit = DEFAULT_LIMIT;
	private Expression where;
	private SortBy sortBy = SortBy.NONE;
	private boolean withScores = false;

	DefaultQueryBuilder(Class<T> select, Class<?> from, Class<?> scope) {
		this.select = select;
		this.from = from;
		this.scope = scope;
	}

	@Override
	public AfterWhereBuilder<T> offset(int offset) {
		this.offset = offset;
		return this;
	}

	@Override
	public AfterWhereBuilder<T> limit(int limit) {
		this.limit = limit;
		return this;
	}

	@Override
	public AfterWhereBuilder<T> where(Expression expression) {
		this.where = expression;
		return this;
	}

	@Override
	public AfterWhereBuilder<T> sortBy(SortBy sortBy) {
		this.sortBy = sortBy;
		return this;
	}
	
	@Override
	public AfterWhereBuilder<T> withScores(boolean withScores) {
		this.withScores = withScores;
		return this;
	}

	@Override
	public Query<T> build() {
		Query<T> query = new Query<T>();
		query.setSelect(select);
		query.setFrom(from);
		query.setParentType(scope);
		query.setWhere(where);
		query.setLimit(limit);
		query.setOffset(offset);
		query.setSortBy(sortBy);
		query.setWithScores(withScores);
		return query;
	}
}