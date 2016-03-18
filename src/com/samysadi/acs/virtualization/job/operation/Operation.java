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

package com.samysadi.acs.virtualization.job.operation;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.Job;

/**
 * An operation is {@link RunnableEntity} that needs a certain amount
 * of running time in order to be completed.<br/>
 * The needed running time directly depends on the allocated resource for the operation.
 *
 * <p>As any other {@link RunnableEntity} you need to listen to the
 * {@link NotificationCodes#RUNNABLE_STATE_CHANGED} notification to know when the operation
 * stops / is completed.
 *
 * <p>See {@link RunnableEntity} for more information on runnable entities.
 *
 * @since 1.0
 */
public interface Operation<Resource> extends Entity, RunnableEntity {

	@Override
	public Operation<Resource> clone();

	/**
	 * Returns parent job which also owns this operation.
	 *
	 * @return the parent job
	 */
	@Override
	public Job getParent();

	/**
	 * Returns the current allocated resources for this operation or <tt>null</tt>
	 * if no resource is allocated for this operation.
	 *
	 * @return the current allocated resources for this operation or <tt>null</tt>
	 */
	public Resource getAllocatedResource();

	/**
	 * Returns the delay remaining until the end of this operation using the current allocated resources.
	 *
	 * <p>If the operation is running, the returned value takes into account the progress made during current activation.
	 *
	 * @return the delay remaining until the end of this operation using the current allocated resources
	 */
	public long getRemainingDelay();

	/**
	 * Returns the total accumulated simulation time when this operation was running.
	 *
	 * @return the total accumulated simulation time when this operation was running
	 */
	public long getTotalRunningTime();
}
