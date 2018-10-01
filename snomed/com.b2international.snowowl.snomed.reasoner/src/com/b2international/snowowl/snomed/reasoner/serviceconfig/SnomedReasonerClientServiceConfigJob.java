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
package com.b2international.snowowl.snomed.reasoner.serviceconfig;

import com.b2international.snowowl.datastore.serviceconfig.AbstractClientServiceConfigJob;
import com.b2international.snowowl.snomed.reasoner.SnomedReasonerActivator;
import com.b2international.snowowl.snomed.reasoner.classification.SnomedReasonerService;

/**
 * 
 */
public class SnomedReasonerClientServiceConfigJob extends AbstractClientServiceConfigJob<SnomedReasonerService> {

	private static final String JOB_NAME = "Snomed reasoner client service configuration...";
	
	public SnomedReasonerClientServiceConfigJob() {
		super(JOB_NAME, SnomedReasonerActivator.PLUGIN_ID);
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.ClientServiceConfigJob#getBranchAwareClass()
	 */
	@Override
	protected Class<SnomedReasonerService> getServiceClass() {
		return SnomedReasonerService.class;
	}
}