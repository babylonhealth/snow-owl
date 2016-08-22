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
package com.b2international.snowowl.snomed.exporter.server;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.snomed.common.ContentSubType;
import com.b2international.snowowl.snomed.exporter.server.rf1.Id2Rf1PropertyMapper;

/**
 * Export context for the SNOMED CT export process.
 */
public class SnomedExportContextImpl implements SnomedExportContext {

	private final IBranchPath currentBranchPath;
	private final ContentSubType contentSubType;
	private final String unsetEffectiveTimeLabel;

	private final Date deltaExportStartEffectiveTime;
	private final Date deltaExportEndEffectiveTime;
	
	// FIXME always false, clients should specify the value
	private boolean includeMapTargetDescription = false; 
	
	//the modules to export
	private Set<String> moduleIds;
	
	private Id2Rf1PropertyMapper id2Rf1PropertyMapper;
	private ExportFormat exportFormat;
	
	public SnomedExportContextImpl(final ExportFormat exportFormat, 
			final IBranchPath currentBranchPath,
			final ContentSubType contentSubType,
			final String unsetEffectiveTimeLabel,
			@Nullable final Date deltaExportStartEffectiveTime, 
			@Nullable final Date deltaExportEndEffectiveTime,
			final Set<String> moduleIds,
			final Id2Rf1PropertyMapper id2Rf1PropertyMapper) {

		this.exportFormat = checkNotNull(exportFormat, "exportFormat");
		this.currentBranchPath = checkNotNull(currentBranchPath, "currentBranchPath");
		this.contentSubType = checkNotNull(contentSubType, "contentSubType");
		this.unsetEffectiveTimeLabel = checkNotNull(unsetEffectiveTimeLabel, "unsetEffectiveTimeLabel");
		this.deltaExportStartEffectiveTime = deltaExportStartEffectiveTime;
		this.deltaExportEndEffectiveTime = deltaExportEndEffectiveTime;
		this.moduleIds = moduleIds;
		this.id2Rf1PropertyMapper = id2Rf1PropertyMapper;
	}
	
	@Override
	public ExportFormat getExportFormat() {
		return exportFormat;
	}
	
	@Override
	public void setExportFormat(ExportFormat exportFormat) {
		this.exportFormat = exportFormat;
	}

	@Override
	public IBranchPath getCurrentBranchPath() {
		return currentBranchPath;
	}

	@Override
	public ContentSubType getContentSubType() {
		return contentSubType;
	}

	@Override
	public String getUnsetEffectiveTimeLabel() {
		return unsetEffectiveTimeLabel;
	}
	
	@Override
	@Nullable public Date getDeltaExportStartEffectiveTime() {
		return deltaExportStartEffectiveTime;
	}

	@Override
	@Nullable public Date getDeltaExportEndEffectiveTime() {
		return deltaExportEndEffectiveTime;
	}
	
	@Override
	public boolean includeMapTargetDescription() {
		return includeMapTargetDescription;
	}
	
	@Override
	public Set<String> getModulesToExport() {
		return moduleIds;
	}

	@Override
	public Id2Rf1PropertyMapper getId2Rf1PropertyMapper() {
		return id2Rf1PropertyMapper;
	}

}
