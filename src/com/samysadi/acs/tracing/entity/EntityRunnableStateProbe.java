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

package com.samysadi.acs.tracing.entity;

import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 *
 * @since 1.0
 */
public class EntityRunnableStateProbe extends AbstractProbe<RunnableState> {
	public static final String KEY = EntityRunnableStateProbe.class.getSimpleName().substring(0,
									EntityRunnableStateProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof RunnableEntity))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(((RunnableEntity) getParent()).getRunnableState());
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					setValue(((RunnableEntity) getParent()).getRunnableState());
				}
			};

			((Notifier) getParent()).addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, l);

			registeredListener(l);
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
