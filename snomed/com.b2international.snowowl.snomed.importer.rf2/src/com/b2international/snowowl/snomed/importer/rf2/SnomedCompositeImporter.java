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
package com.b2international.snowowl.snomed.importer.rf2;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.session.CDORepositoryInfo;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.slf4j.Logger;

import com.b2international.collections.longs.LongCollection;
import com.b2international.commons.collect.LongSets;
import com.b2international.commons.functions.UncheckedCastFunction;
import com.b2international.index.revision.RevisionIndex;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.exceptions.CodeSystemNotFoundException;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.CodeSystemEntry;
import com.b2international.snowowl.datastore.ICodeSystemVersion;
import com.b2international.snowowl.datastore.cdo.CDOCommitInfoUtils;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions;
import com.b2international.snowowl.datastore.server.CDOServerCommitBuilder;
import com.b2international.snowowl.datastore.server.CDOServerUtils;
import com.b2international.snowowl.datastore.server.snomed.index.init.Rf2BasedSnomedTaxonomyBuilder;
import com.b2international.snowowl.datastore.version.ITagConfiguration;
import com.b2international.snowowl.datastore.version.ITagService;
import com.b2international.snowowl.datastore.version.TagConfigurationBuilder;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.importer.AbstractImportUnit;
import com.b2international.snowowl.importer.AbstractLoggingImporter;
import com.b2international.snowowl.importer.ImportException;
import com.b2international.snowowl.importer.Importer;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.ContentSubType;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.datastore.taxonomy.IncompleteTaxonomyException;
import com.b2international.snowowl.snomed.datastore.taxonomy.InvalidRelationship;
import com.b2international.snowowl.snomed.datastore.taxonomy.SnomedTaxonomyBuilder;
import com.b2international.snowowl.snomed.datastore.taxonomy.SnomedTaxonomyBuilderResult;
import com.b2international.snowowl.snomed.importer.rf2.model.AbstractSnomedImporter;
import com.b2international.snowowl.snomed.importer.rf2.model.ComponentImportType;
import com.b2international.snowowl.snomed.importer.rf2.model.ComponentImportUnit;
import com.b2international.snowowl.snomed.importer.rf2.model.EffectiveTimeUnitOrdering;
import com.b2international.snowowl.snomed.importer.rf2.model.SnomedImportContext;
import com.b2international.snowowl.terminologymetadata.CodeSystem;
import com.b2international.snowowl.terminologymetadata.CodeSystemVersion;
import com.b2international.snowowl.terminologyregistry.core.builder.CodeSystemVersionBuilder;
import com.b2international.snowowl.terminologyregistry.core.request.CodeSystemRequests;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * A composite importer that coordinates the operation of its child importers:
 * 
 * <ul>
 * <li>runs all pre-import steps are in order
 * <li>gathers units from all nested importers
 * <li>reorders units are according to the specified ordering
 * <li>carries out the import on the nested units
 * <li>runs all post-import steps in order
 * </ul>
 * 
 */
public class SnomedCompositeImporter extends AbstractLoggingImporter {

	private final List<Importer> importers;
	private final Ordering<AbstractImportUnit> unitOrdering;
	private final SnomedImportContext importContext; //will be used when tagging version (Snow Owl 3.1)
	private final RepositoryState repositoryState;
	
	private Rf2BasedSnomedTaxonomyBuilder inferredTaxonomyBuilder;
	private Rf2BasedSnomedTaxonomyBuilder statedTaxonomyBuilder;
	private Set<String> existingVersions;
	private List<CodeSystemVersion> versionsToCreate;
	
	public SnomedCompositeImporter(final Logger logger,
			final RepositoryState repositoryState,
			final SnomedImportContext importContext,
			final List<Importer> importers, 
			final Ordering<AbstractImportUnit> unitOrdering) {
		super(logger);
		this.repositoryState = repositoryState;
		this.importContext = Preconditions.checkNotNull(importContext, "Import context argument cannot be null.");
		this.importers = ImmutableList.copyOf(checkNotNull(importers, "importers"));
		this.unitOrdering = checkNotNull(unitOrdering, "unitOrdering");
		versionsToCreate = newArrayList();
	}
	
