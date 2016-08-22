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
package com.b2international.snowowl.snomed.datastore.id.reservations;

import java.util.Collection;
import java.util.Collections;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.ReservationRangeImpl;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.UniqueInStoreReservation;
import com.google.inject.Provider;

/**
 * @since 4.0
 */
public class Reservations {

	private Reservations() {
	}

	/**
	 * Creates a {@link Reservation} for the given single componentId.
	 * 
	 * @param componentId
	 * @return a {@link Reservation} instance for the given componentId.
	 */
	public static Reservation single(final String componentId) {
		final SnomedIdentifier id = SnomedIdentifiers.create(componentId);
		return new ReservationRangeImpl(id.getItemId(), id.getItemId(), id.getNamespace(), Collections.singleton(id.getComponentCategory()));
	}

	/**
	 * Creates a new {@link Reservation} for the given range spec. The returned {@link Reservation} may conflict with IDs defined in the given range.
	 * 
	 * @param itemIdMin
	 *            - the range's minimum value
	 * @param itemIdMax
	 *            - the range's maximum value
	 * @param namespace
	 *            - the namespace ID to use, may be <code>null</code> if it is an International SNOMED CT Identifier restriction
	 * @param components
	 *            - the compenent types affected, cannot be empty.
	 * @return
	 */
	public static Reservation range(final long itemIdMin, final long itemIdMax, final String namespace, final Collection<ComponentCategory> components) {
		return new ReservationRangeImpl(itemIdMin, itemIdMax, namespace, components);
	}

	/**
	 * Creates a {@link Reservation} instance to reserve all SNOMED CT Identifiers when generating new IDs.
	 * 
	 * @param bus - the current bus to send reservation check requests
	 * @return
	 */
	public static Reservation uniqueInStore(Provider<IEventBus> bus) {
		return new UniqueInStoreReservation(bus);
	}

}
