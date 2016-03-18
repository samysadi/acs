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
import com.samysadi.acs.core.Logger;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.workload.Workload;

/**
 *
 * @since 1.0
 */
public abstract class TaskImpl implements Task {
	private Workload workload;
	private Config config;
	private boolean done;

	public TaskImpl(Workload workload, Config config) {
		super();
		if (workload == null || config == null)
			throw new NullPointerException();
		this.workload = workload;
		this.config = config;
		this.done = false;
	}

	@Override
	public TaskImpl clone(Workload workload) {
		if (workload == null)
			throw new NullPointerException();

		boolean wasExecuting = this.isExecuting();
		if (wasExecuting)
			this.interrupt();

		TaskImpl clone;
		try {
			clone = (TaskImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		clone.workload = workload;

		if (wasExecuting)
			this.execute();

		return clone;
	}

	@Override
	public Workload getWorkload() {
		return this.workload;
	}

	@Override
	public Config getConfig() {
		return this.config;
	}

	protected abstract boolean isExecuting();

	protected void log(String message) {
		Logger.getGlobal().log(this.getClass().getSimpleName() + ": " + message);
	}

	protected void fail(String message) {
		if (this.isExecuting())
			this.interrupt();

		if (message != null)
			log("Failed: " + message);

		getWorkload().notify(NotificationCodes.WORKLOAD_TASK_FAILED, this);
	}

	protected final void fail() {
		fail(null);
	}

	protected void success() {
		setIsDone();
		getWorkload().notify(NotificationCodes.WORKLOAD_TASK_COMPLETED, this);
	}

	protected boolean isDone() {
		return this.done;
	}

	protected void setIsDone(boolean v) {
		this.done = v;
	}

	protected final void setIsDone() {
		setIsDone(true);
	}
}
