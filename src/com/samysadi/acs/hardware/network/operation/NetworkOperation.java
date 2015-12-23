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

package com.samysadi.acs.hardware.network.operation;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.hardware.network.routingprotocol.Route;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.RemoteOperation;

/**
 * This interface defines methods to simulate a data transmission operation between two Jobs
 * through a given route in the network.
 * 
 * <p>When starting a network operation, it seeks network resource from each provisioner in the allocated {@link Route} and 
 * stays activated until its entire length is processed, until a failure happens or until it is explicitly stopped (using appropriate method).
 * 
 * <p>You must ensure that both the parent Job and the destination Job are started in order to start this operation, otherwise an IllegalStateException is thrown.<br/>
 * If you start this operation while it has a <tt>null</tt> parent, or a <tt>null</tt> destination Job then a NullPointerException is thrown.
 * 
 * <p>If this operation has no allocated {@link Route}, then it will fail to start.<br/>
 * Besides, each link in the {@link Route} must have a non <tt>null</tt> provisioner otherwise a NullPointerException 
 * is thrown.
 * 
 * <p>When activating the operation, implementations must take care to register appropriate listeners 
 * on each device on the route, and on the source and destination jobs (and VMs) to ensure that
 * the operation is deactivated or updated accordingly when:
 * <ul>
 * 		<li> {@link NotificationCodes#OPERATION_RESOURCE_INVALIDATED}
 * 			the allocated BW on the route has changed;
 * 		<li> {@link NotificationCodes#RP_ROUTING_UPDATED}
 * 			the route is no longer valid (ex: because a device or interface has failed).
 * </ul>
 * All implementation classes should provide a constructor with two arguments. The first
 * argument is a {@link Job} that specifies the destination job. The second argument
 * is a <tt>long</tt> that specifies the operation length (length of the transmission).
 * 
 * @since 1.0
 */
public interface NetworkOperation extends RemoteOperation<NetworkResource> {

	@Override
	public NetworkOperation clone();

	/**
	 * Returns <tt>true</tt> if data should be retransmitted on error.
	 * 
	 * <p>If <tt>false</tt> that means that the loss rate between nodes is not taken into account, which may be used to simulate 
	 * communications where data integrity doesn't matter a lot.
	 * 
	 * @return <tt>true</tt> if data should be retransmitted on error
	 */
	public boolean isRetransmitOnError();

	/**
	 * Returns the route that is assigned for this operation or <tt>null</tt>.
	 * 
	 * <p><tt>null</tt> is returned if no route is assigned yet.
	 * 
	 * @return Returns the route that is assigned for this operation or <tt>null</tt>
	 */
	public Route getAllocatedRoute();

	/**
	 * Returns the size in bytes (number of {@link Simulator#BYTE}s) of the data to be transmitted by this operation.
	 * 
	 * @return the size in bytes (number of {@link Simulator#BYTE}s) of the data to be transmitted by this operation
	 */
	public long getLength();

	/**
	 * Returns the maximum bw (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) that this operation can use.
	 * 
	 * @return the maximum bw (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) that this operation can use
	 */
	public long getResourceMax();

	/**
	 * Sets the maximum bw (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) that this operation can use. <br/>
	 * The operation will never allocate more than this bw through the route.<br/>
	 * <b>Default</b> value is {@code Long.MAX_VALUE}.
	 * 
	 * @param maxBw
	 */
	public void setResourceMax(long maxBw);

	/**
	 * Returns the minimum needed Bw (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) by the operation to be activated
	 * 
	 * @return the minimum needed Bw (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) by the operation to be activated
	 */
	public long getResourceMin();

	/**
	 * Sets the minimum usable Bw (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) by the operation to be activated.
	 * 
	 * @param minBw
	 */
	public void setResourceMin(long minBw);

	/**
	 * Returns the total transmitted size (number of {@link Simulator#BYTE}s) that has effectively reached the DestinationJob.
	 * This should not include the data that is being transmitted.
	 * Only the data that has been transmitted until last deactivation should be included.
	 * 
	 * @return The total effectively transmitted size (number of {@link Simulator#BYTE}s) that has reached the DestinationJob
	 * @throws IllegalStateException if the operation is active
	 */
	public long getCompletedLength();
	
}
