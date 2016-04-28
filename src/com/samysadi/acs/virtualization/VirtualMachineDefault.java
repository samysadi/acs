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

package com.samysadi.acs.virtualization;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntityImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.operation.NetworkOperationDelayer;
import com.samysadi.acs.hardware.network.operation.provisioner.NetworkProvisioner;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.pu.operation.provisioner.ComputingProvisioner;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;
import com.samysadi.acs.service.checkpointing.checkpoint.VmCheckpoint;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicy;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;
import com.samysadi.acs.virtualization.job.Job;

/**
 * This implementation will only set parent for its VirtualRam when it is started.
 *
 * @since 1.0
 */
public class VirtualMachineDefault extends RunnableEntityImpl implements VirtualMachine {

	private List<Job> jobs;
	private PuAllocator puAllocator;
	private VirtualRam virtualRam;
	private VirtualStorage virtualStorage;
	private List<ProcessingUnit> usableProcessingUnits;
	private boolean usableProcessingUnitsAllocated;
	private List<NetworkInterface> usableNetworkInterfaces;
	private ComputingProvisioner computingProvisioner;
	private StorageProvisioner storageProvisioner;
	private NetworkProvisioner networkProvisioner;
	private List<VmCheckpoint> checkpoints;
	private VmPlacementPolicy vmPlacementPolicy;
	private User user;

	private WildcardListener wildcardListener;

	private long flag;

	private NetworkOperationDelayer networkOperationDelayer;
	private int epoch;

	public VirtualMachineDefault() {
		super();

		this.flag = 0;
		this.networkOperationDelayer = null;
		this.epoch = 0;
	}

	@Override
	public VirtualMachineDefault clone() {
		final VirtualMachineDefault clone = (VirtualMachineDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.jobs = null;
		this.puAllocator = null;
		this.computingProvisioner = null;
		this.storageProvisioner = null;
		this.networkProvisioner = null;
		this.checkpoints = null;

		this.usableProcessingUnits = null;
		this.usableProcessingUnitsAllocated = false;
		this.usableNetworkInterfaces = null;

		this.virtualRam = null;
		this.virtualStorage = null;

		this.vmPlacementPolicy = null;

		this.wildcardListener = new WildcardListener();

		User user = this.user;
		this.user = null;
		this.setUser(user);
	}

	@Override
	public Host getParent() {
		return (Host) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Host))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	protected void lockParentForUsedResources() {
		if (getVirtualRam() != null)
			getVirtualRam().lockParentRec();
		if (getVirtualStorage() != null)
			getVirtualStorage().lockParentRec();
		if (this.usableProcessingUnits != null)
			for (ProcessingUnit pu: this.usableProcessingUnits)
				pu.lockParentRec();
		if (this.usableNetworkInterfaces != null)
			for (NetworkInterface ni: this.usableNetworkInterfaces)
				ni.lockParentRec();
	}

	protected void unlockParentForUsedResources() {
		if (getVirtualRam() != null)
			getVirtualRam().unlockParentRec();
		if (getVirtualStorage() != null)
			getVirtualStorage().unlockParentRec();
		if (this.usableProcessingUnits != null)
			for (ProcessingUnit pu: this.usableProcessingUnits)
				pu.unlockParentRec();
		if (this.usableNetworkInterfaces != null)
			for (NetworkInterface ni: this.usableNetworkInterfaces)
				ni.unlockParentRec();
	}

	@Override
	public void doCancel() {
		if (this.isTerminated())
			return;

		for (Job job: this.getJobs())
			job.doCancel();

		if (isRunning())
			unlockParentForUsedResources();
		setRunnableState(RunnableState.CANCELED);
		getWildcardListener().removeFailureDependency(getParent());
		getWildcardListener().removePowerDependency(getParent());
//		if (this.getVirtualRam() != null)
//			this.getVirtualRam().setParent(null);
	}

