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
package com.b2international.snowowl.snomed.datastore.internal.id.reservations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.b2international.index.Index;
import com.b2international.index.Indexes;
import com.b2international.index.mapping.Mappings;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.datastore.config.SnomedIdentifierConfiguration;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.id.cis.SctId;
import com.b2international.snowowl.snomed.datastore.id.gen.ItemIdGenerationStrategy;
import com.b2international.snowowl.snomed.datastore.id.memory.DefaultSnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.reservations.Reservation;
import com.b2international.snowowl.snomed.datastore.id.reservations.Reservations;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.util.Providers;

/**
 * @since 4.0
 */
public class ReservationImplTest {

	@Test
	public void whenReservingSingleID_ThenItShouldConflictWithThatIDOnly() throws Exception {
		final Reservation single = Reservations.single(Concepts.ROOT_CONCEPT);
		assertTrue(single.includes(SnomedIdentifiers.create(Concepts.ROOT_CONCEPT)));
		assertFalse(single.includes(SnomedIdentifiers.create(Concepts.FULLY_DEFINED)));
		assertFalse(single.includes(SnomedIdentifiers.create(Concepts.ADDITIONAL_RELATIONSHIP)));
	}
	
	@Test
	public void whenReservingRangeOfIDs_ThenItShouldConflictWithAllIDsInThatRangeIncludingBoundaries() throws Exception {
		final Index store = Indexes.createIndex(UUID.randomUUID().toString(), new ObjectMapper(), new Mappings(SctId.class));
		store.admin().create();
		final ISnomedIdentifierService identifierService = new DefaultSnomedIdentifierService(Providers.of(store), new ItemIdGenerationStrategy() {
			int counter = 200;
			@Override
			public String generateItemId() {
				return String.valueOf(counter++);
			}
		}, new SnomedIdentifierReservationServiceImpl(), new SnomedIdentifierConfiguration());
		final SnomedIdentifiers snomedIdentifiers = new SnomedIdentifiers(identifierService);
		final Set<ComponentCategory> components = Collections.singleton(ComponentCategory.CONCEPT);
		final Reservation range = Reservations.range(200, 300, null, components);
		for (int i = 200; i <= 300; i++) {
			final String id = snomedIdentifiers.generate(null, ComponentCategory.CONCEPT);
			final SnomedIdentifier identifier = SnomedIdentifiers.create(id);
			assertTrue(range.includes(identifier));
		}
		store.admin().delete();
	}
	
}
