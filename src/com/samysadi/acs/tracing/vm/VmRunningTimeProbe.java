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

package com.samysadi.acs.tracing.vm;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.TimeProbe;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 *
 * @since 1.0
 */
public class VmRunningTimeProbe extends AbstractProbe<Long> implements TimeProbe {
	public static final String KEY = VmRunningTimeProbe.class.getSimpleName()
			.substring(0, VmRunningTimeProbe.class.getSimpleName().length() - 5);

	private long lastTick;

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof VirtualMachine))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		this.lastTick = -1l;
		setValue(0l);
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (lastTick >= 0)
						setValue(getValue() + Simulator.getSimulator().getTime() - lastTick);
					VirtualMachine vm = ((VirtualMachine)VmRunningTimeProbe.this.getParent());
					if (vm.isRunning())
						lastTick = Simulator.getSimulator().getTime();
					else
						lastTick = -1l;
				}
			};

			((Notifier) getParent()).addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, l);
			Simulator.getSimulator().addListener(NotificationCodes.SIMULATOR_TICK, l);

			registeredListener(l);
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
