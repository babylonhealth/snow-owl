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
package com.b2international.snowowl.datastore.server.index;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import com.b2international.commons.CompareUtils;
import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.browser.SuperTypeIdProvider;
import com.b2international.snowowl.core.api.index.CommonIndexConstants;
import com.b2international.snowowl.datastore.ICodeSystem;
import com.b2international.snowowl.datastore.ICodeSystemVersion;
import com.b2international.snowowl.datastore.InternalTerminologyRegistryService;
import com.b2international.snowowl.datastore.index.DocumentWithScore;
import com.b2international.snowowl.datastore.index.FolderIndexEntry;
import com.b2international.snowowl.datastore.index.IFolderIndexService;
import com.b2international.snowowl.datastore.index.IndexQueryBuilder;
import com.b2international.snowowl.datastore.index.IndexUtils;
import com.b2international.snowowl.datastore.index.ParentFolderAwareIndexEntry;
import com.b2international.snowowl.datastore.index.field.ComponentIdField;
import com.b2international.snowowl.datastore.index.field.ComponentIdStringField;
import com.b2international.snowowl.datastore.index.field.ComponentParentField;
import com.b2international.snowowl.datastore.index.field.ComponentParentStringField;
import com.b2international.snowowl.datastore.index.field.ComponentStorageKeyField;
import com.b2international.snowowl.datastore.index.field.ComponentTypeField;
import com.b2international.snowowl.datastore.server.TerminologyRegistryServiceWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Server-side implementation of folder index service.
 * 
 */
public abstract class FolderIndexServerService extends FSIndexServerService<ParentFolderAwareIndexEntry> implements IFolderIndexService, InternalTerminologyRegistryService {

	private static final Set<String> FOLDER_FIELD_NAMES_TO_LOAD = ImmutableSet.of(
			ComponentIdField.COMPONENT_ID,
			CommonIndexConstants.COMPONENT_LABEL,
			ComponentParentField.COMPONENT_PARENT,
			ComponentStorageKeyField.COMPONENT_STORAGE_KEY,
			CommonIndexConstants.COMPONENT_RELEASED);
	
	protected FolderIndexServerService(final File indexPath) {
		super(indexPath);
		InternalTerminologyRegistryServiceRegistry.INSTANCE.register(getRepositoryUuid(), this);
	}

	@Override
	public Set<FolderIndexEntry> getTopLevelFolders(final IBranchPath branchPath) {
		checkNotNull(branchPath, "Branch path must not be null.");
		return getSubFoldersByParentField(branchPath, ComponentParentStringField.ROOT_PARENT);
	}
	
	private FolderIndexEntry createFolderResultObject(final IBranchPath branchPath, final Document document) {
		final String code = ComponentIdStringField.getString(document);
		final String displayName = document.get(CommonIndexConstants.COMPONENT_LABEL);
		final String parentId = ComponentParentStringField.getValue(document);
		final long storageKey = ComponentStorageKeyField.getLong(document);
		return new FolderIndexEntry(code, displayName, parentId, storageKey, hasChildren(branchPath, code));
	}

	@Override
	public FolderIndexEntry getFolderById(final IBranchPath branchPath, final String folderId) {
		checkNotNull(branchPath, "Branch path must not be null.");
		checkNotNull(folderId, "Folder id must not be null.");
		final Query query = new ComponentIdStringField(folderId).toQuery();
		return createSingleFolderResultObject(branchPath, search(branchPath, query, 1));
	}
	
	private FolderIndexEntry createSingleFolderResultObject(final IBranchPath branchPath, final TopDocs docs) {
		if (CompareUtils.isEmpty(docs.scoreDocs)) {
			return null;
		}
		return createFolderResultObject(branchPath, document(branchPath, docs.scoreDocs[0].doc, FOLDER_FIELD_NAMES_TO_LOAD));
	}

	@Override
	public Set<FolderIndexEntry> getSubFoldersById(final IBranchPath branchPath, final String folderId) {
		checkNotNull(branchPath, "Branch path must not be null.");
		checkNotNull(folderId, "Folder id must not be null.");

		final ComponentParentStringField parentField = new ComponentParentStringField(folderId);
		return getSubFoldersByParentField(branchPath, parentField);
	}

