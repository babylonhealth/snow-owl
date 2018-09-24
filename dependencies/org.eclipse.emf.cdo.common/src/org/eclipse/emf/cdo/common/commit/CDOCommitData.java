/*
 * Copyright (c) 2004 - 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common.commit;

import java.util.List;

import org.eclipse.emf.cdo.common.model.CDOPackageUnit;

/**
 * {@link CDOChangeSetData Change set data} with detailed information about new {@link #getNewPackageUnits() package
 * units}.
 * 
 * @author Eike Stepper
 * @since 3.0
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface CDOCommitData extends CDOChangeSetData
{
  public List<CDOPackageUnit> getNewPackageUnits();
}