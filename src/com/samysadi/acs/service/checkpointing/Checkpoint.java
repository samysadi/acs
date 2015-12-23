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

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * A checkpoint holds the running state of a {@link VirtualMachine} as it is at a given
 * moment of the simulation. The checkpoint can then be used at any other moment of the simulation
 * to restore the state of the virtual machine.
 * 
 * <p>A checkpoint is defined by its parent virtual machine and a destination host.<br/>
 * The parent virtual machine is the virtual machine that is used when updating
 * the checkpoint.<br/>
 * The destination host is the host where the checkpoint information is saved.<br/>
 * If you change any of them, make sure the checkpoint is not being used or you will get
 * an exception.
 * 
 * <p>Three main methods are offered by this interface. The {@link Checkpoint#update()}, 
 * {@link Checkpoint#recover(Host, VirtualMachine)}, and {@link Checkpoint#transfer(Host)}.<br/>
 * Have a look at their respective documentation for more information.
 * 
 * @since 1.0
 */
public interface Checkpoint extends Entity {
	public enum CheckpointBusyState {
		/**
		 * The checkpoint is not being used for any operation
		 */
		IDLE,
		/**
		 * The checkpoint is being updated
		 */
		UPDATING,
		/**
		 * The checkpoint is being used for recovery
		 */
		RECOVERING,
		/**
		 * The checkpoint is being transfered to another host
		 */
		TRANSFERRING
	}

	@Override
	public VirtualMachine getParent();

	@Override
	public Checkpoint clone();

	/**
	 * Returns the destination host that contains all the data 
	 * of the checkpoint (disk files among other things).
	 * 
	 * @return the destination host
	 */
	public Host getDestinationHost();

	/**
	 * Updates the destination host of the checkpoint and discards all the checkpoint information.
	 * 
	 * <p>The destination host contains the checkpoint and particularly
	 * any files that are used to store the checkpoint.
	 * 
	 * <p>Changing the destination host will delete all checkpoint files,
	 * and the checkpoint is useless until you call {@link Checkpoint#update()}.<br/>
	 * If you want to keep checkpoint state while changing the destination host, then
	 * use the {@link Checkpoint#transfer(Host)} method.
	 * 
	 * @throws IllegalStateException if the Checkpoint is busy (see {@link Checkpoint#getBusyState()})
	 */
	public void setDestinationHost(Host destinationHost);

	/**
	 * Returns the simulation time when this checkpoint was taken or a negative value if the checkpoint has never been updated.
	 * 
	 * @return the simulation time when this checkpoint was taken or a negative value if the checkpoint has never been updated 
	 */
	public long getCheckpointTime();

	/**
	 * Returns the epoch value related to the checkpoint.
	 * 
	 * <p>The epoch value is returned by {@link VirtualMachine#getNotificationsBufferEpoch()} when
	 * the checkpoint was taken.
	 * 
	 * @return the epoch value related to the checkpoint
	 */
	public int getCheckpointEpoch();

	/**
	 * Returns the checkpoint size occupied on the destination host.
	 * 
	 * @return the checkpoint size
	 */
	public long getCheckpointSize();

	/**
	 * Returns <tt>true</tt> if the checkpoint is busy.
	 * 
	 * <p>The checkpoint is busy if it is performing one of these operations:<ul>
	 * <li>Updating: see {@link Checkpoint#update()};
	 * <li>Recovering: see {@link Checkpoint#recover(Host, VirtualMachine)};
	 * <li>Transferring: see {@link Checkpoint#transfer(Host)}.
	 * </ul>
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_BUSY_STATE_CHANGED} notification is thrown when
	 * the busy flag of the checkpoint changes.
	 * 
	 * @return <tt>true</tt> if the checkpoint is busy
	 */
	public CheckpointBusyState getBusyState();

	/**
	 * This method updates the checkpoint accordingly to the state of its parent {@link VirtualMachine} 
	 * at the current simulation time.
	 * 
	 * <p>The parent virtual machine may be paused at some moment of the update, and is resumed 
	 * after that.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_UPDATE_SUCCESS} is thrown when the checkpoint is updated
	 * and acknowledged successfully.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_UPDATE_ERROR} notification is thrown if the checkpoint 
	 * cannot be updated for any reason.
	 * 
	 * @throws IllegalStateException if {@link Checkpoint#canUpdate()} returns <tt>false</tt>.
	 */
	public void update();

	/**
	 * Returns <tt>true</tt> if the checkpoint can be updated.
	 * 
	 * <p>Common reasons to return <tt>false</tt> is if the checkpoint has no defined parent {@link VirtualMachine}
	 * or has no defined destination host (see {@link Checkpoint#setDestinationHost(Host)}).
	 * 
	 * <p>This method will also return <tt>false</tt> if the checkpoint is busy (see {@link Checkpoint#getBusyState()}).
	 * 
	 * @return <tt>true</tt> if the checkpoint can be updated
	 */
	public boolean canUpdate();

	/**
	 * Recovers the VM state using the checkpoint data.<br/>
	 * A new VM is created and is placed on the given <tt>recoveryHost</tt>.<br/>
	 * The new VM is not started.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_RECOVER_SUCCESS} notification is thrown when the 
	 * recovery process ends successfully.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_RECOVER_ERROR} notification is thrown if an
	 * error happens.
	 * 
	 * @param recoveryHost the host where the recovered virtual machine will be placed
	 * @param vmToReplace the virtual machine to replace during recovery. May be <tt>null</tt>
	 * @throws IllegalArgumentException if {@link Checkpoint#canRecover(Host, VirtualMachine)} returns <tt>false</tt>
	 */
	public void recover(Host recoveryHost, VirtualMachine vmToReplace);

	/**
	 * Returns <tt>true</tt> if the recovery process can be engaged on the <tt>recoveryHost</tt>
	 * 
	 * <p>This method returns <tt>false</tt> for instance if there is no saved state to restore.
	 * 
	 * <p>This method also returns <tt>false</tt> if the checkpoint is busy (see {@link Checkpoint#getBusyState()}).
	 * 
	 * @param recoveryHost
	 * @return <tt>true</tt> if the recovery process can be engaged on the <tt>recoveryHost</tt>
	 */
	public boolean canRecover(Host recoveryHost, VirtualMachine vmToReplace);

	/**
	 * Updates the checkpoint's destination host and keeps the checkpoint information.
	 * 
	 * <p>This method returns immediately, you need to listen to appropriate
	 * notification codes to know when the checkpoint have been transfered.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_TRANSFER_SUCCESS} notification is thrown when
	 * the checkpoint have completely been transfered.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINT_TRANSFER_ERROR} notification is thrown if
	 * there is an error during transfer.
	 * 
	 * @param destinationHost
	 * @throws IllegalArgumentException if {@link Checkpoint#canTransfer(Host)} returns <tt>false</tt>
	 */
	public void transfer(Host destinationHost);

	/**
	 * Returns <tt>true</tt> if the checkpoint can be transfered to the <tt>destinationHost</tt>.
	 * 
	 * <p>This method returns <tt>false</tt>, for instance, if the checkpoint is busy (see {@link Checkpoint#getBusyState()}).
	 * 
	 * @param destinationHost
	 * @return <tt>true</tt> if the checkpoint can be transfered to the <tt>destinationHost</tt>
	 */
	public boolean canTransfer(Host destinationHost);
}
