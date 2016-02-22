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

package com.samysadi.acs.utility.workload.task;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.event.EventChain;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;
import com.samysadi.acs.utility.workload.Workload;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * Deploys (ie: creates and places) the given <i>Count</i> of {@link VirtualMachine}s.
 * 
 * @since 1.0
 */
public class DeployVmTask extends TaskImpl {
	private Event event;

	public DeployVmTask(Workload workload, Config config) {
		super(workload, config);

		this.event = null;
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone()) {
			success();
			return;
		}

		User user = getWorkload().getParent().getUser();
		if (user == null) {
			fail("User not found");
			return;
		}

		final GenerationMode vmGenerationMode = Factory.getFactory(getConfig()).newGenerationMode(null, FactoryUtils.VirtualMachine_CONTEXT);
		int count = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.VirtualMachine_CONTEXT), 1);

		while (count-- > 0)
			FactoryUtils.generateVirtualMachine(vmGenerationMode.next(), user.getParent(), user);

		this.event = new EventChain() {
			@Override
			public boolean processStage(int stageNum) {
				if (stageNum < 2) //0:vms generated but not yet placed, 1: powering on hosts, 2: vms placed we can proceed to launch jobs
					return CONTINUE;
				interrupt();
				success();
				return STOP;
			}
		};
		Simulator.getSimulator().schedule(this.event);
	}

	@Override
	public void interrupt() {
		if (this.event == null)
			return;

		if (this.event.getScheduledAt() != null)
			this.event.cancel();

		this.event = null;
	}

	@Override
	public boolean isExecuting() {
		return this.event != null;
	}

}
