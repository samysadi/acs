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

package com.samysadi.acs.hardware.network;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.network.operation.provisioner.NetworkProvisioner;

/**
 * A network link connects two network interfaces and contains
 * a {@link NetworkProvisioner}.
 * 
 * <p>All implementation classes should provide a constructor with two {@link NetworkInterface} arguments
 * that defines the link's end points.
 * 
 * @since 1.0
 */
public interface NetworkLink extends Entity {
	/**
	 * The cloned entity will link the same interfaces.
	 */
	@Override
	public NetworkLink clone();

	public NetworkInterface getFirstInterface();

	public NetworkInterface getSecondInterface();

	public NetworkProvisioner getNetworkProvisioner();
}