	@Override
	public void preImport(final SubMonitor subMonitor) {
		
		subMonitor.setWorkRemaining(importers.size());
		
		for (final Importer importer : importers) {
			importer.preImport(subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
		}
		
		collectExistingVersions();
	}

	private void collectExistingVersions() {
		CodeSystemEntry releaseEntry = getCodeSystemEntry(importContext.getCodeSystemShortName(), importContext.getCodeSystemOID());
		existingVersions = FluentIterable.from(new CodeSystemRequests(SnomedDatastoreActivator.REPOSITORY_UUID)
			.prepareSearchCodeSystemVersion()
			.filterByCodeSystemShortName(releaseEntry.getShortName())
			.build(IBranchPath.MAIN_BRANCH)
			.executeSync(getEventBus())
			.getItems()).transform(new Function<ICodeSystemVersion, String>() {
				@Override public String apply(ICodeSystemVersion input) {
					return EffectiveTimes.format(input.getEffectiveDate(), DateFormats.SHORT);
				}
			}).toSet();
	}

	private CodeSystemEntry getCodeSystemEntry(String shortName, String oid) {
		CodeSystemEntry entry;
		try {
			entry = new CodeSystemRequests(SnomedDatastoreActivator.REPOSITORY_UUID)
				.prepareGetCodeSystem()
				.setUniqueId(oid)
				.build(IBranchPath.MAIN_BRANCH)
				.executeSync(getEventBus());
		} catch (CodeSystemNotFoundException e) {
			try {
				entry = new CodeSystemRequests(SnomedDatastoreActivator.REPOSITORY_UUID)
					.prepareGetCodeSystem()
					.setUniqueId(shortName)
					.build(IBranchPath.MAIN_BRANCH)
					.executeSync(getEventBus());
			} catch (CodeSystemNotFoundException e2) {
				throw new ImportException(String.format(
						"Unable to find the specified SNOMED CT release among the registered terminologies. Short name: %s, OID: %s",
						importContext.getCodeSystemShortName(), importContext.getCodeSystemOID()));
			}
		}
		
		return entry;
	}

	@Override
	public List<AbstractImportUnit> getImportUnits(final SubMonitor subMonitor) {
		return ImmutableList.<AbstractImportUnit>of(getCompositeUnit(subMonitor));
	}

	public SnomedCompositeImportUnit getCompositeUnit(final SubMonitor subMonitor) {
		
		subMonitor.setWorkRemaining(importers.size());
		
		final List<AbstractImportUnit> units = Lists.newArrayList();
		
		for (final Importer importer : importers) {
			units.addAll(importer.getImportUnits(subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE)));
		}
		
		if (ContentSubType.SNAPSHOT.equals(importContext.getContentSubType())) {
			AbstractImportUnit latestUnit = EffectiveTimeUnitOrdering.INSTANCE.max(units);
			String latestKey = ((ComponentImportUnit) latestUnit).getEffectiveTimeKey();
			
			for (AbstractImportUnit unit : units) {
				((ComponentImportUnit) unit).setEffectiveTimeKey(latestKey);
			}
		}
		
		Collections.sort(units, unitOrdering);
		return new SnomedCompositeImportUnit(this, units);
	}

