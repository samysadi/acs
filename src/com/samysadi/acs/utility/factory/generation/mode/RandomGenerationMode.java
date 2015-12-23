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

package com.samysadi.acs.utility.factory.generation.mode;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.utility.random.NumberGenerator;
import com.samysadi.acs.utility.random.Uniform;

/**
 * A {@link AbstractGenerationMode} subclass that selects next candidate
 * configuration randomly between all available configurations.
 * 
 * @since 1.0
 */
public class RandomGenerationMode extends AbstractGenerationMode {
	private NumberGenerator generator;

	public RandomGenerationMode(Config config, String context) {
		super(config, context);

		this.generator = new Uniform(0, this.configurations.size() - 1);
	}

	public RandomGenerationMode(Config config, String context, NumberGenerator generator) {
		super(config, context);

		this.generator = generator;
	}

	@Override
	public RandomGenerationMode clone() {
		RandomGenerationMode clone = (RandomGenerationMode) super.clone();
		//nothing
		return clone;
	}

	@Override
	public Config next() {
		return this.configurations.get(this.generator.nextInt());
	}
}
