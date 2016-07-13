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

package com.samysadi.acs.virtualization.job.operation;

/**
 * A {@link LongOperationImpl} that implements the {@link SynchronizableOperation} interface.
 *
 * @param <Resource>
 *
 * @since 1.2
 */
public abstract class SynchronizableLongOperationImpl<Resource extends LongResource> extends LongOperationImpl<Resource> implements SynchronizableOperation<Resource>{
	public SynchronizableLongOperationImpl() {
		super();
	}

	public SynchronizableLongOperationImpl(long operationLength) {
		super(operationLength);
	}

	@Override
	public void startSynchronization(
			OperationSynchronizer operationSynchronizer) {
		super.startSynchronization(operationSynchronizer);
	}

	@Override
	public void stopSynchronization() {
		super.stopSynchronization();
	}

	@Override
	public OperationSynchronizer getOperationSynchronizer() {
		return super.getOperationSynchronizer();
	}

	@Override
	public void synchronizeWith(SynchronizableOperation<?> operation) {
		super.synchronizeWith(operation);
	}

	@Override
	public void cancelSynchronization() {
		super.cancelSynchronization();
	}

}
