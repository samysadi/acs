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
package com.samysadi.acs.service.checkpointing.checkpoint;

import java.util.ArrayList;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.checkpointing.CheckpointingHandler;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 * Abstract class for checkpoints.
 *
 * @param <E>
 * @param <P>
 *
 * @since 1.2
 */
public abstract class CheckpointAbstract<E extends RunnableEntity, P extends Entity> extends EntityImpl implements Checkpoint<E, P> {
	private static final Object CANCEL_UPDATE_PROP = null;
	private static final Object CANCEL_RECOVER_PROP = null;
	private static final Object CANCEL_COPY_PROP = null;

	private MemoryZone zone;
	private byte state;
	private long time;

	private CheckpointingHandler<E, ? extends Checkpoint<E,P>> checkpointingHandler;

	public CheckpointAbstract() {
		super();

		this.zone = null;
		this.time = -1l;

		this.checkpointingHandler = null;
	}

	@Override
	@Deprecated
	public CheckpointAbstract<E, P> clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.state = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E getParent() {
		return (E) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof RunnableEntity))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");

		if (this.isCheckpointBusy())
			throw new IllegalStateException("Checkpoint is busy");

		super.setParent(parent);
	}

	@Override
	public CheckpointingHandler<E, ? extends Checkpoint<E, P>> getCheckpointingHandler() {
		return this.checkpointingHandler;
	}

	@Override
	public void setCheckpointingHandler(CheckpointingHandler<E, ? extends Checkpoint<E, P>> ch) {
		if (this.checkpointingHandler == ch)
			return;
		this.checkpointingHandler = ch;
		notify(NotificationCodes.CHECKPOINT_CHECKPOINTINGHANDLER_CHANGED, null);
	}

	@Override
	public MemoryZone getMemoryZone() {
		return this.zone;
	}

	@Override
	public void setMemoryZone(MemoryZone zone) {
		if (this.zone == zone)
			return;

		if (!this.canDelete())
			throw new IllegalStateException("MemoryZone cannot be set for the checkpoint because the checkpoint cannot be deleted.");

		this.delete();

		this.zone = zone;

		if (this.getMemoryZone() != null)
			this.getMemoryZone().setSize(0l);

		notify(NotificationCodes.CHECKPOINT_MEMORYZONE_CHANGED, null);
	}

	@Override
	public long getCheckpointTime() {
		return this.time;
	}

	protected void setCheckpointTime(long time) {
		this.time = time;
	}

	@Override
	public boolean isCheckpointStateSet(byte state) {
		return ((this.getCheckpointState() & state) == state);
	}

	protected byte getCheckpointState() {
		return this.state;
	}

	protected void setCheckpointState(int state) {
		if (this.state == state)
			return;
		this.state = (byte) state;
		notify(NotificationCodes.CHECKPOINT_STATE_CHANGED, null);
	}

	@Override
	public boolean isCheckpointBusy() {
		return this.state != 0;
	}

	protected boolean isMemoryZoneUsable(MemoryZone zone) {
		if (zone == null)
			return false;
		if (!zone.hasParentRec())
			return false;

		Entity p = zone;
		while (((p = p.getParent()) != null)) {
			if ((p instanceof FailureProneEntity) && (((FailureProneEntity)p).getFailureState() != FailureState.OK))
				return false;
			if ((p instanceof PoweredEntity) && (((PoweredEntity)p).getPowerState() != PowerState.ON))
				return false;
		}

		return true;
	}

	private boolean canStartProcess() {
		if (!hasParentRec())
			return false;
		if (!isMemoryZoneUsable(getMemoryZone()))
			return false;
		return true;
	}

	private VirtualMachine newTemporaryVm(Host host, User user) {
		final VirtualMachine remoteVm = Factory.getFactory(this).newTemporaryVirtualMachine(null);
		remoteVm.setParent(host);
		remoteVm.setUser(user);
		return remoteVm;
	}

	/**
	 * Creates a new {@link Job} and sets its parent to a newly
	 * created temporary virtual machine.
	 *
	 * <p>The vm has computing capabilities (we set a PuAllocator).
	 *
	 * <p>After the vm and the job are created, they are both started.
	 *
	 * <p>Use {@link #removeTemporaryJob(Job)} to remove the job and its parent VM.
	 *
	 * @param host the parent host of the temporary vm
	 * @param user the owner of the temporary vm
	 * @return the created job
	 */
	protected Job newTemporaryJob(Host host, User user) {
		VirtualMachine vm = newTemporaryVm(host, user);
		Factory.getFactory(vm).newPuAllocator(null, vm); //enable computing capabilities for this vm
		if (vm.canStart())
			vm.doStart();
		Job job = Factory.getFactory(this).newJob(null, null);
		job.setParent(vm);
		if (job.canStart())
			job.doStart();
		return job;
	}

	private static void removeTemporaryVm(VirtualMachine vm) {
		vm.doTerminate();
		vm.setUser(null);
		vm.unplace();
	}

	/**
	 * Removes the job and its parent.
	 *
	 * <p>The job should have been created using {@link #newTemporaryJob(Host, User)}.
	 *
	 * @param job the job to remove
	 */
	protected static void removeTemporaryJob(Job job) {
		for (Operation<?> o: new ArrayList<Operation<?>>(job.getOperations()))
			o.unplace();
		for (Operation<?> o: new ArrayList<Operation<?>>(job.getRemoteOperations()))
			o.unplace();
		removeTemporaryVm(job.getParent());
	}

	private void restoreMemoryZoneSize(long oldSize) {
		if (getMemoryZone() == null)
			return;
		long d = oldSize - getMemoryZone().getSize();
		if (d > 0) {
			//we need to allocate more space
			if (getMemoryZone().getParent().getFreeCapacity() < d) {
				throw new IllegalStateException("Checkpoint update error, cannot restore old memory zone"); //there is not enough memory, shouldn't happen
			}
		}
		getMemoryZone().setSize(oldSize);
	}

	@Override
	public boolean canUpdate() {
		if (!canStartProcess())
			return false;
		if (isCheckpointBusy())
			return false;
		if (getParent().isTerminated())
			return false;
		return true;
	}

	@Override
	public void update() {
		if (!canUpdate())
			throw new IllegalStateException("Cannot update the checkpoint");

		setCheckpointState(getCheckpointState() | CHECKPOINT_STATE_UPDATING);

		beforeUpdate();
		_updateFirstStep();
	}

	protected void setCancelUpdateRunnable(Runnable r) {
		if (r == null)
			this.unsetProperty(CANCEL_UPDATE_PROP);
		else
			this.setProperty(CANCEL_UPDATE_PROP, r);
	}

	protected Runnable getCancelUpdateRunnable() {
		return (Runnable) this.getProperty(CANCEL_UPDATE_PROP);
	}

	@Override
	public void cancelUpdate() {
		if (!isCheckpointStateSet(CHECKPOINT_STATE_UPDATING))
			throw new IllegalStateException("The checkpoint is not being updated");

		Runnable r = getCancelUpdateRunnable();
		if (r != null)
			r.run();
		setCancelUpdateRunnable(null);
	}

	/**
	 * This method performs the first update step.
	 *
	 * <p>This method simulates the update overhead for the checkpoint creation.
	 * Once the update overhead is simulated, this method calls {@link #_updateSecondStep(Job, long, long, Object, long)}.
	 *
	 * <p>Note that if {@link CheckpointAbstract#isLiveUpdateEnabled()} returns <tt>false</tt>, then
	 * the checkpoint state is immediately created after the overhead is simulated.
	 * However, the updated state is not acknowledged (i.e. applied to the checkpoint) only after the checkpoint
	 * is transmitted (meaning that if the update fails, the checkpoint will hold
	 * the old state).
	 *
	 * <p>The parent {@link RunnableEntity} may be paused while simulating the checkpoint overhead if
	 * {@link CheckpointAbstract#isUpdateOverheadBlocking()} returns <tt>true</tt>.
	 * If so, then it is restarted after the checkpoint overhead is
	 * simulated (before calling {@link CheckpointAbstract#_updateSecondStep(Job, long, long, Object, long)}).
	 *
	 */
	protected void _updateFirstStep() {
		setCancelUpdateRunnable(null);

		final Job job = newTemporaryJob(getUpdateHost(), getCheckpointUser());

		long overhead = getUpdateOverhead();
		if (overhead == 0) {
			Object checkpointData = null;
			long checkpointTime = -1l;
			if (!isLiveUpdateEnabled()) {
				checkpointData = takeCheckpoint();
				checkpointTime = Simulator.getSimulator().getTime();
			}
			_updateSecondStep(CheckpointAbstract.this.getMemoryZone().getSize(), 0l, checkpointData, checkpointTime, job);
		} else {
			final boolean parentWasRunning = getParent().isRunning();
			if (parentWasRunning && isUpdateOverheadBlocking())
				getParent().doPause();

			final NotificationListener n = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Operation<?> o = (Operation<?>) notifier;
					if (!o.isTerminated())
						return;
					this.discard();
					Job job = o.getParent();
					o.unplace();

					setCancelUpdateRunnable(null);

					if (parentWasRunning && CheckpointAbstract.this.getParent().canStart())
						CheckpointAbstract.this.getParent().doStart();

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryJob(job);
						CheckpointAbstract.this.afterUpdateError();
						CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
						return;
					}

					Object checkpointData = null;
					long checkpointTime = -1l;
					if (!isLiveUpdateEnabled()) {
						checkpointData = takeCheckpoint();
						checkpointTime = Simulator.getSimulator().getTime();
					}
					CheckpointAbstract.this._updateSecondStep(CheckpointAbstract.this.getMemoryZone().getSize(), 0l, checkpointData, checkpointTime, job);
				}
			};

			Operation<?> o = job.compute(overhead, n);

			Runnable cancelRunnable = new Runnable() {
				@Override
				public void run() {
					setCancelUpdateRunnable(null);
					n.discard();

					if (parentWasRunning && CheckpointAbstract.this.getParent().canStart())
						CheckpointAbstract.this.getParent().doStart();

					removeTemporaryJob(job);
					CheckpointAbstract.this.afterUpdateError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
					return;
				}
			};

			if (o == null) {
				cancelRunnable.run();
				return;
			} else {
				setCancelUpdateRunnable(cancelRunnable);
			}
		}
	}

	/**
	 * This method performs the second update step.
	 *
	 * <p>This method simulates the checkpoint file transmission to the backup
	 * memory zone file.
	 * Once the checkpoint file has been fully transmitted, the method
	 * {@link CheckpointAbstract#_updateLastStep(long, long, Object, long)} is called.
	 *
	 * @param oldSize the old size of the memory zone (i.e. the checkpoint file) before current update
	 * @param totalTransferredSize total transferred size to the backup memory zone in current update
	 * @param checkpointData checkpoint data if available
	 * @param checkpointTime checkpoint time corresponding to checkpoint data if set
	 * @param job the job to be used on the update host (i.e. the host that is creating the checkpoint) for
	 * transmitting the checkpoint. Cannot be <tt>null</tt>. You should use {@link #removeTemporaryJob(Job)} when
	 * you don't need the job anymore.
	 */
	protected void _updateSecondStep(final long oldSize, long totalTransferredSize, final Object checkpointData, final long checkpointTime, final Job job) {
		boolean finalPhaseUpdate = !isLiveUpdateEnabled() ||
				!isLiveUpdateProgressing();

		long size = finalPhaseUpdate ?
				getFinalUpdatePhaseCheckpointSize() :
				getNextUpdatePhaseCheckpointSize();

		final long _totalTransferredSize = totalTransferredSize + size;

		long sendSize = computeCompressedSize(size);

		if (sendSize == 0) {
			if (finalPhaseUpdate) {
				removeTemporaryJob(job);
				_updateThirdStep(oldSize, _totalTransferredSize, checkpointData, checkpointTime);
			} else
				_updateSecondStep(oldSize, _totalTransferredSize, checkpointData, checkpointTime, job);
		} else {
			final boolean startParent;
			if (finalPhaseUpdate && getParent().isRunning()) {
				getParent().doPause();
				startParent = true;
			} else
				startParent = false;

			//allocate space to receive data
			{
				long d = sendSize - getMemoryZone().getSize();
				if (d > 0) {
					//we need to allocate more space
					if (getMemoryZone().getParent().getFreeCapacity() < d) {
						removeTemporaryJob(job);
						restoreMemoryZoneSize(oldSize);
						CheckpointAbstract.this.afterUpdateError();
						CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
						return;
					}
					getMemoryZone().setSize(sendSize);
				}
			}

			final NotificationListener n = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Operation<?> o = (Operation<?>) notifier;
					if (!o.isTerminated())
						return;
					this.discard();
					Job job = o.getParent();
					o.unplace();

					setCancelUpdateRunnable(null);

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryJob(job);
						restoreMemoryZoneSize(oldSize);
						CheckpointAbstract.this.afterUpdateError();
						CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
						return;
					}

					if (startParent && CheckpointAbstract.this.getParent().canStart())
						CheckpointAbstract.this.getParent().doStart();

					CheckpointAbstract.this._updateSecondStep(oldSize, _totalTransferredSize, checkpointData, checkpointTime, job);
				}
			};

			Operation<?> o;
			if (isStorageOperationsEnabled() && (getMemoryZone() instanceof StorageFile)) {
				if (((StorageFile) getMemoryZone()).canWrite())
					o = job.writeFile(((StorageFile) getMemoryZone()), 0, sendSize, n);
				else
					o = null;
			} else
				o = job.sendData(getMemoryZone().getParent().getParentHost(), sendSize, n);

			Runnable cancelRunnable = new Runnable() {
				@Override
				public void run() {
					setCancelUpdateRunnable(null);
					n.discard();

					removeTemporaryJob(job);
					restoreMemoryZoneSize(oldSize);
					CheckpointAbstract.this.afterUpdateError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
					return;
				}
			};

			if (o == null) {
				cancelRunnable.run();
				return;
			} else {
				setCancelUpdateRunnable(cancelRunnable);
			}
		}
	}

	/**
	 * This method performs the third and last update step.
	 *
	 * <p>This method sets the final size of the memory zone (i.e. checkpoint file).
	 * After that, this method calls the {@link CheckpointAbstract#updateCheckpoint()} to update the
	 * effective content of the checkpoint file and sets the checkpoint time.
	 * Finally, it notifies that the checkpoint has been updated.
	 *
	 * @param oldSize the old size of the
	 * @param totalTransferredSize
	 * @param checkpointData checkpoint data if available
	 * @param checkpointTime checkpoint time corresponding to checkpoint data if set
	 */
	protected void _updateThirdStep(long oldSize, long totalTransferredSize, Object checkpointData, long checkpointTime) {
		long checkpointSize = computeCheckpointSize(oldSize, totalTransferredSize);

		//allocate space for checkpoint
		{
			long d = checkpointSize - getMemoryZone().getSize();
			if (d > 0) {
				//we need to allocate more space
				if (getMemoryZone().getParent().getFreeCapacity() < d) {
					restoreMemoryZoneSize(oldSize);
					CheckpointAbstract.this.afterUpdateError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_ERROR, null);
					return;
				}
			}
			getMemoryZone().setSize(checkpointSize);
		}

		if (checkpointData == null) {
			checkpointData = takeCheckpoint();
			checkpointTime = Simulator.getSimulator().getTime();
		}
		updateCheckpoint(checkpointData);
		this.setCheckpointTime(checkpointTime);

		afterUpdate();
		CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_UPDATING));
		CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, null);
	}

	private boolean isParentUsableForRecovery(P parent) {
		if (parent == null)
			return false;
		if (!parent.hasParentRec())
			return false;

		Entity p = parent;
		while (p != null) {
			if ((p instanceof FailureProneEntity) && (((FailureProneEntity)p).getFailureState() != FailureState.OK))
				return false;
			if ((p instanceof PoweredEntity) && (((PoweredEntity)p).getPowerState() != PowerState.ON))
				return false;
			p = p.getParent();
		}

		return true;
	}

	@Override
	public boolean canRecover(P parent, E toReplace) {
		if (!canStartProcess())
			return false;
		if (isCheckpointStateSet(CHECKPOINT_STATE_UPDATING))
			return false;
		if (!hasCheckpoint())
			return false;
		if (parent != null && !isParentUsableForRecovery(parent))
			return false;
		if (toReplace != null && toReplace.getParent() != parent)
			return false;
		return true;
	}

	@Override
	public void recover(P parent, E toReplace) {
		if (parent == null)
			throw new NullPointerException("Cannot use a null parent for the recovery process");
		if (!canRecover(parent, toReplace))
			throw new IllegalStateException("Cannot use the checkpoint for recovery.");

		setCheckpointState(getCheckpointState() | CHECKPOINT_STATE_RECOVERING);

		beforeRecover(parent, toReplace);
		_recoverFirstStep(parent, toReplace);
	}

	protected void setCancelRecoverRunnable(Runnable r) {
		if (r == null)
			this.unsetProperty(CANCEL_RECOVER_PROP);
		else
			this.setProperty(CANCEL_RECOVER_PROP, r);
	}

	protected Runnable getCancelRecoverRunnable() {
		return (Runnable) this.getProperty(CANCEL_RECOVER_PROP);
	}

	@Override
	public void cancelRecover() {
		if (!isCheckpointStateSet(CHECKPOINT_STATE_RECOVERING))
			throw new IllegalStateException("The checkpoint is not being recovered");

		Runnable r = getCancelRecoverRunnable();
		if (r != null)
			r.run();
		setCancelRecoverRunnable(null);
	}

	/**
	 * This method performs the first recovery step.
	 *
	 * <p>This method checks if the checkpoint file is on the same host as the recovery host.
	 * If not, then the checkpoint file is transmitted to the recovery host.
	 * Finally, the method {@link #_recoverSecondStep(Entity, RunnableEntity, Job)} is called.
	 *
	 * @param parent the parent entity where the recovered {@link RunnableEntity} is to be placed
	 * @param toReplace the {@link RunnableEntity} to replace after recovery (if any)
	 */
	protected void _recoverFirstStep(final P parent, final E toReplace) {
		setCancelRecoverRunnable(null);

		Host srcHost = getMemoryZone().getParent().getParentHost();
		Host recoveryHost = getRecoveryHost(parent);

		final Job job = newTemporaryJob(recoveryHost, getCheckpointUser());

		long sendSize = computeCompressedSize(getMemoryZone().getSize());

		if (sendSize > 0) {
			final NotificationListener n = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					NetworkOperation o = (NetworkOperation) notifier;
					if (!o.isTerminated())
						return;
					this.discard();
					Job job = o.getParent();
					o.unplace();

					setCancelRecoverRunnable(null);

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						removeTemporaryJob(job);
						CheckpointAbstract.this.afterRecoverError();
						CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
						return;
					}

					CheckpointAbstract.this._recoverSecondStep(parent, toReplace, job);
				}
			};

			Operation<?> o;
			if (isStorageOperationsEnabled() && (getMemoryZone() instanceof StorageFile)) {
				o = job.readFile((StorageFile) getMemoryZone(), 0, getMemoryZone().getSize(), n);
			} else if (srcHost != recoveryHost) {
				o = job.receiveData(srcHost, sendSize, n);
			} else {
				_recoverSecondStep(parent, toReplace, job);
				return;
			}

			Runnable cancelRunnable = new Runnable() {
				@Override
				public void run() {
					setCancelRecoverRunnable(null);
					n.discard();

					removeTemporaryJob(job);
					CheckpointAbstract.this.afterRecoverError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
					return;
				}
			};

			if (o == null) {
				cancelRunnable.run();
				return;
			} else {
				setCancelRecoverRunnable(cancelRunnable);
			}
		} else {
			_recoverSecondStep(parent, toReplace, job);
		}
	}

	/**
	 * This method performs the second recovery step.
	 *
	 * <p>This method simulates the recovery overhead on the recovery host.
	 * After that, the method {@link #_recoverThirdStep(Entity, RunnableEntity)} is called.
	 *
	 * @param parent the parent entity where the recovered {@link RunnableEntity} is to be placed
	 * @param toReplace the {@link RunnableEntity} to replace after recovery (if any)
	 * @param job a job on the recovery host which is used to simulate the recovery
	 * overhead. Cannot be <tt>null</tt>. You should use {@link #removeTemporaryJob(Job)} when
	 * you don't need the job anymore.
	 */
	protected void _recoverSecondStep(final P parent, final E toReplace, final Job job) {
		long overhead = getRecoveryOverhead();
		if (overhead == 0) {
			removeTemporaryJob(job);
			_recoverThirdStep(parent, toReplace);
		} else {
			final NotificationListener n = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Operation<?> o = (Operation<?>) notifier;
					if (!o.isTerminated())
						return;
					this.discard();
					Job job = o.getParent();
					o.unplace();

					setCancelRecoverRunnable(null);

					removeTemporaryJob(job);

					if (o.getRunnableState() != RunnableState.COMPLETED) {
						CheckpointAbstract.this.afterRecoverError();
						CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
						CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
						return;
					}

					CheckpointAbstract.this._recoverThirdStep(parent, toReplace);
				}
			};

			Operation<?> o = job.compute(overhead, n);

			Runnable cancelRunnable = new Runnable() {
				@Override
				public void run() {
					setCancelRecoverRunnable(null);
					n.discard();

					removeTemporaryJob(job);
					CheckpointAbstract.this.afterRecoverError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
					return;
				}
			};

			if (o == null) {
				cancelRunnable.run();
				return;
			} else {
				setCancelRecoverRunnable(cancelRunnable);
			}
		}
	}

	/**
	 * This method performs the third recovery step.
	 *
	 * <p>This method unplaces the given <tt>toReplace</tt> (if set).
	 * Once this is done, the {@link #_recoverLastStep(Entity)} method is called.
	 *
	 * @param parent the parent entity where the recovered {@link RunnableEntity} is to be placed
	 * @param toReplace the {@link RunnableEntity} to replace after recovery (if any)
	 */
	protected void _recoverThirdStep(final P parent, E toReplace) {
		//unplace the vmToReplace
		if (toReplace != null) {
			unplaceRunnableEntity(parent, toReplace, new _CAMethodReturnSimple() {
				@Override
				public void run() {
					CheckpointAbstract.this._recoverFourthStep(parent);
				}
			}, new _CAMethodReturnSimple() {
				@Override
				public void run() {
					CheckpointAbstract.this.afterRecoverError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
				}
			});
		} else
			_recoverFourthStep(parent);
	}

	/**
	 * This method performs the fourth recovery step.
	 *
	 * <p>This method effectively creates the new recovered {@link RunnableEntity}
	 * using checkpoint data.
	 * The recovered {@link RunnableEntity} is then placed and
	 * appropriate notifications thrown.
	 *
	 * @param parent the parent entity where the recovered {@link RunnableEntity} is to be placed
	 */
	protected void _recoverFourthStep(final P parent) {
		recoverEntity(parent, new _CAMethodReturn() {
			@Override
			public void run(Object param) {
				CheckpointAbstract.this.afterRecover();
				CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
				CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, param);
			}
		}, new _CAMethodReturnSimple() {
			@Override
			public void run() {
				CheckpointAbstract.this.afterRecoverError();
				CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_RECOVERING));
				CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_RECOVER_ERROR, null);
			}
		});
	}

	@Override
	public boolean canCopy(MemoryZone zone) {
		if (!canCopy())
			return false;
		if (zone == getMemoryZone())
			return false;
		if (!isMemoryZoneUsable(zone))
			return false;
		return true;
	}

	@Override
	public boolean canCopy() {
		if (!canStartProcess())
			return false;
		if (isCheckpointStateSet(CHECKPOINT_STATE_UPDATING))
			return false;
		if (!hasCheckpoint())
			return false;
		return true;
	}

	@Override
	public void copy(MemoryZone zone) {
		if (!canCopy(zone))
			throw new IllegalStateException("Cannot copy the checkpoint to the given zone");

		setCheckpointState(getCheckpointState() | CHECKPOINT_STATE_COPYING);

		beforeCopy(zone);

		_copyFirstStep(zone);
	}

	protected void setCancelCopyRunnable(Runnable r) {
		if (r == null)
			this.unsetProperty(CANCEL_COPY_PROP);
		else
			this.setProperty(CANCEL_COPY_PROP, r);
	}

	protected Runnable getCancelCopyRunnable() {
		return (Runnable) this.getProperty(CANCEL_COPY_PROP);
	}

	@Override
	public void cancelCopy() {
		if (!isCheckpointStateSet(CHECKPOINT_STATE_COPYING))
			throw new IllegalStateException("The checkpoint is not being copied");

		Runnable r = getCancelCopyRunnable();
		if (r != null)
			r.run();
		setCancelCopyRunnable(null);
	}

	/**
	 * This method performs the first copy step.
	 *
	 * <p>This method allocates space on the given <tt>zone</tt> and
	 * sends the checkpoint file to that zone through the network.
	 * Once this is done, the {@link #_copyLastStep(MemoryZone)} method is
	 * called.
	 *
	 * @param zone the memory zone where to copy the checkpoint
	 */
	protected void _copyFirstStep(final MemoryZone zone) {
		setCancelCopyRunnable(null);//just make sure it's null

		long sendSize = computeCompressedSize(getMemoryZone().getSize());
		if (sendSize == 0) {
			_copySecondStep(zone);
			return;
		}

		//allocate space to receive checkpoint
		{
			long d = sendSize - zone.getSize();
			if (d > 0) {
				//we need to allocate more space
				if (zone.getParent().getFreeCapacity() < d) {
					CheckpointAbstract.this.afterCopyError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_COPYING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_COPY_ERROR, null);
					return;
				}
			}
			zone.setSize(sendSize);
		}

		//create job on temp VM to handle the transfer
		final Job srcJob = newTemporaryJob(getMemoryZone().getParent().getParentHost(), getCheckpointUser());

		NotificationListener n = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> o = (Operation<?>) notifier;
				if (!o.isTerminated())
					return;
				this.discard();
				Job job = o.getParent();

				setCancelCopyRunnable(null);

				removeTemporaryJob(job);

				if (o.getRunnableState() != RunnableState.COMPLETED) {
					CheckpointAbstract.this.afterCopyError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_COPYING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_COPY_ERROR, null);
					return;
				}

				CheckpointAbstract.this._copySecondStep(zone);
				return;
			}
		};
		Operation<?> o;
		if (isStorageOperationsEnabled() && (zone instanceof StorageFile)) {
			if (((StorageFile) zone).canWrite()) {
				o = srcJob.writeFile((StorageFile) zone, 0, sendSize, n);
			} else
				o = null;
		} else
			o = srcJob.sendData(zone.getParent().getParentHost(), sendSize, n);

		Runnable cancelRunnable = new Runnable() {
			@Override
			public void run() {
				setCancelCopyRunnable(null);

				removeTemporaryJob(srcJob);

				CheckpointAbstract.this.afterCopyError();
				CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_COPYING));
				CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_COPY_ERROR, null);
				return;
			}
		};

		if (o == null) {
			cancelRunnable.run();
			return;
		} else {
			setCancelCopyRunnable(cancelRunnable);
		}
	}

	/**
	 * This method performs the second copy step.
	 *
	 * <p>This method clones current checkpoint and defines its memory zone
	 * to the given <tt>zone</tt>.
	 * The cloned checkpoint is thus completely independent from current checkpoint.
	 * Finally, a notification is thrown.
	 *
	 * @param parent the parent entity where the recovered {@link RunnableEntity} is to be placed
	 * @param toReplace the {@link RunnableEntity} to replace after recovery (if any)
	 * @param job a job on the recovery host which is used to simulate the recovery overhead
	 */
	protected void _copySecondStep(final MemoryZone zone) {
		//allocate space for checkpoint
		{
			long d = getMemoryZone().getSize() - zone.getSize();
			if (d > 0) {
				//we need to allocate more space
				if (zone.getParent().getFreeCapacity() < d) {
					CheckpointAbstract.this.afterCopyError();
					CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_COPYING));
					CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_COPY_ERROR, null);
					return;
				}
			}
			zone.setSize(getMemoryZone().getSize());
		}

		CheckpointAbstract<E, P> newCheckpoint = copyCheckpoint();

		if (newCheckpoint != null) {
			newCheckpoint.zone = zone;
			newCheckpoint.state = 0;
			newCheckpoint.time = CheckpointAbstract.this.getCheckpointTime();

			CheckpointAbstract.this.afterCopy();
			CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_COPYING));
			CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_COPY_SUCCESS, newCheckpoint);
		} else {
			CheckpointAbstract.this.afterCopyError();
			CheckpointAbstract.this.setCheckpointState(CheckpointAbstract.this.getCheckpointState() & (~CHECKPOINT_STATE_COPYING));
			CheckpointAbstract.this.notify(NotificationCodes.CHECKPOINT_COPY_ERROR, null);
		}
	}

	@Override
	public boolean canDelete() {
		if (isCheckpointBusy())
			return false;
		return true;
	}

	@Override
	public void delete() {
		if (!canDelete())
			throw new IllegalStateException("Cannot delete the checkpoint now.");

		if (this.getMemoryZone() != null)
			this.getMemoryZone().setSize(0);

		this.setCheckpointTime(-1l);
		cleanCheckpoint();
	}

	/**
	 * Creates a copy of the state of the parent {@link RunnableEntity} and
	 * returns that state.
	 *
	 * @return the parent {@link RunnableEntity} state to be used in {@link #updateCheckpoint(Object)}.
	 */
	protected abstract Object takeCheckpoint();

	/**
	 * Effectively updates the checkpoint.
	 *
	 * <p>This method updates the internal state of the checkpoint to reflect the parent {@link RunnableEntity} state.
	 *
	 * @param data the data returned by {@link #takeCheckpoint()}
	 */
	protected abstract void updateCheckpoint(Object data);

	/**
	 * @return <tt>true</tt> if the checkpoint has a saved snapshot of a {@link RunnableEntity}.
	 */
	protected abstract boolean hasCheckpoint();

	/**
	 * This method is called to clear the internal state kept by the checkpoint, and which is created after the updates.
	 *
	 * @param destinationHost
	 */
	protected abstract void cleanCheckpoint();

	protected static interface _CAMethodReturnSimple {
		public void run();
	}

	protected static interface _CAMethodReturn {
		public void run(Object param);
	}

	/**
	 * This method will unplace the given entity <tt>toReplace</tt>.
	 * @param parent
	 *
	 * @param toReplace
	 * @param success this is run after the method ends successfully
	 * @param error this is run if an error happens
	 */
	protected abstract void unplaceRunnableEntity(P parent, E toReplace, _CAMethodReturnSimple success, _CAMethodReturnSimple error);

	/**
	 * This method will recover the entity using internal checkpoint state, and then will set the parent of the newly created entity.
	 *
	 * <p>This method is called after the recovery overhead is simulated and that all data has been read from the checkpoint's memory zone.
	 *
	 * @param parent
	 * @param success this is run after the method ran successfully
	 * @param error this is run if an error happens
	 */
	protected abstract void recoverEntity(P parent, _CAMethodReturn success, _CAMethodReturnSimple error);

	/**
	 * This method creates a new checkpoint, set its parent and copies the state of the current checkpoint to
	 * that checkpoint.
	 *
	 * @return the created checkpoint or <tt>null</tt> if an error happens
	 */
	protected abstract CheckpointAbstract<E, P> copyCheckpoint();

	/**
	 * Returns <tt>true</tt> if a {@link StorageOperation} should be used when reading or writing data to
	 * memory zones that implement {@link StorageFile}.
	 *
	 * @return <tt>true</tt> if the checkpoint uses {@link StorageOperation}s
	 */
	protected abstract boolean isStorageOperationsEnabled();

	/**
	 * This method computes data size after compression, if any,
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
	 * Returns <tt>true</tt> if the parent {@link RunnableEntity} should be blocked (ie: paused)
	 * when simulating the update overhead.
	 *
	 * @return <tt>true</tt> if the parent {@link RunnableEntity} should be blocked (ie: paused)
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
	 * a total of <tt>tranferSize</tt> has been sent during the update process.
	 *
	 * @param oldSize size of the checkpoint before update
	 * @param transferSize
	 * @return the new (uncompressed) checkpoint size
	 */
	protected abstract long computeCheckpointSize(long oldSize, long transferSize);

	/**
	 * Returns overhead in {@link Simulator#MI} of the recovery process.
	 *
	 * @return overhead in {@link Simulator#MI} of the recovery process
	 */
	protected abstract long getRecoveryOverhead();

	/**
	 * This method is called before an update (or instant update) is started.
	 */
	protected abstract void beforeUpdate();

	/**
	 * This method is called after that the update (or instant update) has finished successfully, and
	 * the update needs to be acknowledged.
	 */
	protected abstract void afterUpdate();

	/**
	 * This method is called if an error happens during the update (or instant update).
	 */
	protected abstract void afterUpdateError();

	/**
	 * This method is called before a recovery (or instant recovery).
	 */
	protected abstract void beforeRecover(Entity parent, E toReplace);

	/**
	 * This method is called after a successful recovery (or instant recovery).
	 */
	protected abstract void afterRecover();

	/**
	 * This method is called if an error happens during the recovery (or instant recovery).
	 */
	protected abstract void afterRecoverError();

	/**
	 * This method is called before a copy (or instant copy).
	 */
	protected abstract void beforeCopy(MemoryZone zone);

	/**
	 * This method is called after a successful copy (or instant copy).
	 */
	protected abstract void afterCopy();

	/**
	 * This method is called if an error happens during the copy (or instant copy).
	 */
	protected abstract void afterCopyError();

	/**
	 * @return the {@link User} that owns the checkpoint. Resources used by the checkpoint are credited to this user.
	 */
	protected abstract User getCheckpointUser();

	/**
	 * @return the {@link Host} that will create the checkpoint. Usually it is the parent host of the {@link RunnableEntity}.
	 */
	protected abstract Host getUpdateHost();

	/**
	 * @param parent the parent of the {@link RunnableEntity} after recovery
	 *
	 * @return the {@link Host} where the recovered {@link RunnableEntity} is placed
	 */
	protected abstract Host getRecoveryHost(Entity parent);

}
