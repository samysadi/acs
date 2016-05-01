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
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.random.Uniform;

/**
 * This TopologyFactory can be used to generated a given number of hosts that are all
 * interconnected.
 *
 * @since 1.0
 */
public class TopologyFactoryFlat extends TopologyFactory {
	public TopologyFactoryFlat(Config config, CloudProvider cloudProvider) {
		super(config, cloudProvider);
	}

	@Override
	public Object generate() {
		Simulator.getSimulator().setRandomGenerator(this.getClass());

		final Config cfg = getConfig();

		int poweredOnCount = FactoryUtils.generateInt("PoweredOnHostsCount", cfg, 0);
		final int nodes_count = FactoryUtils.generateCount(cfg.addContext("Nodes"), 50);

		getLogger().log(Level.FINE, "Going to generate: " + nodes_count + " hosts ...");

		Switch aggregateSwitch = FactoryUtils.generateSwitch(getConfig().addContext(FactoryUtils.Switch_CONTEXT), getCloudProvider());

		int remainingNodesInRack = 0;
		int remainingRacksInCluster = 0;
		int remainingClustersInDatacenter = 0;
		boolean rackAdded = true; //default added rack when the simulator is created

		for (int i=0; i<nodes_count;) {
			if (remainingClustersInDatacenter == 0) {
				remainingClustersInDatacenter = FactoryUtils.generateCount(cfg.addContext("ClustersPerDatacenter"), 10);
				//getLogger().log(Level.FINEST, "Next Datacenter will contain: " + remainingClustersInDatacenter + " clusters");
				if (!rackAdded)
					getCloudProvider().addDatacenter();
				remainingRacksInCluster = 0;
				rackAdded = true;
			}
			if (remainingRacksInCluster == 0) {
				remainingRacksInCluster = FactoryUtils.generateCount(cfg.addContext("RacksPerCluster"), 10);
				//getLogger().log(Level.FINEST, "Next Cluster will contain: " + remainingRacksInCluster + " racks");
				if (!rackAdded)
					getCloudProvider().addCluster();
				remainingClustersInDatacenter--;
				remainingNodesInRack = 0;
				rackAdded = true;
			}
			if (remainingNodesInRack == 0) {
				remainingNodesInRack = FactoryUtils.generateCount(cfg.addContext("NodesPerRack"), 10);
				//getLogger().log(Level.FINEST, "Next Rack will contain: " + remainingNodesInRack + " hosts");
				if (!rackAdded)
					getCloudProvider().addRack();
				remainingRacksInCluster--;
				rackAdded = true;
			}

			Host h = FactoryUtils.generateHost(getHostGenerationMode().next(), getCloudProvider());

			if (poweredOnCount > 0) {
				poweredOnCount--;
				h.setPowerState(PowerState.ON);
				getCloudProvider().getPowerManager().lockHost(h);
			}

			FactoryUtils.linkDevices(
					getConfig(),
					h, aggregateSwitch,
					0l, 0d
				);

			remainingNodesInRack--;

			i++;
			if ((i << 7) % nodes_count < 128)
				FactoryUtils.logAdvancement("Hosts", i, i * 100d / nodes_count);

			rackAdded = false;
		}

		//connect to the Internet

		final Config internetLayer = cfg.addContext(INTERNETLAYER_CONTEXT);
		final Uniform lossRateGenerator = new Uniform(
				internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMin", 0.0d),
				internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMax", 0.0d)
			);
		final Uniform distanceGenerator = new Uniform(
				internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMin", 0l),
				internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMax", 0l)
			);
		final Config switchCfg = internetLayer.addContext(FactoryUtils.Switch_CONTEXT);
		for (int i=0; i<FactoryUtils.generateCount(switchCfg, 1); i++) {
			Switch s = FactoryUtils.generateSwitch(switchCfg, getCloudProvider());

			FactoryUtils.linkDevices(
					getConfig(),
					aggregateSwitch, s,
					Long.MAX_VALUE,
					Long.MAX_VALUE,
					0l, 0d
				);

			FactoryUtils.connectToInternet(
					internetLayer,
					s,
					distanceGenerator.nextLong() * Simulator.LATENCY_PER_KILOMETER / 1000,
					lossRateGenerator.nextDouble()
				);
		}

		FactoryUtils.logAdvancement("Hosts", nodes_count, 100d);

		Simulator.getSimulator().restoreRandomGenerator();

		if (getCloudProvider()!=null)
			getCloudProvider().notify(NotificationCodes.FACTORY_TOPOLOGY_GENERATED, null);

		return null;
	}

}
