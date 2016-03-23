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

package com.samysadi.acs.utility.factory.generation.flow;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.utility.random.Exponential;
import com.samysadi.acs.utility.random.NumberGenerator;

/**
 *
 * @since 1.0
 */
public class GenerationFlowDefault implements GenerationFlow {
	private Config config;

	private long initialDelay;

	private int initialCount;

	private NumberGenerator generator;

	public GenerationFlowDefault(Config config) {
		super();

		this.config = config;

		long mean = (long) (getConfig().getDouble("FlowMeanTime", 0d) * Simulator.SECOND);
		if (mean == 0l)
			this.initialCount = Integer.MAX_VALUE;
		else
			this.initialCount = getConfig().getInt("FlowInitialCount", 0);

		this.initialDelay = (long) (getConfig().getDouble("FlowInitialDelay", 0d) * Simulator.SECOND);

		this.generator = new Exponential(mean);
	}

	public Config getConfig() {
		return this.config;
	}

	protected NumberGenerator getGenerator() {
		return this.generator;
	}

	@Override
	public GenerationFlowDefault clone() {
		GenerationFlowDefault clone;
		try {
			clone = (GenerationFlowDefault) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		//nothing
		return clone;
	}

	@Override
	public GenerationFlowInfo next() {

		if (this.initialDelay > 0) {
			long c = this.initialDelay;
			this.initialDelay = 0;
			return new GenerationFlowInfo(c, 0);
		}

		if (this.initialCount > 0) {
			int c = this.initialCount;
			this.initialCount = 0;
			return new GenerationFlowInfo(0, c);
		}

		return new GenerationFlowInfo(Math.max(1l, getGenerator().nextLong()), 1);
	}
}
