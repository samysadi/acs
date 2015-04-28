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
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class HostVmsCountProbe extends AbstractProbe<Long> {
	public static final String KEY = HostVmsCountProbe.class.getSimpleName()
			.substring(0, HostVmsCountProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof Host))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(Long.valueOf(((Host) getParent()).getVirtualMachines().size()));
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (!(data instanceof VirtualMachine))
						return;
					long old = getValue() == null ? 0 : getValue();
					if (notification_code == NotificationCodes.ENTITY_ADDED)
						setValue(Long.valueOf(old+1));
					else
						setValue(Long.valueOf(old-1));
				}
			};

			((Notifier) getParent()).addListener(NotificationCodes.ENTITY_ADDED, l);
			((Notifier) getParent()).addListener(NotificationCodes.ENTITY_REMOVED, l);

			registeredListener(l);
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
