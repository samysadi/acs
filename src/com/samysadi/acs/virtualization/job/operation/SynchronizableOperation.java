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
 * An operation that offers methods to easily synchronize it
 * with other operations.
 * 
 * @see OperationSynchronizer
 * @since 1.0
 */
public interface SynchronizableOperation<Resource> extends Operation<Resource> {
	/**
	 * Call this method so that the current operation will adjust its allocated resource
	 * in order to finish (ie: to be completed) after running for <b>at least</b> the given <tt>delay</tt>.
	 * 
	 * <p>Calling this method will guarantee that this operation needs to be running for at least the given <tt>delay</tt>
	 * before it becomes completed.<br/>
	 * But this method does not guarantee that the operation will be completed after it has been running for exactly the given delay, in
	 * the case where it gets a too low resource promise from its provisioner. It may still need extra running time to be completed.
	 * 
	 * <p>If synchronization was already started using this method, then this method should
	 * behave as if it was called after calling {@link SynchronizableOperation#stopSynchronization()}.
	 * 
	 * @param delay minimum remaining delay for this operation
	 * @param operation contains the operation which the current operation is synchronized with, if any
	 * @throws IllegalArgumentException if the given delay is zero or negative
	 * @throws IllegalStateException if this operation is already running
	 */
	public void startSynchronization(long delay, Operation<?> operation);

	/**
	 * Call this method to clear any synchronization restrictions that were set using
	 * {@link SynchronizableOperation#startSynchronization(long, Operation)}.
	 * 
	 * <p>If the operation is not synchronized, then nothing is done.
	 * 
	 * @throws IllegalStateException if this operation is already running
	 */
	public void stopSynchronization();

	/**
	 * Return <tt>true</tt> if current operation is synchronized with the given operation.
	 * 
	 * <p>In other words, this method returns <tt>true</tt> if this operation is synchronized using {@link SynchronizableOperation#startSynchronization(long, Operation)}, AND
	 * the operation which it is synchronized with is the same as the given <tt>operation</tt>.
	 * <tt>false</tt> is returned if the given operation was not synchronized at all, or if {@link SynchronizableOperation#stopSynchronization()} was used.
	 * 
	 * @param operation
	 * @return <tt>true</tt> if current operation is synchronized with the given operation
	 */
	public boolean isSynchronized(Operation<?> operation);
}
