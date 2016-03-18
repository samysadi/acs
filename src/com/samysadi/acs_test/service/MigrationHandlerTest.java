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

package com.samysadi.acs_test.service;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventChain;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.random.Uniform;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs_test.Utils;


/**
 *
 * @since 1.0
 */
public class MigrationHandlerTest {
	Simulator simulator;
	CloudProvider cloudProvider;
	Host h0;
	Host hl;

	ComputingOperation op0;
	Job j0;
	VirtualMachine vm0;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@Before
	public void beforeTest() {
		simulator = Utils.newSimulator();
		simulator.getConfig().setInt("Trace.Count", 0);
		cloudProvider = FactoryUtils.generateCloudProvider(simulator.getConfig());

		h0 = cloudProvider.getHosts().get(0); h0.setName("Host0");
		hl = cloudProvider.getHosts().get(cloudProvider.getHosts().size()-1); hl.setName("Host1");

		cloudProvider.getPowerManager().powerOn(h0);
		cloudProvider.getPowerManager().powerOn(hl);

		vm0 = FactoryUtils.generateVirtualMachine(cloudProvider.getConfig(), null, null);
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				cloudProvider.getVmPlacementPolicy().placeVm(vm0, h0);
			}
		});
	}

	private NotificationListener getOperationListener() {
		return new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> n = (Operation<?>) notifier;
				if (n.getRunnableState() == RunnableState.PAUSED)
					return;

				n.getLogger().log(n, "On " + n.getParent().getParent().getParent().getName() +
						": " + n.getRunnableState() + ": " + n.getAllocatedResource());
			}
		};
	}

	@Test
	public void test0() {
		simulator.schedule(0, new EventChain() {

			@Override
			public boolean processStage(int stageNum) {
				if (stageNum < 10)
					return CONTINUE;
				vm0.doStart();

				j0 = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
				j0.setParent(vm0);
				j0.doStart();

				op0 = j0.compute(10 * 1000 * Simulator.MI, getOperationListener());
				Assert.assertNotNull(op0);

				Simulator.getSimulator().schedule(new EventImpl() {
					@Override
					public void process() {
						if (!op0.isRunning())
							return;
						long s = (new Uniform(1, 500)).nextInt() * Simulator.MEBIBYTE;
						j0.createFile(s);
						Simulator.getSimulator().schedule(1 * Simulator.SECOND, this.clone());
					}
				});


				return STOP;
			}
		});

		simulator.schedule(5 * Simulator.SECOND, new EventImpl() {
			@Override
			public void process() {
				cloudProvider.getMigrationHandler().migrate(vm0, hl);
			}
		});

		simulator.start();
		Assert.assertEquals(hl, vm0.getParent());
		Assert.assertEquals(RunnableState.COMPLETED, op0.getRunnableState());
	}
}