	private Set<FolderIndexEntry> getSubFoldersByParentField(final IBranchPath branchPath, final ComponentParentStringField parentField) {
		final Set<FolderIndexEntry> results = Sets.newHashSet();
		final Query query = new IndexQueryBuilder()
				.require(parentField.toQuery())
				.require(getFolderTypeQuery()).toQuery();

		final Collection<DocumentWithScore> documents = searchUnordered(branchPath, query, null);

		for (final DocumentWithScore documentWithScore : documents) {
			final Document document = documentWithScore.getDocument();
			results.add(createFolderResultObject(branchPath, document));
		}
		return results;
	}

	private Query getFolderTypeQuery() {
		return new ComponentTypeField(getTerminologyFolderComponentNumber()).toQuery();
	}

	@Override
	public Collection<FolderIndexEntry> getAllSubFolders(final IBranchPath branchPath, final String folderId) {
		checkNotNull(branchPath, "Branch path must not be null.");
		checkNotNull(folderId, "Folder id must not be null.");

		final Builder<FolderIndexEntry> builder = ImmutableList.builder();
		buildAllSubFolderListById(branchPath, folderId, builder);
		return builder.build();
	}
	
	/*
	 * Recursive folder collector
	 */
	private void buildAllSubFolderListById(final IBranchPath branchPath, final String id, final Builder<FolderIndexEntry> builder) {
		for (final FolderIndexEntry entry : getSubFoldersById(branchPath, id)) {
			builder.add(entry);
	
			if (getSubFoldersById(branchPath, entry.getId()).size() > 0) {
				buildAllSubFolderListById(branchPath, entry.getId(), builder);
			}
		}
	}

	@Override
	public FolderIndexEntry getTopLevelFolderByName(final IBranchPath branchPath, final String folderName) {
		checkNotNull(branchPath, "Branch path must not be null.");
		checkNotNull(folderName, "Folder name must not be null.");
		
		final Query query = new IndexQueryBuilder()
			.require(ComponentParentStringField.ROOT_PARENT.toQuery())
			.requireExactTerm(CommonIndexConstants.COMPONENT_LABEL_SORT_KEY, IndexUtils.getSortKey(folderName))
			.require(getFolderTypeQuery()).toQuery();
	
		return createSingleFolderResultObject(branchPath, search(branchPath, query, 1));
	}
	
	protected abstract short getTerminologyFolderComponentNumber();
	
	private boolean hasChildren(final IBranchPath branchPath, final String folderId) {
		return getHitCount(branchPath, new ComponentParentStringField(folderId).toQuery(), null) > 0;
	}
	
	// TODO: place this method somewhere which is more component set specific
	@Override
	public Set<? extends ParentFolderAwareIndexEntry> getAllPublishedComponentSetsByFolder(final IBranchPath branchPath, final String folderId) {
		checkNotNull(branchPath, "Branch path must not be null.");

		final Set<ParentFolderAwareIndexEntry> results = Sets.newHashSet();
		final boolean searchForAll = StringUtils.isEmpty(folderId);
		if (searchForAll) {
			final Query query = new IndexQueryBuilder()
					.require(new ComponentTypeField(getTerminologyComponentSetNumber()).toQuery())
					.requireExactTerm(CommonIndexConstants.COMPONENT_RELEASED, "1").toQuery(); // 1: released

			final Collection<DocumentWithScore> documents = searchUnordered(branchPath, query, null);
			for (final DocumentWithScore documentWithScore : documents) {
				final Document document = documentWithScore.getDocument();
				results.add(createComponentSetResultObject(branchPath, document));
			}
			return results;

		} else {
			results.addAll(getPublishedComponentSetsByFolder(branchPath, folderId));
			final Collection<FolderIndexEntry> allSubFolders = getAllSubFolders(branchPath, folderId);
			for (final FolderIndexEntry subFolder : allSubFolders) {
				results.addAll(getPublishedComponentSetsByFolder(branchPath, subFolder.getId()));
			}
		}
		return results;
	}

	// TODO: place this method somewhere which is more component set specific
	protected abstract short getTerminologyComponentSetNumber();

