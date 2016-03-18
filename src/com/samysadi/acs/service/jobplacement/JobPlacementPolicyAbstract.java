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
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.TemporaryVirtualMachine;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;

/**
 *
 * @since 1.0
 */
public abstract class JobPlacementPolicyAbstract extends EntityImpl implements JobPlacementPolicy {
	public JobPlacementPolicyAbstract() {
		super();
	}

	@Override
	public JobPlacementPolicyAbstract clone() {
		final JobPlacementPolicyAbstract clone = (JobPlacementPolicyAbstract) super.clone();
		return clone;
	}

	@Override
	public CloudProvider getParent() {
		return (CloudProvider) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof CloudProvider))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	/**
	 * Computes and returns a score indicating the level of compliancy of the given <tt>vm</tt>
	 * towards the job's constraints.
	 *
	 * <p>A return value of <tt>0.0d</tt> or less indicates that the given vm does not comply (at all). And the <tt>job</tt>
	 * cannot be placed on that vm.
	 *
	 * <p>If this method returns {@link Double#POSITIVE_INFINITY} then the given vm produces the highest matching score for the current job's constraints.
	 *
	 * <p>This method <b>does not</b> take care of the runnable state of the vm.
	 *
	 * @param job
	 * @param vm
	 * @return computed score for placing the given job on the given vm
	 */
	protected double computeJobScore(Job job, VirtualMachine vm) {
		if (vm instanceof TemporaryVirtualMachine)
			return 0.0d;
		int c = vm.getJobs().size();
		if (c == 0)
			return Double.POSITIVE_INFINITY;
		return 1.0d / c;
	}

	/**
	 * Returns the selected vm among all given vms.
	 *
	 * <p>Override this method in order to define your policy for selecting the vm.
	 *
	 * @param job
	 * @param vms
	 * @return the selected vm among all given vms
	 */
	protected abstract VirtualMachine _selectVm(Job job, List<VirtualMachine> vms);

	@Override
	public VirtualMachine selectVm(Job job, List<VirtualMachine> vms) {
		VirtualMachine vm = _selectVm(job, vms);

		if (vm == null) {
			notify(NotificationCodes.JOBPLACEMENT_VMSELECTION_FAILED, job);
			return null;
		}

		notify(NotificationCodes.JOBPLACEMENT_VMSELECTION_SUCCESS, job);
		return vm;
	}
}
