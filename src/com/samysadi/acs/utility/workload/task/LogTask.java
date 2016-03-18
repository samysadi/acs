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
import com.samysadi.acs.utility.workload.Workload;

/**
 * Logs a message to output.
 *
 * @since 1.0
 */
public class LogTask extends TaskImpl {
	public LogTask(Workload workload, Config config) {
		super(workload, config);
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone()) {
			success();
			return;
		}

		final String message = getConfig().getString("Message", null);

		if (message != null)
			log(message);

		success();
	}

	@Override
	public void interrupt() {
		//nothing
	}

	@Override
	public boolean isExecuting() {
		return false;
	}

}
