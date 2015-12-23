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

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.EventChain;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.factory.generation.flow.GenerationFlowInfo;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public class UserFactoryDefault extends UserFactory {

	public UserFactoryDefault(Config config, CloudProvider cloudProvider) {
		super(config, cloudProvider);
	}

	@Override
	public User generate() {
		Simulator.getSimulator().setRandomGenerator(this.getClass());

		final User user = newUser(null, getCloudProvider());
		user.setConfig(getConfig());
		user.setName(getConfig().getString("Name", null));

		generateThinClients(user, FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.VirtualMachine_CONTEXT), 1));

		generateVms(user, FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.VirtualMachine_CONTEXT), 0), false);

		Simulator.getSimulator().schedule(new EventChain() {
			@Override
			public boolean processStage(int i) {
				if (i < 2) //0: vms generated but not yet placed, 1: powering on hosts, 2: vms placed we can proceed to launch jobs
					return CONTINUE;
				Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());
				
				generateWorkloads(user, FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.Workload_CONTEXT), 0));
				
				Simulator.getSimulator().restoreRandomGenerator();
				return STOP;
			}
		});

		FactoryUtils.generateTraces(getConfig(), user);

		Simulator.getSimulator().restoreRandomGenerator();
		return user;
	}

	private void generateThinClients(final User user, final int count) {
		if (count <= 0)
			return;

		GenerationFlowInfo n = getThinClientGenerationFlow().next();

		final int nCount = Math.min(n.getCount(), count);

		Simulator.getSimulator().schedule(n.getDelay(), new EventImpl() {
			@Override
			public void process() {
				Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());

				for (int i=0; i < nCount; i++)
					FactoryUtils.generateThinClient(getThinClientGenerationMode().next(), user);
				generateThinClients(user, count - nCount);

				Simulator.getSimulator().restoreRandomGenerator();
			}
		});
	}

	private void generateVms(final User user, final int count, final boolean resubmitVms) {
		if (count <= 0)
			return;

		GenerationFlowInfo n = getVmGenerationFlow().next();

		final int nCount = Math.min(n.getCount(), count);

		Simulator.getSimulator().schedule(n.getDelay(), new EventImpl() {
			@Override
			public void process() {
				Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());

				int total = 0;
				for (int i=0; i < nCount; i++) {
					VirtualMachine vm = FactoryUtils.generateVirtualMachine(getVmGenerationMode().next(), getCloudProvider(), user);
					if (vm == null && resubmitVms)
						continue;
					total++;
				}
				generateVms(user, count - total, resubmitVms);

				Simulator.getSimulator().restoreRandomGenerator();
			}
		});
	}

	private void generateWorkloads(final User user, final int count) {
		if (count <= 0)
			return;

		GenerationFlowInfo n = getWorkloadGenerationFlow().next();

		final int nCount = Math.min(n.getCount(), count);

		Simulator.getSimulator().schedule(n.getDelay(), new EventImpl() {
			@Override
			public void process() {
				Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());

				for (int i=0; i < nCount; i++)
					FactoryUtils.generateWorkload(getWorkloadGenerationMode().next(), user);
				generateWorkloads(user, count - nCount);

				Simulator.getSimulator().restoreRandomGenerator();
			}
		});
		
	}

}
