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

package com.samysadi.acs.core.entity;

import com.samysadi.acs.core.notifications.CoreNotificationCodes;

/**
 * An entity that can be turned on and off.
 *
 * <p>When the power state changes, a {@link CoreNotificationCodes#POWER_STATE_CHANGED} notification is thrown.
 *
 * <p>Default power state when instantiating a new object which implements this class is {@link PowerState#ON}.
 *
 * @since 1.0
 */
public interface PoweredEntity extends Entity {
	public enum PowerState {
		/**
		 * This entity is powered on. This is the default value.
		 */
		ON,
		/**
		 * This entity is still powered off, but it is being powered on.
		 */
		BOOTING,
		/**
		 * This entity is powered off.
		 */
		OFF,
		/**
		 * This entity is powered off, but it is still being shut down.
		 */
		SHUTTING_DOWN,
		USER1, USER2, USER3
	}

	/**
	 * Returns the {@link PowerState} of this entity.
	 *
	 * @return the {@link PowerState} of this entity
	 */
	public PowerState getPowerState();

	/**
	 * Changes the power state of this entity to the new given <tt>state</tt>.
	 *
	 * <p>Additionally, if the state has changed then a {@link CoreNotificationCodes#POWER_STATE_CHANGED} notification is thrown.
	 *
	 * @param powerState the new {@link PowerState}
	 */
	public void setPowerState(PowerState powerState);
}
