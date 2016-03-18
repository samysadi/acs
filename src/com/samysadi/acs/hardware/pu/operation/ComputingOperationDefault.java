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

package com.samysadi.acs.hardware.pu.operation;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.PuAllocator;
import com.samysadi.acs.virtualization.job.operation.LongOperationImpl;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.SynchronizableOperation;

/**
 * This implementation will automatically
 * seek a {@link ProcessingUnit} from the parent job's VM each time it is activated.
 *
 * <p>Make sure the parent job's VM has a non <tt>null</tt> {@link PuAllocator} or a
 * NullPointerException will be thrown whenever you try to start this operation.
 *
 * @since 1.0
 */
public class ComputingOperationDefault extends LongOperationImpl<ComputingResource> implements ComputingOperation, SynchronizableOperation<ComputingResource> {
	private ProcessingUnit allocatedPu;

	/**
	 * Empty constructor that creates a zero-length operation.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link ComputingOperationDefault#ComputingOperationDefault(long)} though.
	 */
	public ComputingOperationDefault() {
		this(0l);
	}

	public ComputingOperationDefault(long lengthInMips) {
		super(lengthInMips);
	}

	@Override
	public ComputingOperationDefault clone() {
		final ComputingOperationDefault clone = (ComputingOperationDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.allocatedPu = null;
	}

	@Override
	protected boolean registerListeners() {
		if (!super.registerListeners())
			return false;

		//the allocated Pu fails
		if (!addFailureDependency(getAllocatedPu()))
			return false;

		return true;
	}

	@Override
	public boolean canRestart() {
		if (hasParentRec() && getParent().getParent().getPuAllocator() == null)
			return false;
		return super.canRestart();
	}

	@Override
	protected String getCannotRestartReason() {
		if (hasParentRec() && getParent().getParent().getPuAllocator() == null)
			return "This entity (" + this + ") cannot be restarted because the parent VM has a null PuAllocator.";
		return super.getCannotRestartReason();
	}

	@Override
	public boolean canStart() {
		if (hasParentRec() && getParent().getParent().getPuAllocator() == null)
			return false;
		return super.canStart();
	}

	@Override
	protected String getCannotStartReason() {
		if (hasParentRec() && getParent().getParent().getPuAllocator() == null)
			return "This entity (" + this + ") cannot be started because the parent VM has a null PuAllocator.";
		return super.getCannotStartReason();
	}

	@Override
	protected void prepareActivation() {
		ProcessingUnit pu = getParent().getParent().getPuAllocator().chooseProcessingUnit(this);
		setAllocatedPu(pu);
	}

	@Override
	public ProcessingUnit getAllocatedPu() {
		return allocatedPu;
	}

	protected void setAllocatedPu(ProcessingUnit pu) {
		if (this.isRunning())
			throw new IllegalStateException("The operation is activated");
		if (allocatedPu == pu)
			return;
		allocatedPu = pu;
		notify(NotificationCodes.CO_PU_CHANGED, null);
	}

	@Override
	protected ComputingResource getProvisionerPromise() {
		if (getAllocatedPu() == null)
			return null;
		ComputingResource pr = getAllocatedPu().getComputingProvisioner().getResourcePromise(this);
		if (getParent().getParent().getComputingProvisioner() != null) {
			ComputingResource vmPr = getParent().getParent().getComputingProvisioner().getResourcePromise(this);
			if (pr.getLong() > vmPr.getLong())
				pr = pr.clone(vmPr.getLong());
		}
		return validateResourcePromise(pr);
	}

	@Override
	protected void grantAllocatedResource() {
		if (getAllocatedPu() == null)
			return;
		getAllocatedPu().getComputingProvisioner().grantAllocatedResource(this);
	}

	@Override
	protected void revokeAllocatedResource() {
		if (getAllocatedPu() == null)
			return;
		getAllocatedPu().getComputingProvisioner().revokeAllocatedResource(this);
	}

	@Override
	protected ComputingResource computeSynchronizedResource(long delay) {
		return new ComputingResource(Math.round(Math.ceil((double)(this.getLength() - this.getCompletedLength()) * Simulator.SECOND / delay)));
	}

	@Override
	public void startSynchronization(long delay, Operation<?> operation) {
		super.startSynchronization(delay, operation);
	}

	@Override
	public void stopSynchronization() {
		super.stopSynchronization();
	}

	@Override
	public boolean isSynchronized(Operation<?> operation) {
		return super.isSynchronized(operation);
	}
}
