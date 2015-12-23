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

import com.samysadi.acs.virtualization.job.operation.LongResource;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 * This provisioner always give resource promises with a value of {@link Long#MAX_VALUE}.
 * 
 * @since 1.0
 */
public abstract class InfiniteProvisioner<OperationType extends Operation<Resource>, Resource extends LongResource> 
	extends LongProvisionerImpl<OperationType, Resource> {
//	private long grantedCapacity0;
//	private int grantedCapacity1;

	public InfiniteProvisioner() {
		super();
	}

	@Override
	public InfiniteProvisioner<OperationType, Resource> clone() {
		final InfiniteProvisioner<OperationType, Resource> clone = (InfiniteProvisioner<OperationType, Resource>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

//		this.grantedCapacity0 = 0;
//		this.grantedCapacity1 = 0;
	}

	@Override
	public long getCapacity() {
		return Long.MAX_VALUE;
	}

	@Override
	public Resource getResourcePromise(OperationType operation) {
		return makeResource(Long.MAX_VALUE);
	}

	@Override
	public void grantAllocatedResource(OperationType operation) {
		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

//		addGrantedCapacity(operation.getAllocatedResource().getLong());
	}

	@Override
	public void revokeAllocatedResource(OperationType operation) {
		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

//		substractGrantedCapacity(operation.getAllocatedResource().getLong());
	}

//	private void addGrantedCapacity(long val) {
//		long d = val - Long.MAX_VALUE + this.grantedCapacity0; //make sure we stay in long bounds
//		if (d >= 0) {
//			this.grantedCapacity1++;
//			this.grantedCapacity0 = d;
//		} else {
//			this.grantedCapacity0 += val;
//		}
//	}
//
//	private void substractGrantedCapacity(long val) {
//		long d = this.grantedCapacity0 - val;
//		if (d <= 0) {
//			this.grantedCapacity1--;
//			this.grantedCapacity0 = d + Long.MAX_VALUE;
//		} else {
//			this.grantedCapacity0 = d;
//		}
//	}
//
//	@Override
//	public Resource getTotalGrantedResource() {
//		if (this.grantedCapacity1 == 0)
//			return makeResource(this.grantedCapacity0);
//		else if (this.grantedCapacity1 > 0)
//			return makeResource(Long.MAX_VALUE);
//		else 
//			return makeResource(0);
//	}

}
