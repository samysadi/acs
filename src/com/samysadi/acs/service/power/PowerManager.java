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

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;

/**
 *
 * @since 1.0
 */
public interface PowerManager extends Entity {

	@Override
	public PowerManager clone();

	@Override
	public CloudProvider getParent();

	/**
	 * Returns a list of hosts that are powered on.
	 * This list includes only hosts that were powered on using current {@link PowerManager}.
	 *
	 * @return a list of hosts that are powered on
	 */
	public List<Host> getPoweredOnHosts();

	/**
	 * The power manager keeps a lock counter for each host, this method increments
	 * the lock counter which is associated with the given host.
	 *
	 * <p>As long as the counter is not null, calling {@link PowerManager#canPowerOn(Host)}
	 * or {@link PowerManager#canPowerOff(Host)} will return <tt>false</tt>.
	 *
	 * @param host
	 * @see PowerManager#unlockHost(Host)
	 */
	public void lockHost(Host host);

	/**
	 * The power manager keeps a lock counter for each host, this method decrements
	 * the lock counter which is associated with the given host.
	 *
	 * <p>As long as the counter is not null, calling {@link PowerManager#canPowerOn(Host)}
	 * or {@link PowerManager#canPowerOff(Host)} will return <tt>false</tt>.
	 *
	 * @param host
	 * @see PowerManager#lockHost(Host)
	 * @throws IllegalArgumentException if the calling this method would result in a negative lock value for the given host
	 */
	public void unlockHost(Host host);

	/**
	 * Returns <tt>true</tt> if the given <tt>host</tt> is not
	 * powered on and this entity authorizes to power on the given host.
	 *
	 * @param host
	 * @return <tt>true</tt> if and the given <tt>host</tt> is not
	 * powered on and this entity authorizes to power on the given host
	 * @see PowerManager#lockHost(Host)
	 * @see PowerManager#unlockHost(Host)
	 */
	public boolean canPowerOn(Host host);

	/**
	 * Returns <tt>true</tt> if the given <tt>host</tt> is not
	 * powered off and this entity
	 * authorizes to power off the given <tt>host</tt>.
	 *
	 * @param host
	 * @return <tt>true</tt> if the given <tt>host</tt> is not
	 * powered off and this entity
	 * authorizes to power off the given <tt>host</tt>
	 * This is usually <tt>true</tt> only if the host is not in use
	 * @see PowerManager#lockHost(Host)
	 * @see PowerManager#unlockHost(Host)
	 */
	public boolean canPowerOff(Host host);

	/**
	 * Powers on the given host.
	 *
	 * <p>You should listen to appropriate notification code ({@link NotificationCodes#POWER_STATE_CHANGED})
	 * to know when the host is powered on as this may take time.
	 *
	 * @param host
	 * @throws IllegalArgumentException if the host cannot be powered on
	 * @see PowerManager#canPowerOn(Host)
	 */
	public void powerOn(Host host);

	/**
	 * Powers off the given host.
	 *
	 * <p>You should listen to appropriate notification code ({@link NotificationCodes#POWER_STATE_CHANGED})
	 * to know when the host is powered off as this may take time.
	 *
	 * @param host
	 * @throws IllegalArgumentException if the host cannot be powered off
	 * @see PowerManager#canPowerOff(Host)
	 */
	public void powerOff(Host host);
}
