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

package com.samysadi.acs.tracing.sim;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.tracing.IncrementableProbe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.tracing.AbstractProbe;

/**
 * Probe for counting the total of failed workloads.
 * 
 * <p>This probe relies on the Workload implementation to update its value.
 * 
 * @since 1.0
 */
public class SimFailedWorkloadsCountProbe extends AbstractProbe<Long> implements IncrementableProbe<Long> {
	public static final String KEY = SimFailedWorkloadsCountProbe.class.getSimpleName().substring(0, 
									SimFailedWorkloadsCountProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof Simulator))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(Long.valueOf(0l));
	}

	@Override
	public void setValue(Long value) {
		super.setValue(value);
	}

	@Override
	public void increment() {
		setValue(getValue() + 1l);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
