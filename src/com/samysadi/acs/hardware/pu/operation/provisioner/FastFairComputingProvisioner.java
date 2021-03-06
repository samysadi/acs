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

package com.samysadi.acs.hardware.pu.operation.provisioner;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.hardware.pu.operation.ComputingResource;
import com.samysadi.acs.virtualization.job.operation.provisioner.FastFairProvisioner;

public class FastFairComputingProvisioner extends FastFairProvisioner<ComputingOperation, ComputingResource>
	implements ComputingProvisioner  {

	private long capacity;

	/**
	 * Empty constructor that creates a provisioner with zero capacity.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link FastFairComputingProvisioner#FastFairComputingProvisioner(long)} though.
	 */
	public FastFairComputingProvisioner() {
		this(0l);
	}

	public FastFairComputingProvisioner(long mipsCapacity) {
		super();
		this.capacity = mipsCapacity;
	}

	@Override
	public FastFairComputingProvisioner clone() {
		final FastFairComputingProvisioner clone = (FastFairComputingProvisioner) super.clone();
		return clone;
	}

	@Override
	public long getCapacity() {
		return capacity;
	}

	@Override
	protected ComputingResource makeResource(long val) {
		return new ComputingResource(val);
	}

}
