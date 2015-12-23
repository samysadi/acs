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
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.hardware.network.operation.provisioner.NetworkProvisioner;

/**
 * 
 * @since 1.0
 */
public class NetworkLinkDefault extends EntityImpl implements NetworkLink {
	private NetworkInterface networkInterface0;
	private NetworkInterface networkInterface1;

	private NetworkProvisioner networkProvisioner;

	/**
	 * This constructor is provided only to satisfy the {@link Entity} contract.
	 * 
	 * @throws NullPointerException
	 */
	public NetworkLinkDefault() {
		super();
		throw new NullPointerException();
	}

	public NetworkLinkDefault(NetworkInterface ni0, NetworkInterface ni1) {
		super();
		if (ni1 == null)
			throw new NullPointerException();
		if (ni0 == ni1)
			throw new IllegalArgumentException("Cyclic link");
		networkInterface0 = ni0;
		networkInterface1 = ni1;
	}

	@Override
	public NetworkLinkDefault clone() {
		final NetworkLinkDefault clone = (NetworkLinkDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.networkProvisioner = null;
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof NetworkProvisioner) {
			if (this.networkProvisioner == entity)
				return;
			if (this.networkProvisioner != null)
				this.networkProvisioner.setParent(null);
			this.networkProvisioner = (NetworkProvisioner) entity;
		}
		super.addEntity(entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof NetworkProvisioner) {
			if (this.networkProvisioner != entity)
				return;
			this.networkProvisioner = null;
		}
		super.removeEntity(entity);
	}

	@Override
	public NetworkInterface getFirstInterface() {
		return networkInterface0;
	}

	@Override
	public NetworkInterface getSecondInterface() {
		return networkInterface1;
	}

	@Override
	public NetworkProvisioner getNetworkProvisioner() {
		return networkProvisioner;
	}
}
