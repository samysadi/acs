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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.operation.LongResource;
import com.samysadi.acs.virtualization.job.operation.Operation;


/**
 * This provisioner ensures fair distribution of the available capacity through active operations. It
 * gives resource promises that are equal to the average capacity computed
 * by considering all active operations that use this provisioner.<br/>
 * If a operation uses less than the promised average, then this provisioner will ensure that the remaining
 * capacity is fairly distributed among other operations that need it. To do so, they are notified whenever
 * there is more or less resources available (see
 * {@link NotificationCodes#OPERATION_RESOURCE_INVALIDATED}).
 *
 * <p>Note that this provisioner is more accurate and all available capacity is usable,
 * unlike {@link FastFairProvisioner} which may not in some use cases.
 *
 * @since 1.0
 */
public abstract class FairProvisioner<OperationType extends Operation<Resource>, Resource extends LongResource>
	extends LongProvisionerImpl<OperationType, Resource> {

	public static class OperationComparator<OperationType extends Operation<? extends LongResource>> implements Comparator<Object> {
		@SuppressWarnings("unchecked")
		@Override
		public int compare(Object o1, Object o2) {
			if (((OperationType)o1).getAllocatedResource() == null) {
				if (((OperationType)o2).getAllocatedResource() == null)
					return 0;
				return -1;
			}
			return Long.compare(((OperationType) o1).getAllocatedResource().getLong(), ((OperationType) o2).getAllocatedResource().getLong());
		}
	}

	protected HashSet<OperationType> operations;
//	private long grantedCapacity;

	public FairProvisioner() {
		super();
	}

	@Override
	public FairProvisioner<OperationType, Resource> clone() {
		if (operations != null && !operations.isEmpty())
			throw new IllegalStateException("Provisioner cannot be cloned when there is running operations.");
		final FairProvisioner<OperationType, Resource> clone = (FairProvisioner<OperationType, Resource>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.operations = null;
//		this.grantedCapacity = 0;
	}

	protected Iterator<OperationType> getOrderIterator() {
		final Object[] opa = operations.toArray();
		Arrays.sort(opa, new OperationComparator<OperationType>());
		return new OrderedOperationIterator<OperationType>(opa);
	}

	private static final class OrderedOperationIterator<OperationType>
			implements Iterator<OperationType> {
		private final Object[] opa;
		int i = 0;

		private OrderedOperationIterator(Object[] opa) {
			this.opa = opa;
		}

		@Override
		public boolean hasNext() {
			return i<opa.length;
		}

		@SuppressWarnings("unchecked")
		@Override
		public OperationType next() {
//			if (i>=opa.length)
//				throw new NoSuchElementException();
			return (OperationType) opa[i++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Resource getResourcePromise(OperationType operation) {
		long c = getCapacity();

		if (operations == null)
			return makeResource(c);

		int count = operations.size();
		if (!operations.contains(operation))
			count++;
		Iterator<OperationType> it = getOrderIterator();
		while (it.hasNext()) {
			OperationType op = it.next();
			if (op.equals(operation))
				continue;
			long avg = Math.round(Math.floor((double)c/count));
			if (op.getAllocatedResource().getLong() < avg) {
				c-=op.getAllocatedResource().getLong();
			} else {
				return makeResource(avg);
			}
			count--;
		}
		return makeResource(c);
	}

	@Override
	public void grantAllocatedResource(OperationType operation) {
		if (operation.getAllocatedResource() == null)
			throw new NullPointerException();

		if (operations == null)
			operations = new HashSet<OperationType>();

		if (operation.getAllocatedResource().getLong() > getResourcePromise(operation).getLong())
			throw new IllegalArgumentException("We cannot allocate the resource.");

		operations.add(operation);

//		this.grantedCapacity = 0l;
		Iterator<OperationType> it = operations.iterator();
		while (it.hasNext()) {
			OperationType op = it.next();
//			grantedCapacity+= op.getAllocatedResource().getLong();
			if (op.equals(operation))
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
		Iterator<OperationType> it = operations.iterator();
		while (it.hasNext()) {
			OperationType op = it.next();
//			grantedCapacity+= op.getAllocatedResource().getLong();
			op.notify(NotificationCodes.OPERATION_RESOURCE_INVALIDATED, null);
		}
	}

//	@Override
//	public Resource getTotalGrantedResource() {
//		return makeResource(grantedCapacity);
//	}
}
