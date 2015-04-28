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

package com.samysadi.acs.service.power;

import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.collections.WeakLinkedList;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class PowerManagerDefault extends EntityImpl implements PowerManager {
	private static final Object LOCK = new Object(); 
	private WeakLinkedList<Host> onHosts;

	public PowerManagerDefault() {
		super();
	}

	@Override
	public PowerManagerDefault clone() {
		final PowerManagerDefault clone = (PowerManagerDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.onHosts = new WeakLinkedList<Host>();
	}

	@Override
	public CloudProvider getParent() {
		return (CloudProvider) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof CloudProvider))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public void lockHost(Host host) {
		int i = ((Integer)host.getProperty(LOCK, Integer.valueOf(0)));
		host.setProperty(LOCK, i + 1);
	}

	@Override
	public void unlockHost(Host host) {
		int i = ((Integer)host.getProperty(LOCK, Integer.valueOf(0)));
		if (i == 0)
			throw new IllegalArgumentException("Would result in negative PowerState lock on the given host.");
		host.setProperty(LOCK, i - 1);
	}

	@Override
	public boolean canPowerOn(Host host) {
		if (host.getFailureState() != FailureState.OK)
			return false;

		if (host.getPowerState() == PowerState.ON)
			return false;

		if (host.getPowerState() == PowerState.SHUTTING_DOWN)
			return false;

		if (((Integer)host.getProperty(LOCK, Integer.valueOf(0))) != 0)
			return false;

		return true;
	}

	@Override
	public boolean canPowerOff(Host host) {
		if (host.getPowerState() == PowerState.OFF)
			return false;

		if (host.getPowerState() == PowerState.BOOTING)
			return false;

		if (((Integer)host.getProperty(LOCK, Integer.valueOf(0))) != 0)
			return false;

		return true;
	}

	@Override
	public void powerOn(final Host host) {
		if (!canPowerOn(host))
			throw new IllegalArgumentException("Cannot power on the given host now.");

		if (host.getPowerState() == PowerState.BOOTING)
			return; //nothing to do

		host.setPowerState(PowerState.BOOTING);
		Simulator.getSimulator().schedule(host.getConfig() != null ? host.getConfig().getLong("BootingDelay", 0l) * Simulator.SECOND : 0, new EventImpl() {
			@Override
			public void process() {
				if (host.getPowerState() == PowerState.BOOTING) {
					host.setPowerState(PowerState.ON);
					PowerManagerDefault.this.onHosts.add(host);
				}
			}
		});
	}

	@Override
	public void powerOff(Host host) {
		if (!canPowerOff(host))
			throw new IllegalArgumentException("Cannot power off the given host now.");

		if (host.getPowerState() == PowerState.SHUTTING_DOWN)
			return; //nothing to do

		this.onHosts.remove(host);
		host.setPowerState(PowerState.OFF);
	}

	@Override
	public List<Host> getPoweredOnHosts() {
		return Collections.unmodifiableList(onHosts);
	}
}
