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
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.workload.Workload;

/**
 *
 * @since 1.0
 */
public class DelayTask extends TaskImpl {
	private Event event;
	private long remainingDelay;

	public DelayTask(Workload workload, Config config) {
		super(workload, config);

		this.event = null;
		this.remainingDelay = Math.round(FactoryUtils.generateDouble("Delay", getConfig(), 0d) * Simulator.SECOND);
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone() || this.remainingDelay <= 0) {
			success();
			return;
		}

		this.event = new EventImpl() {
			@Override
			public void process() {
				interrupt();
			}
		};
		Simulator.getSimulator().schedule(this.remainingDelay, this.event);
	}

	@Override
	public void interrupt() {
		if (this.event == null)
			return;

		if (this.event.getScheduledAt() != null) {
			this.remainingDelay = this.event.getScheduledAt() - Simulator.getSimulator().getTime();

			this.event.cancel();
			this.event = null;

			if (this.remainingDelay <= 0)
				success();
		} else
			this.event = null;
	}

	@Override
	public boolean isExecuting() {
		return this.event != null;
	}

}
