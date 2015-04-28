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

package com.samysadi.acs.tracing.mz;

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.DataSizeProbe;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Probe for used size by a memory zone.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class MzSizeProbe extends AbstractProbe<Long> implements DataSizeProbe {
	public static final String KEY = MzSizeProbe.class.getSimpleName().substring(0, 
									MzSizeProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof MemoryZone))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(((MemoryZone) getParent()).hasParentRec() ? ((MemoryZone) getParent()).getSize() : 0l);
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					setValue(((MemoryZone) notifier).hasParentRec() ? ((MemoryZone) notifier).getSize() : 0l);
				}
			};

			((Notifier) getParent()).addListener(NotificationCodes.MZ_SIZE_CHANGED, l);
			((Notifier) getParent()).addListener(NotificationCodes.ENTITY_ANCESTOR_CHANGED, l);

			registeredListener(l);
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
