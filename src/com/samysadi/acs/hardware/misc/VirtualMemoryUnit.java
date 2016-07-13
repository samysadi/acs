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

import com.samysadi.acs.core.entity.AllocatableEntity;

/**
 *
 * @param <Zone>
 * @param <Unit>
 *
 * @since 1.0
 */
public interface VirtualMemoryUnit<Zone extends MemoryZone, Unit extends MemoryUnit<Zone>> extends MemoryUnit<Zone>, MemoryZone, AllocatableEntity {
	@Override
	public VirtualMemoryUnit<Zone, Unit> clone();

	@Override
	public Unit getParent();

	/**
	 * Returns real free capacity that can be allocated.
	 *
	 * <p>Use this method to get real free capacity on this virtual memory unit, when considering
	 * parent unit capacity.<br/>
	 * Because the capacity of a virtual memory unit may not be immediately reserved,
	 * {@link VirtualMemoryUnit#getFreeCapacity()} may return a greater value.
	 *
	 * @return real free capacity that can be allocated
	 */
	public long getRealFreeCapacity();

	/**
	 * Returns <tt>true</tt> if the capacity of this virtual memory unit is reserved.
	 *
	 * <p>Default value is <tt>false</tt>.
	 *
	 * @return <tt>true</tt> if the capacity of this virtual memory unit is reserved
	 * @see VirtualMemoryUnit#setIsCapacityReserved(boolean)
	 */
	public boolean isCapacityReserved();

	/**
	 * Updates the isCapacityReserved flag of this virtual memory unit.
	 *
	 * <p>When <tt>true</tt>, this virtual memory unit's capacity is fully allocated on parent memory unit
	 * even if its size (see {@link VirtualMemoryUnit#getSize()}) is smaller.
	 *
	 * <p>When <tt>false</tt>, only the size of this virtual memory unit is allocated on parent
	 * memory unit. This is the default behavior.
	 *
	 * @param v
	 */
	public void setIsCapacityReserved(boolean v);
}
