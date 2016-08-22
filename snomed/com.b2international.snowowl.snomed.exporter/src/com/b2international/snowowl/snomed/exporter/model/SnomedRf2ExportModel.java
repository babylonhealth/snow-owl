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
package com.b2international.snowowl.snomed.exporter.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.net4j.util.StringUtil;

import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.common.ContentSubType;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSet;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSets;
import com.b2international.snowowl.snomed.datastore.SnomedMapSetSetting;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.datastore.services.ISnomedConceptNameProvider;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Model used in the SNOMED CT to RF1/RF2 export process.
 */
public final class SnomedRf2ExportModel extends SnomedExportModel {

	public static final String RELEASE_TYPE_PROPERTY = "releaseType";

	public static final String EXPORT_PATH_PROPERTY = "exportPath";

	/**
	 * Flag to indicate whether the export wizard is for export one single SNOMED CT reference set or other SNOMED CT components.
	 * <br>If {@code true} only one reference set is selected for export otherwise {@code false}.
	 */
	private final boolean singleRefSetExport;
	private final Set<String> refSetIds;
	private boolean coreComponentsToExport;
	private boolean refSetsToExport;
	private boolean exportToRf1;
	private boolean extendedDescriptionTypesForRf1;
	private boolean includeUnpublised;

	private Date deltaExportStartEffectiveTime;
	private Date deltaExportEndEffectiveTime;
	private ContentSubType releaseType;
	private Set<SnomedMapSetSetting> settings;
	private Set<String> modulesToExport;

	private String namespace;
	private final IBranchPath branch;
	private String userId;
	private String unsetEffectiveTimeLabel;

	/**
	 * Creates a new RF2 export model for exporting all core components and reference sets on a given branch 
	 * with the desired {@link ContentSubType release type}.
	 * @param contentSubType the desired release type.
	 * @param branchPath the branch path for the export.
	 * @return a new model instance 
	 */
	public static SnomedRf2ExportModel createExportModelWithAllRefSets(final ContentSubType contentSubType, 
			final IBranchPath branchPath) {
		
		checkNotNull(contentSubType, "contentSubType");
		checkNotNull(branchPath, "branchPath");
		
		final SnomedRf2ExportModel model = new SnomedRf2ExportModel(branchPath);
		model.releaseType = contentSubType;
		
		final SnomedReferenceSets referenceSets = SnomedRequests.prepareSearchRefSet()
			.all()
			.build(branchPath.getPath())
			.execute(ApplicationContext.getServiceForClass(IEventBus.class))
			.getSync();
		for (SnomedReferenceSet refSet : referenceSets) {
			model.updateRefSet(refSet.getId());
		}
		
		return model;
	}
	
	/**
	 * Creates a new RF2 export model for exporting one single reference set on the given branch
	 * with the desired {@link ContentSubType release type}. 
	 * @param refSetId the reference set ID to export.
	 * @param contentSubType the desired release type.
	 * @param branchPath the branch path for the export.
	 * @return a new export model instance for a single reference set export.
	 */
	public static SnomedRf2ExportModel createExportModelForSingleRefSet(final String refSetId, 
			final ContentSubType contentSubType, final IBranchPath branchPath) {
		checkNotNull(contentSubType, "contentSubType");
		checkNotNull(refSetId, "refSetId");
		
		final SnomedRf2ExportModel model = new SnomedRf2ExportModel(branchPath, refSetId);
		model.releaseType = contentSubType;
		return model;
	}
	
	/**
	 * Creates a new model instance for core export.
	 */
	public SnomedRf2ExportModel(IBranchPath branch) {
		this(branch, null);
	}

	/**
	 * Creates a new model instance.
	 * @param refSetId
	 */
	public SnomedRf2ExportModel(final IBranchPath branch, @Nullable final String refSetId) {
		super();
		refSetIds = StringUtils.isEmpty(refSetId) ? Sets.<String> newHashSet() : Sets.newHashSet(refSetId);
		singleRefSetExport = !refSetIds.isEmpty();
		coreComponentsToExport = !singleRefSetExport;
		releaseType = ContentSubType.SNAPSHOT;
		settings = Sets.newHashSet();
		refSetsToExport = true;
		modulesToExport = Sets.newHashSet();
		this.branch = checkNotNull(branch, "branch");
		userId = ApplicationContext.getInstance().getService(ICDOConnectionManager.class).getUserId();
		unsetEffectiveTimeLabel = "";
		setExportPath(initExportPath());
	}

