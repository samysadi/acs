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
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.utility.NotificationCodes;

/**
 *
 * @since 1.0
 */
public class CloudProviderFactoryDefault extends CloudProviderFactory {
	public CloudProviderFactoryDefault(Config config) {
		super(config);
	}

	//topology
	private void _generate0(final CloudProvider cp) {
		getLogger().log(Level.FINER, "Using " + getTopologyFactoryClass().getSimpleName());

		NotificationListener l = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();
				cp.removeListener(NotificationCodes.FACTORY_TOPOLOGY_GENERATED, this);

				_generate1(cp);
			}
		};

		cp.addListener(NotificationCodes.FACTORY_TOPOLOGY_GENERATED, l);
		FactoryUtils.generateTopology(getConfig().addContext(FactoryUtils.Topology_CONTEXT), cp);
	}

	private void generateUser(final CloudProvider cp,
			int index, int count, NotificationListener l) {
		if (index >= count) {
			l.discard();
			cp.removeListener(NotificationCodes.FACTORY_USER_GENERATED, l);
			cp.notify(NotificationCodes.FACTORY_ALL_USERS_GENERATED, null);
			FactoryUtils.logAdvancement("Users", count, 100d);
			_generate2(cp);
			return;
		}

		index++;
		if (index % 1000 == 0)
			FactoryUtils.logAdvancement("Users", index, index * 100d / count);

		FactoryUtils.generateUser(getUserGenerationMode().next(), cp);
	}

	//users
	private void _generate1(final CloudProvider cp) {
		final int count = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.User_CONTEXT), 0);
		getLogger().log(Level.FINE, "Going to generate: " + count + " users ...");
		final int[] indexTab = {0};
		NotificationListener l = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				generateUser(cp, indexTab[0], count, this);
				indexTab[0]++;
			}
		};

		cp.addListener(NotificationCodes.FACTORY_USER_GENERATED, l);
		generateUser(cp, indexTab[0], count, l);
		indexTab[0]++;
	}

	//
	private void _generate2(final CloudProvider cp) {
		FactoryUtils.generateTraces(getConfig(), cp);

		Simulator.getSimulator().restoreRandomGenerator();

		Simulator.getSimulator().notify(NotificationCodes.FACTORY_CLOUDPROVIDER_GENERATED, cp);
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

		FactoryUtils.generateVmCheckpointingHandler(cp, getConfig().addContext(FactoryUtils.VmCheckpointingHandler_CONTEXT));


		newMigrationHandler(null, cp);

		_generate0(cp);

		return cp;
	}
}
