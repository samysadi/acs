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

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.PowerProbe;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Probe for instant power consumption by the whole cloud.<br/>
 * This includes power usage by the hosts and also the power usage for
 * cooling, lighting, networking etc...
 *
 * @since 1.0
 */
public class CpPowerProbe extends AbstractProbe<Long> implements PowerProbe {
	public static final String KEY = CpPowerProbe.class.getSimpleName().substring(0,
									CpPowerProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof CloudProvider))
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

			getParent().getProbe(CpHostsPowerProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
			registeredListener(l);
		}
	}

	protected void recomputeValue() {
		long hostsPower = ((Long)getParent().getProbe(CpHostsPowerProbe.KEY).getValue());

		//may add here other power consumptions (cooling, lighting, networking etc...)

		setValue(Long.valueOf(hostsPower));
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
