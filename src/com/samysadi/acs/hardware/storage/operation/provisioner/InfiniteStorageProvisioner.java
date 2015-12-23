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
import com.samysadi.acs.virtualization.job.operation.provisioner.InfiniteProvisioner;

/**
 * 
 * @since 1.0
 */
public class InfiniteStorageProvisioner extends InfiniteProvisioner<StorageOperation, StorageResource> 
	implements StorageProvisioner {

	/**
	 * Empty constructor that creates a provisioner with infinite capacity.
	 * 
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link InfiniteStorageProvisioner#InfiniteStorageProvisioner(long)} though.
	 */
	public InfiniteStorageProvisioner() {
		this(0);
	}

	/**
	 * Creates a provisioner with infinite capacity.
	 * 
	 * <p><tt>transferRateCapacity</tt> is not taken into consideration and this provisioner will always have infinite capacity.
	 * 
	 * @param transferRateCapacity
	 */
	public InfiniteStorageProvisioner(long transferRateCapacity) {
		super();
	}

	@Override
	public InfiniteStorageProvisioner clone() {
		final InfiniteStorageProvisioner clone = (InfiniteStorageProvisioner) super.clone();
		return clone;
	}

	@Override
	protected StorageResource makeResource(long val) {
		return new StorageResource(val);
	}
}
