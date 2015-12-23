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

import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.Job;

/**
 * This interface describes an {@link Operation} that has also a destination {@link Job}.
 * 
 * <p>{@link RemoteOperation}s cannot be started if their destination job is not running.<br/>
 * Also, {@link Job} implementations must pause / start / cancel {@link RemoteOperation}s accordingly
 * if they are paused / started / canceled. Please see {@link Job} documentation for more information.
 * 
 * @since 1.0
 */
public interface RemoteOperation<Resource> extends Operation<Resource> {

	@Override
	public RemoteOperation<Resource> clone();

	/**
	 * Returns the destination job where the operation's data will be sent.
	 * 
	 * @return the destination job
	 */
	public Job getDestinationJob();

	/**
	 * Sets the new destinationJob.
	 * 
	 * <p>A {@link NotificationCodes#OPERATION_DEST_JOB_CHANGED} is thrown.<br/>
	 * The old destinationJob is notified using {@link NotificationCodes#JOB_DEST_OPERATION_REMOVED}.<br/>
	 * The new destinationJob is notified using {@link NotificationCodes#JOB_DEST_OPERATION_ADDED}.
	 * 
	 * @param destinationJob the new destinationJob
	 * @throws IllegalStateException if this operation is already running
	 */
	public void setDestinationJob(Job destinationJob);
	
}
