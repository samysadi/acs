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
import com.samysadi.acs.core.tracing.ModifiableProbe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.PriceProbe;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.user.UserBillingProbe;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * This probe contains the total bills generated for all users inside
 * parent cloud provider.
 *
 * @since 1.0
 */
public class CpUserBillingProbe extends AbstractProbe<Long> implements ModifiableProbe<Long>, PriceProbe {
	public static final String KEY = CpUserBillingProbe.class.getSimpleName().substring(0,
									CpUserBillingProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof CloudProvider))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		setValue(Long.valueOf(0l));
		//register listeners
		{
			NotificationListener l = new MyStaticListener0();

			((CloudProvider)getParent()).addListener(NotificationCodes.ENTITY_ADDED, l);

			for (User u: ((CloudProvider)getParent()).getUsers())
				u.getProbe(UserBillingProbe.KEY); //create needed probes

			registeredListener(l);
		}
	}

	private static final class MyStaticListener0 extends NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (data instanceof User)
				((User) data).getProbe(UserBillingProbe.KEY); //create needed probes
		}
	}

	@Override
	public void setValue(Long value) {
		super.setValue(value);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
