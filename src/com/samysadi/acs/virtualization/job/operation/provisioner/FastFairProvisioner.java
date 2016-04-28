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
import java.util.Iterator;

import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.operation.LongResource;
import com.samysadi.acs.virtualization.job.operation.Operation;


/**
 * This provisioner gives promises that are equal to the average capacity computed
 * by considering all running operations that use this provisioner.<br/>
 * When resources are granted (or revoked) for (from) a operation, then other running operations are
 * notified that they can use more (or less) resources (see
 * {@link NotificationCodes#OPERATION_RESOURCE_INVALIDATED}).
 *
 * <p>Note that this provisioner will always reserve a average amount of resource for each operation, even
 * if this operation uses less than that average. So, this provisioner may not be accurate.<br/>
 * If you need such level of accuracy you can use {@link FairProvisioner} which may be a bit slower
 * than this provisioner.
 *
 * @since 1.0
 */
public abstract class FastFairProvisioner<OperationType extends Operation<Resource>, Resource extends LongResource>
	extends LongProvisionerImpl<OperationType, Resource> {

	protected HashSet<OperationType> operations;
//	private long grantedCapacity;

	public FastFairProvisioner() {
		super();
	}

	@Override
	public FastFairProvisioner<OperationType, Resource> clone() {
		if (operations != null && !operations.isEmpty())
			throw new IllegalStateException("Provisioner cannot be cloned when there is running operations.");
		final FastFairProvisioner<OperationType, Resource> clone = (FastFairProvisioner<OperationType, Resource>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.operations = null;
//		this.grantedCapacity = 0;
	}

	@Override
	public Resource getResourcePromise(OperationType operation) {
		long c = getCapacity();

		if (operations == null)
			return makeResource(c);

		int count = operations.size();
		if (!operations.contains(operation))
			count++;
		long avg = Math.round(Math.floor((double)c/count));
		return makeResource(avg);
	}

	@Override
	public void grantAllocatedResource(OperationType operation) {
		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

		long avg = getResourcePromise(operation).getLong();

		if (operation.getAllocatedResource().getLong() > avg)
			throw new IllegalArgumentException("We cannot allocate the resource.");

		if (operations == null)
			operations = new HashSet<OperationType>();

		operations.add(operation);

//		this.grantedCapacity = 0l;
		Iterator<OperationType> it = operations.iterator();
		while (it.hasNext()) {
			OperationType op = it.next();
//			grantedCapacity+= op.getAllocatedResource().getLong();
			if (op.equals(operation))
				continue;
			if (op.getAllocatedResource().getLong() <= avg)
				continue;
			op.notify(NotificationCodes.OPERATION_RESOURCE_INVALIDATED, null);
		}
	}

	@Override
	public void revokeAllocatedResource(OperationType operation) {
		if (operations == null)
			return;

		if (!operations.remove(operation))
			return;

		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

//		this.grantedCapacity = 0l;
		if (operations.isEmpty())
			return;

		long avg = Math.round(Math.floor((double)getCapacity()/operations.size()));

		Iterator<OperationType> it = operations.iterator();
		while (it.hasNext()) {
			OperationType op = it.next();
//			grantedCapacity+= op.getAllocatedResource().getLong();
			if (op.getAllocatedResource().getLong() >= avg)
				continue;
			op.notify(NotificationCodes.OPERATION_RESOURCE_INVALIDATED, null);
		}
	}

//	@Override
//	public Resource getTotalGrantedResource() {
//		return makeResource(grantedCapacity);
//	}

}
