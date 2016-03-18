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

package com.samysadi.acs.virtualization.job.operation.provisioner;

import java.util.HashSet;

import com.samysadi.acs.virtualization.job.operation.LongResource;
import com.samysadi.acs.virtualization.job.operation.Operation;


/**
 * This provisioner gives promises that are equal to its current free capacity.<br/>
 * It will also ensure that any granted resource will not be invalidated even if new capacity is freed.
 * Thus, granted resources remain allocated until they are explicitly freed.
 *
 * @since 1.0
 */
public abstract class StaticProvisioner<OperationType extends Operation<Resource>, Resource extends LongResource>
	extends LongProvisionerImpl<OperationType, Resource> {
	private long usedCapacity;
	protected HashSet<OperationType> operations;

	public long getUsedCapacity() {
		return usedCapacity;
	}

	protected void setUsedCapacity(long usedCapacity) {
		if (usedCapacity > getCapacity())
			throw new IllegalArgumentException("The given resource cannot be allocated.");
		if (usedCapacity < 0)
			throw new IllegalArgumentException("The given resource cannot be freed.");
		this.usedCapacity = usedCapacity;
	}

	public StaticProvisioner() {
		super();
	}

	@Override
	public StaticProvisioner<OperationType, Resource> clone() {
		if (operations != null && !operations.isEmpty())
			throw new IllegalStateException("Provisioner cannot be cloned when there is running operations.");
		final StaticProvisioner<OperationType, Resource> clone = (StaticProvisioner<OperationType, Resource>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.operations = null;
		this.usedCapacity = 0l;
	}

	@Override
	public Resource getResourcePromise(OperationType operation) {
		return makeResource(getCapacity() - getUsedCapacity());
	}

	@Override
	public void grantAllocatedResource(OperationType operation) {
		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

		long newUsed = getUsedCapacity() + operation.getAllocatedResource().getLong();

		if (operations == null)
			operations = new HashSet<OperationType>();

		operations.add(operation);

		setUsedCapacity(newUsed);
	}

	@Override
	public void revokeAllocatedResource(OperationType operation) {
		if (operations == null)
			return;

		if (!operations.remove(operation))
			return;

		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

		setUsedCapacity(getUsedCapacity() - operation.getAllocatedResource().getLong());
	}

//	@Override
//	public Resource getTotalGrantedResource() {
//		return makeResource(getUsedCapacity());
//	}

}
