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

package com.samysadi.acs.tracing;

import com.samysadi.acs.core.tracing.FormattableProbe;
import com.samysadi.acs.core.tracing.ModifiableProbe;
import com.samysadi.acs.core.tracing.ProbeValueFormatter;
import com.samysadi.acs.core.tracing.Probed;

/**
 *
 * @since 1.0
 */
public class CustomProbe extends AbstractProbe<Object> implements ModifiableProbe<Object>, FormattableProbe {
	public static final String CUSTOM_PROBE_PREFIX = "_";
	private String key = null;

	private ProbeValueFormatter formatter = null;

	@Override
	public void setup(Probed parent) {
		if (this.key == null)
			throw new IllegalArgumentException("You need to set a key for this probe first, before using it.");

		super.setup(parent);
	}

	@Override
	public void setValue(Object value) {
		super.setValue(value);
	}

	public void setKey(String key) {
		if (!this.isDiscarded())
			throw new IllegalArgumentException("You cannot change this probe's key when it is in use.");
		this.key = key;
	}

	@Override
	public void setProbeValueFormatter(ProbeValueFormatter formatter) {
		this.formatter = formatter;
	}

	@Override
	public ProbeValueFormatter getProbeValueFormatter() {
		return this.formatter;
	}

	@Override
	public String getKey() {
		return this.key;
	}
}