	@Override
	public void doImport(final SubMonitor subMonitor, final AbstractImportUnit unit) {
		final IBranchPath branchPath = getImportBranchPath();
		
		final SnomedCompositeImportUnit compositeUnit = (SnomedCompositeImportUnit) unit;
		final UncheckedCastFunction<AbstractImportUnit, ComponentImportUnit> castFunction = new UncheckedCastFunction<AbstractImportUnit, ComponentImportUnit>(ComponentImportUnit.class);
		final List<ComponentImportUnit> units = Lists.newArrayList(Iterables.transform(compositeUnit.getUnits(), castFunction));
		
		subMonitor.setWorkRemaining(units.size() + 1);

		if (isRefSetImport(units)) {
			// enable commit notifications in case of refset import
			importContext.setCommitNotificationEnabled(true);
			
			String lastUnitEffectiveTimeKey = units.get(0).getEffectiveTimeKey();
			
			for (final ComponentImportUnit subUnit : units) {
				
				final String currentUnitEffectiveTimeKey = subUnit.getEffectiveTimeKey();
				
				if (!Objects.equal(lastUnitEffectiveTimeKey, currentUnitEffectiveTimeKey)) {
					createSnomedVersionFor(lastUnitEffectiveTimeKey);
					lastUnitEffectiveTimeKey = currentUnitEffectiveTimeKey;
				}
				
				subUnit.doImport(subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
			}
			
			createSnomedVersionFor(lastUnitEffectiveTimeKey);
			
		} else {
			
			String lastUnitEffectiveTimeKey = units.get(0).getEffectiveTimeKey();
			
			for (final ComponentImportUnit subUnit : units) {
				
				/*
				 * First import unit seen with an effective time different from the previous set of import units;
				 * initialize taxonomy builder and update import index server service, then perform tagging if
				 * required.
				 * 
				 * Note that different effective times should only be seen in FULL or DELTA import, and the 
				 * collected values can be used as is.
				 */
				final String currentUnitEffectiveTimeKey = subUnit.getEffectiveTimeKey();
				
				if (!Objects.equal(lastUnitEffectiveTimeKey, currentUnitEffectiveTimeKey)) {
					updateInfrastructure(units, branchPath, lastUnitEffectiveTimeKey);
					createSnomedVersionFor(lastUnitEffectiveTimeKey);
					lastUnitEffectiveTimeKey = currentUnitEffectiveTimeKey;
				}
					
				subUnit.doImport(subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
			}
			
			updateInfrastructure(units, branchPath, lastUnitEffectiveTimeKey);
			createSnomedVersionFor(lastUnitEffectiveTimeKey);
		}
	}

	private IBranchPath getImportBranchPath() {
		return BranchPathUtils.createPath(importContext.getEditingContext().getTransaction());
	}

	private boolean isRefSetImport(final Iterable<? extends ComponentImportUnit> units) {
		for (final ComponentImportUnit unit : units) {
			if (!ComponentImportType.isRefSetType(unit.getType())) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void postImport(final SubMonitor subMonitor) {
	
		subMonitor.setWorkRemaining(importers.size());
		
		for (final Importer importer : importers) {
			importer.postImport(subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
		}
		
		if (getImportBranchPath().getPath().equals(IBranchPath.MAIN_BRANCH)) {
			createNewVersions(importContext.getEditingContext());
		} else {
			try (SnomedEditingContext snomedEditingContext = new SnomedEditingContext(BranchPathUtils.createMainPath())) {
				createNewVersions(snomedEditingContext);
			}
		}
	}

	private void createNewVersions(SnomedEditingContext snomedEditingContext) {
		try {
			final CodeSystem codeSystem = checkNotNull(snomedEditingContext.lookup(importContext.getCodeSystemShortName(), CodeSystem.class));
			codeSystem.getCodeSystemVersions().addAll(versionsToCreate);
			
			if (snomedEditingContext.isDirty()) {
				
				new CDOServerCommitBuilder(importContext.getUserId(), String.format("Created %s SNOMED CT versions for branch '%s'", versionsToCreate.size(), getImportBranchPath().getPath()), snomedEditingContext.getTransaction())
					.sendCommitNotification(false)
					.parentContextDescription(DatastoreLockContextDescriptions.IMPORT)
					.commit();
				
				getLogger().info(String.format("Version tags created: %s", Joiner.on(", ").join(FluentIterable.from(versionsToCreate).transform(new Function<CodeSystemVersion, String>() {
					@Override public String apply(CodeSystemVersion input) {
						return String.format("[%s]", input.getVersionId());
					}
				}))));
				
			}
			
		} catch (NullPointerException e) {
			throw new ImportException(String.format(
					"Unable to find the specified SNOMED CT release among the registered terminologies. Short name: %s, OID: %s",
					importContext.getCodeSystemShortName(), importContext.getCodeSystemOID()));
		} catch (CommitException e) {
			throw new ImportException(String.format("Unable to commit SNOMED CT versions for branch %s", getImportBranchPath().getPath()), e);
		}
	}

	private void updateInfrastructure(final List<ComponentImportUnit> units, final IBranchPath branchPath, final String lastUnitEffectiveTimeKey) {

		if (0 == importContext.getVisitedConcepts().size() && 0 == importContext.getVisitedRefSets().size()) {
			//nothing changed
			return;
		}
		
		String conceptFilePath = null;
		Set<String> descriptionFilePaths = newHashSet();
		String relationshipFilePath = null;
		Set<String> languageFilePaths = newHashSet();
		String statedRelationshipFilePath = null;
		
		for (final ComponentImportUnit unit : units) {
			
			// Consider all reference set files if importing a SNAPSHOT, check matching effective time otherwise 
			if (Objects.equal(lastUnitEffectiveTimeKey, unit.getEffectiveTimeKey())) {
				final String path = unit.getUnitFile().getAbsolutePath();
				
				switch (unit.getType()) {
					case CONCEPT: 
						if (null == conceptFilePath) {
							conceptFilePath = path;
						}
						break;
					case DESCRIPTION: 
					case TEXT_DEFINITION: 
						descriptionFilePaths.add(path); 
						break;
					case LANGUAGE_TYPE_REFSET: 
						languageFilePaths.add(path); 
						break;
					case RELATIONSHIP: 
						if (null == relationshipFilePath) {
							relationshipFilePath = path; 
						}
						break;
					case STATED_RELATIONSHIP:
						if (null == statedRelationshipFilePath) {
							statedRelationshipFilePath = path; 
						}
					default: /*intentionally ignored*/ break;
				}
			}
		}
		
		if (null == inferredTaxonomyBuilder) {
			// First iteration: initialize release file-based builder with existing contents (if any)
			inferredTaxonomyBuilder = buildTaxonomy(Concepts.INFERRED_RELATIONSHIP);
		}
		
		inferredTaxonomyBuilder.applyNodeChanges(conceptFilePath);
		inferredTaxonomyBuilder.applyEdgeChanges(relationshipFilePath);
		final SnomedTaxonomyBuilderResult inferredTaxonomyResult = inferredTaxonomyBuilder.build();
		
		if (null == statedTaxonomyBuilder) {
			// First iteration: initialize release file-based builder with existing contents (if any)
			statedTaxonomyBuilder = buildTaxonomy(Concepts.STATED_RELATIONSHIP);
		}
		
		statedTaxonomyBuilder.applyNodeChanges(conceptFilePath);
		statedTaxonomyBuilder.applyEdgeChanges(statedRelationshipFilePath);
		final SnomedTaxonomyBuilderResult statedTaxonomyResult = statedTaxonomyBuilder.build();
		
		if (!inferredTaxonomyResult.getStatus().isOK() || !statedTaxonomyResult.getStatus().isOK()) {
			throw new IncompleteTaxonomyException(ImmutableList.<InvalidRelationship> builder()
					.addAll(inferredTaxonomyResult.getInvalidRelationships())
					.addAll(statedTaxonomyResult.getInvalidRelationships())
					.build());
		}
		
		final Set<String> synonymAndDescendants = LongSets.toStringSet(inferredTaxonomyBuilder.getAllDescendantNodeIds(Concepts.SYNONYM));
		synonymAndDescendants.add(Concepts.SYNONYM);
		
		initializeIndex(importContext, lastUnitEffectiveTimeKey, units);
	}

	private Rf2BasedSnomedTaxonomyBuilder buildTaxonomy(final String characteristicType) {
		final LongCollection conceptIds = repositoryState.getConceptIds();
		final Collection<SnomedRelationshipIndexEntry.Views.StatementWithId> statements = Concepts.INFERRED_RELATIONSHIP.equals(characteristicType) ? repositoryState.getInferredStatements() : repositoryState.getStatedStatements();
		return Rf2BasedSnomedTaxonomyBuilder.newInstance(new SnomedTaxonomyBuilder(conceptIds, statements), characteristicType);
	}

	private RevisionIndex getIndex() {
		return ApplicationContext.getInstance().getService(RepositoryManager.class).get(SnomedDatastoreActivator.REPOSITORY_UUID).service(RevisionIndex.class);
	}
	
	private void initializeIndex(final SnomedImportContext context, final String lastUnitEffectiveTimeKey, final List<ComponentImportUnit> units) {
		final SnomedRf2IndexInitializer snomedRf2IndexInitializer = new SnomedRf2IndexInitializer(getIndex(), context, lastUnitEffectiveTimeKey, units, inferredTaxonomyBuilder, statedTaxonomyBuilder);
		snomedRf2IndexInitializer.run(new NullProgressMonitor());
	}

	protected void createSnomedVersionFor(final String lastUnitEffectiveTimeKey) {
		
		if (AbstractSnomedImporter.UNPUBLISHED_KEY.equals(lastUnitEffectiveTimeKey)) {
			return;
		}
		
		final SnomedEditingContext editingContext = importContext.getEditingContext();
		
		try {

			boolean existingVersionFound = false;

			if (importContext.isVersionCreationEnabled()) {
				
				Set<String> existingEffectiveTimes = Sets.union(FluentIterable.from(versionsToCreate).transform(new Function<CodeSystemVersion, String>() {
					@Override public String apply(CodeSystemVersion input) {
						return EffectiveTimes.format(input.getEffectiveDate(), DateFormats.SHORT);
					}
				}).toSet(), existingVersions);
				
				for (String existingEffectiveTime : existingEffectiveTimes) {
					if (lastUnitEffectiveTimeKey.equals(existingEffectiveTime)) {
						existingVersionFound = true;
						break;
					}
				}
				
				if (!existingVersionFound) {
					versionsToCreate.add(createVersion(lastUnitEffectiveTimeKey));
				} else {
					getLogger().warn("Existing SNOMED CT version found for effective time {}.", lastUnitEffectiveTimeKey);
				}
			}
			
			if (editingContext.isDirty()) {
				new CDOServerCommitBuilder(importContext.getUserId(), importContext.getCommitMessage(), importContext.getAggregator(lastUnitEffectiveTimeKey))
					.sendCommitNotification(false)
					.parentContextDescription(DatastoreLockContextDescriptions.IMPORT)
					.commit();
			}
			
			if (!existingVersionFound && importContext.isVersionCreationEnabled()) {
				final IBranchPath snomedBranchPath = getImportBranchPath();
				final Date effectiveDate = EffectiveTimes.parse(lastUnitEffectiveTimeKey, DateFormats.SHORT);
				final String formattedEffectiveDate = EffectiveTimes.format(effectiveDate);
				
				final ITagConfiguration configuration = TagConfigurationBuilder.createForRepositoryUuid(SnomedDatastoreActivator.REPOSITORY_UUID, formattedEffectiveDate)
					.setBranchPath(snomedBranchPath)
					.setUserId(importContext.getUserId())
					.setParentContextDescription(DatastoreLockContextDescriptions.IMPORT)
					.build();
				
				ApplicationContext.getInstance().getService(ITagService.class).tag(configuration);
			}
			
		} catch (final CommitException e) {
			throw new ImportException("Cannot create tag for SNOMED CT " + lastUnitEffectiveTimeKey, e);
		} finally {
			importContext.setCommitTime(CDOServerUtils.getLastCommitTime(editingContext.getTransaction().getBranch()));
			final CDOCommitInfo commitInfo = createCommitInfo(importContext.getCommitTime(), importContext.getPreviousTime());
			CDOServerUtils.sendCommitNotification(commitInfo);
		}
	}

	private CDOCommitInfo createCommitInfo(final long timestamp, final long previousTimestamp) {
		
		final CDOTransaction transaction = importContext.getEditingContext().getTransaction();
		final CDOSession session = transaction.getSession();
		final CDORepositoryInfo info = session.getRepositoryInfo();
		final String repositoryUuid = info.getUUID();
		final IBranchPath branchPath = getImportBranchPath();
		
		return CDOCommitInfoUtils.createEmptyCommitInfo(repositoryUuid, branchPath, importContext.getUserId(), Strings.nullToEmpty(importContext.getCommitMessage()), timestamp, previousTimestamp);
		
	}
	
	private CodeSystemVersion createVersion(final String version) {
		final Date effectiveDate = EffectiveTimes.parse(version, DateFormats.SHORT);
		final String formattedEffectiveDate = EffectiveTimes.format(effectiveDate);
		
		return new CodeSystemVersionBuilder()
			.withVersionId(formattedEffectiveDate)
			.withDescription("SNOMED CT version created by an RF2 import process")
			.withImportDate(new Date())
			.withEffectiveDate(effectiveDate)
			.withParentBranchPath(getImportBranchPath().getPath())
			.build();
	}

	private IEventBus getEventBus() {
		return ApplicationContext.getServiceForClass(IEventBus.class);
	}
}
