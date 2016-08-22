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
package com.b2international.snowowl.core.domain;

import org.eclipse.emf.ecore.EObject;

import com.b2international.snowowl.core.exceptions.ComponentNotFoundException;

/**
 * @since 4.5
 */
public interface TransactionContext extends BranchContext, AutoCloseable {

	/**
	 * Adds the given {@link EObject} to this transaction context.
	 * 
	 * @param o
	 */
	void add(EObject o);

	/**
	 * Removes the given EObject from the transaction context and from the store as well.
	 * 
	 * @param o
	 */
	void delete(EObject o);
	
	/**
	 * Forcefully removes the given EObject from the transaction context and from the store as well.
	 * 
	 * @param o
	 */
	void delete(EObject o, boolean force);

	/**
	 * Prepares the commit.
	 */
	void preCommit();

	/**
	 * Commits any changes made to {@link EObject}s into the store.
	 * 
	 * @param userId
	 *            - the owner of the commit
	 * @param commitComment
	 *            - a message for the commit
	 * @return - the timestamp of the successful commit
	 */
	long commit(String userId, String commitComment);

	/**
	 * Rolls back any changes the underlying transaction has since its creation.
	 */
	void rollback();

	/**
	 * Returns a persisted component from the store with the given component id and type.
	 * 
	 * @param componentId
	 * @param type
	 * @return
	 * @throws ComponentNotFoundException
	 *             - if the component cannot be found
	 */
	<T extends EObject> T lookup(String componentId, Class<T> type);

}
