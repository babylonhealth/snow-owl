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
package com.b2international.snowowl.snomed.reasoner.server.changeprocessor;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.datastore.ICDOChangeProcessor;
import com.b2international.snowowl.datastore.server.CDOChangeProcessorFactory;

/**
 * CDO change processor factory responsible to create {@link SnomedReasonerChangeProcessor change processors} for SNOMED CT ontologies.
 * 
 */
public class SnomedReasonerChangeProcessorFactory implements CDOChangeProcessorFactory {

	private static final String FACTORY_NAME = "SNOMED CT OWL change processor factory";

	/* 
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.CDOChangeProcessorFactory#getFactoryName()
	 */
	@Override
	public String getFactoryName() {
		return FACTORY_NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.server.CDOChangeProcessorFactory#createChangeProcessor(com.b2international.snowowl.core.api.IBranchPath, boolean)
	 */
	@Override
	public ICDOChangeProcessor createChangeProcessor(IBranchPath branchPath, final boolean canCopyThreadLocal) throws SnowowlServiceException {
		return new SnomedReasonerChangeProcessor(branchPath);
	}
}