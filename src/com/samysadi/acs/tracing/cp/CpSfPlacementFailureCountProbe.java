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
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Probe for number of File placement failures.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class CpSfPlacementFailureCountProbe extends AbstractProbe<Long> {
	public static final String KEY = CpSfPlacementFailureCountProbe.class.getSimpleName().substring(0, 
									CpSfPlacementFailureCountProbe.class.getSimpleName().length() - 5);

	protected int getNotificationCode() {
		return NotificationCodes.SFP_STORAGESELECTION_FAILED;
	}

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof CloudProvider))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(0l);
		//register listeners
		{
			final NotificationListener l_counter = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					setValue(getValue() + 1l);
				}
			};

			final int code = getNotificationCode();

			NotificationListener l_added = new MyStaticListener0(code, l_counter);

			((CloudProvider)getParent()).getStaas().addListener(NotificationCodes.ENTITY_ADDED, l_added);
			((CloudProvider)getParent()).getStaas().addListener(NotificationCodes.ENTITY_REMOVED, l_added);

			((CloudProvider)getParent()).addListener(NotificationCodes.ENTITY_ADDED, l_added);
			((CloudProvider)getParent()).addListener(NotificationCodes.ENTITY_REMOVED, l_added);

			((CloudProvider)getParent()).getStaas().getPlacementPolicy().addListener(code, l_counter);


			registeredListener(l_added);
			registeredListener(l_counter);
		}
	}

	private static final class MyStaticListener0 extends NotificationListener {
		private final int code;
		private final NotificationListener l_counter;

		private MyStaticListener0(int code, NotificationListener l_counter) {
			this.code = code;
			this.l_counter = l_counter;
		}

		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			SfPlacementPolicy p = null;
			if (data instanceof Staas) {
				if (notification_code == NotificationCodes.ENTITY_ADDED) {
					((Staas) data).addListener(NotificationCodes.ENTITY_ADDED, this);
					((Staas) data).addListener(NotificationCodes.ENTITY_REMOVED, this);
				} else {
					((Staas) data).removeListener(NotificationCodes.ENTITY_ADDED, this);
					((Staas) data).removeListener(NotificationCodes.ENTITY_REMOVED, this);
				}
				p = ((Staas) data).getPlacementPolicy();
			}
			if (data instanceof SfPlacementPolicy)
				p = (SfPlacementPolicy) data;

			if (p != null) {
				if (notification_code == NotificationCodes.ENTITY_ADDED)
					p.addListener(code, l_counter);
				else
					p.removeListener(code, l_counter);
			}
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
