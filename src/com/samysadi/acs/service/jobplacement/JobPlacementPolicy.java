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

package com.samysadi.acs.service.jobplacement;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.TemporaryVirtualMachine;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;

/**
 * Defines methods for selecting a VM among all available VMs
 * when placing a Job.
 *
 * <p>Implementations must take care not to select {@link TemporaryVirtualMachine}s for
 * placing jobs.
 *
 * @since 1.0
 */
public interface JobPlacementPolicy extends Entity {

	@Override
	public CloudProvider getParent();

	@Override
	public JobPlacementPolicy clone();

	/**
	 * Selects and returns a virtual machine where to place the given <tt>job</tt>.
	 *
	 * <p>Depending on whether a VM was found or not, a {@link NotificationCodes#JOBPLACEMENT_VMSELECTION_SUCCESS} or
	 * {@link NotificationCodes#JOBPLACEMENT_VMSELECTION_FAILED} is thrown.
	 *
	 * @param job the job you want to place
	 * @param vms a list containing all the virtual machines where to make the selection
	 * @return the selected virtual machine or <tt>null</tt> if no virtual machine was found
	 */
	public VirtualMachine selectVm(Job job, List<VirtualMachine> vms);

}
