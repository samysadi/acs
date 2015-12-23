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

package com.samysadi.acs.utility.factory;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.user.User;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public abstract class VirtualMachineFactory extends Factory {

	private CloudProvider cloudProvider;
	/**
	 * The owner
	 */
	private User user;

	public VirtualMachineFactory(Config config, CloudProvider cloudProvider, User user) {
		super(config);

		this.cloudProvider = cloudProvider;
		this.user = user;
	}

	public CloudProvider getCloudProvider() {
		return cloudProvider;
	}

	public User getUser() {
		return this.user;
	}
	

	/**
	 * Generates a VM.<br/>
	 * If the {@link VirtualMachineFactory#getCloudProvider()} does not return <tt>null</tt>, then the VM is also
	 * placed using the Cloud Provider's VM placement policy.
	 */
	@Override
	public abstract VirtualMachine generate();

}
