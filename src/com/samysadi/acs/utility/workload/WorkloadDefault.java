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
package com.samysadi.acs.utility.workload;

import java.util.ArrayList;
import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.IncrementableProbe;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.tracing.sim.SimCompletedWorkloadsCountProbe;
import com.samysadi.acs.tracing.sim.SimFailedWorkloadsCountProbe;
import com.samysadi.acs.tracing.sim.SimSubmittedWorkloadsCountProbe;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.ShuffledIterator;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.factory.generation.mode.AbstractGenerationMode;
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;
import com.samysadi.acs.utility.random.Uniform;
import com.samysadi.acs.utility.workload.task.Task;
import com.samysadi.acs.virtualization.TemporaryVirtualMachine;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.JobDefault;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 *
 * @since 1.0
 */
public class WorkloadDefault extends JobDefault implements Workload {
	private GenerationMode taskGenerationMode;
	private int taskGenerationCount;

	private Task currentTask;
	private boolean currentTaskIsFailSafe;

	private RamZone ramZone;
	private StorageFile storageFile;
	private Job remoteJob;
	/**
	 * Used to keep track of the jobs that are associated with current workload.
	 * Those jobs are checked for termination and terminated accordingly if the workload is terminated.
	 */
	private ArrayList<Job> registeredJobs;

	private final static NotificationListener mainListener = new MainListener();

	public WorkloadDefault() {
		super();

		((IncrementableProbe<?>) Simulator.getSimulator().
				getProbe(SimSubmittedWorkloadsCountProbe.KEY)).increment();

		this.ramZone = null;
		this.storageFile = null;
		this.registeredJobs = null;

		reloadConfiguration();
	}

	@Override
	public WorkloadDefault clone() {
		WorkloadDefault clone = (WorkloadDefault) super.clone();

		if (clone.registeredJobs != null)
			clone.registeredJobs = new ArrayList<Job>(clone.registeredJobs);

		if (clone.taskGenerationMode != null)
			clone.taskGenerationMode = clone.taskGenerationMode.clone();

		if (this.currentTask != null)
			clone.currentTask = this.currentTask.clone(clone);

		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		//
		registerMainListener();
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);

