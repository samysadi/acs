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

import java.util.ArrayList;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.misc.MemoryUnitImpl;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;


/**
 * 
 * @since 1.0
 */
public class StorageDefault extends MemoryUnitImpl<StorageFile> implements Storage {
	private StorageProvisioner storageProvisioner;

	/**
	 * Empty constructor that creates a storage with zero capacity.
	 * 
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link StorageDefault#StorageDefault(long)} though.
	 */
	public StorageDefault() {
		this(0);
	}

	public StorageDefault(long capacity) {
		super(capacity);
	}

	@Override
	public StorageDefault clone() {
		final StorageDefault clone = (StorageDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.storageProvisioner = null;
	}

	@Override
	public Host getParent() {
		return (Host) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Host))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public Host getParentHost() {
		return getParent();
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof StorageProvisioner) {
			if (this.storageProvisioner == entity)
				return;
			if (this.storageProvisioner != null)
				this.storageProvisioner.setParent(null);
			this.storageProvisioner = (StorageProvisioner) entity;
		} else if ((entity instanceof MemoryZone) && (!(entity instanceof StorageFile))) {
			throw new IllegalArgumentException("Only StorageFile MemoryZones are allowed");
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof StorageProvisioner) {
			if (this.storageProvisioner != entity)
				return;
			this.storageProvisioner = null;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<Entity> l = new ArrayList<Entity>(1);
		if (this.storageProvisioner != null)
			l.add(this.storageProvisioner);

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		r.add(l);
		return new MultiListView<Entity>(r);
	}

	@Override
	final public List<StorageFile> getStorageFiles() {
		return getMemoryZones();
	}

	@Override
	public StorageProvisioner getStorageProvisioner() {
		return storageProvisioner;
	}
}
