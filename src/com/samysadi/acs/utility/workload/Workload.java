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
package com.samysadi.acs.utility.workload;

import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.virtualization.job.Job;

/**
 *
 * @since 1.0
 */
public interface Workload extends Job {

	/**
	 * Returns this workload's RamZone.
	 *
	 * <p>If there is no RamZone associated with the current workload, then one is created automatically.
	 *
	 * @return this workload's RamZone
	 */
	public RamZone getRamZone();

	/**
	 * Returns current workload's {@link StorageFile}.
	 *
	 * <p>By default, a random file is selected among all current user's files.<br/>
	 * And, if the current user has no attached files, then the default is <tt>null</tt>.
	 * Current user is the owner of the parent virtual machine.
	 *
	 * <p>All tasks that works on {@link StorageFile}s will be given this file.
	 *
	 * @return current workload's {@link StorageFile}
	 */
	public StorageFile getStorageFile();

	/**
	 * Updates current workload's {@link StorageFile}.
	 *
	 * @param storageFile
	 */
	public void setStorageFile(StorageFile storageFile);

	/**
	 * Returns current workload's remote {@link Job}.
	 *
	 * <p>By default, a job is created on one of the current user's {@link ThinClient}s.<br/>
	 * And, if the current user has no attached {@link ThinClient}s then one is created.
	 * Current user is the owner of the parent virtual machine.
	 *
	 * <p>All tasks that needs a remote {@link Job} will be given this job.
	 *
	 * @return current workload's remote {@link Job}
	 */
	public Job getRemoteJob();

	/**
	 * Updates current workload's remote {@link Job}.
	 *
	 * @param remoteJob
	 */
	public void setRemoteJob(Job remoteJob);
}
