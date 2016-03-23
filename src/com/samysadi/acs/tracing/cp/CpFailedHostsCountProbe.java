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

package com.samysadi.acs.tracing.cp;

import com.samysadi.acs.core.tracing.ModifiableProbe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.tracing.AbstractProbe;

/**
 * Probe for number of active (not powered off and not failed) hosts
 * in the Cloud.
 *
 * @since 1.0
 */
public class CpFailedHostsCountProbe extends AbstractProbe<Long> implements ModifiableProbe<Long> {
	public static final String KEY = CpFailedHostsCountProbe.class.getSimpleName().substring(0,
									CpFailedHostsCountProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof CloudProvider))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(0l);

		//this probe depends on CpActiveHostsCountProbe
		getParent().getProbe(CpActiveHostsCountProbe.KEY);
	}

	@Override
	public void setValue(Long value) {
		super.setValue(value);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
