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

/**
 * An abstract class for receiving notification events.
 * 
 * <p>When you need to listen for a given notification code, you need to extend
 * this class and add an instance of it to the appropriate {@link Notifier}.
 * 
 * <p>See the {@link Notifier} documentation to learn more about how you can use notifications 
 * in the simulation.
 * 
 * @since 1.0
 */
public abstract class NotificationListener {
	boolean discarded = false;

	public NotificationListener() {
		super();
	}

	/**
	 * Returns <tt>true</tt> if this NotificationListener can be notified again.
	 * <tt>false</tt> is returned when this NotificationListener has reached its notification limit and cannot be notified again.
	 * 
	 * @return <tt>true</tt> if this NotificationListener can be notified again
	 */
	final public boolean canBeNotified() {
		return !this.discarded;
	}

	/**
	 * Marks this listener as discarded.
	 * 
	 * <p><b>Note</b> this listener will remain referenced on all entities where it was registered. It is only
	 * effectively removed by the {@link Notifier} when calling a <tt>notify</tt> method or when it explicitly asked for using the
	 * {@link Notifier#cleanupListeners()} method.
	 */
	public void discard() {
		this.discarded = true;
	}

	/**
	 * Returns <tt>true</tt> if this listener's code needs to be immediately executed after a notification.
	 * Have a look at {@link Notifier#notify(int, Object)} for more information.
	 * 
	 * <p><b>Note:</b> for most use cases returning <tt>false</tt> is just fine.
	 * 
	 * @return <tt>true</tt> if this listener's code needs to be immediately executed after a notification
	 */
	public boolean isInstantNotification() {
		return false;
	}

	final void notificationPerformed0(Notifier notifier,
			int notification_code, Object data) {
		notificationPerformed(notifier, notification_code, data);
	}

	/**
	 * Is called by any {@link Notifier} where this NotificationListener is registered (using addListener methods) when
	 * {@link NotificationListener#canBeNotified() this.canBeNotified()} is <tt>true</tt> and the 
	 * method {@link Notifier#notifyNow(int, Object)} is called on the Notifier.
	 * 
	 * @param notifier the {@link Notifier} where this NotificationListener is registered
	 * @param notification_code
	 * @param data
	 */
	protected abstract void notificationPerformed(Notifier notifier,
			int notification_code, Object data);
}
