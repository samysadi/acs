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
 * For more details on the implementation, refer to the cumulative distribution function in <a href="http://en.wikipedia.org/wiki/Pareto_distribution">wikipedia</a>.
 *
 * @since 1.0
 */
public class Pareto extends AbstractRandom {
	private double xm;
	private double invAlpha;

	/**
	 * @param xm <math><msub><mi>x</mi><mi>m</mi></msub><mo>&gt;</mo><mn>0</mn></math> (see {@link Pareto}).
	 * @param alpha <math><mi>&alpha;</mi><mo>&gt;</mo><mn>0</mn></math> (see {@link Pareto}).
	 * @param generator
	 */
	public Pareto(double xm, double alpha, Random generator) {
		super(generator);
		this.xm = xm;
		this.invAlpha = 1.0d/alpha;
	}

	/**
	 * Creates a new instance based on the current simulator's random generator.
	 *
	 * @see Pareto#Pareto(double, double, Random)
	 */
	public Pareto(double xm, double alpha) {
		this(xm, alpha, new Random(Simulator.getSimulator().getRandomGenerator().nextLong()));
	}

	@Override
	public double nextDouble() {
		double v;
        while ((v = 1.0d - getGenerator().nextDouble()) == 0.0d);
        return this.xm / Math.pow(v, this.invAlpha);
	}
}
