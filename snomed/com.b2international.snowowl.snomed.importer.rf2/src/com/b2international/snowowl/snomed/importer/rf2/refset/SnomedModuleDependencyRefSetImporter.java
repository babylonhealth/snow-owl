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
package com.b2international.snowowl.snomed.importer.rf2.refset;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.supercsv.cellprocessor.NullObjectPattern;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ift.CellProcessor;

import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.importer.rf2.csv.ModuleDependencyRefSetRow;
import com.b2international.snowowl.snomed.importer.rf2.csv.cellprocessor.ParseUuid;
import com.b2international.snowowl.snomed.importer.rf2.model.ComponentImportType;
import com.b2international.snowowl.snomed.importer.rf2.model.IndexConfiguration;
import com.b2international.snowowl.snomed.importer.rf2.model.SnomedImportConfiguration;
import com.b2international.snowowl.snomed.importer.rf2.model.SnomedImportContext;
import com.b2international.snowowl.snomed.snomedrefset.SnomedModuleDependencyRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetFactory;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @since Snow&nbsp;Owl 2.9
 */
public class SnomedModuleDependencyRefSetImporter extends AbstractSnomedRefSetImporter<ModuleDependencyRefSetRow, SnomedModuleDependencyRefSetMember>{
	
	private static final Map<String, CellProcessor> CELL_PROCESSOR_MAPPING = ImmutableMap.<String, CellProcessor>builder()
			.put(ModuleDependencyRefSetRow.PROP_UUID, new ParseUuid())
			.put(ModuleDependencyRefSetRow.PROP_EFFECTIVE_TIME, createEffectiveTimeCellProcessor())
			.put(ModuleDependencyRefSetRow.PROP_ACTIVE, new ParseBool("1", "0"))
			.put(ModuleDependencyRefSetRow.PROP_MODULE_ID, NullObjectPattern.INSTANCE)
			.put(ModuleDependencyRefSetRow.PROP_REF_SET_ID, NullObjectPattern.INSTANCE)
			.put(ModuleDependencyRefSetRow.PROP_REFERENCED_COMPONENT_ID, NullObjectPattern.INSTANCE)
			.put(ModuleDependencyRefSetRow.PROP_SOURCE_EFFECTIVE_TIME, createEffectiveTimeCellProcessor())
			.put(ModuleDependencyRefSetRow.PROP_TARGET_EFFECTIVE_TIME, createEffectiveTimeCellProcessor())
			.build();
	
	public static final List<IndexConfiguration> INDEXES = ImmutableList.<IndexConfiguration>builder()
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER_IDX1000", "SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER", "CDO_CREATED"))
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER_IDX1001", "SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER", "CDO_CONTAINER", "CDO_BRANCH", "CDO_VERSION"))
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER_IDX1002", "SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER", "REFERENCEDCOMPONENTID/*!(255)*/", "CDO_BRANCH", "CDO_VERSION"))
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER_IDX1003", "SNOMEDREFSET_SNOMEDMODULEDEPENDENCYREFSETMEMBER", "UUID/*!(255)*/", "CDO_BRANCH", "CDO_VERSION"))
			.build();

	private static final SnomedImportConfiguration<ModuleDependencyRefSetRow> IMPORT_CONFIGURATTION = new SnomedImportConfiguration<ModuleDependencyRefSetRow>(
			ComponentImportType.MODULE_DEPENDENCY_REFSET,
			CELL_PROCESSOR_MAPPING,
			ModuleDependencyRefSetRow.class,
			SnomedRf2Headers.MODULE_DEPENDENCY_HEADER,
			INDEXES);
	
	public SnomedModuleDependencyRefSetImporter(final SnomedImportContext importContext, final InputStream releaseFileStream, final String releaseFileIdentifier) {
		super(IMPORT_CONFIGURATTION, importContext, releaseFileStream, releaseFileIdentifier);
	}
	
	@Override
	protected SnomedModuleDependencyRefSetMember doImportRow(ModuleDependencyRefSetRow currentRow) {
		final SnomedModuleDependencyRefSetMember editedMember = getOrCreateMember(currentRow.getUuid());
		
		if (skipCurrentRow(currentRow, editedMember)) {
			getLogger().warn("Not importing module dependency reference set member '{}' with effective time '{}'; it should have been filtered from the input file.",
					currentRow.getUuid(), 
					EffectiveTimes.format(currentRow.getEffectiveTime(), DateFormats.SHORT));
			
			return null;
		}
		
		if (currentRow.getEffectiveTime() != null) {
			editedMember.setEffectiveTime(currentRow.getEffectiveTime());
			editedMember.setReleased(true);
		} else {
			editedMember.unsetEffectiveTime();
		}

		editedMember.setRefSet(getOrCreateRefSet(currentRow.getRefSetId(), currentRow.getReferencedComponentId()));
		editedMember.setActive(currentRow.isActive());
		editedMember.setModuleId(currentRow.getModuleId());
		editedMember.setReferencedComponentId(currentRow.getReferencedComponentId());
		editedMember.setSourceEffectiveTime(currentRow.getSourceEffectiveTime());
		editedMember.setTargetEffectiveTime(currentRow.getTargetEffectiveTime());
		
		return editedMember;
	}

	@Override
	protected String getIdentifierParentConceptId(String refSetId) {
		return Concepts.REFSET_MODULE_DEPENDENCY_TYPE;
	}

	@Override
	protected SnomedModuleDependencyRefSetMember createRefSetMember() {
		return SnomedRefSetFactory.eINSTANCE.createSnomedModuleDependencyRefSetMember();
	}

	@Override
	protected SnomedRefSetType getRefSetType() {
		return SnomedRefSetType.MODULE_DEPENDENCY;
	}
	
}