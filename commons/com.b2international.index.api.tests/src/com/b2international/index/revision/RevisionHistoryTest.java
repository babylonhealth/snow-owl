/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.revision;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.b2international.index.revision.RevisionFixtures.Data;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * @since 6.6
 */
public class RevisionHistoryTest extends BaseRevisionIndexTest {

	private final Data newData = new Data(STORAGE_KEY1, "field1", "field2");
	private final Data changedData = new Data(STORAGE_KEY1, "field1Changed", "field2");
	
	@Override
	protected Collection<Class<?>> getTypes() {
		return ImmutableList.<Class<?>>of(Data.class);
	}
	
	@Test
	public void historyOfNewComponent() throws Exception {
		indexRevision(MAIN, newData);
		List<Commit> commits = index().history(STORAGE_KEY1);
		assertThat(commits).hasSize(1);
		final Commit commit = Iterables.getOnlyElement(commits);
		assertThat(commit.getChanges()).hasSize(1);
		final CommitChange expectedChange = CommitChange.builder(STORAGE_KEY1).newComponents(ImmutableSet.of(STORAGE_KEY1)).build();
		assertThat(commit.getChangesByContainer(STORAGE_KEY1)).isEqualTo(expectedChange);
	}
	
	@Test
	public void historyOfChangedComponent() throws Exception {
		// create a new component
		historyOfNewComponent();
		// index a change
		indexChange(MAIN, changedData);
		// history should contain two commits now
		List<Commit> commits = index().history(STORAGE_KEY1);
		assertThat(commits).hasSize(2);
		// first element should be the latest commit
		final Commit commit = Iterables.getFirst(commits, null);
		assertThat(commit.getChanges()).hasSize(1);
		final CommitChange expectedChange = CommitChange.builder(STORAGE_KEY1).changedComponents(ImmutableSet.of(STORAGE_KEY1)).build();
		assertThat(commit.getChangesByContainer(STORAGE_KEY1)).isEqualTo(expectedChange);
	}
	
	@Test
	public void historyOfRemovedComponent() throws Exception {
		historyOfNewComponent();
		// index deletion
		indexRemove(MAIN, newData);
		// history should contain two commits now
		List<Commit> commits = index().history(STORAGE_KEY1);
		assertThat(commits).hasSize(2);
		// first element should be the latest commit
		final Commit commit = Iterables.getFirst(commits, null);
		assertThat(commit.getChanges()).hasSize(1);
		final CommitChange expectedChange = CommitChange.builder(STORAGE_KEY1).removedComponents(ImmutableSet.of(STORAGE_KEY1)).build();
		assertThat(commit.getChangesByContainer(STORAGE_KEY1)).isEqualTo(expectedChange);
	}
	
}
