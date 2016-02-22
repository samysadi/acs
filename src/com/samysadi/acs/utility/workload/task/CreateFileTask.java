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
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.event.EventChain;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.workload.Workload;

/**
 * Creates a file with the given initial <i>Size</i> in {@link Simulator#MEBIBYTE}
 * using the cloud provider's {@link Staas}.
 * 
 * <p>The created file is set as the current active file in the 
 * workload (using {@link Workload#setStorageFile(StorageFile)}).
 * 
 * @since 1.0
 */
public class CreateFileTask extends TaskImpl {
	private Event event;

	public CreateFileTask(Workload workload, Config config) {
		super(workload, config);

		this.event = null;
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone()) {
			success();
			return;
		}

		User user = getWorkload().getParent().getUser();
		if (user == null) {
			fail("User not found");
			return;
		}

		final long size = FactoryUtils.generateLong("Size", getConfig(), 0l) * Simulator.MEBIBYTE;

		final StorageFile file = user.getParent().getStaas()
				.createFile(size, user);
		if (file == null) {
			fail("File cannot be created.");
			return;
		}

		this.event = new EventChain() {
			@Override
			public boolean processStage(int stageNum) {
				if (stageNum < 2) //0:file created but not yet placed, 1: powering on hosts, 2: files placed
					return CONTINUE;
				getWorkload().setStorageFile(file);
				interrupt();
				success();
				return STOP;
			}
		};
		Simulator.getSimulator().schedule(event);
	}

	@Override
	public void interrupt() {
		if (this.event == null)
			return;

		if (this.event.getScheduledAt() != null)
			this.event.cancel();

		this.event = null;
	}

	@Override
	public boolean isExecuting() {
		return this.event != null;
	}

}
