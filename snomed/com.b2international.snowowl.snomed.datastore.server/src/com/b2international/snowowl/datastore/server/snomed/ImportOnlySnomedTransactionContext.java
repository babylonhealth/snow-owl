/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.snomed;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.ecore.EObject;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.datastore.server.CDOServerUtils;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.inject.Provider;

/**
 * @since 4.6
 */
public class ImportOnlySnomedTransactionContext implements TransactionContext {

	private final SnomedEditingContext editingContext;
	private final SnomedIdentifiers snomedIdentifiers;
	private Branch branch;

	public ImportOnlySnomedTransactionContext(final SnomedEditingContext editingContext) {
		this.editingContext = editingContext;
		final ISnomedIdentifierService identifierService = ApplicationContext.getInstance().getServiceChecked(ISnomedIdentifierService.class);
		snomedIdentifiers = new SnomedIdentifiers(identifierService);
	}
	
	@Override
	public Branch branch() {
		if (null == branch) {
			branch = SnomedRequests
						.branching()
						.prepareGet(editingContext.getBranch())
						.executeSync(ApplicationContext.getServiceForClass(IEventBus.class));
		}
		return branch;
	}

	@Override
	public SnowOwlConfiguration config() {
		return SnowOwlApplication.INSTANCE.getConfiguration();
	}

	@Override
	public String id() {
		// FIXME hardcoded ID
		return SnomedDatastoreActivator.REPOSITORY_UUID;
	}

	@Override
	public <T> T service(final Class<T> type) {
		if (type.isAssignableFrom(SnomedIdentifiers.class)) {
			return type.cast(snomedIdentifiers);
		} else if (type.isAssignableFrom(SnomedEditingContext.class)) {
			return type.cast(editingContext);
		}
		return ApplicationContext.getInstance().getServiceChecked(type);
	}

	@Override
	public <T> Provider<T> provider(final Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws Exception {
		editingContext.close();
	}

	@Override
	public void add(final EObject o) {
		editingContext.add(o);
	}

	@Override
	public void delete(final EObject o) {
		editingContext.delete(o);
	}
	
	@Override
	public void delete(EObject o, boolean force) {
		editingContext.delete(o, force);
	}

	@Override
	public void preCommit() {
		editingContext.preCommit();
	}

	@Override
	public long commit(final String userId, final String commitComment) {
		try {
			final CDOCommitInfo info = CDOServerUtils.commit(editingContext.getTransaction(), userId, commitComment, new NullProgressMonitor());
			return info.getTimeStamp();
		} catch (final CommitException e) {
			throw new SnowowlRuntimeException(e);
		}
	}

	@Override
	public void rollback() {
		editingContext.rollback();
	}

	@Override
	public <T extends EObject> T lookup(final String componentId, final Class<T> type) {
		return editingContext.lookup(componentId, type);
	}

	public SnomedEditingContext getEditingContext() {
		return editingContext;
	}
	
	public String getDefaultLanguageRefsetId() {
		return editingContext.getLanguageRefSetId();
	}

	public String getDefaultLanguageCode() {
		return editingContext.getDefaultLanguageCode();
	}
	
	public SnomedCoreConfiguration getSnomedCoreConfig() {
		return config().getModuleConfig(SnomedCoreConfiguration.class);
	}
}
