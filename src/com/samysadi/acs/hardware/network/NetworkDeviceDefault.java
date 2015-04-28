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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntityImpl;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;


/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class NetworkDeviceDefault extends FailureProneEntityImpl implements NetworkDevice {
	private PowerState powerState;
	private RoutingProtocol routingProtocol;
	private List<NetworkInterface> interfaces;

	public NetworkDeviceDefault() {
		super();

		this.powerState = PowerState.ON;
	}

	@Override
	public NetworkDeviceDefault clone() {
		final NetworkDeviceDefault clone = (NetworkDeviceDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.routingProtocol = null;
		this.interfaces = new ArrayList<NetworkInterface>();
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof RoutingProtocol) {
			if (entity == this.routingProtocol)
				return;
			if (this.routingProtocol != null)
				this.routingProtocol.setParent(null);
			this.routingProtocol = (RoutingProtocol) entity;
		} else if (entity instanceof NetworkInterface) {
			if (!interfaces.add((NetworkInterface) entity))
				return;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof RoutingProtocol) {
			if (entity != this.routingProtocol)
				return;
			this.routingProtocol = null;
		} else if (entity instanceof NetworkInterface) {
			if (!interfaces.remove(entity))
				return;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<Entity> l = new ArrayList<Entity>(1);
		if (this.routingProtocol != null)
			l.add(this.routingProtocol);

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		r.add(l);
		r.add(this.interfaces);
		return new MultiListView<Entity>(r);
	}

	@Override
	public List<NetworkInterface> getInterfaces() {
		return Collections.unmodifiableList(interfaces);
	}

	@Override
	public PowerState getPowerState() {
		return this.powerState;
	}

	@Override
	public void setPowerState(PowerState powerState) {
		if (powerState == this.powerState)
			return;

		this.powerState = powerState;
		notify(NotificationCodes.POWER_STATE_CHANGED, null);
	}

	/*@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public void setLocation(Location location) {
		if (this.location.equals(location))
			return;

		this.location = location;
		notify(Notifications.ND_LOCATION_CHANGED, null);
	}*/

	@Override
	public RoutingProtocol getRoutingProtocol() {
		return routingProtocol;
	}

	@Override
	public CloudProvider getCloudProvider() {
		if (getParent() instanceof CloudProvider)
			return (CloudProvider) getParent();
		return null;
	}
}
