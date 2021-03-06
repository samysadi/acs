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

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.operation.NetworkResource;
import com.samysadi.acs.virtualization.job.operation.provisioner.InfiniteProvisioner;

/**
 *
 * @since 1.0
 */
public class InfiniteNetworkProvisioner extends InfiniteProvisioner<NetworkOperation, NetworkResource>
	implements NetworkProvisioner {
	private long latency;
	private double lossRate;

	/**
	 * Empty constructor that creates a provisioner with infinite capacity and zero latency and lossRate.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link InfiniteNetworkProvisioner#InfiniteNetworkProvisioner(long, long, double)} though.
	 */
	public InfiniteNetworkProvisioner() {
		this(0l, 0l, 0d);
	}

	/**
	 * Creates a provisioner with infinite capacity.
	 *
	 * <p><tt>bwCapacity</tt> is not taken into consideration and this provisioner will always have infinite capacity.
	 *
	 * @param bwCapacity
	 * @param latency
	 * @param lossRate
	 */
	public InfiniteNetworkProvisioner(long bwCapacity, long latency, double lossRate) {
		super();
		this.latency = latency;
		this.lossRate = lossRate;
	}

	@Override
	public InfiniteNetworkProvisioner clone() {
		final InfiniteNetworkProvisioner clone = (InfiniteNetworkProvisioner) super.clone();
		return clone;
	}

	@Override
	public long getLatency() {
		return latency;
	}

	@Override
	public double getLossRate() {
		return lossRate;
	}

	@Override
	public NetworkResource makeResource(long bw) {
		return new NetworkResource(bw, getLatency(), getLossRate());
	}
}
