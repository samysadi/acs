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

package com.samysadi.acs.utility.workload.task;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.workload.Workload;

/**
 * Deletes the workload's active file.
 *
 * <p>You need to define the file to delete using appropriate tasks.
 *
 * @since 1.0
 */
public class DeleteFileTask extends TaskImpl {
	private NotificationListener listener;

	public DeleteFileTask(Workload workload, Config config) {
		super(workload, config);

		this.listener = null;
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone()) {
			success();
			return;
		}

		StorageFile file = getWorkload().getStorageFile();
		if (file == null) {
			fail("There is no active file");
			return;
		}

		this.listener = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();
				interrupt();
				success();
			}
		};

		file.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, this.listener);

		getWorkload().getParent().getParent().getCloudProvider().getStaas().deleteFile(file);
	}

	@Override
	public void interrupt() {
		if (this.listener == null)
			return;

		this.listener.discard();
		this.listener = null;
	}

	@Override
	public boolean isExecuting() {
		return this.listener != null;
	}

}
