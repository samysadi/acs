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

package com.samysadi.acs.service.staas.sfreplication;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * 
 * @since 1.0
 */
public interface SfReplicationManager extends Entity {

	@Override
	public SfReplicationManager clone();

	@Override
	public Staas getParent();

	/**
	 * Registers the given <tt>storageFile</tt> for replication by
	 * the current entity.
	 * 
	 * <p>If the given <tt>storageFile</tt> or one of its replicas is already registered for replication
	 * by this entity, nothing is done and this method returns silently.
	 * 
	 * <p>After successful call to this method, calling {@link SfReplicationManager#getReplicas(StorageFile)} with
	 * the given <tt>storageFile</tt> will return the file and all of its replicas.
	 * 
	 * <p>A {@link NotificationCodes#SFRM_REGISTERED} notification is thrown.
	 * 
	 * <p><b>Note:</b> implementations need not to effectively copy the <tt>storageFile</tt> to its replica (by creating
	 * needed operations).<br/>
	 * This task should be delegated to the consistency manager.
	 * 
	 * @param storageFile
	 */
	public void register(StorageFile storageFile);

	/**
	 * Unregisters the given <tt>storageFile</tt> and stops replicating it.<br/>
	 * The already created replicas are not deleted, as they may still be used 
	 * by other entities.
	 * 
	 * <p>Calling {@link SfReplicationManager#getReplicas(StorageFile)} after this method returns,
	 * will return a one item list that contains the given <tt>storageFile</tt>.
	 * 
	 * <p>If neither the given <tt>storageFile</tt> nor any of its replicas is registered
	 * for replication by this entity, then nothing is done and this methods
	 * returns silently.
	 * 
	 * <p>A {@link NotificationCodes#SFRM_UNREGISTERED} notification is thrown.
	 * 
	 * @param storageFile
	 */
	public void unregister(StorageFile storageFile);

	/**
	 * Returns a list containing all replicas of the given <tt>storageFile</tt> including itself.
	 * 
	 * <p>If the given <tt>storageFile</tt> is not replicated then an one item list that contains the given <tt>storageFile</tt>
	 * is returned.
	 * 
	 * @param storageFile
	 * @return a list containing all replicas of the given <tt>storageFile</tt> including itself
	 */
	public List<StorageFile> getReplicas(StorageFile storageFile);
}
