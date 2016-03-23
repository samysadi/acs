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

import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.ModifiableProbe;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Probe for number of active (not powered off and not failed) hosts
 * in the Cloud.
 *
 * @since 1.0
 */
public class CpActiveHostsCountProbe extends AbstractProbe<Long> {
	public static final String KEY = CpActiveHostsCountProbe.class.getSimpleName().substring(0,
									CpActiveHostsCountProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof CloudProvider))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(0l);
		//register listeners
		{
			final NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					recomputeValue();
				}
			};

			NotificationListener l_added = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (data instanceof Host) {
						if (notification_code == NotificationCodes.ENTITY_ADDED) {
							registerHost((Host)data, l);
						} else {
							unregisterHost((Host)data, l);
						}
						recomputeValue();
					}
				}
			};

			((CloudProvider)getParent()).addListener(NotificationCodes.ENTITY_ADDED, l_added);
			((CloudProvider)getParent()).addListener(NotificationCodes.ENTITY_REMOVED, l_added);

			for (Host h: ((CloudProvider)getParent()).getHosts())
				registerHost(h, l);
			recomputeValue();

			registeredListener(l_added);
			registeredListener(l);
		}
	}

	@SuppressWarnings("unchecked")
	private void recomputeValue() {
		int s_active = 0;
		int s_failed = 0;
		for (Host h: ((CloudProvider)getParent()).getHosts()) {
			if (h.getFailureState() != FailureState.OK) {
				s_failed++;
				continue;
			}
			if (h.getPowerState() != PowerState.ON) {
				continue;
			}
			s_active++;
		}
		setValue(Long.valueOf(s_active));

		Probe<?> _p = (getParent().getProbe(CpFailedHostsCountProbe.KEY));
		if (_p instanceof ModifiableProbe)
			((ModifiableProbe<Long>) _p).setValue(Long.valueOf(s_failed));
	}

	private static void registerHost(Host h, NotificationListener l) {
		h.addListener(NotificationCodes.POWER_STATE_CHANGED, l);
		h.addListener(NotificationCodes.FAILURE_STATE_CHANGED, l);
	}

	private static void unregisterHost(Host h, NotificationListener l) {
		h.removeListener(NotificationCodes.POWER_STATE_CHANGED, l);
		h.removeListener(NotificationCodes.FAILURE_STATE_CHANGED, l);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
