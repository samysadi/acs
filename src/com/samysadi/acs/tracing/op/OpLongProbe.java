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

package com.samysadi.acs.tracing.op;

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.entity.EntityRunnableStateProbe;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.operation.LongOperationImpl;

/**
 * 
 * @since 1.0
 */
public abstract class OpLongProbe extends AbstractProbe<Long> {

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof LongOperationImpl<?>))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		recomputeValue();
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					recomputeValue();
				}
			};

			getParent().getProbe(EntityRunnableStateProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
			((Notifier) getParent()).addListener(NotificationCodes.OPERATION_RESOURCE_CHANGED, l);

			registeredListener(l);
		}
	}

	private void recomputeValue() {
		final LongOperationImpl<?> op = (LongOperationImpl<?>) getParent();

		if (op == null)
			setValue(null);
		else if (!op.isRunning() || op.getAllocatedResource() == null)
			setValue(Long.valueOf(0));
		else
			setValue(op.getAllocatedResource().getLong());
	}
}
