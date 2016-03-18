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

package com.samysadi.acs.tracing.user;

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.MipsProbe;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.vm.VmMipsProbe;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 *
 * @since 1.0
 */
public class UserMipsProbe extends AbstractProbe<Long> implements MipsProbe {
	public static final String KEY = UserMipsProbe.class.getSimpleName().substring(0,
									UserMipsProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof User))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		//register listeners
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (notification_code == NotificationCodes.USER_VM_ATTACHED) {
						((Probed) data).getProbe(VmMipsProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					} else if (notification_code == NotificationCodes.USER_VM_DETACHED) {
						((Probed) data).getProbe(VmMipsProbe.KEY).removeListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					}
					UserMipsProbe.this.recomputeValue();
				}
			};

			registeredListener(l);

			((Notifier) getParent()).addListener(NotificationCodes.USER_VM_ATTACHED, l);
			((Notifier) getParent()).addListener(NotificationCodes.USER_VM_DETACHED, l);
			for (VirtualMachine vm: ((User)getParent()).getVirtualMachines()) {
					Probe<?> p = vm.getProbe(VmMipsProbe.KEY);
					p.addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
				}
			recomputeValue();
		}
	}

	private void recomputeValue() {
		long v = 0;
		for (VirtualMachine vm: ((User)getParent()).getVirtualMachines()) {
				Probe<?> p = vm.getProbe(VmMipsProbe.KEY);
				v+= ((Number) p.getValue()).longValue();
			}
		setValue(Long.valueOf(v));
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
