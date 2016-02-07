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

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.RemoteOperation;
import com.samysadi.acs.virtualization.job.operation.TimerOperation;

/**
 * A {@link Job} is a {@link RunnableEntity} that runs on top of a {@link VirtualMachine}.
 * 
 * <p>A job may own a variable amount of children {@link Operation}s. It may also have
 * a variable of {@link RemoteOperation}s where it is defined as their <i>destination job</i>.<br/>
 * You can use {@link Job#getOperations()} and {@link Job#getRemoteOperations()} to list them.
 * 
 * <p>Accordingly to the {@link RunnableEntity} contract children {@link Operation} must be paused / started / terminated
 * if their parent job's state is updated.<br/>
 * Following the same logic and regarding {@link RemoteOperation}s (which have defined a given job 
 * as their <i>destination job</i>), implementations of this interface must also apply these next rules:<ul>
 * <li>when the job is paused, then the {@link RemoteOperation}s are paused;<li>
 * <li>when the job is started, then the {@link RemoteOperation}s are started (if they can be);<li>
 * <li>when the job is restarted, then the {@link RemoteOperation}s are canceled;<li>
 * <li>when the job is canceled, then the {@link RemoteOperation}s are canceled;<li>
 * <li>when the job fails, then the {@link RemoteOperation}s fail;<li>
 * <li>when the job is terminated, then the {@link RemoteOperation}s are terminated;<li>
 * </ul>
 * @since 1.0
 */
public interface Job extends Entity, RunnableEntity {

	@Override
	public Job clone();

	@Override
	public VirtualMachine getParent();

	/**
	 * Returns a list containing children operations of this job.
	 * 
	 * @return a list containing children operations
	 */
	public List<Operation<?>> getOperations();

	/**
	 * Returns a list containing operations that have defined this job as their destination job.
	 * 
	 * @return a list containing operations that have defined this job as their destination job
	 * @see RemoteOperation
	 */
	public List<RemoteOperation<?>> getRemoteOperations();

	/**
	 * Returns current job priority.
	 * 
	 * <p>Default value is <tt>0</tt>.
	 * 
	 * @return current job priority
	 */
	public int getPriority();

	/**
	 * Updates the job's priority.
	 * 
	 * <p>The job needs to be in a non running state.
	 * 
	 * <p>A {@link NotificationCodes#JOB_PRIORITY_CHANGED} notification is thrown.
	 * 
	 * @param priority
	 * @throws IllegalStateException if the job is running
	 */
	public void setPriority(int priority);

	/* MISCELLANEOUS METHODS
	 * ***********************************************************************/

	/**
	 * Creates and starts a new {@link ComputingOperation} with the given length in MI.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * @param lengthInMi
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created {@link ComputingOperation}, or <tt>null</tt>
	 */
	public ComputingOperation compute(long lengthInMi, NotificationListener listener);

	/**
	 * Creates and returns a new {@link RamZone} after putting it inside the VirtualRam of the parent's VM.
	 * <tt>null</tt> can be returned if the parent VM has no virtual ram or not enough space.
	 * 
	 * @param size
	 * @return the created {@link RamZone}, or <tt>null</tt>
	 */
	public RamZone allocateRam(long size);

	/**
	 * Creates and returns a new {@link StorageFile} after putting it inside the VirtualStorage of the parent's VM.
	 * <tt>null</tt> can be returned if the parent VM has no virtual storage or not enough space.
	 * 
	 * @param size
	 * @return the created {@link StorageFile}, or <tt>null</tt> 
	 */
	public StorageFile createFile(long size);

	/**
	 * Creates and starts a read operation on the given file.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * @param file
	 * @param filePos 0 or the position where to start to read from.
	 * @param size maximum size to read (if not EOF)
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created operation, or <tt>null</tt>
	 * @see StorageOperation
	 */
	public StorageOperation readFile(StorageFile file, long filePos, long size, NotificationListener listener);

	/**
	 * Creates and starts a write operation on the given file.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * @param file
	 * @param filePos 0 or the position where to start to write from.
	 * @param size maximum size to write (if not EOF)
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created operation, or <tt>null</tt>
	 * @see StorageOperation
	 */
	public StorageOperation writeFile(StorageFile file, long filePos, long size, NotificationListener listener);

	/**
	 * Creates and starts an append operation on the given file.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * @param file
	 * @param size the size to append to the given file.
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created operation, or <tt>null</tt>
	 * @see StorageOperation
	 */
	public StorageOperation appendFile(StorageFile file, long size, NotificationListener listener);

	/**
	 * Creates and starts a network operation to send the given <tt>dataSize</tt> to the given <tt>destinationJob</tt>.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * @param destinationJob
	 * @param dataSize
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created operation, or <tt>null</tt>
	 * @see NetworkOperation
	 */
	public NetworkOperation sendData(Job destinationJob, long dataSize, NotificationListener listener);

	/**
	 * Creates and starts a network operation to send the given <tt>dataSize</tt> to the given <tt>destinationHost</tt>.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * <p>This method will automatically create a job and a virtual machine on the destination host to receive the data.<br/>
	 * Created virtual machine is owned by the same user as the parent virtual machine of the current job.<br/>
	 * When the operation ends, the created virtual machine is discarded.
	 * 
	 * @see Job#sendData(Job, long, NotificationListener)
	 */
	public NetworkOperation sendData(Host destinationHost, long dataSize, NotificationListener listener);

	/**
	 * Creates and starts a network operation that sends data from the given <tt>srcJob</tt> to the current job.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * <p>This method is equivalent to:
	 * {@code srcJob.sendData(this, dataSize, listener);}
	 * 
	 * @param srcJob
	 * @param dataSize
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created operation, or <tt>null</tt>
	 * @see NetworkOperation
	 */
	public NetworkOperation receiveData(Job srcJob, long dataSize, NotificationListener listener);

	/**
	 * Creates and starts a network operation on the <tt>srcHost</tt> which will send the the given <tt>dataSize</tt>
	 * to the current job.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * <p>This method will automatically create a job and a virtual machine on the source host to send the data.<br/>
	 * Created virtual machine is owned by the same user as the parent virtual machine of the current job.<br/>
	 * When the operation ends, the created virtual machine is discarded.
	 * 
	 * @see Job#receiveData(Job, long, NotificationListener)
	 */
	public NetworkOperation receiveData(Host srcHost, long dataSize, NotificationListener listener);

	/**
	 * Creates and starts a {@link TimerOperation} with the given delay.
	 * The created operation is returned, but <tt>null</tt> can also be returned if the operation cannot be created/started.
	 * 
	 * @param delay
	 * @param listener a listener that is added for the {@link NotificationCodes#RUNNABLE_STATE_CHANGED}
	 * notification code, before the operation is started. Can be <tt>null</tt>.
	 * @return the created {@link TimerOperation}, or <tt>null</tt> 
	 */
	public TimerOperation scheduleSignal(long delay, NotificationListener listener);
}