		//make sure ramZone is consistent
		if (this.ramZone != null) {
			if (oldParent != null && this.ramZone.getParent() != null && this.ramZone.getParent().getParentHost() == ((VirtualMachine)oldParent).getParent())
				this.ramZone.setParent(getParent());
			else if (getParent() != null) {
				this.ramZone = allocateRam(this.ramZone.getSize());
			}
		}
	}

	@Override
	public void setConfig(Config config) {
		if (config == getConfig())
			return;
		super.setConfig(config);

		reloadConfiguration();
	}

	protected GenerationMode getTaskGenerationMode() {
		return this.taskGenerationMode;
	}

	protected void reloadConfiguration() {
		if (getConfig() == null) {
			this.taskGenerationMode = null;
			this.taskGenerationCount = 0;
		} else {
			this.taskGenerationMode = Factory.getFactory(getConfig()).newGenerationMode(null, FactoryUtils.Workload_TASK_CONTEXT);

			this.taskGenerationCount = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.Workload_TASK_CONTEXT), -1);
			if (this.taskGenerationCount == -1 && (getTaskGenerationMode() instanceof AbstractGenerationMode))
				this.taskGenerationCount = ((AbstractGenerationMode)getTaskGenerationMode()).getConfigurations().size();
			else
				this.taskGenerationCount = 0;
		}

		this.currentTask = null;
		this.currentTaskIsFailSafe = false;
	}

	@Override
	protected void setRunnableState(RunnableState state) {
		if (state == getRunnableState())
			return;
		super.setRunnableState(state);
		switch (state) {
		case COMPLETED:
			((IncrementableProbe<?>) Simulator.getSimulator().
					getProbe(SimCompletedWorkloadsCountProbe.KEY)).increment();
			break;
		case FAILED:
			((IncrementableProbe<?>) Simulator.getSimulator().
					getProbe(SimFailedWorkloadsCountProbe.KEY)).increment();
			break;
		default:
			break;
		}
	}

	@Override
	public void doCancel() {
		super.doCancel();

		if (this.currentTask != null)
			this.currentTask.interrupt();
	}

	@Override
	public void doFail() {
		super.doFail();

		if (this.currentTask != null)
			this.currentTask.interrupt();
	}

	@Override
	public void doPause() {
		super.doPause();

		if (this.currentTask != null)
			this.currentTask.interrupt();
	}

	@Override
	public void doRestart() {
		if (!canRestart())
			throw new IllegalStateException(getCannotRestartReason());

		reloadConfiguration();

		super.doRestart();
	}

	@Override
	public void doStart() {
		super.doStart();

		runNextTask();
	}

	private void runNextTask() {
		//Run next task/ continue current task
		if (this.currentTask != null)
			this.currentTask.execute();
		else {
			if (this.taskGenerationCount <= 0) {
				if (this.registeredJobs != null) {
					for (Job job: this.registeredJobs)
						checkJobTermination(job);
				}
				checkJobTermination(this);

				return;
			}

			this.taskGenerationCount--;

			Task t = FactoryUtils.generateWorkloadTask(this, getTaskGenerationMode().next());
			if (t == null) {
				runNextTask();
				return;
			}

			this.currentTaskIsFailSafe = t.getConfig().getBoolean("FailSafe", false);
			this.currentTask = t;

			t.execute();
		}
	}

	private static final class MainListener extends NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (notification_code == NotificationCodes.WORKLOAD_TASK_COMPLETED) {
				WorkloadDefault workload = (WorkloadDefault) notifier;
				Task task = (Task) data;
				if (task != workload.currentTask)
					return;

				workload.currentTask = null;
				if (workload.isRunning())
					workload.runNextTask();
			} else if (notification_code == NotificationCodes.WORKLOAD_TASK_FAILED) {
				WorkloadDefault workload = (WorkloadDefault) notifier;
				Task task = (Task) data;
				if (task != workload.currentTask)
					return;

				workload.currentTask = null;
				if (workload.currentTaskIsFailSafe) {
					if (workload.isRunning())
						workload.runNextTask();
				} else
					workload.doFail();
			}
		}
	}

	private void registerMainListener() {
		this.addListener(NotificationCodes.WORKLOAD_TASK_COMPLETED, mainListener);
		this.addListener(NotificationCodes.WORKLOAD_TASK_FAILED, mainListener);
	}


	/**
	 * Checks if the job is terminated, and if not
	 * registers notifications on still running operations to
	 * re-check at another time of the simulation.
	 */
	private static void checkJobTermination(final Job job) {
		if (job == null)
			return;

		if (job.isTerminated()) {
			checkRemoveParentVm(job);
			return;
		}

		if (job.canStart())
			job.doStart();

		NotificationListener nl = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> o = (Operation<?>) notifier;
				if (!o.isRunning()) {
					if (o.getRunnableState() == RunnableState.FAILED) {
						job.doFail();
						checkRemoveParentVm(job);
					} else
						checkJobTermination(job);
					this.discard();
				}
			}
		};

		for (Operation<?> o: job.getOperations())
			if (o.isRunning() || o.canStart()) {
				o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, nl);
				if (!o.isRunning())
					o.doStart();
				return;
			}

		for (Operation<?> o: job.getRemoteOperations())
			if (o.isRunning() || o.canStart()) {
				o.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, nl);
				if (!o.isRunning())
					o.doStart();
				return;
			}

		job.doTerminate();
		checkRemoveParentVm(job);
	}

	@Override
	public RamZone getRamZone() {
		if (getParent() == null)
			return null;
		if (this.ramZone == null)
			this.ramZone = allocateRam(0);
		return this.ramZone;
	}

	@Override
	public StorageFile getStorageFile() {
		if (this.storageFile == null)
			updateDefaultStorageFile();
		if (this.storageFile == null || !this.storageFile.hasParentRec())
			return null;
		return this.storageFile;
	}

	@Override
	public void setStorageFile(StorageFile storageFile) {
		this.storageFile = storageFile;
	}

	protected void updateDefaultStorageFile() {
		this.storageFile = null;

		if (getParent() == null || getParent().getUser() == null)
			return;

		List<StorageFile> l = getParent().getUser().getStorageFiles();
		if (l.size() > 0) {
			ShuffledIterator<StorageFile> it = new ShuffledIterator<StorageFile>(l);
			while (it.hasNext()) {
				this.storageFile = it.next();
				if (this.storageFile instanceof VirtualStorage)
					this.storageFile = null;
				else
					break;
			}
		}
	}

	@Override
	public Job getRemoteJob() {
		if (this.remoteJob == null)
			updateDefaultRemoteJob();
		if (this.remoteJob == null || !this.remoteJob.isRunning())
			return null;
		return this.remoteJob;
	}

	@Override
	public void setRemoteJob(Job remoteJob) {
		if (remoteJob == this.remoteJob)
			return;
		this.remoteJob = remoteJob;
		if (this.remoteJob != null)
			this.registeredJobs.add(this.remoteJob);
	}


	private static void checkRemoveParentVm(Job job) {
		if (job == null || job.getParent() == null)
			return;
		if (!(job.getParent() instanceof TemporaryVirtualMachine))
			return;

		for (Job j: job.getParent().getJobs())
			if (!j.isTerminated())
				return;

		removeTemporaryVm(job.getParent());
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

	protected void updateDefaultRemoteJob() {
		this.remoteJob = null;

		if (getParent() == null)
			return;

		CloudProvider cp = getParent().getUser() != null ? getParent().getUser().getParent() : (
						getParent().getParent() != null ? getParent().getParent().getCloudProvider() : null
				);

		if (cp == null)
			return;

		List<Host> l = cp.getPowerManager().getPoweredOnHosts();

		if (l.size() == 0)
			return;

		Host h = l.get((new Uniform(0, l.size() - 1)).nextInt());

		VirtualMachine vm = newTemporaryVm(h, getParent().getUser());
		if (vm.canStart())
			vm.doStart();

		final Job rJob = FactoryUtils.generateJob(getConfig().addContext("RemoteJob"));
		rJob.setParent(vm);
		if (rJob.canStart()) {
			rJob.doStart();
			setRemoteJob(rJob);
		} else {
			removeTemporaryVm(vm);
		}
	}
}
