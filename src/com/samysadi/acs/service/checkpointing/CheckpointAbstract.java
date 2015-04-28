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
package com.samysadi.acs.service.checkpointing;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 * Abstract class for checkpoints.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class CheckpointAbstract extends EntityImpl implements Checkpoint {
	private Host destinationHost;
	private CheckpointBusyState busyState;
	private long time;
	private int epoch;
	private long size;
	private VirtualMachine vmSnapshot;
	private VirtualRam ramSnapshot;
	private VirtualStorage storageSnapshot;

	public CheckpointAbstract() {
		super();

		this.destinationHost = null;
		deleteCheckpoint();
	}

	@Override
	public CheckpointAbstract clone() {
		final CheckpointAbstract clone = (CheckpointAbstract) super.clone();
		if (clone.vmSnapshot != null)
			clone.vmSnapshot = clone.vmSnapshot.clone();
		if (clone.ramSnapshot != null)
			clone.ramSnapshot = clone.ramSnapshot.clone();
		if (clone.storageSnapshot != null)
			clone.storageSnapshot = clone.storageSnapshot.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.busyState = CheckpointBusyState.IDLE;
	}

	@Override
	public VirtualMachine getParent() {
		return (VirtualMachine) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof VirtualMachine))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");

		if (this.getBusyState() != CheckpointBusyState.IDLE)
			throw new IllegalStateException("Checkpoint is busy");

		super.setParent(parent);
	}

	@Override
	public Host getDestinationHost() {
		return this.destinationHost;
	}

	protected void _setDestinationHost(Host destinationHost) {
		if (this.destinationHost == destinationHost)
			return;

		this.destinationHost = destinationHost;

		notify(NotificationCodes.CHECKPOINT_DESTINATION_HOST_CHANGED, null);
	}

	@Override
	public void setDestinationHost(Host destinationHost) {
		if (this.getDestinationHost() == destinationHost)
			return;

		if (this.getBusyState() != CheckpointBusyState.IDLE)
			throw new IllegalStateException("Checkpoint is busy");

		deleteCheckpoint();

		_setDestinationHost(destinationHost);
	}

	@Override
	public long getCheckpointTime() {
		return this.time;
	}

	protected void setCheckpointTime(long time) {
		this.time = time;
	}

	@Override
	public int getCheckpointEpoch() {
		return this.epoch;
	}

	protected void setCheckpointEpoch(int epoch) {
		this.epoch = epoch;
	}

	@Override
	public long getCheckpointSize() {
		return this.size;
	}

	protected void setCheckpointSize(long size) {
		this.size = size;
	}

	@Override
	public CheckpointBusyState getBusyState() {
		return this.busyState;
	}

	protected void setBusyState(CheckpointBusyState busyState) {
		if (this.busyState == busyState)
			return;
		this.busyState = busyState;
		notify(NotificationCodes.CHECKPOINT_BUSY_STATE_CHANGED, null);
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
		vm.setParent(null);
	}

	@Override
	public void update() {
		if (!canUpdate())
			throw new IllegalStateException("Cannot update the checkpoint.");

		setBusyState(CheckpointBusyState.UPDATING);

		beforeUpdate();
		_update0();
	}

	//step 0: simulate update overhead
	private void _update0() {
		VirtualMachine vm = newTemporaryVm(getParent().getParent(), getParent().getUser());
		Factory.getFactory(vm).newPuAllocator(null, vm); //enable computing capabilities for this vm
		if (vm.canStart())
			vm.doStart();
		Job job = Factory.getFactory(this).newJob(null, null);
		job.setParent(vm);
		if (job.canStart())
			job.doStart();

		long overhead = getUpdateOverhead();

		if (overhead == 0)
			_update1(job, 0l);
		else {
			final boolean parentWasRunning = getParent().isRunning();
			if (parentWasRunning && isUpdateOverheadBlocking())
				getParent().doPause();
				
			Operation<?> o = job.compute(overhead, new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Operation<?> o = (Operation<?>) notifier;
					if (!o.isTerminated())
						return;
					this.discard();

					if (parentWasRunning && CheckpointAbstract.this.getParent().canStart())
						CheckpointAbstract.this.getParent().doStart();

					Job job = o.getParent();

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryVm(job.getParent());
						CheckpointAbstract.this.afterUpdateError();
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
						CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
						return;
					}

					CheckpointAbstract.this._update1(job, 0l);
				}
			});
			if (o == null) {
				removeTemporaryVm(job.getParent());
				afterUpdateError();
				notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
				setBusyState(CheckpointBusyState.IDLE);
				return;
			}
		}
	}

	//step 1: compute checkpoint size and send it to destination host 
	private void _update1(Job job, long addedCheckpointSize) {
		boolean finalPhaseUpdate = !isLiveUpdateEnabled() ||
				!isLiveUpdateProgressing();

		long size = finalPhaseUpdate ?
				getFinalUpdatePhaseCheckpointSize() :
				getNextUpdatePhaseCheckpointSize();

		final long _addedCheckpointSize = addedCheckpointSize + size;

		long sendSize = computeCompressedSize(size);

		if (sendSize == 0) {
			if (finalPhaseUpdate) {
				removeTemporaryVm(job.getParent());
				_update2(_addedCheckpointSize);
			} else
				_update1(job, _addedCheckpointSize);
		} else {
			final boolean startParent;
			if (finalPhaseUpdate && getParent().isRunning()) {
				getParent().doPause();
				startParent = true;
			} else
				startParent = false;

			StorageFile sf = null;
			if (isCheckpointStoredOnDisk()) {
				sf = prepareStorageFile(getDestinationHost(), sendSize);
				if (sf == null) {
					removeTemporaryVm(job.getParent());
					afterUpdateError();
					notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
					setBusyState(CheckpointBusyState.IDLE);
					return;
				}
				if (sf.getParent().getParentHost() != getDestinationHost() || sf.getSize() < sendSize)
					throw new IllegalStateException();
			}

			NotificationListener n = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Operation<?> o = (Operation<?>) notifier;
					if (!o.isTerminated())
						return;
					this.discard();

					Job job = o.getParent();

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryVm(job.getParent());
						CheckpointAbstract.this.afterUpdateError();
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
						CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
						return;
					}

					if (startParent && CheckpointAbstract.this.getParent().canStart())
						CheckpointAbstract.this.getParent().doStart();

					CheckpointAbstract.this._update1(job, _addedCheckpointSize);
				}
			};

			Operation<?> o;
			if (sf == null)
				o = job.sendData(getDestinationHost(), sendSize, n);
			else
				o = job.writeFile(sf, sf.getSize() - sendSize, sendSize, n);
			if (o == null) {
				removeTemporaryVm(job.getParent());
				afterUpdateError();
				notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
				setBusyState(CheckpointBusyState.IDLE);
				return;
			}
		}
	}

	//step 2: checkpoint sent, so acknowledge everything
	private void _update2(long addedCheckpointSize) {
		this.vmSnapshot = getParent().clone();
		this.ramSnapshot = getParent().getVirtualRam() == null ? null : getParent().getVirtualRam().clone();
		this.storageSnapshot = getParent().getVirtualStorage() == null ? null : getParent().getVirtualStorage().clone();

		this.setCheckpointEpoch(getParent().getNotificationsBufferEpoch());
		this.setCheckpointTime(Simulator.getSimulator().getTime());
		this.setCheckpointSize(computeCheckpointSize(addedCheckpointSize));

		afterUpdate();

		notify(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, null);
		setBusyState(CheckpointBusyState.IDLE);
	}

	@Override
	public boolean canUpdate() {
		if (!this.hasParentRec())
			return false;

		if (this.getDestinationHost() == null)
			return false;

		if (this.getBusyState() != CheckpointBusyState.IDLE)
			return false;

		return true;
	}

	@Override
	public void recover(Host recoveryHost, final VirtualMachine vmToReplace) {
		if (!canRecover(recoveryHost, vmToReplace))
			throw new IllegalStateException("Cannot use the checkpoint for recovery.");

		setBusyState(CheckpointBusyState.RECOVERING);

		beforeRecover(recoveryHost, vmToReplace);

		_recover1(recoveryHost, vmToReplace);
	}

	//step 1: Send checkpoint information to the recovery host (if it is not already there)
	private void _recover1(final Host recoveryHost, final VirtualMachine vmToReplace) {
		VirtualMachine vm = newTemporaryVm(getDestinationHost(), this.vmSnapshot.getUser());
		Factory.getFactory(vm).newPuAllocator(null, vm); //enable computing capabilities for this vm
		if (vm.canStart())
			vm.doStart();
		Job job = Factory.getFactory(this).newJob(null, null);
		job.setParent(vm);
		if (job.canStart())
			job.doStart();

		long sendSize = computeCompressedSize(getCheckpointSize());

		if (sendSize > 0 && getDestinationHost() != recoveryHost) {
			NetworkOperation o = job.sendData(recoveryHost, sendSize, new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					NetworkOperation o = (NetworkOperation) notifier;
					if (!o.isTerminated())
						return;
					this.discard();

					Host recoveryHost = o.getDestinationJob().getParent().getParent();
					Job job = o.getParent();

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryVm(job.getParent());
						CheckpointAbstract.this.afterRecoverError();
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
						CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
						return;
					}

					CheckpointAbstract.this._recover2(recoveryHost, vmToReplace, job);
				}
			});
			if (o == null) {
				removeTemporaryVm(job.getParent());
				afterRecoverError();
				notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
				setBusyState(CheckpointBusyState.IDLE);
				return;
			}
		} else {
			_recover2(recoveryHost, vmToReplace, job);
		}
	}

	//step 2: Checkpoint information is on the recovery host, now let's simulate recovery overhead
	private void _recover2(final Host recoveryHost, final VirtualMachine vmToReplace, Job job) {
		long overhead = getRecoveryOverhead();
		if (overhead == 0)
			_recover3(recoveryHost, vmToReplace, job);
		else {
			Operation<?> o = job.compute(overhead, new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Operation<?> o = (Operation<?>) notifier;
					if (!o.isTerminated())
						return;
					this.discard();

					Job job = o.getParent();

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryVm(job.getParent());
						CheckpointAbstract.this.afterRecoverError();
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
						CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
						return;
					}

					CheckpointAbstract.this._recover3(recoveryHost, vmToReplace, job);
				}
			});
			if (o == null) {
				removeTemporaryVm(job.getParent());
				afterRecoverError();
				notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
				setBusyState(CheckpointBusyState.IDLE);
				return;
			}
		}
	}

	//step 3: now let's unplace the vmToReplace
	private void _recover3(final Host recoveryHost, VirtualMachine vmToReplace, Job job) {
		removeTemporaryVm(job.getParent());

		//unplace the vmToReplace
		if (vmToReplace != null) {
			if (vmToReplace.getVirtualRam() != null)
				vmToReplace.getVirtualRam().setParent(null);
			if (vmToReplace.getVirtualStorage() != null && vmToReplace.getVirtualStorage().getParentHost() == recoveryHost)
				vmToReplace.getVirtualStorage().setParent(null);
			recoveryHost.getCloudProvider().getPowerManager().lockHost(recoveryHost);
			vmToReplace.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					this.discard();

					recoveryHost.getCloudProvider().getPowerManager().unlockHost(recoveryHost);

					CheckpointAbstract.this._recover4(recoveryHost);
				}
			});
			vmToReplace.unplace();
		} else
			_recover4(recoveryHost);
	}

	//step 4: create new virtual machine
	private void _recover4(Host recoveryHost) {
		VirtualMachine vm = this.vmSnapshot.clone();
		if (this.ramSnapshot != null)
			vm.setVirtualRam(this.ramSnapshot.clone());
		if (this.storageSnapshot != null)
			vm.setVirtualStorage(this.storageSnapshot.clone());

		if (!recoveryHost.getCloudProvider().getVmPlacementPolicy().canPlaceVm(vm, recoveryHost)) {
			CheckpointAbstract.this.afterRecoverError();
			CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
			CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
			return;
		}

		vm.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				VirtualMachine vm = (VirtualMachine) notifier;

				this.discard();

				CheckpointAbstract.this.afterRecover();
				CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, vm);
				CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
			}
		});

		recoveryHost.getCloudProvider().getVmPlacementPolicy().placeVm(vm, recoveryHost);
	}

	@Override
	public boolean canRecover(Host recoveryHost, final VirtualMachine vmToReplace) {
		if (recoveryHost == null)
			return false;

		if (vmToReplace != null && vmToReplace.getParent() != recoveryHost)
			return false;

		if (this.vmSnapshot == null)
			return false;

		if (this.getBusyState() != CheckpointBusyState.IDLE)
			return false;

		return true;
	}

	@Override
	public void transfer(Host destinationHost) {
		if (!canTransfer(destinationHost))
			throw new IllegalStateException("Cannot transfer the checkpoint to the given host.");

		setBusyState(CheckpointBusyState.TRANSFERRING);

		beforeTransfer(destinationHost);

		_transfer0(destinationHost);
	}

	private void _transfer0(Host destinationHost) {
		long sendSize = computeCompressedSize(getCheckpointSize());
		if (sendSize == 0 || this.vmSnapshot == null) {
			_transfer1(destinationHost);
			return;
		}

		StorageFile sf = null;
		if (isCheckpointStoredOnDisk()) {
			sf = prepareStorageFile(destinationHost, sendSize);
			if (sf == null) {
				CheckpointAbstract.this.afterTransferError();
				notify(NotificationCodes.CHECKPOINT_TRANSFER_ERROR, null);
				setBusyState(CheckpointBusyState.IDLE);
				return;
			}
			if (sf.getParent().getParentHost() != destinationHost || sf.getSize() < sendSize)
				throw new IllegalStateException();
		}

		VirtualMachine vm = newTemporaryVm(getDestinationHost(), this.vmSnapshot.getUser());
		Factory.getFactory(vm).newPuAllocator(null, vm); //enable computing capabilities for this vm
		if (vm.canStart())
			vm.doStart();
		Job job = Factory.getFactory(this).newJob(null, null);
		job.setParent(vm);
		if (job.canStart())
			job.doStart();

		NotificationListener n = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> o = (Operation<?>) notifier;
				if (!o.isTerminated())
					return;

				this.discard();

				StorageFile sf;
				Host destinationHost;
				if (o instanceof StorageOperation) {
					sf = ((StorageOperation) o).getStorageFile();
					destinationHost = sf.getParent().getParentHost();
				} else {
					sf = null;
					destinationHost = ((NetworkOperation) o).getDestinationJob().getParent().getParent();
				}

				removeTemporaryVm(o.getParent().getParent());

				if (o.getRunnableState() != RunnableState.COMPLETED) {
					CheckpointAbstract.this.afterTransferError();
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_TRANSFER_ERROR, null);
					CheckpointAbstract.this.setBusyState(CheckpointBusyState.IDLE);
					return;
				}

				CheckpointAbstract.this._transfer1(destinationHost);
				return;
			}
		};
		Operation<?> o;
		if (sf == null)
			o = job.sendData(destinationHost, sendSize, n);
		else
			o = job.writeFile(sf, sf.getSize() - sendSize, sendSize, n);
		if (o == null) {
			removeTemporaryVm(vm);
			CheckpointAbstract.this.afterTransferError();
			notify(NotificationCodes.CHECKPOINT_TRANSFER_ERROR, null);
			setBusyState(CheckpointBusyState.IDLE);
			return;
		}
	}

	private void _transfer1(Host destinationHost) {
		_setDestinationHost(destinationHost);
		afterTransfer();
		notify(NotificationCodes.CHECKPOINT_TRANSFER_SUCCESS, null);
		setBusyState(CheckpointBusyState.IDLE);
	}

	@Override
	public boolean canTransfer(Host destinationHost) {
		if (destinationHost == null)
			return false;

		if (this.getDestinationHost() == null)
			return false;

		if (destinationHost == this.getDestinationHost())
			return false;

		if (this.getBusyState() != CheckpointBusyState.IDLE)
			return false;

		return true;
	}

	/**
	 * Deletes the checkpoint and frees any of the
	 * resources it uses.
	 */
	protected void deleteCheckpoint() {
		if (this.getBusyState() != CheckpointBusyState.IDLE)
			throw new IllegalStateException("Checkpoint is busy");

		this.epoch = 0;
		this.time = -1l;
		this.size = 0l;
		this.vmSnapshot = null;
		this.ramSnapshot = null;
		this.storageSnapshot = null;

		freeCheckpointResources();
	}

	/**
	 * This method is called to free any utilized resource by the checkpoint
	 * on the destination host.
	 * 
	 * @param destinationHost
	 */
	protected abstract void freeCheckpointResources();

	/**
	 * Prepares the destination host to receive the checkpoint, and
	 * returns the {@link StorageFile} where the checkpoint must be saved.
	 * 
	 * <p>The storage file's size needs to be greater or equal than <tt>expectedCheckpointSize</tt>.<br/>
	 * <tt>null</tt> is returned if there is not enough space for the checkpoint.
	 * 
	 * <p>This method is called when the checkpoint needs to be transferred (see {@link Checkpoint#transfer(Host)})
	 * to a new host and a new file is needed to store the checkpoint.
	 * 
	 * <p>This method is also called when the checkpoint is being updated (see {@link Checkpoint#update()}).
	 * 
	 * <p>If {@link Checkpoint#isCheckpointStoredOnDisk()} returns <tt>false</tt>
	 * then this method is never called during updates nor during transfers.
	 * 
	 * @param destinationHost
	 * @param expectedCheckpointSize the minimum size needed to receive the checkpoint
	 * @return the {@link StorageFile} or <tt>null</tt>
	 */
	protected abstract StorageFile prepareStorageFile(Host destinationHost, long expectedCheckpointSize);

	/**
	 * Returns <tt>true</tt> if the checkpoint is stored on disk (in the destination host).
	 * 
	 * <p>If this method returns <tt>true</tt>, then storage operations should be created when transferring the checkpoint to handle
	 * write operations.
	 * 
	 * @return <tt>true</tt> if the checkpoint is stored on disk (in the destination host)
	 */
	protected abstract boolean isCheckpointStoredOnDisk();

	/**
	 * This method computes data size after compression if any
	 * and returns the computed size.
	 * 
	 * @return new size after compression
	 */
	protected abstract long computeCompressedSize(long size);

	/**
	 * Returns the overhead in {@link Simulator#MI} of the update process.
	 * 
	 * @return the overhead in {@link Simulator#MI} of the update process
	 */
	protected abstract long getUpdateOverhead();

	/**
	 * Returns <tt>true</tt> if the parent virtual machine should be blocked (ie: paused)
	 * when simulating the update overhead.
	 * 
	 * @return <tt>true</tt> if the parent virtual machine should be blocked (ie: paused)
	 * when simulating the update overhead
	 */
	protected abstract boolean isUpdateOverheadBlocking();

	/**
	 * Returns <tt>true</tt> if live update is enabled.
	 * 
	 * <p>When live update is enabled, the parent virtual machine is not
	 * immediately paused during updates. Instead its state is
	 * recursively sent to the destination host. After a certain
	 * time, when no progress is made regarding virtual machine state, the
	 * virtual machine is paused and final vm's state is sent.
	 * 
	 * @return <tt>true</tt> if live update is enabled
	 */
	protected abstract boolean isLiveUpdateEnabled();

	/**
	 * Returns <tt>true</tt> when last live update phase should be engaged.
	 * 
	 * <p>During live update, at a certain moment the virtual machine needs to be paused
	 * for final phase update.<br/>
	 * This method is called to determine if the virtual machine needs to be paused or not
	 * yet.
	 * 
	 * @return <tt>true</tt> when last live update phase should be engaged
	 */
	protected abstract boolean isLiveUpdateProgressing();

	/**
	 * Returns the checkpoint size for the next update phase.
	 * 
	 * Computes the checkpoint size that needs to be sent to destination host 
	 * during a given update phase.<br/>
	 * This method is only meaningful if live update is enabled.
	 * 
	 * <p>Note: effectively transmitted size is computed using the returned value which 
	 * is passed to {@link CheckpointAbstract#computeCompressedSize(long)}.
	 * 
	 * @return the checkpoint size for the next update phase
	 */
	protected abstract long getNextUpdatePhaseCheckpointSize();

	/**
	 * Returns the checkpoint size for the final update phase.
	 * 
	 * Computes the checkpoint size that needs to be sent to destination host 
	 * during the last update phase.
	 * 
	 * <p>Note: effectively transmitted size is computed using the returned value which 
	 * is passed to {@link CheckpointAbstract#computeCompressedSize(long)}.
	 * 
	 * @return the checkpoint size for the final update phase
	 */
	protected abstract long getFinalUpdatePhaseCheckpointSize();

	/**
	 * Computes and returns the new checkpoint size, assuming that 
	 * a total <tt>addedSize</tt> has been sent to destinationHost.
	 * 
	 * <p>Because addedSize may overlap, the new checkpoint size may be different
	 * from {@code old_checkpoint_size + addedSize}.
	 * 
	 * @param addedSize
	 * @return the new (uncompressed) checkpoint size
	 */
	protected abstract long computeCheckpointSize(long addedSize);

	/**
	 * Returns overhead in {@link Simulator#MI} of the recovery process.
	 * 
	 * @return overhead in {@link Simulator#MI} of the recovery process
	 */
	protected abstract long getRecoveryOverhead();

	/**
	 * This method is called before an update is started.
	 */
	protected abstract void beforeUpdate();

	/**
	 * This method is called after that the update has finished successfully, and
	 * the update needs to be acknowledged.
	 */
	protected abstract void afterUpdate();

	/**
	 * This method is called if an error happens during the update.
	 */
	protected abstract void afterUpdateError();

	/**
	 * This method is called before a recovery.
	 */
	protected abstract void beforeRecover(Host recoveryHost, VirtualMachine vmToReplace);

	/**
	 * This method is called after a successful recovery.
	 */
	protected abstract void afterRecover();

	/**
	 * This method is called if an error happens during the recovery.
	 */
	protected abstract void afterRecoverError();

	/**
	 * This method is called before a transfer.
	 */
	protected abstract void beforeTransfer(Host destinationHost);

	/**
	 * This method is called after a successful transfer.
	 */
	protected abstract void afterTransfer();

	/**
	 * This method is called if an error happens during the transfer.
	 */
	protected abstract void afterTransferError();

}
