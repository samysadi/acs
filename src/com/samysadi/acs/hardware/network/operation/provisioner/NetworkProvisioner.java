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

package com.samysadi.acs.hardware.network.operation.provisioner;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.operation.NetworkResource;
import com.samysadi.acs.virtualization.job.operation.provisioner.Provisioner;

/**
 * All implementation classes should provide a constructor with three arguments. The first
 * of <tt>long</tt> type specifies the provisioner's capacity. The second also of <tt>long</tt>
 * type specifies the induced latency when using the provisioner. The last specified the
 * induced loss rate.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface NetworkProvisioner extends Provisioner<NetworkOperation, NetworkResource> {

	@Override
	public NetworkProvisioner clone();

	/**
	 * Returns the BW (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) total capacity of this provisioner.
	 * 
	 * @return the BW (number of {@link Simulator#BYTE}s per one {@link Simulator#SECOND}) total capacity of this provisioner
	 */
	public long getCapacity();

	/**
	 * Returns the measured link latency that is induced when transferring data.
	 * 
	 * @return the measured link latency that is induced when transferring data
	 */
	public long getLatency();

	/**
	 * Returns a double representing the percentage of data loss during transmission.
	 * 
	 * <p>For example: 0.05 indicates a loss rate of 5%.<br/>
	 * That means that 5% of the total transmitted date will be resent.
	 * This also means that a total of 105% of dataSize were effectively transmitted.
	 * 
	 * @return the loss rate that is induced by using this provisioner
	 */
	public double getLossRate();

}
