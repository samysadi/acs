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
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.Trace;

/**
 *
 * @since 1.0
 */
public abstract class TraceFactory extends Factory {
	public static boolean IS_TRACING_DISABLED = false;
	/**
	 * Extension for trace files created by this factory
	 */
	public static final String Trace_Ext = ".trace";

	private Probed probed;

	public TraceFactory(Config config, Probed probed) {
		super(config);
		this.probed = probed;
	}

	public Probed getProbed() {
		return this.probed;
	}

	/**
	 * Generates a trace and returns it.
	 *
	 * @return the generated trace
	 */
	@Override
	public abstract Trace<?> generate();
}
