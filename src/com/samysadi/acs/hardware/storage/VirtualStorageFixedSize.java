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

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.misc.VirtualMemoryUnitFixedSize;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.hardware.storage.operation.StorageOperation.StorageOperationType;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;


/**
 * @see VirtualMemoryUnitFixedSize
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class VirtualStorageFixedSize extends VirtualMemoryUnitFixedSize<StorageFile, Storage> implements VirtualStorage {

	private SfPlacementPolicy sfPlacementPolicy;
	private User user;

	/**
	 * Empty constructor that creates a storage with zero capacity.
	 * 
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link VirtualStorageFixedSize#VirtualStorageFixedSize(long)} though.
	 */
	public VirtualStorageFixedSize() {
		this(0);
	}

	public VirtualStorageFixedSize(long capacity) {
		super(capacity);
		this.sfPlacementPolicy = null;
		this.user = null;
	}

	@Override
	public VirtualStorageFixedSize clone() {
		final VirtualStorageFixedSize clone = (VirtualStorageFixedSize) super.clone();
		return clone;
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Storage))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	final public List<StorageFile> getStorageFiles() {
		return getMemoryZones();
	}

	@Override
	public StorageProvisioner getStorageProvisioner() {
		return getParent().getStorageProvisioner();
	}

	@Override
	public StorageFileShareMode getShareMode() {
		return StorageFileShareMode.READWRITE;
	}

	@Override
	public void setShareMode(StorageFileShareMode shareMode) {
		throw new UnsupportedOperationException("Not allowed for a VirtualStorage");
	}

	@Override
	public boolean canAppend() {
		return false;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public void dealWithOperationActivation(StorageOperation operation) {
		if (operation.getType() != StorageOperationType.READ)
			throw new UnsupportedOperationException("Direct operations other than read operations on a VirtualStorage are not allowed.");
	}

	@Override
	public void dealWithOperationDeactivation(StorageOperation operation, long oldCompletedLength) {
		if (operation.getType() != StorageOperationType.READ)
			throw new UnsupportedOperationException("Direct operations other than read operations on a VirtualStorage are not allowed.");
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
		this.user.notify(NotificationCodes.USER_STORAGEFILE_ATTACHED, this);
	}
}
