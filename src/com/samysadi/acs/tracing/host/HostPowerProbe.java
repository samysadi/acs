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

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.ModifiableProbe;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.PowerProbe;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.cp.CpHostsPowerProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Probe for instant power consumption by a host.
 *
 * <p>This implementation is based on the model described by <i>X. Fan et al.</i>
 * in the 2007 paper entitled <b>Power provisioning for a warehouse-sized computer</b>.
 *
 * <p>These three parameters are used in power calculation:<ul>
 * <li>The host's maximum power consumption (when fully used), this parameter is read from
 * configuration value: <i>PowerConsumption</i> (in Watts);
 * <li>The host's power percentage when it is idle (when cpu usage == 0%), this parameter is read from
 * configuration value: <i>PowerPercentageWhenIdle</i> and should be comprised between 0 and 1;
 * <li>The host's current total cpu usage.
 * </ul>
 * The power usage of a host at a given moment is then:<br/>
 * <math><mi>P</mi><mo>=</mo><msub><mi>P</mi><mi>idle</mi></msub><mo>+</mo><mo>(</mo><msub><mi>P</mi><mi>busy</mi></msub><mo>-</mo><msub><mi>P</mi><mi>idle</mi></msub><mo>)</mo><mi>u</mi></math>
 *
 * @since 1.0
 */
public class HostPowerProbe extends AbstractProbe<Long> implements PowerProbe {
	public static final String KEY = HostPowerProbe.class.getSimpleName().substring(0,
									HostPowerProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof Host))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		recomputeValue();
		//register listeners
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					recomputeValue();
				}
			};

			getParent().getProbe(HostMipsProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
			((Host)getParent()).addListener(NotificationCodes.POWER_STATE_CHANGED, l);
			((Host)getParent()).addListener(NotificationCodes.FAILURE_STATE_CHANGED, l);
			registeredListener(l);
		}
	}

	private void recomputeValue() {
		if (((Host)HostPowerProbe.this.getParent()).getPowerState() != PowerState.ON ||
				((Host)HostPowerProbe.this.getParent()).getFailureState() != FailureState.OK) {
			setValue(0l);
			return;
		}

		Probe<?> p = ((Host)HostPowerProbe.this.getParent()).getProbe(HostMipsProbe.KEY);

		//maximum power when host is fully used
		long Pmax	= ((Host)HostPowerProbe.this.getParent()).getConfig().getLong("PowerConsumption", 250l) * Simulator.WATT;
		//idle power fraction
		double k	= ((Host)HostPowerProbe.this.getParent()).getConfig().getDouble("PowerPercentageWhenIdle", 0.7d);
		//cpu usage
		long uCurrent	= ((Long)p.getValue()).longValue();
		long uTotal		= 0;
		for (ProcessingUnit pu: ((Host)HostPowerProbe.this.getParent()).getProcessingUnits())
			uTotal+= pu.getComputingProvisioner().getCapacity();

		//power = Pidle + (Pmax - Pidle) * u
		//power = Pmax * k + (Pmax - Pmax * k) * u
		//power = Pmax * (k + (1-k) * u), with u = uCurrent / uTotal (normalization to 0..1 range)
		long power = Math.round(Pmax * (k + (1d-k) * uCurrent / uTotal));

		setValue(Long.valueOf(power));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setValue(Long value) {
		if (value != null) {
			if (value < 0l)
				throw new IllegalStateException("Negative power");

			long delta = value.longValue() - (getValue() == null ? 0l : getValue().longValue());

			super.setValue(value);

			{ //update Cp probes if needed
				Entity e = ((Host)this.getParent()).getCloudProvider();
				if (e != null) {
					Probe<?> cp = e.getProbe(CpHostsPowerProbe.KEY);
					if (cp instanceof ModifiableProbe<?>)
						((ModifiableProbe<Long>) cp).setValue(((ModifiableProbe<Long>) cp).getValue() + delta);
				}
			}
		} else
			super.setValue(null);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
