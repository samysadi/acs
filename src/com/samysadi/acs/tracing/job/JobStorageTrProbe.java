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

package com.samysadi.acs.tracing.job;

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.DataRateProbe;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.op.OpStorageTrProbe;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 * 
 * @since 1.0
 */
public class JobStorageTrProbe extends AbstractProbe<Long> implements DataRateProbe {
	public static final String KEY = JobStorageTrProbe.class.getSimpleName().substring(0, 
									JobStorageTrProbe.class.getSimpleName().length() - 5);

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof Job))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		//register listeners
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (notification_code == NotificationCodes.JOB_SRC_OPERATION_ADDED
							&& data instanceof StorageOperation) {
						((Probed) data).getProbe(OpStorageTrProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					} else if (notification_code == NotificationCodes.JOB_SRC_OPERATION_REMOVED
							&& data instanceof StorageOperation) {
						((Probed) data).getProbe(OpStorageTrProbe.KEY).removeListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					}
					JobStorageTrProbe.this.recomputeValue();
				}
			};

			registeredListener(l);
			((Notifier) getParent()).addListener(NotificationCodes.JOB_SRC_OPERATION_ADDED, l);
			((Notifier) getParent()).addListener(NotificationCodes.JOB_SRC_OPERATION_REMOVED, l);

			for (Operation<?> op: ((Job)getParent()).getOperations())
				if (op instanceof StorageOperation) {
					Probe<?> p = op.getProbe(OpStorageTrProbe.KEY);
					p.addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
				}

			recomputeValue();
		}
	}

	private void recomputeValue() {
		long v = 0;
		for (Operation<?> op: ((Job)getParent()).getOperations())
			if (op instanceof StorageOperation) {
				Probe<?> p = op.getProbe(OpStorageTrProbe.KEY);
				v+= ((Number) p.getValue()).longValue();
			}
		setValue(Long.valueOf(v));
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
