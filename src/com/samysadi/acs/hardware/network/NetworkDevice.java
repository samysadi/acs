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

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.PoweredEntity;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol;
import com.samysadi.acs.service.CloudProvider;


/**
 * 
 * @since 1.0
 */
public interface NetworkDevice extends Entity, FailureProneEntity, PoweredEntity {

	@Override
	public NetworkDevice clone();

	/*public Location getLocation();
	public void setLocation(Location location);*/

	/**
	 * Returns the list of interfaces contained within this NetworkDevice.
	 * 
	 * @return the list of interfaces contained within this NetworkDevice
	 */
	public List<NetworkInterface> getInterfaces();

	/**
	 * Returns <tt>true</tt> if this device can route data received from one interface to another interface.
	 * 
	 * <p>This flag is used by {@link RoutingProtocol}s when looking for routes.
	 * 
	 * @return <tt>true</tt> if this device can route data
	 */
	public boolean isRoutingEnabled();

	/**
	 * Returns the {@link RoutingProtocol} associated with this NetworkDevice.
	 * 
	 * @return the {@link RoutingProtocol} associated with this NetworkDevice
	 */
	public RoutingProtocol getRoutingProtocol();

	/**
	 * Returns the {@link CloudProvider} to whom this device belongs to, or <tt>null</tt> if this device does not belong to any {@link CloudProvider}.
	 * 
	 * @return the {@link CloudProvider} to whom this device belongs to, or <tt>null</tt>
	 */
	public CloudProvider getCloudProvider();
}
