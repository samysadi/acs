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
 * For more details on the implementation, refer to the cumulative distribution function in <a href="http://en.wikipedia.org/wiki/Weibull_distribution">wikipedia</a>.
 *
 * @since 1.0
 */
public class Weibull extends AbstractRandom {
	private double k_1;
	private double lambda;

	/**
	 * @param k <math><mi>k</mi><mo>&gt;</mo><mn>0</mn></math> (see {@link Weibull}).
	 * @param lambda <math><mi>&lambda;</mi><mo>&gt;</mo><mn>0</mn></math> (see {@link Weibull}).
	 * @param generator
	 */
	public Weibull(double k, double lambda, Random generator) {
		super(generator);
		this.k_1 = 1.0d/k;
		this.lambda = lambda;
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 *
	 * @see Weibull#Weibull(double, double, Random)
	 */
	public Weibull(double k, double lambda) {
		this(k, lambda, new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	@Override
	public double nextDouble() {
		return this.lambda * Math.pow(-Math.log(1.0d-getGenerator().nextDouble()), this.k_1);
		//could be simplified because we assume that nextDouble() is uniform which implies that 1-nextDouble() is also uniform
		//but this will also imply the possibility to generate infinity, and excludes 0. We don't want that.
		//return negLambda * Math.pow(Math.log(getGenerator().nextDouble()), this.k_1);
	}
}
