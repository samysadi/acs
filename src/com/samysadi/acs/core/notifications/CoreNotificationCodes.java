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

package com.samysadi.acs.core.notifications;

import java.util.HashMap;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.AllocatableEntity;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.tracing.Probe;

/**
 * You can use notifications to easily take specific actions
 * at different moments of the simulation.
 *
 * <p>This class contains only core package notification codes.
 *
 * <p><tt>0x80XXXXXX</tt> notification codes are reserved for core package.
 *
 * @since 1.0
 */
public class CoreNotificationCodes {
	protected CoreNotificationCodes() {
	}

	private static int mask0 = 0x80000000;
	protected static int nextMask() {
		int r = mask0;
		mask0+=0x00000100;
		return r;
	}

	private static HashMap<Integer, String> codeToString = null;
	protected static String notificationCodeToString(Class<?> clazz, int notification_code) {
		if (codeToString == null) {
			java.lang.reflect.Field[] fields = clazz.getFields();
			HashMap<Integer, String> h = new HashMap<Integer, String>(fields.length);
			try {
				for (java.lang.reflect.Field f:fields) {
					if (f.getType() != int.class)
						continue;
					h.put(Integer.valueOf(f.getInt(null)), f.getName());
				}
			} catch (IllegalArgumentException e) {
				//
			} catch (IllegalAccessException e) {
				//
			}
			codeToString = h;
		}
		String r = codeToString.get(Integer.valueOf(notification_code));
		if (r==null)
			return String.valueOf(notification_code);
		else
			return r;
	}

	public static String notificationCodeToString(int notification_code) {
		return notificationCodeToString(CoreNotificationCodes.class, notification_code);
	}


	/* ENTITY
	 * ------------------------------------------------------------------------
	 */

	private static final int ENTITY_MASK					= nextMask();

	/**
	 * <b>Description:</b> Thrown when a entity adds a new child entity<br/>
	 * <b>Notifier:</b> {@link Entity} <br/>
	 * <b>Object:</b> {@link Entity} the entity that has been added
	 */
	public static final int ENTITY_ADDED					= ENTITY_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown when a entity removes a child entity<br/>
	 * <b>Notifier:</b> {@link Entity} <br/>
	 * <b>Object:</b> {@link Entity} the entity that has been removed
	 */
	public static final int ENTITY_REMOVED					= ENTITY_MASK | 0x01;

	/**
	 * <b>Description:</b> Thrown when a entity's parent has changed<br/>
	 * <b>Notifier:</b> {@link Entity}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int ENTITY_PARENT_CHANGED			= ENTITY_MASK | 0x10;

	/**
	 * <b>Description:</b> Thrown when a entity's ancestor has changed<br/>
	 * <b>Notifier:</b> {@link Entity}<br/>
	 * <b>Object:</b> {@link Entity} the ancestor whose parent was changed
	 */
	public static final int ENTITY_ANCESTOR_CHANGED			= ENTITY_MASK | 0x11;

	/**
	 * <b>Description:</b> Thrown when this entity is cloned.<br/>
	 * <b>Notifier:</b> {@link Entity} the entity that is being cloned (source)<br/>
	 * <b>Object:</b> the new {@link Entity} that was created after cloning.
	 */
	public static final int ENTITY_CLONED					= ENTITY_MASK | 0x20;

	/**
	 * <b>Description:</b> Thrown when this entity allocated flag is changed.<br/>
	 * <b>Notifier:</b> {@link AllocatableEntity}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int ENTITY_ALLOCATED_FLAG_CHANGED	= ENTITY_MASK | 0x50;

	/**
	 * <b>Description:</b> Thrown when the {@link FailureState} of this entity changes.<br/>
	 * <b>Notifier:</b> {@link FailureProneEntity}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FAILURE_STATE_CHANGED			= ENTITY_MASK | 0x80;

	/**
	 * <b>Description:</b> Thrown when the {@link PowerState} of this entity changes (is powered-on or powered-off).<br/>
	 * <b>Notifier:</b> {@link PoweredEntity}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int POWER_STATE_CHANGED				= ENTITY_MASK | 0x81;

	/**
	 * <b>Description:</b> Thrown when the {@link RunnableState} of this entity changes (becomes completed, failed etc..).<br/>
	 * <b>Notifier:</b> {@link RunnableEntity}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int RUNNABLE_STATE_CHANGED			= ENTITY_MASK | 0x82;


	/* SIMULATOR
	 * ------------------------------------------------------------------------
	 */

	private static final int SIMULATOR_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when the simulation starts<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int SIMULATOR_STARTED				= SIMULATOR_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown when the simulation stops. Check {@link Simulator#hasMoreEvents()} to see
	 * if the simulation has ended or not.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int SIMULATOR_STOPPED				= SIMULATOR_MASK | 0x01;

	/**
	 * <b>Description:</b> Thrown when the simulation time advances and before events of the current simulation time are processed.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt> or
	 */
	public static final int SIMULATOR_TICK					= SIMULATOR_MASK | 0x10;

	/**
	 * <b>Description:</b> Thrown when the simulator has processed all events at the current simulation time.
	 * Note that you should not schedule events at current simulator's time or you will get an exception.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt> or
	 */
	public static final int SIMULATOR_TICK_PASSED			= SIMULATOR_MASK | 0x11;

	/**
	 * <b>Description:</b> Thrown each time the simulator processes a new event.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> {@link Event} that has been processed
	 */
	public static final int SIMULATOR_EVENT_PROCESSED		= SIMULATOR_MASK | 0x20;

	/* PROBE
	 * ------------------------------------------------------------------------
	 */
	private static final int PROBE_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after that a probe value changes.<br/>
	 * <b>Notifier:</b> {@link Probe}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int PROBE_VALUE_CHANGED				= PROBE_MASK | 0x80;
}
