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

import java.util.List;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.utility.NotificationCodes;


/**
 *
 * @since 1.0
 */
public interface MemoryUnit<Zone extends MemoryZone> extends Entity {

	@Override
	public MemoryUnit<Zone> clone();

	/**
	 * Returns the Host that contains this memory unit.
	 * This may be different from {@link Entity#getParent()}.
	 *
	 * @return the Host that contains this memory unit
	 */
	public Host getParentHost();

	/**
	 * Returns the total capacity of this memory unit in number of {@link Simulator#BYTE}s.
	 *
	 * @return the total capacity of this memory unit in number of {@link Simulator#BYTE}s
	 */
	public long getCapacity();

	/**
	 * Updates the capacity (in number of {@link Simulator#BYTE}s) of this {@link MemoryUnit}.
	 *
	 * <p>The capacity difference is added or subtracted to/from the
	 * free capacity of this unit.
	 *
	 * <p>A {@link NotificationCodes#MU_CAPACITY_CHANGED} notification is thrown.
	 *
	 * @param capacity
	 * @throws IllegalArgumentException if the new capacity cannot be set, if is negative or if it will
	 * result in a negative free capacity
	 */
	public void setCapacity(long capacity);

	/**
	 * Returns the total free capacity (in number of {@link Simulator#BYTE}s) on this memory unit.
	 *
	 * @return the total free capacity (in number of {@link Simulator#BYTE}s) on this memory unit
	 */
	public long getFreeCapacity();

	/**
	 * Allocates the given <tt>size</tt> on the memory unit.<br/>
	 * You should not have to call this method directly.
	 * Throws a {@link NotificationCodes#MU_FREE_CAPACITY_CHANGED} notification.
	 *
	 * @param size the size in number of {@link Simulator#BYTE}s
	 * @throws IllegalArgumentException if the given size cannot be allocated, use {@code getFreeCapacity} first
	 */
	public void allocate(long size);

	/**
	 * Frees size on the memory unit.<br/>
	 * You should not have to call this method directly.
	 * Throws a {@link NotificationCodes#MU_FREE_CAPACITY_CHANGED} notification.
	 *
	 * @param size the size in number of {@link Simulator#BYTE}s
	 * @throws IllegalArgumentException if the given size cannot be freed
	 */
	public void free(long size);

	/**
	 * Returns a list containing all memory zones inside this MemoryUnit.
	 *
	 * @return a list containing all memory zones inside this MemoryUnit
	 */
	public List<Zone> getMemoryZones();

	/**
	 * Returns the first MemoryZone candidate that satisfies {@code candidate.isReplicaOf(zone)}, or <tt>null</tt>.
	 *
	 * <p>If the given <tt>zone</tt>'s parent is the current memory unit, then it is returned directly.
	 *
	 * @param zone
	 * @return the first MemoryZone candidate that satisfies {@code candidate.isReplicaOf(zone)}, or <tt>null</tt>
	 */
	public Zone findReplica(Zone zone);

}
