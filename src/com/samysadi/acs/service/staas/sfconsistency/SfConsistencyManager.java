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

package com.samysadi.acs.service.staas.sfconsistency;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.service.staas.sfreplication.SfReplicationManager;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface SfConsistencyManager extends Entity {

	@Override
	public SfConsistencyManager clone();

	@Override
	public Staas getParent();

	/**
	 * Registers the given <tt>storageFile</tt> for consistency management by
	 * the current entity.<br/>
	 * This method will make use of {@link SfReplicationManager#getReplicas(StorageFile)} to
	 * list all replicas that need to be kept consistent.
	 * 
	 * <p>If the given <tt>storageFile</tt> or one of its replicas is already registered for consistency management
	 * by this entity, nothing is done and this method returns silently.
	 * 
	 * <p>A {@link NotificationCodes#SFCM_REGISTERED} notification is thrown.
	 * 
	 * @param storageFile
	 */
	public void register(StorageFile storageFile);

	/**
	 * Unregisters the given <tt>storageFile</tt> and stops managing
	 * consistency for it.
	 * 
	 * <p>If neither the given <tt>storageFile</tt> nor any of its replicas is registered
	 * for consistency management by this entity, then nothing is done and this methods
	 * returns silently.
	 * 
	 * <p>A {@link NotificationCodes#SFCM_UNREGISTERED} notification is thrown.
	 * 
	 * @param storageFile
	 */
	public void unregister(StorageFile storageFile);

}
