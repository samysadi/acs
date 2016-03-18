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

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.Event;

/**
 * This class extends {@link NotificationListener}, and gives an
 * easy way to listen and execute code when a specific event type has
 * been processed.<br/>
 * To do so, call the {@link EventNotificationListener#autoAdd()} method or
 * add an instance of this class to the simulator to listen for the appropriate notification code
 * ({@link CoreNotificationCodes#SIMULATOR_EVENT_PROCESSED}).
 *
 * @since 1.0
 */
public abstract class EventNotificationListener extends NotificationListener {
	private Class<? extends Event> eventType;
	private boolean includeSuperClasses;

	public EventNotificationListener(Class<? extends Event> eventType,
			boolean listenToSuperClasses) {
		super();
		this.eventType = eventType;
		this.includeSuperClasses = listenToSuperClasses;
	}

	public EventNotificationListener(Class<? extends Event> eventType) {
		this(eventType, true);
	}

	@Override
	protected void notificationPerformed(Notifier notifier, int notification_code,
			Object data) {
		if (!(data instanceof Event))
			throw new IllegalArgumentException("No event supplied.");
		Event event = (Event) data;
		boolean ok = false;
		Class<?> c=event.getClass();
		do {
			ok = (c.equals(eventType));
			if (this.includeSuperClasses)
				c = c.getSuperclass();
			else
				c = null;
		} while (!ok && c != null && (!c.equals(Event.class)));
		if (!ok)
			return;
		eventNotificationPerformed(notifier, event);
	}

	public abstract void eventNotificationPerformed(Notifier notifier, Event event);

	/**
	 * Adds current listener to the simulator to listen for the appropriate notification code
	 * ({@link CoreNotificationCodes#SIMULATOR_EVENT_PROCESSED}).
	 */
	public final void autoAdd() {
		Simulator.getSimulator().addListener(CoreNotificationCodes.SIMULATOR_EVENT_PROCESSED, this);
	}

}
