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

package com.samysadi.acs.service.staas.sfreplicaselection;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.staas.Staas;

/**
 * 
 * @since 1.0
 */
public interface SfReplicaSelectionPolicy extends Entity {

	@Override
	public SfReplicaSelectionPolicy clone();

	@Override
	public Staas getParent();

	/**
	 * Selects the best replica of the given <tt>storageFile</tt> and returns it.<br/>
	 * The selection is done, considering the replica will be accessed from the given
	 * <tt>host</tt>, and for the given <tt>operationType</tt>.
	 * 
	 * @param storageFile
	 * @param operationType
	 * @param host
	 * @return the selected replica
	 */
	public StorageFile selectBestReplica(StorageFile storageFile, 
			StorageOperation.StorageOperationType operationType,
			Host host);

}
