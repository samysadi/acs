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

import com.samysadi.acs.virtualization.job.operation.LongResource;

/**
 *
 * @since 1.0
 */
public class NetworkResource extends LongResource {
	private long bw;
	private long latency;
	private double lossRate;

	public NetworkResource(long bw, long latency, double lossRate) {
		super();
		this.bw = bw;
		this.latency = latency;
		this.lossRate = lossRate;
	}

	public long getBw() {
		return bw;
	}

	public long getLatency() {
		return latency;
	}

	public double getLossRate() {
		return lossRate;
	}

	@Override
	public long getLong() {
		return getBw();
	}

	@Override
	public NetworkResource clone() {
		final NetworkResource clone = (NetworkResource) super.clone();
		return clone;
	}

	@Override
	public NetworkResource clone(long bw) {
		final NetworkResource clone = this.clone();
		clone.bw = bw;
		return clone;
	}
}