	public Set<String> getRefSetIds() {
		return refSetIds;
	}

	public void updateRefSet(final String refSetId) {
		if (refSetIds.contains(refSetId)) {
			refSetIds.remove(refSetId);
		} else {
			refSetIds.add(refSetId);
		}
	}

	public ContentSubType getReleaseType() {
		return releaseType;
	}

	public void setReleaseType(ContentSubType releaseType) {
		this.releaseType = releaseType;
	}

	/**
	 * Returns {@code true} if the wizard should perform only one SNOMED CT reference set export. Otherwise it returns with {@code false}.
	 * @return the value of the {@link #singleRefSetExport} flag.
	 * @see #singleRefSetExport
	 */
	public boolean isSingleRefSetExport() {
		return singleRefSetExport;
	}

	public boolean isCoreComponentsToExport() {
		return coreComponentsToExport;
	}

	public void setCoreComponentsToExport(final boolean coreComponentsToExport) {
		this.coreComponentsToExport = coreComponentsToExport;
	}

	public boolean isRefSetsToExport() {
		return refSetsToExport;
	}

	public void setRefSetsToExport(final boolean refSetsToExport) {
		this.refSetsToExport = refSetsToExport;
	}

	private String initExportPath() {
		final String token;
		if (!singleRefSetExport) {
			token = new StringBuilder().append("SnomedCT_Release_INT_").append(Dates.formatByHostTimeZone(new Date(), "yyyyMMdd-HHmm")).toString();
		} else {
			token = StringUtil.capAll(ApplicationContext.getServiceForClass(ISnomedConceptNameProvider.class).getComponentLabel(branch, Iterables.getOnlyElement(refSetIds)));
		}
		final StringBuilder sb = new StringBuilder();
		return sb.append(System.getProperty("user.home")).append(File.separatorChar).append(token).append(".zip").toString();
	}

	public boolean isExportToRf1() {
		return exportToRf1;
	}

	public void setExportToRf1(boolean exportToRf1) {
		this.exportToRf1 = exportToRf1;
	}

	public Set<SnomedMapSetSetting> getSettings() {
		return settings;
	}

	public void setSettings(Set<SnomedMapSetSetting> settings) {
		this.settings = settings;
	}

	public boolean isExtendedDescriptionTypesForRf1() {
		return extendedDescriptionTypesForRf1;
	}

	public void setExtendedDescriptionTypesForRf1(boolean extendedDescriptionTypesForRf1) {
		this.extendedDescriptionTypesForRf1 = extendedDescriptionTypesForRf1;
	}

	public Set<String> getModulesToExport() {
		return modulesToExport;
	}

	public Date getDeltaExportStartEffectiveTime() {
		return deltaExportStartEffectiveTime;
	}

	public void setDeltaExportStartEffectiveTime(Date deltaExportStartEffectiveTime) {
		this.deltaExportStartEffectiveTime = deltaExportStartEffectiveTime;
	}

	public Date getDeltaExportEndEffectiveTime() {
		return deltaExportEndEffectiveTime;
	}

	public void setDeltaExportEndEffectiveTime(Date deltaExportEndEffectiveTime) {
		this.deltaExportEndEffectiveTime = deltaExportEndEffectiveTime;
	}

	public boolean includeUnpublised() {
		return includeUnpublised;
	}

	public void setIncludeUnpublised(boolean includeUnpublised) {
		this.includeUnpublised = includeUnpublised;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getNamespace() {
		return namespace;
	}

	public IBranchPath getClientBranch() {
		return branch;
	}

	public String getUserId() {
		return userId;
	}

	public String getUnsetEffectiveTimeLabel() {
		return unsetEffectiveTimeLabel;
	}
	
	public void setUnsetEffectiveTimeLabel(final String unsetEffectiveTimeLabel) {
		this.unsetEffectiveTimeLabel = unsetEffectiveTimeLabel;
	}
}
