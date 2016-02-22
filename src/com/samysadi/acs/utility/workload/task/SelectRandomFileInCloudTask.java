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

import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.collections.ShuffledIterator;
import com.samysadi.acs.utility.workload.Workload;

/**
 * Selects a random file (between all files in the cloud)
 * and defines it as the workload's active file.
 * 
 * @since 1.0
 */
public class SelectRandomFileInCloudTask extends TaskImpl {
	public SelectRandomFileInCloudTask(Workload workload, Config config) {
		super(workload, config);
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone()) {
			success();
			return;
		}

		ShuffledIterator<User> itt = new ShuffledIterator<User>(getWorkload().getParent().getParent().getCloudProvider().getUsers());

		StorageFile file = null;
		List<StorageFile> l = null;

		MAIN:while (itt.hasNext()) {
			User user = itt.next();
			l = user.getStorageFiles();
			if (l.size() > 0) {
				ShuffledIterator<StorageFile> it = new ShuffledIterator<StorageFile>(l);
				while (it.hasNext()) {
					file = it.next();
					if (file instanceof VirtualStorage || !file.hasParentRec())
						file = null;
					else
						break MAIN;
				}
			}
			l = null;
		}

		if (file == null) {
			fail("Cannot select file");
			return;
		}

		getWorkload().setStorageFile(file);

		success();
	}

	@Override
	public void interrupt() {
		//nothing
	}

	@Override
	public boolean isExecuting() {
		return false;
	}

}
