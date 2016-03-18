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

import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.random.Uniform;
import com.samysadi.acs.utility.workload.Workload;
import com.samysadi.acs.virtualization.job.Job;

/**
 * Creates a job and places it
 * in one of the current user's {@link ThinClient}s.<br/>
 * The created job is defined as the current workload's remote job.
 *
 * <p>Set the <i>NewThinClient</i> configuration value to <tt>true</tt> if you wish
 * to create a new thin client (instead of selecting one from existing).
 *
 * @since 1.0
 */
public class CreateClientJobTask extends TaskImpl {
	private Event event;

	public CreateClientJobTask(Workload workload, Config config) {
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

		boolean newThinClient = getConfig().getBoolean("NewThinClient", false);

		ThinClient h;
		if (newThinClient)
			h = FactoryUtils.generateThinClient(getConfig().addContext(FactoryUtils.ThinClient_CONTEXT), user);
		else {
			List<ThinClient> l = user.getThinClients();
			if (l.size() == 0) {
				fail("The workload's user does not own any ThinClient");
				return;
			}
			h = l.get((new Uniform(l.size() - 1)).nextInt());
		}

		final Job job = FactoryUtils.generateJob(getConfig());
		job.setParent(h.getVirtualMachine());

		this.event = new EventImpl() {
			@Override
			public void process() {
				if (!job.canStart()) {
					job.setParent(null);
					fail("The job cannot be started on the user's ThinClient");
					return;
				}

				job.doStart();
				getWorkload().setRemoteJob(job);
				interrupt();
				success();
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
