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

package com.samysadi.acs.utility.random;

import java.util.Random;


/**
 * A generator that always generate the same number.
 *
 * @param <E>
 *
 * @since 1.0
 */
public class Constant<E extends Number> implements NumberGenerator {
	private E val;

	public Constant(E value) {
		super();
		this.val = value;
	}

	public Constant(E val, Random generator) {
		this(val);
	}

	@Override
	public boolean nextBoolean() {
		return nextInt() != 0;
	}

	@Override
	public byte nextByte() {
		return val.byteValue();
	}

	@Override
	public short nextShort() {
		return val.shortValue();
	}

	@Override
	public int nextInt() {
		return val.intValue();
	}

	@Override
	public float nextFloat() {
		return val.floatValue();
	}

	@Override
	public long nextLong() {
		return val.longValue();
	}

	@Override
	public double nextDouble() {
		return val.doubleValue();
	}
}
