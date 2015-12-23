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

package com.samysadi.acs.core.tracing;

import com.samysadi.acs.core.Simulator;

/**
 * This class defines a pair that contains a traced value,
 * and the time when the traced value was taken.
 * 
 * @since 1.0
 */
public class TraceItem<T> {
	private long time;
	private T value;

	public TraceItem(long time, T value) {
		super();
		this.time = time;
		this.value = value;
	}

	public long getTime() {
		return this.time;
	}

	public T getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "[" + Simulator.formatTime(this.time) + "," + 
				(this.value == null ? "null" : this.value.toString()) + "]";
	}
}