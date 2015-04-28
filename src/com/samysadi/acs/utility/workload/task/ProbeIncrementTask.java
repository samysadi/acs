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
import com.samysadi.acs.tracing.CustomProbe;
import com.samysadi.acs.utility.workload.Workload;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class ProbeIncrementTask extends TaskImpl {
	public ProbeIncrementTask(Workload workload, Config config) {
		super(workload, config);
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		String probe_key = getConfig().getString("Probe", null);
		if (probe_key == null) {
			fail("Probe must be specified.");
			return;
		}

		CustomProbe p = (CustomProbe) Simulator.getSimulator().getProbe(CustomProbe.CUSTOM_PROBE_PREFIX + probe_key);
		Object old = p.getValue();
		if (old instanceof Number) {
			if ((old instanceof Double) || (old instanceof Float)) {
				Double i = getConfig().getDouble("Value", 1d);
				p.setValue(((Number) old).doubleValue() + i);
			} else {
				Long i = getConfig().getLong("Value", 1l);
				p.setValue(((Number) old).longValue() + i);
			}
		} else
			p.setValue(Long.valueOf(1l));

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
