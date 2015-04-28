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
 * Use this class to simulate dynamically allocated virtual memory units.
 * 
 * <p>This VirtualMemoryUnit will initially not occupy any space on the parent memory unit, but will grow every time a 
 * new zone is created, until the virtual memory unit reaches the maximum capacity chosen when it was created.
 * 
 * <p>The occupied space on the parent memory unit will not decrease even if some zones are deleted.
 * The occupied space can only increase.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class VirtualMemoryUnitDynamic<Zone extends MemoryZone, Unit extends MemoryUnit<Zone>> extends VirtualMemoryUnitImpl<Zone, Unit> {
	/**
	 * The size that was allocated in the parent {@link MemoryUnit}
	 */
	private long size;

	/**
	 * Creates a new VirtualMemoryUnit which size can grow up to the maximum given <tt>capacity</tt>.
	 * Its initial size is {@code 0l}.
	 * 
	 * @param capacity the maximum capacity of this Virtual Memory Unit
	 */
	public VirtualMemoryUnitDynamic(long capacity) {
		super(capacity);
		this.size = 0l;
	}

	@Override
	public VirtualMemoryUnitDynamic<Zone, Unit> clone() {
		final VirtualMemoryUnitDynamic<Zone, Unit> clone = (VirtualMemoryUnitDynamic<Zone, Unit>) super.clone();
		return clone;
	}

	@Override
	public void allocate(long size) {
		super.allocate(size);

		//Now let's grow, and allocate new space in the parent memory unit if needed
		//we need to recompute deltaSize, it may be different from size
		//this is because this implementation will grow in size but will not shrink
		long newSize = getCapacity() - getFreeCapacity();
		long deltaSize = newSize - this.size;
		if (deltaSize > 0) {
			if (getParent() != null)
				getParent().allocate(deltaSize);
			this.size = newSize;
			notify(NotificationCodes.MZ_SIZE_CHANGED, null);
		}
	}

	@Override
	public void free(long size) {
		super.free(size);

		//nothing else. For this virtual memory unit, size can only grow up.
	}

	@Override
	public long getSize() {
		return this.size;
	}
}
