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

package com.samysadi.acs.user;

import java.util.ArrayList;
import java.util.Collections;
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
public class ThinClientDefault extends NetworkDeviceDefault implements ThinClient {
	private ThinClientVirtualMachine mainVm;

	public ThinClientDefault() {
		super();
	}

	@Override
	public ThinClientDefault clone() {
		final ThinClientDefault clone = (ThinClientDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.mainVm = null;
	}

	@Override
	public User getParent() {
		return (User) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof User))
				throw new IllegalArgumentException("The given entity cannot be a parent of this entity");

		boolean wasRunning = false;
		if (this.mainVm != null && parent != getParent()) {
			if (this.mainVm.isRunning()) {
				wasRunning = true;
				this.mainVm.doPause();
			}
		}

		super.setParent(parent);

		if (wasRunning)
			this.mainVm.doStart();
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof ThinClientVirtualMachine) {
			if (this.mainVm == entity)
				return;
			if (this.mainVm != null)
				this.mainVm.setParent(null);
			this.mainVm = (ThinClientVirtualMachine) entity;
		} else if (
			(entity instanceof VirtualMachine) ||
			(entity instanceof Ram) ||
			(entity instanceof ProcessingUnit) ||
			(entity instanceof Storage)) {
			throw new IllegalArgumentException("You cannot add such entity to a ThinClient");
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof ThinClientVirtualMachine) {
			if (this.mainVm != entity)
				return;
			this.mainVm = null;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		if (this.mainVm != null) {
			List<Entity> l = new ArrayList<Entity>(1);
			l.add(this.mainVm);

			List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
			r.add(s);
			r.add(l);
			return new MultiListView<Entity>(r);
		} else
			return s;
	}

	@Override
	public CloudProvider getCloudProvider() {
		if (getParent() == null)
			return null;
		return getParent().getParent();
	}

	@Override
	public Rack getRack() {
		return null;
	}

	@Override
	public Ram getRam() {
		return null;
	}

	@Override
	public List<VirtualMachine> getVirtualMachines() {
		VirtualMachine vm = getVirtualMachine();
		if (vm != null) {
			List<VirtualMachine> l = new ArrayList<VirtualMachine>(1);
			l.add(vm);
			return Collections.unmodifiableList(l);
		} else
			return Collections.emptyList();
	}

	@Override
	public List<ProcessingUnit> getProcessingUnits() {
		return Collections.emptyList();
	}

	@Override
	public List<Storage> getStorages() {
		return Collections.emptyList();
	}

	@Override
	public boolean isRoutingEnabled() {
		return false;
	}

	@Override
	public boolean supportsFailureStateUpdate() {
		return false;
	}

	@Override
	public ThinClientVirtualMachine getVirtualMachine() {
		return this.mainVm;
	}
}
