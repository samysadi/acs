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

import com.samysadi.acs.utility.NotificationCodes;

/**
 * Use this class to simulate virtual memory units whose size grows and shrinks depending on the size allocated
 * to children zones.
 * 
 * <p>Initially this virtual memory unit will not occupy any space on the parent memory unit. When creating new zones
 * the occupied size grows up, and it decreases when those zones are deleted. Anyhow, the occupied size cannot
 * increase more than the initial given capacity.
 * 
 * <p>This implementation is different from {@link VirtualMemoryUnitDynamic} in the fact that the occupied size on 
 * the parent memory unit can decrease.
 * 
 * @since 1.0
 */
public abstract class VirtualMemoryUnitElastic<Zone extends MemoryZone, Unit extends MemoryUnit<Zone>> extends VirtualMemoryUnitImpl<Zone, Unit> {
	/**
	 * Creates a new VirtualMemoryUnit which size can grow up to the maximum given <tt>capacity</tt>, but can also
	 * decrease when zones are freed.<br/>
	 * Its initial size is {@code 0l}.
	 * 
	 * @param capacity the maximum capacity of this Virtual Memory Unit
	 */
	public VirtualMemoryUnitElastic(long capacity) {
		super(capacity);
	}

	@Override
	public VirtualMemoryUnitElastic<Zone, Unit> clone() {
		final VirtualMemoryUnitElastic<Zone, Unit> clone = (VirtualMemoryUnitElastic<Zone, Unit>) super.clone();
		return clone;
	}

	@Override
	public void allocate(long size) {
		if (size == 0)
			return;

		super.allocate(size);

		//Now let's grow, and allocate new space in the parent memory unit
		if (getParent() != null)
			getParent().allocate(size);
		notify(NotificationCodes.MZ_SIZE_CHANGED, null);
	}

	@Override
	public void free(long size) {
		if (size == 0)
			return;

		super.free(size);

		//Now let's shrink, and free allocated space in the parent memory unit
		if (getParent() != null)
			getParent().free(size);
		notify(NotificationCodes.MZ_SIZE_CHANGED, null);
	}

	@Override
	public long getSize() {
		return getCapacity() - getFreeCapacity();
	}
}
