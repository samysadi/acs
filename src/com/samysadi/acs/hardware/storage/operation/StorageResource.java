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

package com.samysadi.acs.hardware.storage.operation;

import com.samysadi.acs.virtualization.job.operation.LongResource;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class StorageResource extends LongResource {
	private long transferRate;

	public StorageResource(long transferRate) {
		super();
		this.transferRate = transferRate;
	}

	public long getTransferRate() {
		return this.transferRate ;
	}

	@Override
	public long getLong() {
		return getTransferRate();
	}

	@Override
	public StorageResource clone() {
		final StorageResource clone = (StorageResource) super.clone();
		return clone;
	}

	@Override
	public StorageResource clone(long transferRate) {
		final StorageResource clone = this.clone();
		clone.transferRate = transferRate;
		return clone;
	}
}
