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
 * An operation that can be synchronized with other operations.
 *
 * @param <Resource>
 *
 * @see OperationSynchronizer
 * @since 1.2
 */
public interface SynchronizableOperation<Resource> extends Operation<Resource> {
	public OperationSynchronizer getOperationSynchronizer();

	/**
	 * Call this method so that the current operation will adjust its allocated resource
	 * in order to finish (ie: to be completed) after running for <b>at least</b> the
	 * delay returned by {@link OperationSynchronizer#getSynchronizationDelay()}.
	 *
	 * <p>Calling this method will guarantee that this operation needs to be running for at least that delay
	 * before it becomes completed.<br/>
	 * But this method does not guarantee that the operation will be completed after it has been running for exactly that delay.
	 * If the operation gets a too low resource promise from its provisioner, then it may still need extra running time to be completed.
	 *
	 * <p>If {@link OperationSynchronizer#getSynchronizationDelay()} returns a negative <tt>delay</tt>, then the current operation
	 * is delayed (i.e. will run forever).
	 *
	 * <p>Additionally, after calling this method,
	 * and if {@link OperationSynchronizer#isSynchronizeDelayBeforeCompletion()} returns <tt>true</tt>,
	 * then current operation is delayed before completion.
	 * In such case, for the operation to resume, you need to pause the operation,
	 * call {@link SynchronizableOperation#stopSynchronization()} and
	 * resume it.
	 *
	 * <p><b>Note:</b> You should not need to call this method directly.
	 * If you need to synchronize an operation with an other, then use
	 * {@link SynchronizableOperation#synchronizeWith(SynchronizableOperation)} or
	 * {@link OperationSynchronizer#addOperation(SynchronizableOperation)}.
	 *
	 * @param operationSynchronizer the operationSynchronizer
	 * @throws IllegalStateException if this operation is running
	 */
	public void startSynchronization(OperationSynchronizer operationSynchronizer);

	/**
	 * Call this method to clear any synchronization restrictions that were set using
	 * {@link SynchronizableOperation#startSynchronization(OperationSynchronizer)}.
	 *
	 * <p><b>Note:</b> You should not need to call this method directly.
	 * If you need to stop synchronizing an operation then use
	 * {@link SynchronizableOperation#cancelSynchronization()} or
	 * {@link OperationSynchronizer#removeOperation(SynchronizableOperation)}.
	 *
	 * @throws IllegalStateException if this operation is running
	 */
	public void stopSynchronization();

	/**
	 * Synchronizes current operation with the given operation.
	 *
	 * <p>This method will create an {@link OperationSynchronizer} and add both
	 * the current operation and the given <tt>operation</tt> to it.
	 *
	 * <p>If current operation or the given <tt>operation</tt> is already synchronized
	 * then a new {@link OperationSynchronizer} is created, and all operations synchronized with
	 * either current operation or the given <tt>operation</tt> are added to the
	 * new {@link OperationSynchronizer}.
	 *
	 * @param operation
	 */
	public void synchronizeWith(SynchronizableOperation<?> operation);

	/**
	 * Removes current operation from its {@link OperationSynchronizer} (see {@link SynchronizableOperation#getOperationSynchronizer()}).
	 *
	 * <p>If current operation is not synchronized (has no defined {@link OperationSynchronizer}) then nothing is done.
	 *
	 */
	public void cancelSynchronization();
}
