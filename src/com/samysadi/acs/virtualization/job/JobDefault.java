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

package com.samysadi.acs.virtualization.job;

import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntityImpl;
import com.samysadi.acs.core.event.DispensableEventImpl;
import com.samysadi.acs.core.notifications.InstantNotificationListener;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.hardware.storage.operation.StorageOperation.StorageOperationType;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.RemoteOperation;
import com.samysadi.acs.virtualization.job.operation.TimerOperation;

/**
 *
 * @since 1.0
 */
public class JobDefault extends RunnableEntityImpl implements Job {
	private int priority;
	private List<Operation<?>> operations;
	private List<RemoteOperation<?>> remoteOperations;

	private static final InstantNotificationListener destOpListener = new InstantNotificationListener() {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (notification_code == NotificationCodes.JOB_DEST_OPERATION_ADDED) {
				if (data instanceof RemoteOperation<?>) {
					if (((JobDefault)notifier).remoteOperations == null)
						((JobDefault)notifier).remoteOperations = ((JobDefault)notifier).newArrayList();
					((JobDefault)notifier).remoteOperations.add((RemoteOperation<?>)data);
				}
			} else if (notification_code == NotificationCodes.JOB_DEST_OPERATION_REMOVED) {
				if (data instanceof RemoteOperation<?>) {
					if (((JobDefault)notifier).remoteOperations != null) {
						((JobDefault)notifier).remoteOperations.remove((RemoteOperation<?>)data);
						if (((JobDefault)notifier).remoteOperations.isEmpty())
							((JobDefault)notifier).remoteOperations = null;
					}
				}
			}
		}
	};

	public JobDefault() {
		super();

		this.priority = 0;
	}

	@Override
	public JobDefault clone() {
		final JobDefault clone = (JobDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.operations = null;

		this.remoteOperations = null;

		//register listeners, to add/remove remote operations
		addListener(NotificationCodes.JOB_DEST_OPERATION_ADDED, JobDefault.destOpListener);
		addListener(NotificationCodes.JOB_DEST_OPERATION_REMOVED, JobDefault.destOpListener);
	}

	@Override
	public VirtualMachine getParent() {
		return (VirtualMachine) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof VirtualMachine))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof Operation<?>) {
			if (this.operations == null)
				this.operations = newArrayList();
			if (!this.operations.add((Operation<?>) entity))
				return;
		} else if (entity instanceof RunnableEntity) {
				throw new UnsupportedOperationException("Adding this RunnableEntity is not supported by this implementation.");
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof Operation<?>) {
			if (this.operations == null)
				return;
			if (!this.operations.remove(entity))
				return;
			if (this.operations.isEmpty())
				this.operations = null;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<List<? extends Entity>> r = newArrayList(2);
		r.add(s);
		if (this.operations != null)
			r.add(this.operations);
		return new MultiListView<Entity>(r);
	}

	@Override
	public List<Operation<?>> getOperations() {
		if (this.operations == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.operations);
	}

	@Override
	public List<RemoteOperation<?>> getRemoteOperations() {
		if (this.remoteOperations == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.remoteOperations);
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public void setPriority(int priority) {
		if (this.priority == priority)
			return;

		if (this.isRunning())
			throw new IllegalStateException("Not allowed when the VM is running");

		this.priority = priority;
		notify(NotificationCodes.JOB_PRIORITY_CHANGED, null);
	}

	@Override
	public void doCancel() {
		if (this.isTerminated())
			return;

		for (Operation<?> operation: this.getOperations())
			operation.doCancel();

		for (Operation<?> operation: this.getRemoteOperations())
			operation.doCancel();

		setRunnableState(RunnableState.CANCELED);
	}

	@Override
	public void doFail() {
		if (this.isTerminated())
			return;

		for (Operation<?> operation: this.getOperations())
			operation.doFail();

		for (Operation<?> operation: this.getRemoteOperations())
			operation.doFail();

		setRunnableState(RunnableState.FAILED);
	}

	@Override
	public void doPause() {
		if (!isRunning())
			throw new IllegalStateException("This job (" + this + ") is not running.");

		for (Operation<?> operation: this.getOperations())
			if (operation.isRunning())
				operation.doPause();

		for (Operation<?> operation: this.getRemoteOperations())
			if (operation.isRunning())
				operation.doPause();

		setRunnableState(RunnableState.PAUSED);
	}

	@Override
	public void doRestart() {
		if (!canRestart())
			throw new IllegalStateException(getCannotRestartReason());

		for (Operation<?> operation: this.getOperations())
			operation.doCancel();

		for (Operation<?> operation: this.getRemoteOperations())
			operation.doCancel();

		setRunnableState(RunnableState.PAUSED);
		doStart();
	}

	@Override
	public void doStart() {
		if (!canStart())
			throw new IllegalStateException(getCannotStartReason());

		setRunnableState(RunnableState.RUNNING);

		for (Operation<?> operation: this.getOperations())
			if (operation.canStart())
				operation.doStart();

		for (RemoteOperation<?> operation: this.getRemoteOperations())
			if (operation.canStart())
				operation.doStart();
	}

	/**
	 * Will cancel the job, or will declare it as completed if all its operations are terminated.
	 */
	@Override
	public void doTerminate() {
		if (this.isTerminated())
			return;
		if (this.isRunning())
			this.doPause();

		for (Operation<?> o: this.getOperations())
			if (!o.isTerminated()) {
				this.doCancel();
				return;
			}

		for (Operation<?> o: this.getRemoteOperations())
			if (!o.isTerminated()) {
				this.doCancel();
				return;
			}

		this.setRunnableState(RunnableState.COMPLETED);
	}


	/* COMPUTING OPERATIONS
	 * ------------------------------------------------------------------------ */

	@Override
	public ComputingOperation compute(long lengthInMi, NotificationListener listener) {
		ComputingOperation o = Factory.getFactory(this).newComputingOperation(null, this, lengthInMi);
		if (!o.canStart()) {
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);
		o.doStart();
		return o;
	}

	/* RAM OPERATIONS
	 * ------------------------------------------------------------------------ */

	@Override
	public RamZone allocateRam(long size) {
		VirtualRam p = getParent().getVirtualRam();
		if (p == null)
			return null;
		if (p.getFreeCapacity() < size)
			return null;
		RamZone rz = Factory.getFactory(this).newRamZone(null, p, size);
		return rz;
	}

	/* STORAGE OPERATIONS
	 * ------------------------------------------------------------------------ */

	@Override
	public StorageFile createFile(long size) {
		VirtualStorage p = getParent().getVirtualStorage();
		if (p == null)
			return null;
		if (p.getFreeCapacity() < size)
			return null;
		StorageFile sf = Factory.getFactory(this).newStorageFile(null, p, size);
		sf.setUser(getParent().getUser());
		return sf;
	}

	@Override
	public StorageOperation readFile(StorageFile file, long filePos, long size, NotificationListener listener) {
		StorageOperation o = Factory.getFactory(this).newStorageOperation(null, this, file, StorageOperationType.READ, filePos, size);
		if (!o.canStart()) {
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);
		o.doStart();
		return o;
	}

	@Override
	public StorageOperation writeFile(StorageFile file, long filePos, long size, NotificationListener listener) {
		StorageOperation o = Factory.getFactory(this).newStorageOperation(null, this, file, StorageOperationType.WRITE, filePos, size);
		if (!o.canStart()) {
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);
		o.doStart();
		return o;
	}

	@Override
	public StorageOperation appendFile(StorageFile file, long size, NotificationListener listener) {
		StorageOperation o = Factory.getFactory(this).newStorageOperation(null, this, file, StorageOperationType.APPEND, 0, size);
		if (!o.canStart()) {
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);
		o.doStart();
		return o;
	}

	/* NETWORK OPERATIONS
	 * ------------------------------------------------------------------------ */

	@Override
	public NetworkOperation sendData(Job destinationJob, long dataSize, NotificationListener listener) {
		NetworkOperation o = Factory.getFactory(this).newNetworkOperation(null, this, destinationJob, dataSize);
		if (!o.canStart()) {
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);
		o.doStart();
		return o;
	}

	private VirtualMachine newTemporaryVm(Host host, User user) {
		final VirtualMachine remoteVm = Factory.getFactory(this).newTemporaryVirtualMachine(null);
		remoteVm.setParent(host);
		remoteVm.setUser(user);
		return remoteVm;
	}

	private static void removeTemporaryVm(VirtualMachine vm) {
		vm.doTerminate();
		vm.setUser(null);
		vm.unplace();
	}

	private static void scheduleRemoveTemporaryVm(final VirtualMachine vm, Operation<?> o) {
		NotificationListener n = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				final NetworkOperation o = (NetworkOperation) notifier;
				if (o.getParent() == null || o.isTerminated()) {
					//make sure all listeners are invoked before discarding temporary vm
					Simulator.getSimulator().schedule(1l, new DispensableEventImpl() {
						@Override
						public void process() {
							if (vm.hasParentRec()) {
								Operation<?> toWait = null;
								for (Job j: vm.getJobs()) {
									if (j.isTerminated())
										continue;
									for (Operation<?> o: j.getOperations())
										if (!o.isTerminated()) {
											toWait = o;
											break;
										}
									if (toWait != null)
										break;
									for (Operation<?> o: j.getRemoteOperations())
										if (!o.isTerminated()) {
											toWait = o;
											break;
										}
									if (toWait != null)
										break;
								}

								if (toWait != null) {
									scheduleRemoveTemporaryVm(vm, toWait);
									return;
								}
							}

							removeTemporaryVm(vm);
						}
					});
					this.discard();
				}
			}
		};
		o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, n);
		o.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, n);
	}

	@Override
	public NetworkOperation sendData(Host destinationHost, long dataSize,
			final NotificationListener listener) {
		if (getParent() == null)
			return null;
		VirtualMachine destinationVm = newTemporaryVm(destinationHost, getParent().getUser());
		if (destinationVm.canStart())
			destinationVm.doStart();
		Job destinationJob = Factory.getFactory(this).newJob(null, destinationVm);
		if (destinationJob.canStart())
			destinationJob.doStart();

		NetworkOperation o = Factory.getFactory(this).newNetworkOperation(null, this, destinationJob, dataSize);
		if (!o.canStart()) {
			removeTemporaryVm(destinationVm);
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);

		scheduleRemoveTemporaryVm(destinationVm, o);

		o.doStart();

		return o;
	}

	@Override
	public NetworkOperation receiveData(Job srcJob, long dataSize,
			NotificationListener listener) {
		return srcJob.sendData(this, dataSize, listener);
	}

	@Override
	public NetworkOperation receiveData(Host srcHost, long dataSize,
			NotificationListener listener) {
		if (getParent() == null)
			return null;
		VirtualMachine srcVm = newTemporaryVm(srcHost, getParent().getUser());
		if (srcVm.canStart())
			srcVm.doStart();
		Job srcJob = Factory.getFactory(this).newJob(null, srcVm);
		if (srcJob.canStart())
			srcJob.doStart();

		NetworkOperation o = Factory.getFactory(this).newNetworkOperation(null, srcJob, this, dataSize);
		if (!o.canStart()) {
			removeTemporaryVm(srcVm);
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);

		scheduleRemoveTemporaryVm(srcVm, o);

		o.doStart();

		return o;
	}

	/* SIGNAL OPERATIONS
	 * ------------------------------------------------------------------------ */

	@Override
	public TimerOperation scheduleSignal(long delay, NotificationListener listener) {
		TimerOperation o = new TimerOperation(delay);
		o.setParent(this);
		if (!o.canStart()) {
			o.unplace();
			return null;
		}
		if (listener != null)
			o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, listener);
		o.doStart();
		return o;
	}
}
