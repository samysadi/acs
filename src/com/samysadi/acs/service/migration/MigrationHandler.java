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

package com.samysadi.acs.service.migration;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * A migration handler defines methods for handling migration of a virtual machine
 * from one host to another.
 * 
 * <p>Depending on the implementations you should check the value returned by the 
 * {@link MigrationHandler#supportsLiveMigration()} method  to
 * see if the migration process is live or cold.<br/>
 * Likewise you should check the value returned by the {@link MigrationHandler#supportsStorageMigration()}
 * to see if the storage is also copied during the migration or not.
 * 
 * <p><b>Note</b> you cannot migrate a virtual machine that is already being migrated, 
 * if you try to do so an IllegalArgumentException is thrown.
 * 
 * @since 1.0
 */
public interface MigrationHandler extends Entity {
	@Override
	public MigrationHandler clone();

	@Override
	public CloudProvider getParent();

	/**
	 * Returns <tt>true</tt> if this entity supports live migration.
	 * 
	 * <p>This method returns <tt>false</tt> if this entity only supports cold migration.
	 * 
	 * @return <tt>true</tt> if this entity supports live migration
	 */
	public boolean supportsLiveMigration();

	/**
	 * Returns <tt>true</tt> if the storage is also copied during the migration.
	 * This method returns <tt>false</tt> if the storage is not copied during the migration process,
	 * so that the migrated VM will continue to use its old storage.
	 * 
	 * <p>You probably want to use a migration handler that does not support storage migration if the storage is a SAN storage
	 * that is accessible from the new host.
	 * 
	 * @return <tt>true</tt> if the storage is also copied during the migration
	 */
	public boolean supportsStorageMigration();

	/**
	 * Migrates the given virtual machine <tt>vm</tt> to the destination host.<br/>
	 * The virtual machine may remain running for some time when it is being transmitted to destination
	 * host.
	 * 
	 * <p>A {@link NotificationCodes#MIGRATION_SUCCESS} notification is thrown if the virtual machine was successfully migrated.<br/>
	 * A {@link NotificationCodes#MIGRATION_ERROR} notification is thrown if an error happens when the virtual machine is being migrated.
	 * 
	 * <p>Additionally, the given <tt>vm</tt> is notified using a {@link NotificationCodes#VM_MIGRATED} notification to let it know
	 * that it was migrated. 
	 * 
	 * @param vm the {@link VirtualMachine} that will be migrated
	 * @param destinationHost the {@link Host} that will receive the new <tt>vm</tt>
	 * @throws IllegalArgumentException if the given <tt>vm</tt> is already being migrated
	 * (see {@link VirtualMachine#FLAG_IS_MIGRATING}).
	 */
	public void migrate(VirtualMachine vm, Host destinationHost);
}
