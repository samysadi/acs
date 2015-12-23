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

package com.samysadi.acs.tracing.host;

import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.DataSizeProbe;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.tracing.AbstractLongIntegratorProbe;

/**
 * 
 * @since 1.0
 */
public class HostDownBwInternetTotalProbe extends AbstractLongIntegratorProbe implements DataSizeProbe {
	public static final String KEY = HostDownBwInternetTotalProbe.class.getSimpleName().substring(0, 
									HostDownBwInternetTotalProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof Host))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);
	}

	@Override
	protected Probe<?> getWatchedProbe() {
		return ((Host)HostDownBwInternetTotalProbe.this.getParent()).getProbe(HostDownBwInternetProbe.KEY);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
