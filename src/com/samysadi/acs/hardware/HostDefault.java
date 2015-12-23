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

package com.samysadi.acs.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.network.NetworkDeviceDefault;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.ram.Ram;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;
import com.samysadi.acs.utility.collections.infrastructure.Rack;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public class HostDefault extends NetworkDeviceDefault implements Host {
	private List<VirtualMachine> vms;
	private Rack rack;
	private Ram ram;
	private List<ProcessingUnit> processingUnits;
	private List<Storage> storages;

	public HostDefault() {
		super();
	}

	@Override
	public HostDefault clone() {
		final HostDefault clone = (HostDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.vms = new ArrayList<VirtualMachine>();
		this.rack = null;
		this.ram = null;
		this.processingUnits = new LinkedList<ProcessingUnit>();
		this.storages = new ArrayList<Storage>();
	}

	@Override
	public CloudProvider getParent() {
		return (CloudProvider) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof CloudProvider))
				throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);
		this.rack = (getParent() != null) ? getParent().getDefaultRack() : null;
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof VirtualMachine) {
			if (!this.vms.add((VirtualMachine) entity))
				return;
		} else if (entity instanceof Ram) {
			if (this.ram == entity)
				return;
			if (this.ram != null)
				this.ram.setParent(null);
			this.ram = (Ram) entity;
		} else if (entity instanceof ProcessingUnit) {
			if (!this.processingUnits.add((ProcessingUnit) entity))
				return;
		} else if (entity instanceof Storage) {
			if (!this.storages.add((Storage) entity))
				return;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof VirtualMachine) {
			if (!this.vms.remove(entity))
				return;
		} else if (entity instanceof Ram) {
			if (this.ram != entity)
				return;
			this.ram = null;
		} else if (entity instanceof ProcessingUnit) {
			if (!this.processingUnits.remove(entity))
				return;
		} else if (entity instanceof Storage) {
			if (!this.storages.remove(entity))
				return;
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
		if (this.ram != null)
			l.add(this.ram);

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		r.add(l);
		r.add(this.vms);
		r.add(this.processingUnits);
		r.add(this.storages);
		return new MultiListView<Entity>(r);
	}

	@Override
	public Rack getRack() {
		return this.rack;
	}

	@Override
	public Ram getRam() {
		return this.ram;
	}

	@Override
	public List<VirtualMachine> getVirtualMachines() {
		return Collections.unmodifiableList(this.vms);
	}

	@Override
	public List<ProcessingUnit> getProcessingUnits() {
		return Collections.unmodifiableList(this.processingUnits);
	}

	@Override
	public List<Storage> getStorages() {
		return Collections.unmodifiableList(this.storages);
	}

	@Override
	public boolean isRoutingEnabled() {
		return false;
	}
}