	// TODO: place this method somewhere which is more component set specific
	private Set<? extends ParentFolderAwareIndexEntry> getPublishedComponentSetsByFolder(final IBranchPath branchPath, final String folderId) {
		checkNotNull(branchPath, "Branch path must not be null.");

		final Query query = new IndexQueryBuilder()
				.require(new ComponentParentStringField(StringUtils.isEmpty(folderId) ? ComponentParentStringField.ROOT_ID : folderId).toQuery())
				.require(new ComponentTypeField(getTerminologyComponentSetNumber()).toQuery())
				.requireExactTerm(CommonIndexConstants.COMPONENT_RELEASED, "1").toQuery(); // 1: released

		final Set<ParentFolderAwareIndexEntry> results = Sets.newHashSet();
		final Collection<DocumentWithScore> documents = searchUnordered(branchPath, query, null);

		for (final DocumentWithScore documentWithScore : documents) {
			final Document document = documentWithScore.getDocument();
			results.add(createComponentSetResultObject(branchPath, document));
		}
		return results;
	}

	/**
	 * See {@link SuperTypeIdProvider#getSuperTypeIds(IBranchPath, Object)}.
	 */
	public Collection<String> getSuperTypeIds(final IBranchPath branchPath, final String conceptId) {

		checkNotNull(branchPath, "branchPath");
		checkNotNull(conceptId, "conceptId");
		
		final Query query = new ComponentIdStringField(conceptId).toQuery();
		final TopDocs topDocs = search(branchPath, query, 1);
		
		if (IndexUtils.isEmpty(topDocs)) {
			return Collections.emptySet();
		}
		
		final Document doc = document(branchPath, topDocs.scoreDocs[0].doc, ComponentParentField.FIELDS_TO_LOAD);
		return ComponentParentStringField.getValues(doc);
	}
	
	// TODO: place this method somewhere which is more component set specific
	protected abstract ParentFolderAwareIndexEntry createComponentSetResultObject(IBranchPath branchPath, Document document);
	

	@Override
	public Collection<ICodeSystem> getCodeSystems(final IBranchPath branchPath) {
		return new TerminologyRegistryServiceWrapper(this).getCodeSystems(checkNotNull(branchPath, "branchPath"));
	}

	@Override
	public Collection<ICodeSystemVersion> getCodeSystemVersions(final IBranchPath branchPath, final String codeSystemShortName) {
		return new TerminologyRegistryServiceWrapper(this).getCodeSystemVersions(branchPath, codeSystemShortName);
	}

	@Override
	public Collection<ICodeSystemVersion> getCodeSystemVersionsFromRepository(final IBranchPath branchPath, final String repositoryUuid) {
		return new TerminologyRegistryServiceWrapper(this).getCodeSystemVersionsFromRepository(branchPath, repositoryUuid);
	}
	
	@Override
	public Collection<ICodeSystemVersion> getCodeSystemVersionsFromRepositoryWithInitVersion(final IBranchPath branchPath, final String repositoryUuid) {
		return new TerminologyRegistryServiceWrapper(this).getCodeSystemVersionsFromRepositoryWithInitVersion(branchPath, repositoryUuid);
	}
	
	@Override
	public ICodeSystem getCodeSystemByShortName(final IBranchPath branchPath, final String codeSystemShortName) {
		return new TerminologyRegistryServiceWrapper(this).getCodeSystemByShortName(branchPath, codeSystemShortName);
	}

	@Override
	public ICodeSystem getCodeSystemByOid(final IBranchPath branchPath, final String codeSystemOid) {
		return new TerminologyRegistryServiceWrapper(this).getCodeSystemByOid(branchPath, codeSystemOid);
	}

	@Override
	public Map<String, ICodeSystem> getTerminologyComponentIdCodeSystemMap(final IBranchPath branchPath) {
		return new TerminologyRegistryServiceWrapper(this).getTerminologyComponentIdCodeSystemMap(branchPath);
	}

	@Override
	public Map<String, Collection<ICodeSystem>> getTerminologyComponentIdWithMultipleCodeSystemsMap(final IBranchPath branchPath) {
		return new TerminologyRegistryServiceWrapper(this).getTerminologyComponentIdWithMultipleCodeSystemsMap(branchPath);
	}

	@Override
	public String getTerminologyComponentIdByShortName(final IBranchPath branchPath, final String codeSystemShortName) {
		return new TerminologyRegistryServiceWrapper(this).getTerminologyComponentIdByShortName(branchPath, codeSystemShortName);
	}

	@Override
	public String getVersionId(final IBranchPath branchPath, final ICodeSystem codeSystem) {
		return new TerminologyRegistryServiceWrapper(this).getVersionId(branchPath, codeSystem);
	}
	
}