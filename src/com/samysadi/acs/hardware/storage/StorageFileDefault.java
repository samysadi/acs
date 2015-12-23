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
import com.samysadi.acs.hardware.misc.MemoryZoneImpl;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.hardware.storage.operation.StorageOperation.StorageOperationType;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * 
 * @since 1.0
 */
public class StorageFileDefault extends MemoryZoneImpl implements StorageFile {
	private StorageFileShareMode shareMode;

	private int writersCount;
	private int readersCount;

	private SfPlacementPolicy sfPlacementPolicy;
	private User user;

	/**
	 * Empty constructor that creates an empty file (with size = 0).
	 * 
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link StorageFileDefault#StorageFileDefault(long)} though.
	 */
	public StorageFileDefault() {
		this(0);
	}

	public StorageFileDefault(long size) {
		super(size);
		this.shareMode = StorageFileShareMode.READWRITE;
		this.writersCount = 0;
		this.readersCount = 0;
		this.sfPlacementPolicy = null;
	}

	@Override
	public StorageFileDefault clone() {
		final StorageFileDefault clone = (StorageFileDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		User user = this.user;
		this.user = null;
		this.setUser(user);
	}

	@Override
	public Storage getParent() {
		return (Storage) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Storage))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public StorageFileShareMode getShareMode() {
		return shareMode;
	}

	@Override
	public void setShareMode(StorageFileShareMode shareMode) {
		if (shareMode == this.shareMode)
			return;

		//don't allow changing share mode if doing so would result in inconsistent states
		if ((shareMode == StorageFileShareMode.EXCLUSIVE && (this.writersCount > 0 || this.readersCount > 0))
				|| (shareMode == StorageFileShareMode.READ && this.writersCount > 0)
				|| (shareMode == StorageFileShareMode.WRITE && this.readersCount > 0))
			throw new IllegalArgumentException("Cannot set the given share mode.");

		this.shareMode = shareMode;
		notify(NotificationCodes.SF_SHAREMODE_CHANGED, null);
	}

	@Override
	public boolean canAppend() {
		return canWrite();
	}

	@Override
	public boolean canWrite() {
		return this.getShareMode() == StorageFileShareMode.WRITE
				|| this.getShareMode() == StorageFileShareMode.READWRITE;
	}

	@Override
	public boolean canRead() {
		return this.getShareMode() == StorageFileShareMode.READ
				|| this.getShareMode() == StorageFileShareMode.READWRITE;
	}

	@Override
	public void dealWithOperationActivation(StorageOperation operation) {
		if (operation.getType() == StorageOperationType.APPEND) {
			//update size
			long toAllocate = operation.getLength() - operation.getCompletedLength();
			if (toAllocate > 0)
				setSize(getSize() + toAllocate);
		}
	
		if (operation.getType() == StorageOperationType.APPEND || operation.getType() == StorageOperationType.WRITE)
			writersCount++;
		else if (operation.getType() == StorageOperationType.READ)
			readersCount++;
	}

	@Override
	public void dealWithOperationDeactivation(StorageOperation operation, long oldCompletedLength) {
		if (operation.getType() == StorageOperationType.APPEND) {
			//free the not appended size
			long toFree = operation.getLength() - operation.getCompletedLength();
			if (toFree > 0)
				setSize(getSize() - toFree);
		}

		if (operation.getType() == StorageOperationType.APPEND || operation.getType() == StorageOperationType.WRITE)
			writersCount = Math.max(0, writersCount - 1);
		else if (operation.getType() == StorageOperationType.READ)
			readersCount = Math.max(0, readersCount - 1);

		//update this zones' MetaData if it is modified
		if (operation.getType() != StorageOperationType.READ)
			modify(oldCompletedLength + operation.getFilePos(), operation.getCompletedLength() - oldCompletedLength);
	}

	@Override
	public SfPlacementPolicy getPlacementPolicy() {
		return this.sfPlacementPolicy;
	}

	@Override
	public void setPlacementPolicy(SfPlacementPolicy policy) {
		this.sfPlacementPolicy = policy;
	}

	@Override
	public void unplace() {
		if (this.getPlacementPolicy() != null)
			this.getPlacementPolicy().unplaceStorageFile(this);
		else
			this.setParent(null);
	}

	@Override
	public User getUser() {
		return this.user;
	}

	@Override
	public void setUser(User user) {
		if (this.user == user)
			return;

		final User old = this.user;

		this.user = user;
		notify(NotificationCodes.SF_USER_CHANGED, null);

		if (old != null)
			old.notify(NotificationCodes.USER_STORAGEFILE_DETACHED, this);
		if (this.user != null)
			this.user.notify(NotificationCodes.USER_STORAGEFILE_ATTACHED, this);
	}
}
