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

import java.util.Collection;
import java.util.List;

import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.collect.Lists;

/**
 * Model used in the reference set DSV export process.
 */
public class SnomedRefSetDSVExportModel extends SnomedExportModel {

	private String refSetId;
	private String refSetLabel;
	private SnomedRefSetType refSetType;
	private int conceptSize;

	private boolean descriptionIdExpected;
	private boolean relationshipTargetExpected;
	private List<AbstractSnomedDsvExportItem> exportItems = Lists.newArrayList();
	private long languageConfigurationId;
	private String delimiter;
	private int branchID;
	private long branchBase;
	// used for simple and complex map type refsets
	private String branchPath;
	
	private String userId;

	public SnomedRefSetDSVExportModel() {
		super();
	}
	
	public int getConceptSize() {
		return conceptSize;
	}

	public void setConceptSize(int conceptSize) {
		this.conceptSize = conceptSize;
	}

	public SnomedRefSetType getRefSetType() {
		return refSetType;
	}

	public String getRefSetId() {
		return refSetId;
	}
	
	public void setRefSetId(String refSetId) {
		this.refSetId = refSetId;
	}
	
	public String getRefSetLabel() {
		return refSetLabel;
	}
	
	public Long getLanguageConfigurationId() {
		return languageConfigurationId;
	}

	public void setLanguageConfigurationId(Long languageConfigurationId) {
		this.languageConfigurationId = languageConfigurationId;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public int getBranchID() {
		return branchID;
	}

	public void setBranchID(int branchID) {
		this.branchID = branchID;
	}

	public long getBranchBase() {
		return branchBase;
	}

	public void setBranchBase(long branchBase) {
		this.branchBase = branchBase;
	}

	public boolean isDescriptionIdExpected() {
		return descriptionIdExpected;
	}

	public void setDescriptionIdExpected(boolean descriptionIdExpected) {
		this.descriptionIdExpected = descriptionIdExpected;
	}

	public List<AbstractSnomedDsvExportItem> getExportItems() {
		return exportItems;
	}

	public void clearExportItems() {
		exportItems.clear();
	}

	public void addExportItem(AbstractSnomedDsvExportItem exportItem) {
		exportItems.add(exportItem);
	}
	
	public void addExportItems(Collection<AbstractSnomedDsvExportItem> items) {
		exportItems.addAll(items);
	}

	public boolean isRelationshipTargetExpected() {
		return relationshipTargetExpected;
	}

	public void setRelationshipTargetExpected(boolean relationshipTargetExpected) {
		this.relationshipTargetExpected = relationshipTargetExpected;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}
	
	public String getUserId() {
		return userId;
	}
}