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

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Abstract probe to compute the integral of a given probe's value
 * over time.
 *
 * <p>If the value of the probe you want to integrate is
 * <math><mi>f</mi><mo>(</mo><mi>t</mi><mo>)</mo></math>, then
 * the value of this probe is:<br/>
 * <math><mi>F</mi><mo>(</mo><mi>t</mi><mo>)</mo><mo>=</mo><mo>&int;</mo><mi>f</mi><mo>(</mo><mi>t</mi><mo>)</mo><mo>&dd;</mo><mi>t</mi></math>
 *
 * @since 1.0
 */
public abstract class AbstractLongIntegratorProbe extends AbstractProbe<Long> {
	private double value;
	private long lastValue;
	private long lastTime;

	@Override
	public void setup(Probed parent) {
		super.setup(parent);

		setValue(0l);
		//register listeners
		{
			this.value = 0d;
			this.lastValue = 0l;
			this.lastTime = Simulator.getSimulator().getTime();

			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Probe<?> p = getWatchedProbe();

					long newTime = Simulator.getSimulator().getTime();
					double total = ((double) (newTime - AbstractLongIntegratorProbe.this.lastTime) *
							AbstractLongIntegratorProbe.this.lastValue / getUnitOfTime());
					AbstractLongIntegratorProbe.this.lastTime = newTime;
					AbstractLongIntegratorProbe.this.lastValue = ((Long)p.getValue()).longValue();

					AbstractLongIntegratorProbe.this.value += total;
					setValue(Long.valueOf(
								(long) AbstractLongIntegratorProbe.this.value
							));
				}
			};

			getWatchedProbe().addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
			Simulator.getSimulator().addListener(NotificationCodes.SIMULATOR_TICK, l);
			registeredListener(l);
		}
	}

	protected abstract Probe<?> getWatchedProbe();

	protected long getUnitOfTime() {
		return Simulator.SECOND;
	}
}
