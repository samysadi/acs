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

package com.samysadi.acs.utility.factory;

import java.util.logging.Level;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;


/**
 * 
 * @since 1.0
 */
public class SimulatorFactoryDefault extends SimulatorFactory {

	public SimulatorFactoryDefault(Config config) {
		super(config);
	}

	@Override
	public Simulator generate() {
		Simulator simulator = newSimulator(null, getConfig());

		getLogger().log(Level.INFO, "Simulator is being initialized ...");

		final long tick = System.nanoTime();

		getLogger().log(Level.INFO, "Generating infrastructure ...");
		{
			GenerationMode g = newGenerationMode(null, FactoryUtils.CloudProvider_CONTEXT);
			int cp_count = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.CloudProvider_CONTEXT), 1);
			for (int i=0; i<cp_count; i++) {
				getLogger().log(Level.FINER, "Generating CloudProvider " + (i+1) + "/" + cp_count);
				FactoryUtils.generateCloudProvider(g.next());
			}
		}

		FactoryUtils.generateFailures(getConfig().addContext(FactoryUtils.Failures_CONTEXT));

		FactoryUtils.generateTraces(getConfig(), simulator);

		getLogger().log(Level.INFO, "Simulator was initialized. Initialization took: " +
					Simulator.formatTime((System.nanoTime()-tick) * Simulator.MILLISECOND / 1000000) +
					"."
				);

		return simulator;
	}
}
