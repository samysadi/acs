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
 * Selects a random file (between all user's files)
 * and defines it as the workload's active file.
 * 
 * <p>If the <i>Create</i> flag is specified, then 
 * a file is create if no file is found for the user.
 * 
 * @since 1.0
 */
public class SelectRandomFileTask extends CreateFileTask {
	public SelectRandomFileTask(Workload workload, Config config) {
		super(workload, config);
	}

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		User user = getWorkload().getParent().getUser();
		if (user == null) {
			fail("User not found");
			return;
		}

		StorageFile file = null;

		List<StorageFile> l = user.getStorageFiles();
		if (l.size() != 0) {
			ShuffledIterator<StorageFile> it = new ShuffledIterator<StorageFile>(l);
			while (it.hasNext()) {
				file = it.next();
				if (file instanceof VirtualStorage || !file.hasParentRec())
					file = null;
				else
					break;
			}
		}

		if (file == null) {
			if (!getConfig().getBoolean("Create", false)) {
				fail("Cannot select file");
				return;
			} else {
				super.execute();
				return;
			}
		}
		
		getWorkload().setStorageFile(file);

		success();
	}

	@Override
	public void interrupt() {
		super.interrupt();
	}

	@Override
	public boolean isExecuting() {
		return super.isExecuting();
	}

}
