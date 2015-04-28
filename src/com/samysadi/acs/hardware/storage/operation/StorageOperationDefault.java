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

package com.samysadi.acs.hardware.storage.operation;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.LongOperationImpl;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.OperationSynchronizer;
import com.samysadi.acs.virtualization.job.operation.SynchronizableOperation;

/**
 * This implementations uses, when its {@link StorageFile}
 * is located in a remote host, a {@link OperationSynchronizer} in order to synchronize this storage operation with
 * the network operation.<br/>
 * When a remote job is needed, it is created under a new VM or, if the remote host is a {@link ThinClient}, using its main vm.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class StorageOperationDefault extends LongOperationImpl<StorageResource> implements StorageOperation, SynchronizableOperation<StorageResource> {
	private StorageFile storageFile;
	private StorageOperationType type;
	private long filePos;

	/**
	 * The operation synchronizer used to synchronize this operation with
	 * the {@link NetworkOperation} that is used to retrieve data if the storageFile is on a remote device
	 */
	private OperationSynchronizer operationSynchronizer;

	/**
	 * Empty constructor that creates a zero-length operation with a <tt>null</tt> storage file.
	 * 
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link StorageOperationDefault#StorageOperationDefault(StorageFile, com.samysadi.acs.hardware.storage.operation.StorageOperation.StorageOperationType, long, long)} though.
	 */
	public StorageOperationDefault() {
		this(null, StorageOperationType.NONE, 0l, 0l);
	}

	/**
	 * Creates a read, write or append operation on the given <tt>file</tt>.
	 * 
	 * <p>If the <tt>type</tt> of the operation is APPEND,
	 * then <tt>filePos</tt> is always assumed to be equal to the size of the given <tt>file</tt>.
	 * In other words, you can only append data to the end of the file.
	 * Also, a total of <tt>size</tt> is appended to the given <tt>file</tt> after the operation ends.
	 * 
	 * <p>If the <tt>type</tt> of the operation is READ or WRITE, then you must ensure that <tt>filePos</tt> and the
	 * the <tt>file</tt>'s size demarcates a valid file zone (ie: a zone inside the bounds of the given file).<br/>
	 * More formally, the given <tt>filePos</tt> must be greater than or equal to <tt>0</tt> and lesser than or equal to the given <tt>file</tt>'s size.<br/>
	 * Also, if {@code filePos+size>file.getSize()} then an IllegalArgumentException exception is thrown (ie: you cannot read or write
	 * outside the <tt>file</tt>'s bounds).
	 * 
	 * @param file
	 * @param type
	 * @param filePos
	 * @param size
	 * @throws NullPointerException if <tt>file</tt> is <tt>null</tt>
	 * @throws IllegalArgumentException if <tt>filePos</tt> and/or <tt>size</tt> are invalid
	 */
	public StorageOperationDefault(StorageFile file,
			StorageOperationType type, long filePos, long size) {
		super(size);

		this.setType(type);
		if (type == StorageOperationType.APPEND)
			this.setFilePos(file.getSize());
		else
			this.setFilePos(filePos);

		this.storageFile = null;
		this.setStorageFile(file);
	}

	@Override
	public StorageOperationDefault clone() {
		final StorageOperationDefault clone = (StorageOperationDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.operationSynchronizer = null;
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);

		//old network operation may still be available if we just paused this operation, so make sure it is discarded
		discardOperationSynchronizer();
	}

	@Override
	public StorageFile getStorageFile() {
		return storageFile;
	}

	@Override
	public void setStorageFile(StorageFile storageFile) {
		if (this.storageFile == storageFile)
			return;

		if (this.isRunning())
			throw new IllegalStateException("You cannot set a new file when this operation is running");

		if (storageFile == null)
			throw new NullPointerException();

		if (getFilePos() < 0)
			throw new IllegalArgumentException("The operation's filePos is negative.");
		else if (getFilePos() > storageFile.getSize())
			throw new IllegalArgumentException("The operation's filePos is greater than the file's size.");

		this.storageFile = storageFile;

		if (getType() == StorageOperationType.READ || getType() == StorageOperationType.WRITE) {
			final long maxLength = storageFile.getSize() - getFilePos();
			if (getLength() > maxLength)
				throw new IllegalArgumentException("The operation's filePos and size implies to read/write data outside the file's bounds.");
		}

		discardOperationSynchronizer();

		notify(NotificationCodes.SO_SF_CHANGED, null);
	}

	@Override
	public StorageOperationType getType() {
		return type;
	}

	private void setType(StorageOperationType type) {
		this.type = type;
	}

	@Override
	public long getFilePos() {
		return filePos;
	}

	private void setFilePos(long filePos) {
		this.filePos = filePos;
	}

	private VirtualMachine newTemporaryVm(Host host) {
		if (host instanceof ThinClient)
			return ((ThinClient) host).getVirtualMachine();
		final VirtualMachine remoteVm = Factory.getFactory(this).newTemporaryVirtualMachine(null);
		remoteVm.setParent(host);
		remoteVm.setUser(getParent().getParent().getUser());
		return remoteVm;
	}

	private static void removeTemporaryVm(VirtualMachine vm) {
		if (vm.getParent() instanceof ThinClient)
			return;
		vm.doTerminate();
		vm.setUser(null);
		vm.setParent(null);
	}

	private String getCannotActivateReason() {
		if (getStorageFile() == null)
			return "This operation (" + this + ") cannot be (re)started because it has a null StorageFile.";

		if (getType() == StorageOperationType.APPEND) {
			//check we can allocate size
			if (getLength() > getStorageFile().getParent().getFreeCapacity())
				return "This operation (" + this + ") cannot be (re)started because there is not enough space in parent storage.";
		}

		if (getType() == StorageOperationType.APPEND) {
			if (!getStorageFile().canAppend())
				return "This operation (" + this + ") cannot be (re)started because append operations are not allowed on the given file.";
		} else if (getType() == StorageOperationType.WRITE) {
			if (!getStorageFile().canWrite())
				return "This operation (" + this + ") cannot be (re)started because write operations are not allowed on the given file.";
		} else if (getType() == StorageOperationType.READ) {
			if (!getStorageFile().canRead())
				return "This operation (" + this + ") cannot be (re)started because read operations are not allowed on the given file.";
		}

		final Host localHost = getParent().getParent().getParent();
		final Host remoteHost = getStorageFile().getParent().getParentHost();
		if (localHost != remoteHost) {
			final VirtualMachine vm = newTemporaryVm(remoteHost);
			boolean ok = vm.isRunning() || vm.canStart();
			removeTemporaryVm(vm);
			if (!ok)
				return "This operation (" + this + ") cannot be (re)started because data cannot be transferred to/from the remote host where the given file is located.";
		}

		return null;
	}

	@Override
	public boolean canRestart() {
		if (!super.canRestart())
			return false;
		return getCannotActivateReason() == null;
	}

	@Override
	protected String getCannotRestartReason() {
		if (!super.canRestart())
			return super.getCannotRestartReason();
		return getCannotActivateReason();
	}

	@Override
	public boolean canStart() {
		if (!super.canStart())
			return false;
		return getCannotStartReason() == null;
	}

	@Override
	protected String getCannotStartReason() {
		if (!super.canStart())
			return super.getCannotStartReason();
		return getCannotActivateReason();
	}

	@Override
	protected boolean registerListeners() {
		if (!super.registerListeners())
			return false;

		//the Storage fails
		if (!addFailureDependency(getStorageFile().getParent()))
			return false;

		//the host that contains the Storage fails
		if (!addFailureDependency(getStorageFile().getParent().getParentHost()))
			return false;

		//the host that contains the Storage is powered-off
		if (!addPowerDependency(getStorageFile().getParent().getParentHost()))
			return false;

		return true;
	}

	@Override
	protected boolean activate0() {
		if (!super.activate0())
			return false;

		getStorageFile().dealWithOperationActivation(this);
		return true;
	}

	@Override
	protected boolean deactivate0() {
		final long oldCompletedLength = getCompletedLength();

		if (!super.deactivate0())
			return false;

		getStorageFile().dealWithOperationDeactivation(this, oldCompletedLength);
		return true;
	}

	protected void discardOperationSynchronizer() {
		if (this.operationSynchronizer == null)
			return;

		NetworkOperation old = (NetworkOperation) this.operationSynchronizer.getOperation2();
		this.operationSynchronizer.discard();
		this.operationSynchronizer = null;

		if (old == null)
			return;

		old.doTerminate();
		old.getParent().doTerminate();
		old.getDestinationJob().doTerminate();

		final Job pJob;
		final VirtualMachine tempVm;
		if (isSendingData()) {
			pJob = old.getParent();
			tempVm = old.getDestinationJob().getParent();
		} else {
			pJob = old.getDestinationJob();
			tempVm = old.getParent().getParent();
		}

		//schedule free resources.
		//Don't free them right away, may provoke concurrent modification exceptions.
		Simulator.getSimulator().schedule(new MyStaticEvent0(tempVm, pJob));
	}

	private static final class MyStaticEvent0 extends EventImpl {
		private final VirtualMachine tempVm;
		private final Job pJob;

		private MyStaticEvent0(VirtualMachine tempVm, Job pJob) {
			this.tempVm = tempVm;
			this.pJob = pJob;
		}

		@Override
		public void process() {
			pJob.setParent(null);
			removeTemporaryVm(tempVm);
		}
	}

	private void prepareOperationSynchronizer() {
		if (this.operationSynchronizer != null)
			return;
	
		if (this.getStorageFile() == null)
			return;

		final Host localHost = getParent().getParent().getParent();
		final Host remoteHost = getStorageFile().getParent().getParentHost();
		if (remoteHost != localHost) {
			//the file is on a remote host, so create a network operation to handle the transmission

			final boolean sendingData = isSendingData();

			final VirtualMachine localVm = getParent().getParent();

			final VirtualMachine remoteVm = newTemporaryVm(remoteHost);
			if (!remoteVm.isRunning())
				remoteVm.doStart();

			final Job sourceJob = Factory.getFactory(this).newJob(null, null);
			sourceJob.setParent((!sendingData) ? remoteVm : localVm);
			sourceJob.doStart();

			final Job destinationJob = Factory.getFactory(this).newJob(null, null);
			destinationJob.setParent(sendingData ? remoteVm : localVm);
			destinationJob.doStart();

			NetworkOperation networkOperation = Factory.getFactory(this).newNetworkOperation(null, sourceJob, destinationJob,
					StorageOperationDefault.this.getLength() - StorageOperationDefault.this.getCompletedLength());

			this.operationSynchronizer = OperationSynchronizer.synchronizeOperations(this, networkOperation);
		}
	}

	/**
	 * Returns <tt>true</tt> if the operation is writing or appending data.
	 * 
	 * <p>In other words, this method returns <tt>true</tt> if we need to send the data
	 * to the remote host. And returns </tt>false</tt> if we are receiving the data from the remote host. 
	 * 
	 * <p>This method is used when the file is on a remote host.
	 * 
	 * @return <tt>true</tt> if the operation is writing or appending data
	 */
	protected boolean isSendingData() {
		return getType() == StorageOperationType.APPEND || getType() == StorageOperationType.WRITE;
	}

	@Override
	protected void prepareActivation() {
		prepareOperationSynchronizer();
	}

	@Override
	protected StorageResource getProvisionerPromise() {
		if (getStorageFile() == null)
			return null;
		StorageResource pr = getStorageFile().getParent().getStorageProvisioner().getResourcePromise(this);
		if (getParent().getParent().getStorageProvisioner() != null) {
			StorageResource vmPr = getParent().getParent().getStorageProvisioner().getResourcePromise(this);
			if (pr.getLong() > vmPr.getLong())
				pr = pr.clone(vmPr.getLong());
		}
		return validateResourcePromise(pr);
	}

	@Override
	protected void grantAllocatedResource() {
		if (getStorageFile() == null)
			return;
		getStorageFile().getParent().getStorageProvisioner().grantAllocatedResource(this);
	}

	@Override
	protected void revokeAllocatedResource() {
		if (getStorageFile() == null)
			return;
		getStorageFile().getParent().getStorageProvisioner().revokeAllocatedResource(this);
	}

	@Override
	protected StorageResource computeSynchronizedResource(long delay) {
		return new StorageResource(Math.round(Math.ceil((double)(this.getLength() - this.getCompletedLength()) * Simulator.SECOND / delay)));
	}

	@Override
	public void startSynchronization(long delay, Operation<?> operation) {
		super.startSynchronization(delay, operation);
	}

	@Override
	public void stopSynchronization() {
		super.stopSynchronization();
	}

	@Override
	public boolean isSynchronized(Operation<?> operation) {
		return super.isSynchronized(operation);
	}
}
