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
 * For more details on the implementation, refer to the cumulative distribution function in <a href="http://en.wikipedia.org/wiki/Uniform_distribution_(continuous)">wikipedia</a>.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class Uniform extends AbstractRandom {
	private double m;
	private double b;

	/**
	 * @param min inclusive
	 * @param max inclusive
	 * @param generator
	 */
	public Uniform(double min, double max, Random generator) {
		super(generator);
		this.m = max - min;
		if (this.m<0)
			throw new IllegalArgumentException("min is greater than max.");
		this.b = min;
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 * 
	 * @see Uniform#Uniform(double, double, Random)
	 */
	public Uniform(double min, double max) {
		this(min, max, new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	/**
	 * Equivalent to {@code Uniform(generator, 0.0d, max)}.
	 * 
	 * @param max inclusive
	 * @param generator
	 */
	public Uniform(double max, Random generator) {
		this(0.0d, max, generator);
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 * 
	 * @see Uniform#Uniform(double, Random)
	 */
	public Uniform(double max) {
		this(max, new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	/**
	 * Equivalent to {@code Uniform(generator, 0.0d, Double.MAX_VALUE)}.
	 * 
	 * @param generator
	 */
	public Uniform(Random generator) {
		this(0.0d, Double.MAX_VALUE, generator);
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 * 
	 * @see Uniform#Uniform(double, double, Random)
	 */
	public Uniform() {
		this(new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	@Override
	public boolean nextBoolean() {
		return getGenerator().nextBoolean();
	}

	@Override
	public byte nextByte() {
		return (byte) nextInt();
	}

	@Override
	public short nextShort() {
		return (short) nextInt();
	}

	@Override
	public int nextInt() {
		return getGenerator().nextInt((int) this.m + 1) + (int) this.b;
	}

	@Override
	public long nextLong() {
		if (this.m < Integer.MAX_VALUE)
			return getGenerator().nextInt((int) this.m + 1) + (long) this.b;
		else
			return (long) nextDouble();
	}

	@Override
	public float nextFloat() {
		return (float) nextDouble();
	}

	@Override
	public double nextDouble() {
		return getGenerator().nextDouble() * this.m + this.b;
	}
}
