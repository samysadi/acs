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

import java.util.HashMap;
import java.util.Map;

import com.samysadi.acs.utility.collections.Bitmap;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
class MemoryMaps extends HashMap<Object, Bitmap> {
	private static final long serialVersionUID = 1L;

	public MemoryMaps() {
		super();
	}

	public MemoryMaps(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public MemoryMaps(int initialCapacity) {
		super(initialCapacity);
	}

	public MemoryMaps(Map<? extends Object, ? extends Bitmap> m) {
		super(m);
	}

	/**
	 * Creates a deep copy of this MemoryMaps
	 */
	@Override
	public MemoryMaps clone() {
		MemoryMaps clone = new MemoryMaps(this.size());
		for (Map.Entry<Object, Bitmap> e: this.entrySet())
			clone.put(e.getKey(), e.getValue());
		return clone;
	}

	public Bitmap getBitmap(Object id, long zoneSize) {
		Bitmap b = this.get(id);
		if (b == null) {
			b = new Bitmap();
			b.mark(0, zoneSize);
			this.put(id, b);
		}
		return b;
	}
}
