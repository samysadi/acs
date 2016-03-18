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


/**
 * Use this class to simulate fixed-size virtual memory units.
 *
 * <p>A fixed-size VirtualMemoryUnit immediately allocates its capacity on the parent MemoryUnit.<br/>
 * So, if you create a 5GB virtual Memory Unit and only use 3GB, then 5GB are still allocated on the parent MemoryUnit.
 *
 * @since 1.0
 */
public abstract class VirtualMemoryUnitFixedSize<Zone extends MemoryZone, Unit extends MemoryUnit<Zone>> extends VirtualMemoryUnitImpl<Zone, Unit>  {

	/**
	 * Creates a new VirtualMemoryUnit with the given <tt>capacity</tt>. Its capacity is immediately
	 * allocated on the parent MemoryUnit.
	 *
	 * @param capacity the maximum capacity of this VirtualMemoryUnit
	 */
	public VirtualMemoryUnitFixedSize(long capacity) {
		super(capacity);
	}

	@Override
	public VirtualMemoryUnitFixedSize<Zone, Unit> clone() {
		final VirtualMemoryUnitFixedSize<Zone, Unit> clone = (VirtualMemoryUnitFixedSize<Zone, Unit>) super.clone();
		return clone;
	}

	@Override
	public long getSize() {
		return getCapacity();
	}
}
