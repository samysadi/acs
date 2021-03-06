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

package com.samysadi.acs.hardware.pu.operation;

import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.operation.LongOperation;

/**
 * This interface defines methods to simulate a computing operation.
 *
 * <p>When starting a computing operation, it seeks computing resource from its {@link ProcessingUnit} and
 * keeps running until its entire length is processed, until a failure happens or until it is explicitly stopped (using appropriate method).
 *
 * <p>You must ensure that the parent Job is started in order to start this operation, otherwise an IllegalStateException is thrown.<br/>
 * If you start this operation while it has a <tt>null</tt> parent then a NullPointerException is thrown.
 *
 * <p>If this operation has no allocated {@link ProcessingUnit}, then it will fail to start.<br/>
 * Besides, the allocated {@link ProcessingUnit} must have a non <tt>null</tt> provisioner otherwise a NullPointerException
 * is thrown.
 *
 * <p>Implementations must take care to listen to the following notifications to stop or update the operation when:<ul>
 * 		<li> {@link NotificationCodes#OPERATION_RESOURCE_INVALIDATED}
 * 			The mips allocated for this operation changes (because other operations are started / stopped).
 * 		<li> {@link NotificationCodes#FAILURE_STATE_CHANGED}
 * 			A processing unit fails. Or the device that contains the VM (of the sourceJob) fails.
 * </ul>
 * All implementation classes should provide a constructor with one argument
 * of <tt>long</tt> type that specifies the operation length.
 *
 * @since 1.0
 */
public interface ComputingOperation extends LongOperation<ComputingResource> {

	@Override
	public ComputingOperation clone();

	/**
	 * Returns the allocated {@link ProcessingUnit} that is used (or going to be used) for the computing task.
	 *
	 * @return the allocated {@link ProcessingUnit} that is used (or going to be used) for the computing task
	 */
	public ProcessingUnit getAllocatedPu();
}
