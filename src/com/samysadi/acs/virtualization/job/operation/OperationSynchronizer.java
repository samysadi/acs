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

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Use this class to synchronize two {@link SynchronizableOperation} operations.
 * 
 * <p>After calling {@link OperationSynchronizer#synchronizeOperations(Operation, Operation, RunnableStateChanged)}
 * on the two operations you want to synchronize, this implementation will ensure that the two operations
 * will finish at the same time.
 * 
 * <p>If one of the operations fails or is canceled then the other will also fail or is canceled.<br/>
 * The two operations should always have the same {@link RunnableState}.
 * But if for some reason they do not, then an IllegalStateException is thrown.
 * This should only happen if one of the 
 * operations makes a bad implementation of {@link SynchronizableOperation}.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class OperationSynchronizer extends NotificationListener {
	public static abstract class RunnableStateChanged {
		public abstract void run(OperationSynchronizer sync);
	}

	private RunnableStateChanged customListener;
	private SynchronizableOperation<?> operation1;
	private SynchronizableOperation<?> operation2;

	private OperationSynchronizer(SynchronizableOperation<?> operation1,
			SynchronizableOperation<?> operation2,
			RunnableStateChanged customListener) {
		super();
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.customListener = customListener;

		if (operation1.isRunning() ||
				operation1.getRunnableState() != operation2.getRunnableState())
			throw new IllegalStateException("Synchronization failed for :" + this.toString());
	}

	public SynchronizableOperation<?> getOperation1() {
		return operation1;
	}

	public SynchronizableOperation<?> getOperation2() {
		return operation2;
	}

	@Override
	protected void notificationPerformed(Notifier notifier,
			int notification_code, Object data) {
		final SynchronizableOperation<?> operation, otherOperation;
		if (notifier == this.operation1) {
			operation = this.operation1;
			otherOperation = this.operation2;
		} else if (notifier == this.operation2) {
			operation = this.operation2;
			otherOperation = this.operation1;
		} else
			return;

		if (otherOperation == null)
			return;

		if (operation.isRunning()) {
			if (otherOperation.isRunning()) {
				long d1 = operation.getRemainingDelay();
				long d2 = otherOperation.getRemainingDelay();

				if (d1 > d2) {
					if (operation.isSynchronized(otherOperation)) {
						operation.doPause();
						operation.stopSynchronization();
						operation.doStart();
						notificationPerformed(notifier, notification_code, data);
						return;
					}
					otherOperation.doPause();
					otherOperation.startSynchronization(d1, operation);
					otherOperation.doStart();
				} else if (d2 > d1) {
					if (otherOperation.isSynchronized(operation)) {
						otherOperation.doPause();
						otherOperation.stopSynchronization();
						otherOperation.doStart();
						notificationPerformed(notifier, notification_code, data);
						return;
					}
					operation.doPause();
					operation.startSynchronization(d2, otherOperation);
					operation.doStart();
				}
			} else {
				if (operation.isSynchronized(otherOperation)) {
					operation.doPause();
					operation.stopSynchronization();
					operation.doStart();
				}
				otherOperation.startSynchronization(operation.getRemainingDelay(), operation);
				if (otherOperation.isTerminated())
					otherOperation.doRestart();
				else
					otherOperation.doStart();
			}
		} else {
			if (operation.isTerminated()) {
				switch (operation.getRunnableState()) {
				case CANCELED:
					otherOperation.doCancel();
					break;
				case FAILED:
					otherOperation.doFail();
					break;
				default:
					otherOperation.doTerminate();
				}
			} else {
				if (otherOperation.isRunning())
					otherOperation.doPause();
			}
			operation.stopSynchronization();
			otherOperation.stopSynchronization();
		}

		if (otherOperation.getRunnableState() != operation.getRunnableState())
			throw new IllegalStateException("Synchronization failed for: " + this.toString() + " (" + otherOperation.getRunnableState() + " != " + operation.getRunnableState() + ")");

		if (this.customListener != null)
			this.customListener.run(this);
	}

	/**
	 * Discards this listener, and stop synchronization of the two {@link SynchronizableOperation}s.<br/>
	 * If there is a supplied custom listener, then it is discarded too.
	 */
	@Override
	public void discard() {
		super.discard();

		this.customListener = null;

		{
			boolean wasRunning = this.operation1.isRunning();
			if (wasRunning)
				this.operation1.doPause();
			this.operation1.stopSynchronization();
			if (wasRunning)
				this.operation1.doStart();
			this.operation1 = null;
		}

		{
			boolean wasRunning = this.operation2.isRunning();
			if (wasRunning)
				this.operation2.doPause();
			this.operation2.stopSynchronization();
			if (wasRunning)
				this.operation2.doStart();
			this.operation2 = null;
		}
	}

	@Override
	public String toString() {
		return "Sync(" + this.operation1.toString() + ", " + this.operation2.toString() + ")";
	}

	/**
	 * Creates and returns an operation synchronizer for the two operations.
	 * 
	 * <p>You can supply a {@link RunnableStateChanged} object to be called when
	 * the {@link RunnableState} of one of two operations changes.
	 * 
	 * @param operation1
	 * @param operation2
	 * @param r 
	 * @return the {@link OperationSynchronizer}
	 * @throws IllegalArgumentException if one of the two given operations does not
	 * implement the {@link SynchronizableOperation}.
	 */
	public static OperationSynchronizer synchronizeOperations(Operation<?> operation1, Operation<?> operation2, RunnableStateChanged r) {
		if (!((operation1 instanceof SynchronizableOperation<?>) && (operation2 instanceof SynchronizableOperation<?>)))
			throw new IllegalArgumentException("Both operation must be implementing SynchronizableOperation interface");

		boolean op1WasRunning = operation1.isRunning();
		if (op1WasRunning)
			operation1.doPause();

		boolean op2WasRunning = operation2.isRunning();
		if (op2WasRunning)
			operation2.doPause();

		//both operations needs to be not running
		final OperationSynchronizer sync = new OperationSynchronizer(
						(SynchronizableOperation<?>)operation1,
						(SynchronizableOperation<?>)operation2,
						r
				);

		if ((op1WasRunning || op2WasRunning) &&
				operation1.isTerminated()) {
			if (r == null)
				return sync;
			//doPause() may terminate operation, we need to notify here since listeners were not added before
			Simulator.getSimulator().schedule(new EventImpl() {
				@Override
				public void process() {
					sync.customListener.run(sync);
				}
			});
			return sync;
		}

		operation1.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, sync);
		operation1.addListener(NotificationCodes.OPERATION_RESOURCE_CHANGED, sync);

		operation2.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, sync);
		operation2.addListener(NotificationCodes.OPERATION_RESOURCE_CHANGED, sync);

		if (op1WasRunning)
			operation1.doStart();

		if (op2WasRunning)
			operation2.doStart();

		return sync;
	}

	/**
	 * Same as {@link OperationSynchronizer#synchronizeOperations(Operation, Operation, RunnableStateChanged)}
	 * with <tt>null</tt> {@link RunnableStateChanged}.
	 */
	public static OperationSynchronizer synchronizeOperations(Operation<?> operation1, Operation<?> operation2) {
		return synchronizeOperations(operation1, operation2, null);
	}

}
