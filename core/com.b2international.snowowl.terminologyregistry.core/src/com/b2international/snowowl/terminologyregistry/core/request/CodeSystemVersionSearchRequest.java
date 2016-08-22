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
package com.b2international.snowowl.terminologyregistry.core.request;

import java.io.IOException;

import com.b2international.commons.StringUtils;
import com.b2international.index.Hits;
import com.b2international.index.Searcher;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.query.Query;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.datastore.CodeSystemVersionEntry;
import com.b2international.snowowl.datastore.CodeSystemVersions;
import com.b2international.snowowl.datastore.request.RevisionSearchRequest;

/**
 * @since 4.7
 */
final class CodeSystemVersionSearchRequest extends RevisionSearchRequest<CodeSystemVersions> {

	private static final long serialVersionUID = 1L;

	private String codeSystemShortName;
	private String versionId;
	
	CodeSystemVersionSearchRequest() {
	}

	void setCodeSystemShortName(String codeSystemShortName) {
		this.codeSystemShortName = codeSystemShortName;
	}
	
	void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	@Override
	protected CodeSystemVersions doExecute(final BranchContext context) throws IOException {
		final ExpressionBuilder query = Expressions.builder();

		if (!StringUtils.isEmpty(codeSystemShortName)) {
			query.must(CodeSystemVersionEntry.Expressions.shortName(codeSystemShortName));
		}
		
		if (!StringUtils.isEmpty(versionId)) {
			query.must(CodeSystemVersionEntry.Expressions.versionId(versionId));
		}
		
		final Searcher searcher = context.service(Searcher.class);
		
		final Hits<CodeSystemVersionEntry> hits = searcher.search(Query.select(CodeSystemVersionEntry.class)
				.where(query.build())
				.offset(offset())
				.limit(limit())
				.build());
		
		return new CodeSystemVersions(hits.getHits(), offset(), limit(), hits.getTotal());
	}

	@Override
	protected Class<CodeSystemVersions> getReturnType() {
		return CodeSystemVersions.class;
	}

}
