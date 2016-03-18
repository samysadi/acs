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

package com.samysadi.acs.service.staas.sfplacement;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Defines methods for selecting a storage among all available storages in
 * all hosts in the cloud when placing a {@link StorageFile}.
 *
 * <p>This interface also define a method for placing a {@link StorageFile} after a storage has been selected,
 * and a method to unplace it after it has been placed (see each method documentation for more information).
 *
 * @since 1.0
 */
public interface SfPlacementPolicy extends Entity {

	@Override
	public SfPlacementPolicy clone();

	@Override
	public Staas getParent();

	/**
	 * Selects a host and a storage where to place the given <tt>storageFile</tt> and returns that storage.
	 *
	 * <p>A storage among all available storages will be selected such that it has enough space
	 * to save the given <tt>storageFile</tt>.
	 *
	 * <p>Among other conditions, the selected storage and its parent host must be in
	 * {@link FailureState#OK} state.
	 * But, the parent host can be in a state other than {@link PowerState#ON} if the cloud provider's
	 * power manager allows it to be powered on.
	 * In which case, it is left to the placement method to power it on.
	 *
	 * <p>Depending on whether a storage was found or not, a {@link NotificationCodes#SFP_STORAGESELECTION_SUCCESS} or
	 * {@link NotificationCodes#SFP_STORAGESELECTION_FAILED} is thrown.
	 *
	 * @param storageFile
	 * @param hosts possible hosts. May be <tt>null</tt> in which case, all hosts in the cloud are considered.
	 * @param excludedHosts a list of hosts that should not be selected. May be <tt>null</tt>.
	 * @return the selected storage or <tt>null</tt> if no storage was found
	 */
	public Storage selectStorage(StorageFile storageFile, List<Host> hosts, List<Host> excludedHosts);

	/**
	 * Alias for {@link SfPlacementPolicy#selectStorage(StorageFile, List, List)} where
	 * excluded hosts is <tt>null</tt>.
	 */
	public Storage selectStorage(StorageFile storageFile, List<Host> hosts);

	/**
	 * Alias for {@link SfPlacementPolicy#selectStorage(StorageFile, List)} where
	 * possible hosts is <tt>null</tt>.
	 */
	public Storage selectStorage(StorageFile storageFile);

	/**
	 * Returns <tt>true</tt> if the <tt>storageFile</tt> can be placed on the given <tt>storage</tt>.
	 *
	 * @param storageFile
	 * @param storage
	 * @return <tt>true</tt> if the <tt>storageFile</tt> can be placed on the given <tt>storage</tt>
	 */
	public boolean canPlaceStorageFile(StorageFile storageFile, Storage storage);

	/**
	 * Takes all actions in order to place the given <tt>storageFile</tt> on the given <tt>storage</tt>.
	 * When all actions were taken, the <tt>storageFile</tt>'s parent is updated.<br/>
	 * You need to listen to the {@link NotificationCodes#ENTITY_PARENT_CHANGED} to know when the placement
	 * has ended, as it may not be the case when this method returns.
	 *
	 * <p>The parent host of the given <tt>storage</tt> may be in another state than {@link PowerState#ON}.
	 * If so then this method asks the cloud provider's power manger to power it on.
	 *
	 * <p>You need to check for the return value of {@link SfPlacementPolicy#canPlaceStorageFile(StorageFile, Storage)} to
	 * see if the <tt>storageFile</tt> can be placed on the given <tt>storage</tt>.<br/>
	 * If you selected the <tt>storage</tt> using {@link SfPlacementPolicy#selectStorage(StorageFile)}
	 * then this method should succeed.
	 *
	 * @param storageFile the {@link StorageFile} to place
	 * @param storage the {@link Storage} where to place the <tt>storageFile</tt>
	 * @throws IllegalArgumentException if one of the following condition(s) are true:<ul>
	 * <li>the <tt>storageFile</tt> has already a defined parent;
	 * <li>the <tt>storageFile</tt> cannot be placed on the given storage (ex: if there is not enough space).
	 * </ul>
	 */
	public void placeStorageFile(StorageFile storageFile, Storage storage);

	/**
	 * Takes all actions in order to unplaces the given <tt>storageFile</tt>.<br/>
	 * When all actions were taken, the StorageFile's parent is updated (set to <tt>null</tt>).
	 *
	 * <p>The parent host of the storageFile's Storage should be powered off if
	 * the cloud provider's power manger allows it to.
	 *
	 * @param storageFile
	 * @throws IllegalArgumentException if the storageFile was not placed using this entity
	 */
	public void unplaceStorageFile(StorageFile storageFile);
}
