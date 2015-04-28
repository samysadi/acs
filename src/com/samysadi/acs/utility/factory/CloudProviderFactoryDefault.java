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
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.staas.Staas;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class CloudProviderFactoryDefault extends CloudProviderFactory {
	public CloudProviderFactoryDefault(Config config) {
		super(config);
	}

	@Override
	public CloudProvider generate() {
		Simulator.getSimulator().setRandomGenerator(this.getClass());

		CloudProvider cp = newCloudProvider(null, Simulator.getSimulator());
		cp.setConfig(getConfig());
		cp.setName(getConfig().getString("Name", null));

		newPowerManager(null, cp);
		newJobPlacementPolicy(null, cp);
		newVmPlacementPolicy(null, cp);

		boolean staas_enabled = getConfig().getBoolean("STaaS.Enabled", true);
		Staas staas = newStaas(null, cp);
		if (staas_enabled)
			Factory.getFactory(staas).newSfConsistencyManager(null, staas);
		Factory.getFactory(staas).newSfPlacementPolicy(null, staas);
		Factory.getFactory(staas).newSfReplicaSelectionPolicy(null, staas);
		if (staas_enabled)
			Factory.getFactory(staas).newSfReplicationManager(null, staas);

		FactoryUtils.generateCheckpointingHandler(cp, getConfig().addContext(FactoryUtils.CheckpointingHandler_CONTEXT));


		newMigrationHandler(null, cp);

		//generate topology
		{
			getLogger().log(Level.FINEST, "Using " + getTopologyFactoryClass().getSimpleName());
			FactoryUtils.generateTopology(getConfig().addContext(FactoryUtils.Topology_CONTEXT), cp);
		}

		//generate users
		{
			int users_count = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.User_CONTEXT), 0);
			getLogger().log(Level.FINE, "Going to generate: " + users_count + " users ...");

			for (int i=0; i<users_count;) {

				FactoryUtils.generateUser(getUserGenerationMode().next(), cp);

				i++;
				if (i % 1000 == 0)
					FactoryUtils.logAdvancement("Users", i, i * 100d / users_count);
			}

			FactoryUtils.logAdvancement("Users", users_count, 100d);
		}

		FactoryUtils.generateTraces(getConfig(), cp);

		Simulator.getSimulator().restoreRandomGenerator();
		return cp;
	}
}
