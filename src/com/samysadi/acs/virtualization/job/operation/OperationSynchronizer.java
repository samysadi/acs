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

import java.util.List;

import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;

/**
 * This interface defines an operation synchronizer, which can be used to synchronize multiple {@link SynchronizableOperation}s.
 *
 * <p>After calling {@link OperationSynchronizer#addOperation(SynchronizableOperation)}
 * on all operations you want to synchronize, the implementation will ensure that the operations
 * will finish at the same time.
 *
 * <p>Additionally, if any of the operations completes, fails or is canceled then the other operations will also
 * complete, fail or be canceled.<br/>
 * All the operations will always have the same {@link RunnableState}.
 *
 * @since 1.2
 */
public interface OperationSynchronizer {
	/**
	 * Adds the given operation to the list of operations that are synchronized.
	 *
	 * <p>If the operation was already added, then nothing is done.
	 *
	 * @param operation
	 * @throws IllegalArgumentException if the given operation is already synchronized using another {@link OperationSynchronizer}
	 */
	public void addOperation(SynchronizableOperation<?> operation);

	/**
	 * Removes the given operation from the list of operations that are synchronized.
	 *
	 * @param operation
	 * @throws IllegalArgumentException if the given <tt>operation</tt> is not synchronized using this {@link OperationSynchronizer}.
	 */
	public void removeOperation(SynchronizableOperation<?> operation);

	/**
	 * Removes all operations, and stop their synchronization.
	 */
	public void removeAllOperations();

	/**
	 * Returns an unmodifiable list containing all synchronized operations.
	 *
	 * @return an unmodifiable list containing all synchronized operations.
	 */
	public List<SynchronizableOperation<?>> getOperations();

	/**
	 * Returns the synchronization delay that is used for synchronizing all operations.
	 *
	 * <p>A negative delay indicates that the operations are delayed.
	 *
	 * @return the synchronization delay that is used for synchronizing all operations.
	 */
	public long getSynchronizationDelay();

	/**
	 * Returns a boolean indicating if the synchronized operations should be delayed
	 * before completion.
	 *
	 * @return a boolean indicating if the synchronized operations should be delayed
	 * before completion.
	 */
	public boolean isSynchronizeDelayBeforeCompletion();
}
