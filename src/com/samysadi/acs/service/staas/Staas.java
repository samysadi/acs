/*
===============================================================================
Copyright (c) 2014-2015, Samy Sadi. All rights reserved.
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

This file is part of ACS - Advanced Cloud Simulator.

ACS is part of a research project undertaken by
Samy Sadi (samy.sadi.contact@gmail.com) and supervised by
Belabbas Yagoubi (byagoubi@gmail.com) in the
University of Oran1 Ahmed Benbella, Algeria.

ACS is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

ACS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ACS. If not, see <http://www.gnu.org/licenses/>.
===============================================================================
*/

package com.samysadi.acs.service.staas;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.staas.sfconsistency.SfConsistencyManager;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.service.staas.sfreplicaselection.SfReplicaSelectionPolicy;
import com.samysadi.acs.service.staas.sfreplication.SfReplicationManager;
import com.samysadi.acs.user.User;

/**
 * This interface contains methods to provide Storage as a Service (STaaS)
 * for users.
 * 
 * <p>The service may include replication of files and may grant consistency between
 * multiple replicas.
 * Also it may include different placement policies and replica selection policies.
 * 
 * @since 1.0
 */
public interface Staas extends Entity {

	@Override
	public Staas clone();

	@Override
	public CloudProvider getParent();

	/**
	 * Returns the {@link SfConsistencyManager}.
	 * 
	 * @return the {@link SfConsistencyManager}
	 */
	public SfConsistencyManager getConsistencyManager();

	/**
	 * Returns the {@link SfPlacementPolicy}.
	 * 
	 * @return the {@link SfPlacementPolicy}
	 */
	public SfPlacementPolicy getPlacementPolicy();

	/**
	 * Returns the {@link SfReplicaSelectionPolicy}.
	 * 
	 * @return the {@link SfReplicaSelectionPolicy}
	 */
	public SfReplicaSelectionPolicy getReplicaSelectionPolicy();

	/**
	 * Returns the {@link SfReplicationManager}.
	 * 
	 * @return the {@link SfReplicationManager}
	 */
	public SfReplicationManager getReplicationManager();

	/**
	 * Creates and returns a new {@link StorageFile} and registers it for auto-replication,
	 * and consistency management.
	 * 
	 * <p>The created file is placed using current placement policy.
	 * 
	 * @param size initial size of the file
	 * @param user the user that asks to create the file (ie: the owner)
	 * @return the created {@link StorageFile}
	 */
	public StorageFile createFile(long size, User user);

	/**
	 * Deletes the given {@link StorageFile} (set its parent to <tt>null</tt>), and unregisters it from auto-replication and
	 * consistency management.
	 * 
	 * <p>All replicas of the given file are also deleted.
	 * 
	 * @param storageFile
	 */
	public void deleteFile(StorageFile storageFile);
}
