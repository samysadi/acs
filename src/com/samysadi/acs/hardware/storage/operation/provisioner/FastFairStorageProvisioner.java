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

package com.samysadi.acs.hardware.storage.operation.provisioner;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.hardware.storage.operation.StorageResource;
import com.samysadi.acs.virtualization.job.operation.provisioner.FastFairProvisioner;

/**
 *
 * @since 1.0
 */
public class FastFairStorageProvisioner extends FastFairProvisioner<StorageOperation, StorageResource>
	implements StorageProvisioner {

	private long capacity;

	/**
	 * Empty constructor that creates a provisioner with zero capacity.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link FastFairStorageProvisioner#FastFairStorageProvisioner(long)} though.
	 */
	public FastFairStorageProvisioner() {
		this(0l);
	}

	public FastFairStorageProvisioner(long transferRateCapacity) {
		super();
		this.capacity = transferRateCapacity;
	}

	@Override
	public FastFairStorageProvisioner clone() {
		final FastFairStorageProvisioner clone = (FastFairStorageProvisioner) super.clone();
		return clone;
	}

	@Override
	public long getCapacity() {
		return this.capacity;
	}

	@Override
	protected StorageResource makeResource(long val) {
		return new StorageResource(val);
	}
}
