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

package com.samysadi.acs.hardware.pu;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntityImpl;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.pu.operation.provisioner.ComputingProvisioner;
import com.samysadi.acs.utility.NotificationCodes;

/**
 *
 * @since 1.0
 */
public class ProcessingUnitDefault extends FailureProneEntityImpl implements ProcessingUnit {
	private ComputingProvisioner computingProvisioner;

	private boolean isAllocatedFlag;

	public ProcessingUnitDefault() {
		super();
	}

	@Override
	public ProcessingUnitDefault clone() {
		final ProcessingUnitDefault clone = (ProcessingUnitDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.isAllocatedFlag = false;
		this.computingProvisioner = null;
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
	public void addEntity(Entity entity) {
		if (entity instanceof ComputingProvisioner) {
			if (this.computingProvisioner == entity)
				return;
			if (this.computingProvisioner != null)
				this.computingProvisioner.setParent(null);
			this.computingProvisioner = (ComputingProvisioner) entity;
		}
		super.addEntity(entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof ComputingProvisioner) {
			if (this.computingProvisioner != entity)
				return;
			this.computingProvisioner = null;
		}
		super.removeEntity(entity);
	}

	@Override
	public ComputingProvisioner getComputingProvisioner() {
		return computingProvisioner;
	}

	@Override
	public boolean isAllocated() {
		return isAllocatedFlag;
	}

	@Override
	public void setAllocated(boolean b) {
		if (b == isAllocatedFlag)
			return;
		isAllocatedFlag = b;
		notify(NotificationCodes.ENTITY_ALLOCATED_FLAG_CHANGED, null);
	}
}
