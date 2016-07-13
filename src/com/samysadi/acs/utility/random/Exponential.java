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
 * For more details on the implementation, refer to the cumulative distribution function in <a href="http://en.wikipedia.org/wiki/Exponential_distribution">wikipedia</a>.
 *
 * @since 1.0
 */
public class Exponential extends AbstractRandom {
	private double negMean;

	/**
	 * Generated numbers are between 0 (inclusive) and positive infinity (exclusive).
	 *
	 * @param mean <math><mi>mean</mi><mo>&gt;</mo><mn>0</mn></math> also equal to <math><msup><mi>λ</mi><mrow><mo>-</mo><mn>1</mn></mrow></msup></math> (see {@link Exponential}).
	 * @param generator
	 */
	public Exponential(double mean, Random generator) {
		super(generator);
		this.negMean = -mean;
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 *
	 * @param mean
	 * @see Exponential#Exponential(double, Random)
	 */
	public Exponential(double mean) {
		this(mean, new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	@Override
	public double nextDouble() {
		return Math.log(1.0d-getGenerator().nextDouble()) * this.negMean;
		//could be simplified because we assume that nextDouble() is uniform which implies that 1-nextDouble() is also uniform
		//but this will also imply the possibility to generate infinity, and excludes 0. We don't want that.
		//return Math.log(getGenerator().nextDouble()) * this.negMean;
	}
}
