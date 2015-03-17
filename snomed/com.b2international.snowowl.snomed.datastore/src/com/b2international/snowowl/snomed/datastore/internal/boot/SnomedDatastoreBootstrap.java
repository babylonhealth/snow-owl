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
package com.b2international.snowowl.snomed.datastore.internal.boot;

import org.eclipse.core.runtime.IProgressMonitor;

import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.setup.BootstrapFragment;
import com.b2international.snowowl.core.setup.Environment;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.reservations.ISnomedIdentiferReservationService;
import com.b2international.snowowl.snomed.datastore.internal.id.SnomedIdentifierServiceImpl;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.SnomedIdentifierReservationServiceImpl;

/**
 * @since 4.0
 */
public class SnomedDatastoreBootstrap implements BootstrapFragment {

	@Override
	public void init(SnowOwlConfiguration configuration, Environment env) throws Exception {
		final ISnomedIdentiferReservationService reservationService = new SnomedIdentifierReservationServiceImpl();
		final ISnomedIdentifierService idService = new SnomedIdentifierServiceImpl(reservationService);
		env.services().registerService(ISnomedIdentiferReservationService.class, reservationService);
		env.services().registerService(ISnomedIdentifierService.class, idService);
	}

	@Override
	public void run(SnowOwlConfiguration configuration, Environment env, IProgressMonitor monitor) throws Exception {
	}

}