	@Override
	public void doFail() {
		if (this.isTerminated())
			return;

		for (Job job: this.getJobs())
			job.doFail();

		if (isRunning())
			unlockParentForUsedResources();
		setRunnableState(RunnableState.FAILED);
		getWildcardListener().removeFailureDependency(getParent());
		getWildcardListener().removePowerDependency(getParent());
//		if (this.getVirtualRam() != null)
//			this.getVirtualRam().setParent(null);
	}

	@Override
	public void doPause() {
		if (!isRunning())
			throw new IllegalStateException("This VM (" + this + ") is not running");

		for (Job job: this.getJobs())
			if (job.isRunning())
				job.doPause();

		unlockParentForUsedResources();
		setRunnableState(RunnableState.PAUSED);
		getWildcardListener().removeFailureDependency(getParent());
		getWildcardListener().removePowerDependency(getParent());
//		if (this.getVirtualRam() != null)
//			this.getVirtualRam().setParent(null);
	}

	@Override
	public void doRestart() {
		if (!canRestart())
			throw new IllegalStateException(getCannotRestartReason());

		for (Job job: this.getJobs())
			job.doCancel();

		setRunnableState(RunnableState.PAUSED);
		getWildcardListener().removeFailureDependency(getParent());
		getWildcardListener().removePowerDependency(getParent());
//		if (this.getVirtualRam() != null)
//			this.getVirtualRam().setParent(null);

		doStart();
	}

	@Override
	public void doStart() {
		if (!canStart())
			throw new IllegalStateException(getCannotStartReason());

		if (!getWildcardListener().addFailureDependency(getParent())) {
			doFail();
			return;
		}

		if (!getWildcardListener().addPowerDependency(getParent())) {
			doFail();
			return;
		}

//		if (this.getVirtualRam() != null)
//			this.getVirtualRam().setParent(getParent().getRam());

		//Make sure everything is consistent
		checkConsistency();

		lockParentForUsedResources();
		setRunnableState(RunnableState.RUNNING);

		for (Job job: this.getJobs())
			if (job.getRunnableState() == RunnableState.PAUSED)
				job.doStart();
	}

	/**
	 * Will cancel the VM, or will declare it as completed if all its jobs are terminated.
	 */
	@Override
	public void doTerminate() {
		if (this.isTerminated())
			return;
		if (this.isRunning())
			this.doPause();

		for (Job j: this.getJobs())
			if (!j.isTerminated()) {
				this.doCancel();
				return;
			}

		this.setRunnableState(RunnableState.COMPLETED);
	}

