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

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation.StorageOperationType;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.utility.collections.ShuffledIterator;

/**
 *
 * @since 1.0
 */
public class SfReplicaSelectionPolicyDefault extends EntityImpl implements SfReplicaSelectionPolicy {

	public SfReplicaSelectionPolicyDefault() {
		super();
	}

	@Override
	public SfReplicaSelectionPolicyDefault clone() {
		final SfReplicaSelectionPolicyDefault clone = (SfReplicaSelectionPolicyDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();


	}

	@Override
	public Staas getParent() {
		return (Staas) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Staas))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public StorageFile selectBestReplica(StorageFile storageFile,
			StorageOperationType operationType, Host host) {

		if (getParent().getReplicationManager() == null)
			return storageFile;

		List<StorageFile> r = getParent().getReplicationManager().getReplicas(storageFile);

		if (operationType != StorageOperationType.READ)
			return r.get(0); //if modification operation, then return primary

		ShuffledIterator<StorageFile> it = new ShuffledIterator<StorageFile>(r);

		return it.next();
	}
}
