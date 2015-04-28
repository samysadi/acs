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

import com.samysadi.acs.core.Simulator;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class AbstractRandom implements NumberGenerator {
	private java.util.Random generator;

	public AbstractRandom(Random generator) {
		super();
		this.generator = generator;
	}

	public AbstractRandom() {
		this(new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	protected Random getGenerator() {
		return generator;
	}

	@Override
	public boolean nextBoolean() {
		return 0 == (nextLong() & 1);
	}

	@Override
	public byte nextByte() {
		return (byte) nextLong();
	}

	@Override
	public short nextShort() {
		return (short) nextLong();
	}

	@Override
	public int nextInt() {
		return (int) nextLong();
	}

	@Override
	public float nextFloat() {
		return (float) nextDouble();
	}

	@Override
	public long nextLong() {
		return (long) nextDouble();
	}

	@Override
	public abstract double nextDouble();
}
