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
package com.b2international.snowowl.snomed.datastore.id;

import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.index.Index;
import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.setup.DefaultBootstrapFragment;
import com.b2international.snowowl.core.setup.Environment;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedIdentifierConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedIdentifierConfiguration.IdGenerationStrategy;
import com.b2international.snowowl.snomed.datastore.id.cis.CisSnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.gen.ItemIdGenerationStrategy;
import com.b2international.snowowl.snomed.datastore.id.memory.DefaultSnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.reservations.ISnomedIdentiferReservationService;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.SnomedIdentifierReservationServiceImpl;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.UniqueInStoreReservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

/**
 * @since 4.5
 */
public class SnomedIdentifierBootstrap extends DefaultBootstrapFragment {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedIdentifierBootstrap.class);

	private static final String STORE_RESERVATIONS = "store_reservations";

	@Override
	public void init(final SnowOwlConfiguration configuration, final Environment env) throws Exception {
		checkIdGenerationSource(configuration);

		final ISnomedIdentiferReservationService reservationService = new SnomedIdentifierReservationServiceImpl();
		env.services().registerService(ISnomedIdentiferReservationService.class, reservationService);

		registerTerminologyBrowser(env, reservationService);
	}

	@Override
	public void run(SnowOwlConfiguration configuration, Environment env, IProgressMonitor monitor) throws Exception {
		if (env.isServer() || env.isEmbedded()) {
			final ISnomedIdentiferReservationService reservationService = env.service(ISnomedIdentiferReservationService.class);
			final SnomedIdentifierConfiguration conf = configuration.getModuleConfig(SnomedCoreConfiguration.class).getIds();
			registerSnomedIdentifierService(conf, env, reservationService);
		}
	}

	private void registerTerminologyBrowser(final Environment env, final ISnomedIdentiferReservationService reservationService) {
		final UniqueInStoreReservation storeReservation = new UniqueInStoreReservation(env.provider(IEventBus.class));
		reservationService.create(STORE_RESERVATIONS, storeReservation);
	}

	private void checkIdGenerationSource(final SnowOwlConfiguration configuration) {
		final SnomedIdentifierConfiguration conf = configuration.getModuleConfig(SnomedIdentifierConfiguration.class);
		final IdGenerationStrategy idGenerationSource = conf.getStrategy();

		if (null == idGenerationSource) {
			throw new IllegalStateException("ID generation source is not configured.");
		}
	}

	private void registerSnomedIdentifierService(final SnomedIdentifierConfiguration conf, final Environment env,
			final ISnomedIdentiferReservationService reservationService) {
		ISnomedIdentifierService identifierService = null;

		switch (conf.getStrategy()) {
		case EMBEDDED:
			final Provider<Index> indexProvider = new Provider<Index>() {
				@Override
				public Index get() {
					return env.service(RepositoryManager.class).get(SnomedDatastoreActivator.REPOSITORY_UUID).service(Index.class);
				}
			};
			LOGGER.info("Snow Owl is configured to use embedded identifier service.");
			identifierService = new DefaultSnomedIdentifierService(indexProvider, ItemIdGenerationStrategy.RANDOM, reservationService, conf);
			break;
		case CIS:
			final ObjectMapper mapper = new ObjectMapper();
			LOGGER.info("Snow Owl is configured to use CIS based identifier service.");
			identifierService = new CisSnomedIdentifierService(conf, reservationService, mapper);
			break;
		default:
			throw new IllegalStateException(String.format("Unknown ID generation source configured: %s. ", conf.getStrategy()));
		}

		env.services().registerService(ISnomedIdentifierService.class, identifierService);
	}

}
