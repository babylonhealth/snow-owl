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
package com.b2international.snowowl.core.domain;

import com.b2international.snowowl.core.branch.Branch;

/**
 * @since 4.5
 */
public class DelegatingBranchContext extends DelegatingRepositoryContext implements BranchContext {

	public DelegatingBranchContext(BranchContext context) {
		super(context);
	}

	@Override
	public Branch branch() {
		return getDelegate().branch();
	}
	
	@Override
	public String branchPath() {
		return getDelegate().branchPath();
	}
	
	@Override
	protected BranchContext getDelegate() {
		return (BranchContext) super.getDelegate();
	}
	
	public static DelegatingBranchContext.Builder<DelegatingBranchContext> basedOn(BranchContext context) {
		return new DelegatingServiceProvider.Builder<>(new DelegatingBranchContext(context));
	}

}
