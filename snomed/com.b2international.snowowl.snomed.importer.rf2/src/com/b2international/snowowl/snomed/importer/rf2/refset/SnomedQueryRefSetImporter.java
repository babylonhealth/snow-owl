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
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.importer.rf2.csv.QueryRefSetRow;
import com.b2international.snowowl.snomed.importer.rf2.csv.cellprocessor.ParseUuid;
import com.b2international.snowowl.snomed.importer.rf2.model.ComponentImportType;
import com.b2international.snowowl.snomed.importer.rf2.model.IndexConfiguration;
import com.b2international.snowowl.snomed.importer.rf2.model.SnomedImportConfiguration;
import com.b2international.snowowl.snomed.importer.rf2.model.SnomedImportContext;
import com.b2international.snowowl.snomed.snomedrefset.SnomedQueryRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetFactory;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class SnomedQueryRefSetImporter extends AbstractSnomedRefSetImporter<QueryRefSetRow, SnomedQueryRefSetMember> {
	
	private static final Map<String, CellProcessor> CELL_PROCESSOR_MAPPING = ImmutableMap.<String, CellProcessor>builder()
			.put(QueryRefSetRow.PROP_UUID, new ParseUuid())
			.put(QueryRefSetRow.PROP_EFFECTIVE_TIME, createEffectiveTimeCellProcessor())
			.put(QueryRefSetRow.PROP_ACTIVE, new ParseBool("1", "0"))
			.put(QueryRefSetRow.PROP_MODULE_ID, NullObjectPattern.INSTANCE)
			.put(QueryRefSetRow.PROP_REF_SET_ID, NullObjectPattern.INSTANCE)
			.put(QueryRefSetRow.PROP_REFERENCED_COMPONENT_ID, NullObjectPattern.INSTANCE)
			.put(QueryRefSetRow.PROP_QUERY, NullObjectPattern.INSTANCE)
			.build();
	
	public static final List<IndexConfiguration> INDEXES = ImmutableList.<IndexConfiguration>builder()
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER_IDX1000", "SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER", "CDO_CREATED"))
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER_IDX1001", "SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER", "CDO_CONTAINER", "CDO_BRANCH", "CDO_VERSION"))
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER_IDX1002", "SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER", "REFERENCEDCOMPONENTID/*!(255)*/", "CDO_BRANCH", "CDO_VERSION"))
			.add(new IndexConfiguration("SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER_IDX1003", "SNOMEDREFSET_SNOMEDQUERYREFSETMEMBER", "UUID/*!(255)*/", "CDO_BRANCH", "CDO_VERSION"))
			.build();
	
	private static final SnomedImportConfiguration<QueryRefSetRow> IMPORT_CONFIGURATION = new SnomedImportConfiguration<QueryRefSetRow>(
			ComponentImportType.QUERY_TYPE_REFSET, 
			CELL_PROCESSOR_MAPPING, 
			QueryRefSetRow.class, 
			SnomedRf2Headers.QUERY_TYPE_HEADER,
			INDEXES);

	public SnomedQueryRefSetImporter(final SnomedImportContext importContext, final InputStream releaseFileStream, final String releaseFileIdentifier) {
		super(IMPORT_CONFIGURATION, importContext, releaseFileStream, releaseFileIdentifier);
	}

	@Override
	protected void initRefSet(final SnomedRefSet refSet, final String referencedComponentId) {
		refSet.setReferencedComponentType(SnomedTerminologyComponentConstants.REFSET_NUMBER);
	}

	@Override
	protected SnomedRefSetType getRefSetType() {
		return SnomedRefSetType.QUERY;
	}

	@Override
	protected SnomedQueryRefSetMember doImportRow(final QueryRefSetRow currentRow) {

		final SnomedQueryRefSetMember editedMember = (SnomedQueryRefSetMember) getOrCreateMember(currentRow.getUuid());
		
		if (skipCurrentRow(currentRow, editedMember)) {
			getLogger().warn("Not importing query type reference set member '{}' with effective time '{}'; it should have been filtered from the input file.",
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
		editedMember.setQuery(currentRow.getQuery());
		
		return editedMember;
	}

	@Override
	protected String getIdentifierParentConceptId(final String refSetId) {
		return Concepts.REFSET_QUERY_SPECIFICATION_TYPE;
	}

	@Override
	protected SnomedQueryRefSetMember createRefSetMember() {
		return SnomedRefSetFactory.eINSTANCE.createSnomedQueryRefSetMember(); 
	}
}