	protected void checkConsistency() {
		if (getParent() != null) {
			if (getVirtualRam() != null && getVirtualRam().getParentHost() != getParent())
				throw new IllegalArgumentException("The parent host is different from the host that contains this VirtualMachine's VirtualRam");

			if (this.usableProcessingUnits != null) {
				final List<ProcessingUnit> pus = getUsableProcessingUnits();
				if (pus != null && pus.size() != 0 && pus.iterator().next().getParent() != getParent())
					throw new IllegalArgumentException("The parent host is different from the host that contains one of this VirtualMachine's ProcessingUnit");
			}

			//don't allow the use of remote storages
			if (getVirtualStorage() != null && getVirtualStorage().getParentHost() != getParent())
				throw new IllegalArgumentException("The new parent host is different from the host that contains this VirtualMachine's VirtualStorage");

			if (this.usableNetworkInterfaces != null) {
				final List<NetworkInterface> nis = getUsableNetworkInterfaces();
				if (nis != null && nis.size() != 0 && nis.iterator().next().getParent() != getParent())
					throw new IllegalArgumentException("The parent host is different from the host that contains one of this VirtualMachine's NetworkInterface");
			}
		}
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof Job) {
			if (this.jobs == null)
				this.jobs = newArrayList();
			if (!this.jobs.add((Job) entity))
				return;
		} else if (entity instanceof RunnableEntity) {
			throw new UnsupportedOperationException("Adding this RunnableEntity is not supported by this implementation.");
		} else if (entity instanceof VmCheckpoint) {
			if (this.checkpoints == null)
				this.checkpoints = newArrayList();
			if (!this.checkpoints.add((VmCheckpoint) entity))
				return;
		} else if (entity instanceof PuAllocator) {
			if (this.puAllocator == entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You cannot change the PuAllocator when this VM is running");
			if (this.puAllocator != null)
				this.puAllocator.setParent(null);
			this.puAllocator = (PuAllocator) entity;
		} else if (entity instanceof ComputingProvisioner) {
			if (this.computingProvisioner == entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You are not allowed to add a provisioner when this VM is running");
			if (this.computingProvisioner != null)
				this.computingProvisioner.setParent(null);
			this.computingProvisioner = (ComputingProvisioner) entity;
		} else if (entity instanceof StorageProvisioner) {
			if (this.storageProvisioner == entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You are not allowed to add a provisioner when this VM is running");
			if (this.storageProvisioner != null)
				this.storageProvisioner.setParent(null);
			this.storageProvisioner = (StorageProvisioner) entity;
		} else if (entity instanceof NetworkProvisioner) {
			if (this.networkProvisioner == entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You are not allowed to add a provisioner when this VM is running");
			if (this.networkProvisioner != null)
				this.networkProvisioner.setParent(null);
			this.networkProvisioner = (NetworkProvisioner) entity;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof Job) {
			if (this.jobs == null)
				return;
			if (!this.jobs.remove(entity))
				return;
			if (this.jobs.isEmpty())
				this.jobs = null;
		} else if (entity instanceof VmCheckpoint) {
			if (this.checkpoints == null)
				return;
			if (!this.checkpoints.remove(entity))
				return;
			if (this.checkpoints.isEmpty())
				this.checkpoints = null;
		} else if (entity instanceof PuAllocator) {
			if (this.puAllocator != entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You cannot change the PuAllocator when this VM is running");
			this.puAllocator = null;
		} else if (entity instanceof ComputingProvisioner) {
			if (this.computingProvisioner != entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You are not allowed to remove provisioners when this VM is running");
			this.computingProvisioner = null;
		} else if (entity instanceof StorageProvisioner) {
			if (this.storageProvisioner != entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You are not allowed to remove provisioners when this VM is running");
			this.storageProvisioner = null;
		} else if (entity instanceof NetworkProvisioner) {
			if (this.networkProvisioner != entity)
				return;
			if (this.isRunning())
				throw new IllegalStateException("You are not allowed to remove provisioners when this VM is running");
			this.networkProvisioner = null;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<Entity> l = newArrayList(4);
		if (this.puAllocator != null)
			l.add(this.puAllocator);
		if (this.computingProvisioner != null)
			l.add(this.computingProvisioner);
		if (this.storageProvisioner != null)
			l.add(this.storageProvisioner);
		if (this.networkProvisioner != null)
			l.add(this.networkProvisioner);

		List<List<? extends Entity>> r = newArrayList(4);
		r.add(s);
		r.add(l);
		if (this.jobs != null)
			r.add(this.jobs);
		if (this.checkpoints != null)
			r.add(this.checkpoints);
		return new MultiListView<Entity>(r);
	}

	@Override
	public List<Job> getJobs() {
		if (this.jobs == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.jobs);
	}

	@Override
	public List<VmCheckpoint> getCheckpoints() {
		if (this.checkpoints == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.checkpoints);
	}

	@Override
	public ComputingProvisioner getComputingProvisioner() {
		return this.computingProvisioner;
	}

	@Override
	public StorageProvisioner getStorageProvisioner() {
		return this.storageProvisioner;
	}

	@Override
	public NetworkProvisioner getNetworkProvisioner() {
		return this.networkProvisioner;
	}

	@Override
	public PuAllocator getPuAllocator() {
		return this.puAllocator;
	}

	@Override
	public List<ProcessingUnit> getUsableProcessingUnits() {
		if (this.usableProcessingUnits == null)
			return getParent().getProcessingUnits();
		else
			return this.usableProcessingUnits;
	}

	@Override
	public void setUsableProcessingUnits(List<ProcessingUnit> processingUnits, boolean allocatePu) {
		if (this.usableProcessingUnits == processingUnits)
			return;

		if (this.isRunning())
			throw new IllegalStateException("Not allowed when the VM is running");

		if (this.usableProcessingUnits != null && this.usableProcessingUnitsAllocated)
			for (ProcessingUnit pu: this.usableProcessingUnits)
				pu.setAllocated(false);

		if (processingUnits != null && allocatePu) {
			for (ProcessingUnit pu: processingUnits) {
				if (pu.isAllocated())
					throw new IllegalArgumentException("One of the given ProcessingUnits is already allocated for another VM");
				pu.setAllocated(true);
			}
			this.usableProcessingUnitsAllocated = true;
		} else {
			this.usableProcessingUnitsAllocated = false;
		}

		this.usableProcessingUnits = processingUnits;

		notify(NotificationCodes.VM_USABLE_PROCESSING_UNITS_CHANGED, null);
	}

	@Override
	public final void setUsableProcessingUnits(List<ProcessingUnit> processingUnits) {
		setUsableProcessingUnits(processingUnits, true);
	}

	@Override
	public List<NetworkInterface> getUsableNetworkInterfaces() {
		if (this.usableNetworkInterfaces == null)
			return getParent().getInterfaces();
		else
			return this.usableNetworkInterfaces;
	}

	@Override
	public void setUsableNetworkInterfaces(List<NetworkInterface> networkInterfaces) {
		if (this.usableNetworkInterfaces == networkInterfaces)
			return;

		if (this.isRunning())
			throw new IllegalStateException("Not allowed when the VM is running");

		this.usableNetworkInterfaces = networkInterfaces;
		notify(NotificationCodes.VM_USABLE_NETWORK_INTERFACES_CHANGED, null);
	}

	@Override
	public VirtualRam getVirtualRam() {
		return this.virtualRam;
	}

	@Override
	public void setVirtualRam(VirtualRam v) {
		if (v == this.virtualRam)
			return;

		if (this.isRunning())
			throw new IllegalStateException("Not allowed when the VM is running");

		if (v != null) {
			if (v.isAllocated())
				throw new IllegalArgumentException("The given VirtualRam is already allocated for another VM");
			v.setAllocated(true);
		}

		if (this.virtualRam != null)
			this.virtualRam.setAllocated(false);

//		if (v != null)
//			v.setParent(null); //set parent only when the VM is started

		this.virtualRam = v;
		notify(NotificationCodes.VM_RAM_CHANGED, null);
	}

	@Override
	public VirtualStorage getVirtualStorage() {
		return this.virtualStorage;
	}

	@Override
	public void setVirtualStorage(VirtualStorage v) {
		if (v == this.virtualStorage)
			return;

		if (this.isRunning())
			throw new IllegalStateException("Not allowed when the VM is running");

		if (v != null) {
			if (v.isAllocated())
				throw new IllegalArgumentException("The given VirtualStorage is already allocated for another VM");
			v.setAllocated(true);
		}

		if (this.virtualStorage != null)
			this.virtualStorage.setAllocated(false);

		this.virtualStorage = v;
		notify(NotificationCodes.VM_STORAGE_CHANGED, null);
	}

	@Override
	public VmPlacementPolicy getPlacementPolicy() {
		return this.vmPlacementPolicy;
	}

	@Override
	public void setPlacementPolicy(VmPlacementPolicy policy) {
		this.vmPlacementPolicy = policy;
	}

	@Override
	public void unplace() {
		if (this.getPlacementPolicy() != null)
			this.getPlacementPolicy().unplaceVm(this);
		else
			this.setParent(null);
	}

	@Override
	public User getUser() {
		return this.user;
	}

	@Override
	public void setUser(User user) {
		if (this.user == user)
			return;

		final User old = this.user;

		this.user = user;
		notify(NotificationCodes.VM_USER_CHANGED, null);

		if (old != null)
			old.notify(NotificationCodes.USER_VM_DETACHED, this);
		if (this.user != null)
			this.user.notify(NotificationCodes.USER_VM_ATTACHED, this);
	}

	@Override
	public boolean getFlag(long flag) {
		return (this.flag & flag) == flag;
	}

	@Override
	public void setFlag(long flag) {
		long old = this.flag;
		this.flag |= flag;
		if (old != this.flag)
			notify(NotificationCodes.VM_FLAG_CHANGED, null);
	}

	@Override
	public void unsetFlag(long flag) {
		this.flag &= ~flag;
	}

	@Override
	public NetworkOperationDelayer getNetworkOperationDelayer() {
		return this.networkOperationDelayer;
	}

	@Override
	public void setNetworkOperationDelayer(NetworkOperationDelayer operationDelayer) {
		if (this.networkOperationDelayer == operationDelayer)
			return;

		boolean wasRunning = this.isRunning();
		if (wasRunning)
			this.doPause();

		this.networkOperationDelayer = operationDelayer;
		notify(NotificationCodes.VM_OPERATION_DELAYER_CHANGED, null);

		if (wasRunning)
			this.doStart();
	}

	@Override
	public int getEpoch() {
		return this.epoch;
	}

	@Override
	public void setEpoch(int epoch) {
		if (this.epoch == epoch)
			return;

		this.epoch = epoch;
		notify(NotificationCodes.VM_OPERATION_EPOCH_CHANGED, null);
	}

	protected WildcardListener getWildcardListener() {
		return wildcardListener;
	}

	//one listener for many purpose, in order to not have to keep a field and a listener for each notification_code
	protected class WildcardListener extends NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (notification_code == NotificationCodes.FAILURE_STATE_CHANGED) {
				//the parent device has failed
				handleFailureStateChange(notifier, notification_code, data);
			} else if (notification_code == NotificationCodes.POWER_STATE_CHANGED) {
				//the parent device is powered-off
				handlePowerStateChange(notifier, notification_code, data);
			}
		}

		private void handleFailureStateChange(Notifier notifier,
				int notification_code, Object data) {
			FailureProneEntity e = (FailureProneEntity) notifier;
			if (e.getFailureState() != FailureState.OK) {
				getLogger().log(Level.FINEST, VirtualMachineDefault.this, "Failed because a device (" + e + ") has stopped.");
				VirtualMachineDefault.this.doFail();
			}
		}

		public boolean addFailureDependency(FailureProneEntity e) {
			if (e.getFailureState() != FailureState.OK) {
				getLogger().log(Level.FINEST, VirtualMachineDefault.this, "Failed because a device (" + e + ") has stopped.");
				return false;
			}

			e.addListener(NotificationCodes.FAILURE_STATE_CHANGED, this);
			return true;
		}

		public boolean removeFailureDependency(FailureProneEntity e) {
			if (e == null)
				return false;
			return e.removeListener(NotificationCodes.FAILURE_STATE_CHANGED, this);
		}

		private void handlePowerStateChange(Notifier notifier,
				int notification_code, Object data) {
			PoweredEntity e = (PoweredEntity) notifier;
			if (e.getPowerState() != PowerState.ON) {
				getLogger().log(Level.FINEST, VirtualMachineDefault.this, "Failed because a device (" + e + ") is powered-off.");
				VirtualMachineDefault.this.doFail();
			}
		}

		public boolean addPowerDependency(PoweredEntity e) {
			if (e.getPowerState() != PowerState.ON) {
				getLogger().log(Level.FINEST, VirtualMachineDefault.this, "Failed because a device (" + e + ") is powered-off.");
				return false;
			}

			e.addListener(NotificationCodes.POWER_STATE_CHANGED, this);
			return true;
		}

		public boolean removePowerDependency(PoweredEntity e) {
			if (e == null)
				return false;
			return e.removeListener(NotificationCodes.POWER_STATE_CHANGED, this);
		}
	}
}
