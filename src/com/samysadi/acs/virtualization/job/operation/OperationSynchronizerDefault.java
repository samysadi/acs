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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.InstantNotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * Default implementation of the {@link OperationSynchronizer} interface.
 *
 * @since 1.2
 */
public class OperationSynchronizerDefault implements OperationSynchronizer {
	private final List<SynchronizableOperation<?>> operations;

	private SynchronizingListener listener;
	private SynchronizingEvent syncEvent;

	private long syncDelay;
	private boolean syncIsDelayedBeforeCompletion;

	public OperationSynchronizerDefault() {
		super();
		this.operations = new ArrayList<SynchronizableOperation<?>>(2);

		this.listener = new SynchronizingListener();
		this.syncEvent = null;

		this.syncDelay = 0l;
		this.syncIsDelayedBeforeCompletion = false;
	}

	protected void scheduleSyncEvent(RunnableState runnableState) {
//		if (notifier != null && !this.operations.contains(notifier))
//			return;

		if (this.syncEvent != null)
			this.syncEvent.cancel();

		this.syncEvent = new SynchronizingEvent(runnableState);

		Simulator.getSimulator().schedule(this.syncEvent);
	}

	private class SynchronizingListener extends InstantNotificationListener {
		private boolean disabled = false;

		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (isDisabled())
				return;
			scheduleSyncEvent(((SynchronizableOperation<?>) notifier).getRunnableState());
		}

		public boolean isDisabled() {
			return disabled;
		}

		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}
	}

	private class SynchronizingEvent extends EventImpl {
		private RunnableState runnableState;

		public SynchronizingEvent(RunnableState runnableState) {
			super();
			this.runnableState = runnableState;
		}

		@Override
		public void process() {
			OperationSynchronizerDefault.this.syncEvent = null;
			doSynchronize(this.runnableState);
		}
	}

	@Override
	public String toString() {
		return "Sync" + this.operations.toString();
	}

	@Override
	public List<SynchronizableOperation<?>> getOperations() {
		return Collections.unmodifiableList(this.operations);
	}

	@Override
	public long getSynchronizationDelay() {
		return this.syncDelay;
	}

	@Override
	public boolean isSynchronizeDelayBeforeCompletion() {
		return this.syncIsDelayedBeforeCompletion;
	}

	@Override
	public void addOperation(SynchronizableOperation<?> operation) {
		if (operation.getOperationSynchronizer() == this)
			return;
		if (operation.getOperationSynchronizer() == null) {
			if (getOperations().contains(operation))
				return;
		} else
			throw new IllegalArgumentException("Given operation already synchronized using another OperationSynchronizer");

		boolean wasRunning = operation.isRunning();
		if (wasRunning)
			operation.doPause();

		operation.startSynchronization(this);

		if (wasRunning && operation.canStart())
			operation.doStart();

		operation.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, this.listener);
		operation.addListener(NotificationCodes.OPERATION_RESOURCE_CHANGED, this.listener);

		this.operations.add(operation);

		doSynchronize(operation.getRunnableState());
	}

	@Override
	public void removeOperation(SynchronizableOperation<?> operation) {
		if (!getOperations().contains(operation))
			throw new IllegalArgumentException("The given operation is not synchronized using this OperationSynchronizer");

		operation.removeListener(NotificationCodes.RUNNABLE_STATE_CHANGED, this.listener);
		operation.removeListener(NotificationCodes.OPERATION_RESOURCE_CHANGED, this.listener);

		boolean wasRunning = operation.isRunning();
		if (wasRunning)
			operation.doPause();

		operation.stopSynchronization();

		this.operations.remove(operation);

		if (wasRunning && operation.canStart())
			operation.doStart();
	}

	@Override
	public void removeAllOperations() {
		List<SynchronizableOperation<?>> l = new ArrayList<SynchronizableOperation<?>>(this.getOperations());
		for (SynchronizableOperation<?> o: l)
			removeOperation(o);
	}

	protected final void doSynchronize(final RunnableState runnableState) {
		doSynchronize(runnableState, false);
	}

	protected void doSynchronize(RunnableState runnableState, boolean checked) {
		this.listener.setDisabled(true);

		if (this.operations.size() == 0)
			return;

		switch (runnableState) {
		case RUNNING:
			//check that all other operations are running, and check sync
			boolean needResync = false;
			boolean isSDBC = false;
			long maxDelay = 0;
			for (SynchronizableOperation<?> o: this.operations) {
				if (!o.isRunning()) {
					if (o.isTerminated()) {
						if (o.canRestart())
							o.doRestart();
					} else {
						if (o.canStart())
							o.doStart();
					}

					//if the operation cannot be started, we need to resync
					if (!o.isRunning()) {
						doSynchronize(runnableState, checked);
						return;
					}
				}

				long d = o.getRemainingDelay();
				if (d == -1l) {
					maxDelay = -1l;
				} else if (d > maxDelay) {
					maxDelay = d;
				}

				boolean b = o.isDelayedBeforeCompletion();
				if (b)
					isSDBC = true;

				if (!needResync)
					if (d != this.syncDelay ||
							b != this.syncIsDelayedBeforeCompletion)
						needResync = true;
			}

			if (!checked && ((needResync && isSDBC) || (!needResync && this.syncIsDelayedBeforeCompletion))) {
				checked = true;

				//1- pause all ops (note that at this point all ops are running)
				for (SynchronizableOperation<?> o: this.operations) {
					o.doPause();
					o.stopSynchronization();
				}

				//2- get new isSDBC
				boolean b = false;
				for (SynchronizableOperation<?> o: this.operations) {
					if (o.isDelayedBeforeCompletion())
						b = true;
				}

				//3- set sync info
				this.syncDelay = 0;
				this.syncIsDelayedBeforeCompletion = b;

				//4- resync
				for (SynchronizableOperation<?> o: this.operations)
					o.startSynchronization(this);

				doSynchronize(runnableState, checked);
				return;
			}

			if (needResync) {
				//need to resync
				//1- pause all ops (note that at this point all ops are running)
				for (SynchronizableOperation<?> o: this.operations) {
					o.doPause();
					o.stopSynchronization();
				}

				//2- set new sync restrictions
				this.syncDelay = maxDelay;
				this.syncIsDelayedBeforeCompletion = isSDBC;

				for (SynchronizableOperation<?> o: this.operations)
					o.startSynchronization(this);

				doSynchronize(RunnableState.RUNNING, checked);
				return;
			}
			break;
		case PAUSED:
			for (SynchronizableOperation<?> o: this.operations)
				if (o.isRunning())
					o.doPause();
			break;
		case FAILED:
			for (SynchronizableOperation<?> o: this.operations)
				o.doFail();
			break;
		case CANCELED:
			for (SynchronizableOperation<?> o: this.operations)
				o.doCancel();
			break;
		default:
			for (SynchronizableOperation<?> o: this.operations)
				o.doTerminate();
		}

		//XXX debug only
		for (SynchronizableOperation<?> o: this.operations)
			if (o.getRunnableState() != runnableState)
				throw new IllegalStateException("Synchronization failed for: " + this.toString() + " (" + o.getRunnableState() + " != " + runnableState + ")");

		//
		this.listener.setDisabled(false);
	}
}
