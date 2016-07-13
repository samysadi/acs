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

package com.samysadi.acs.core.tracing;

import com.samysadi.acs.core.notifications.CoreNotificationCodes;

/**
 * This interface defines a modifiable {@link Probe}.<br/>
 * Such probes' value can be updated through other objects.
 *
 * <p>Unlike other {@link Probe}s, you don't need to register listeners
 * to update this probe's value. But you can instead rely on updates
 * from other Probes for instance.
 *
 * @param <ValueType>
 *
 * @since 1.0
 */
public interface ModifiableProbe<ValueType> extends Probe<ValueType> {

	/**
	 * Updates the probe's value.
	 *
	 * <p>A {@link CoreNotificationCodes#PROBE_VALUE_CHANGED} is thrown.
	 *
	 * @param value
	 * @throws IllegalStateException if the probe is discarded
	 * @see Probe#isDiscarded()
	 */
	public void setValue(ValueType value);
}
