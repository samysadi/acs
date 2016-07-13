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
 * For more details on the implementation, refer to the cumulative distribution function in <a href="http://en.wikipedia.org/wiki/Geometric_distribution">wikipedia</a>.
 *
 * @since 1.0
 */
public class Geometric extends AbstractRandom {
	private double p;

	/**
	 * @param p <math><mi>p</mi><mo>∈</mo><mo>]</mo><mn>0</mn><mo>..</mo><mn>1</mn><mo>]</mo></math> (see {@link Geometric}).
	 * @param generator
	 */
	public Geometric(double p, Random generator) {
		super(generator);
		this.p = p;
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 *
	 * @param p
	 * @see Geometric#Geometric(double, Random)
	 */
	public Geometric(double p) {
		this(p, new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	@Override
	public double nextDouble() {
		return Math.round(Math.log(getGenerator().nextDouble()/p) / Math.log(1.0d-p) - 1.0d);
	}
}
