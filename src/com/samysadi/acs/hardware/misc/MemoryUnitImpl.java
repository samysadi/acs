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

package com.samysadi.acs.hardware.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntityImpl;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;


/**
 *
 * @since 1.0
 */
public abstract class MemoryUnitImpl<Zone extends MemoryZone> extends FailureProneEntityImpl implements MemoryUnit<Zone> {
	private long capacity;
	private long freeCapacity;
	private List<Zone> zones;

	protected MemoryUnitImpl(long capacity) {
		super();
		setCapacity(capacity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public MemoryUnitImpl<Zone> clone() {
		final MemoryUnitImpl<Zone> clone = (MemoryUnitImpl<Zone>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.zones = null;
		this.freeCapacity = this.capacity;
	}

	@Override
	public long getCapacity() {
		return this.capacity;
	}

	@Override
	public void setCapacity(long capacity) {
		long delta = capacity - this.capacity;
		if (capacity < 0)
			throw new IllegalArgumentException("Illegal capacity.");
		long newFreeCapacity = this.getFreeCapacity() + delta;
		if (newFreeCapacity < 0)
			throw new IllegalArgumentException("Illegal capacity (negative).");

		this.capacity = capacity;
		notify(NotificationCodes.MU_CAPACITY_CHANGED, null);
		setFreeCapacity(newFreeCapacity);
	}

	@Override
	public long getFreeCapacity() {
		return this.freeCapacity;
	}

	protected void setFreeCapacity(long freeCapacity) {
		if ((freeCapacity < 0) || (freeCapacity > getCapacity()))
			throw new IllegalArgumentException("Illegal new free capacity");

		this.freeCapacity = freeCapacity;
		notify(NotificationCodes.MU_FREE_CAPACITY_CHANGED, null);
	}

	@Override
	public void allocate(long size) {
		if (size < 0)
			throw new IllegalArgumentException("Cannot allocate the requested size");
		setFreeCapacity(this.getFreeCapacity() - size);
	}

	@Override
	public void free(long size) {
		if (size < 0)
			throw new IllegalArgumentException("Cannot free the requested size");
		setFreeCapacity(this.getFreeCapacity() + size);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof MemoryZone) {
			if (zones == null)
				zones = new ArrayList<Zone>();
			if (!zones.add((Zone) entity))
				return;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof MemoryZone) {
			if (zones == null)
				return;
			if (!zones.remove(entity))
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

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		if (this.zones != null)
			r.add(this.zones);
		return new MultiListView<Entity>(r);
	}

	@Override
	public List<Zone> getMemoryZones() {
		if (this.zones == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.zones);
	}

	@Override
	public Zone findReplica(Zone zone) {
		if (zone.getParent() == this)
			return zone;
		for (Zone r: getMemoryZones())
			if (r.isReplicaOf(zone))
				return r;
		return null;
	}
}
