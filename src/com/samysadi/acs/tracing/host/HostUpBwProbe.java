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

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.DataRateProbe;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.vm.VmUpBwProbe;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public class HostUpBwProbe extends AbstractProbe<Long> implements DataRateProbe {
	public static final String KEY = HostUpBwProbe.class.getSimpleName().substring(0, 
									HostUpBwProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof Host))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		//register listeners
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (notification_code == NotificationCodes.ENTITY_ADDED
							&& data instanceof VirtualMachine) {
						((Probed) data).getProbe(VmUpBwProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					} else if (notification_code == NotificationCodes.ENTITY_REMOVED
							&& data instanceof VirtualMachine) {
						((Probed) data).getProbe(VmUpBwProbe.KEY).removeListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					}
					HostUpBwProbe.this.recomputeValue();
				}
			};

			registeredListener(l);

			((Notifier) getParent()).addListener(NotificationCodes.ENTITY_ADDED, l);
			((Notifier) getParent()).addListener(NotificationCodes.ENTITY_REMOVED, l);
			for (VirtualMachine vm: ((Host)getParent()).getVirtualMachines()) {
					Probe<?> p = vm.getProbe(VmUpBwProbe.KEY);
					p.addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
				}
			recomputeValue();
		}
	}

	private void recomputeValue() {
		long v = 0;
		for (VirtualMachine vm: ((Host)getParent()).getVirtualMachines()) {
				Probe<?> p = vm.getProbe(VmUpBwProbe.KEY);
				v+= ((Number) p.getValue()).longValue();
			}
		setValue(Long.valueOf(v));
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
