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

package com.samysadi.acs.hardware.storage;

import java.util.List;

import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.hardware.misc.MemoryUnit;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;


/**
 * All implementation classes should provide a constructor with one argument
 * of <tt>long</tt> type that specifies the Storage's capacity.
 *
 * @since 1.0
 */
public interface Storage extends MemoryUnit<StorageFile>, FailureProneEntity {

	@Override
	public Storage clone();

	/**
	 * Alias for {@link MemoryUnit#getMemoryZones()}.
	 *
	 * @return a list containing all {@link StorageFile}s in this Storage
	 */
	public List<StorageFile> getStorageFiles();

	/**
	 * Returns the {@link StorageProvisioner}.
	 *
	 * @return the {@link StorageProvisioner}
	 */
	public StorageProvisioner getStorageProvisioner();
}
