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
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.workload.Workload;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 * Computes for the given <i>Length</i> in {@link Simulator#MI}.
 *
 * @since 1.0
 */
public class ComputeTask extends TaskImpl {
	private ComputingOperation operation;
	private long remainingLength;

	public ComputeTask(Workload workload, Config config) {
		super(workload, config);

		this.operation = null;
		this.remainingLength = FactoryUtils.generateLong("Length", getConfig(), 1l) * Simulator.MI;
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone() || this.remainingLength <= 0) {
			success();
			return;
		}

		this.operation = getWorkload().compute(this.remainingLength, new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> o = ((Operation<?>) notifier);
				if (o.isTerminated()) {
					this.discard();

					if (o.getRunnableState() == RunnableState.FAILED)
						fail();
					else
						interrupt();
				}
			}
		});
		if (this.operation == null)
			fail();
	}

	@Override
	public void interrupt() {
		if (this.operation == null)
			return;

		ComputingOperation o = this.operation;
		this.operation = null;

		if (o.isRunning())
			o.doPause();

		this.remainingLength-= o.getCompletedLength();

		o.setParent(null);

		if (this.remainingLength <= 0)
			success();
	}

	@Override
	public boolean isExecuting() {
		return this.operation != null;
	}

}
