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

import com.samysadi.acs.hardware.Host;

/**
 * This interface describes a thin client.<br/>
 * It is used by users to have access to the Cloud and use its resources.
 * 
 * <p>A thin client is a host which has access to the network but cannot have any other kind of resources (ram, storage or pu).<br/>
 * Also, if you try to add a VM to a thin client (or any of the previously cited resources) you will
 * get an IllegalArgumentException.
 * 
 * <p>You can still run network operations on top of a thin client through its {@link ThinClientVirtualMachine}.
 * 
 * @since 1.0
 */
public interface ThinClient extends Host {
	@Override
	public ThinClient clone();

	@Override
	public User getParent();

	/**
	 * Returns the main virtual machine of this {@link ThinClient}.
	 * 
	 * <p>The returned virtual machine should be running. and you should not stop it or remove it from its parent.
	 * 
	 * @return main virtual machine of this {@link ThinClient}
	 */
	public ThinClientVirtualMachine getVirtualMachine();
}
