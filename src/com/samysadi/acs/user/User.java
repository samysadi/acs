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

package com.samysadi.acs.user;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * A user is a simulation entity that utilizes Cloud resources.<br/>
 * It owns a set of VMs and storage Files.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface User extends Entity {
	@Override
	public User clone();

	@Override
	public CloudProvider getParent();

	public List<ThinClient> getThinClients();

	/**
	 * Returns a list of VirtualMachine that are owned by this user.
	 * 
	 * <p>The returned virtual machines are not actual children of this User entity, and the list is kept updated
	 * indirectly through listening to {@link NotificationCodes#USER_VM_ATTACHED} and
	 * {@link NotificationCodes#USER_VM_DETACHED}.
	 * 
	 * <p>The returned list should not include {@link ThinClientVirtualMachine}s. Use {@link User#getThinClients()}
	 * to get {@link ThinClient}s first if you want to list those.
	 * 
	 * @return a list of VirtualMachine that are owned by this user
	 */
	public List<VirtualMachine> getVirtualMachines();

	/**
	 * Returns a list of files that are owned by this user.
	 * 
	 * <p>The returned files are not actual children of this User entity, and the list is kept updated
	 * indirectly through listening to {@link NotificationCodes#USER_STORAGEFILE_ATTACHED} and
	 * {@link NotificationCodes#USER_STORAGEFILE_DETACHED}.
	 * 
	 * @return a list of files that are owned by this user
	 */
	public List<StorageFile> getStorageFiles();
}
