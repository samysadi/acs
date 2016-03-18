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

package com.samysadi.acs.hardware.ram;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.misc.MemoryZoneImpl;

/**
 *
 * @since 1.0
 */
public class RamZoneDefault extends MemoryZoneImpl implements RamZone {

	/**
	 * Empty constructor that creates an empty ram zone.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link RamZoneDefault#RamZoneDefault(long)} though.
	 */
	public RamZoneDefault() {
		this(0);
	}

	public RamZoneDefault(long size) {
		super(size);
	}

	@Override
	public RamZoneDefault clone() {
		final RamZoneDefault clone = (RamZoneDefault) super.clone();
		return clone;
	}

	@Override
	public Ram getParent() {
		return (Ram) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Ram))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}
}
