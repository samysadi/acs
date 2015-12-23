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
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * This interface defines methods to register virtual machines for automatic checkpointing and
 * recovery.
 * 
 * <p><b>Note</b> if you try to register for checkpointing a virtual machine that is already registered 
 * for checkpointing, then an IllegalArgumentException is thrown.
 * 
 * @since 1.0
 */
public interface CheckpointingHandler extends Entity {

	@Override
	public CheckpointingHandler clone();

	@Override
	public CloudProvider getParent();

	/**
	 * Selects and returns a host where to place the secondary vm.
	 * 
	 * <p>This method is the same as {@link CheckpointingHandler#register(VirtualMachine, Host)}, and
	 * the destination host is selected using the parent {@link CloudProvider}'s
	 * placement policy.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINTINGHANDLER_REGISTERED} notification is thrown.
	 * 
	 * @param vm
	 * @return <tt>true</tt> if a host where to place the secondary VM was found
	 * @throws IllegalArgumentException if the given <tt>vm</tt> is already registered
	 */
	public boolean register(VirtualMachine vm);

	/**
	 * Registers the given VM, and enables automatic checkpointing and recovery.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINTINGHANDLER_REGISTERED} notification is thrown.
	 * 
	 * @param vm
	 * @param destinationHost the host where to place the checkpoint of the vm
	 * @throws IllegalArgumentException if the given <tt>vm</tt> is already registered
	 */
	public void register(VirtualMachine vm, Host destinationHost);

	/**
	 * Unregisters the given VM. No further automatic checkpointing or
	 * automatic recovery is possible.
	 * 
	 * <p>A {@link NotificationCodes#CHECKPOINTINGHANDLER_UNREGISTERED} notification is thrown.
	 * 
	 * @param vm
	 * @throws IllegalArgumentException if the given <tt>vm</tt> was not registered
	 */
	public void unregister(VirtualMachine vm);

	/**
	 * Returns <tt>true</tt> if the given <tt>vm</tt> was already registered.
	 * 
	 * @param vm
	 * @return <tt>true</tt> if the given <tt>vm</tt> was already registered
	 */
	public boolean isRegistered(VirtualMachine vm);

	/**
	 * Returns the checkpoint that is associated with the given <tt>vm</tt> or
	 * <tt>null</tt> if the given <tt>vm</tt> is not registered for checkpointing.
	 * 
	 * @param vm
	 * @return the checkpoint that is associated with the given <tt>vm</tt> or <tt>null</tt>
	 */
	public Checkpoint getCheckpoint(VirtualMachine vm);
}
