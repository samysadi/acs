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

package com.samysadi.acs.hardware.storage;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Contains methods that defines a file stored on a {@link Storage}.
 *
 * <p>All implementation classes should provide a constructor with one argument
 * of <tt>long</tt> type that specifies the StorageFile's size.
 *
 * @since 1.0
 */
public interface StorageFile extends MemoryZone {
	public enum StorageFileShareMode {
		/** only one operation can run concurrently on this file */
		EXCLUSIVE,
		/** only read operations can run concurrently on this file */
		READ,
		/** only write operations can run concurrently on this file */
		WRITE,
		/** both read and write operations can run concurrently on this file */
		READWRITE,
		USER1, USER2, USER3
	}

	@Override
	public StorageFile clone();

	@Override
	public Storage getParent();

	/**
	 * Returns the {@link StorageFileShareMode} of this file.
	 *
	 * @return the {@link StorageFileShareMode} of this file
	 */
	public StorageFileShareMode getShareMode();

	/**
	 * Updates the {@link StorageFileShareMode} of this file.
	 *
	 * @param newShareMode
	 */
	public void setShareMode(StorageFileShareMode newShareMode);

	/**
	 * Returns <tt>true</tt> if an append operation can run now on this file.
	 *
	 * @return <tt>true</tt> if an append operation can run now on this file
	 */
	public boolean canAppend();

	/**
	 * Returns <tt>true</tt> if a write operation can run now on this file.
	 *
	 * @return <tt>true</tt> if a write operation can run now on this file
	 */
	public boolean canWrite();

	/**
	 * Returns <tt>true</tt> if a read operation can run now on this file.
	 *
	 * @return <tt>true</tt> if a read operation can run now on this file
	 */
	public boolean canRead();

	/**
	 * This method is called by a {@link StorageOperation} when it is started.
	 * @param operation
	 */
	public void dealWithOperationActivation(StorageOperation operation);

	/**
	 * This method is called by a {@link StorageOperation} when it is stopped.
	 * @param operation
	 * @param oldCompletedLength the completed length before this operation is started. This can be used to
	 * compute newly completed length during current run (using {@code operation.getCompletedLength() - oldCompletedLength}).
	 */
	public void dealWithOperationDeactivation(StorageOperation operation, long oldCompletedLength);

	/**
	 * Returns the {@link SfPlacementPolicy} that was used when placing this StorageFile in the current storage.
	 * <tt>null</tt> is returned if this StorageFile has no defined {@link SfPlacementPolicy}.
	 *
	 * @return the {@link SfPlacementPolicy} that was used when placing this StorageFile or <tt>null</tt>
	 */
	public SfPlacementPolicy getPlacementPolicy();

	/**
	 * This method is called by {@link SfPlacementPolicy} when placing this StorageFile on a storage.
	 *
	 * <p>You <b>should not</b> need to call this method if you are not implementing a placement policy.
	 *
	 * @param policy
	 */
	public void setPlacementPolicy(SfPlacementPolicy policy);

	/**
	 * Unplaces current StorageFile using current set {@link SfPlacementPolicy}, or sets a <tt>null</tt> parent for current VM
	 * if no {@link SfPlacementPolicy} was set.<br/>
	 * In both situations, this method guarantees that the current StorageFile's parent will be set to <tt>null</tt>.
	 * Because this may take time, you need to listen to {@link NotificationCodes#ENTITY_PARENT_CHANGED} to know when
	 * all actions have been taken.
	 *
	 * <p>If you only want to set a <tt>null</tt> parent for this StorageFile then use {@link StorageFile#setParent(Entity)}.
	 * Which will set the <tt>null</tt> parent immediately.
	 */
	public void unplace();

	/**
	 * Returns the {@link User} that owns this StorageFile.
	 *
	 * @return the {@link User} that owns this StorageFile
	 */
	public User getUser();

	/**
	 * Updates the {@link User} that owns this StorageFile.
	 *
	 * @param user
	 */
	public void setUser(User user);

}
