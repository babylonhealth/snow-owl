/*
 * Copyright 2011-2019 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.core.mrcm.io;

import static java.util.stream.Collectors.toList;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.core.domain.constraint.SnomedConstraint;
import com.b2international.snowowl.snomed.core.domain.constraint.SnomedConstraints;
import com.b2international.snowowl.snomed.core.mrcm.ConceptModelComponentRenderer;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

/**
 * Exporter to create a delimiter separated file for the MRCM rules in the system.
 * 
 * @since 4.6
 */
class CsvMrcmExporter {

	private static final Joiner TAB_JOINER = Joiner.on('\t').useForNull("");

	public void doExport(IEventBus bus, OutputStream stream, String branch) {
		final ConceptModelComponentRenderer renderer = new ConceptModelComponentRenderer(branch);

		final SnomedConstraints constraints = SnomedRequests.prepareSearchConstraint()
			.all()
			.build(SnomedDatastoreActivator.REPOSITORY_UUID, branch)
			.execute(bus)
			.getSync();

		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, Charsets.UTF_8)))) {
			
			writer.println(
				TAB_JOINER.join(
					"uuid",
					"effectiveTime",
					"author",
					"strength",
					"description",
					"validationMessage",
					"form",
					"domain",
					"predicate"
				)
			);
			
			for (SnomedConstraint constraint : constraints.stream().sorted((c1,c2) -> c1.getId().compareTo(c2.getId())).collect(toList())) {
				
				writer.print(
					TAB_JOINER.join(
						constraint.getId(),
						EffectiveTimes.format(constraint.getEffectiveTime()),
						constraint.getAuthor(),
						constraint.getStrength(),
						constraint.getDescription(),
						constraint.getValidationMessage(),
						constraint.getForm(),
						renderer.getHumanReadableRendering(constraint.getDomain(), Integer.MAX_VALUE),
						renderer.getHumanReadableRendering(constraint.getPredicate(), Integer.MAX_VALUE)
					)
				);
				
				writer.println();
				
			}
			
		}
	}

}
