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

import java.util.Collection;

/**
 * A Notifier holds a set of listeners (see {@link NotificationListener}) that can be
 * notified at different moments of the simulation.
 * 
 * <p>Use the {@link Notifier#addListener(int, NotificationListener)} method to add listeners for specific notification codes,
 * and the {@link Notifier#addGlobalListener(NotificationListener)} to register a global listener (for all notification codes).
 * 
 * <p>Two methods can be used to notify the listeners: the {@link Notifier#notifyNow(int, Object)} method and
 * the {@link Notifier#notify(int, Object)} method.
 * These two methods take two parameters. The first is the notification code that is used
 * to determine which listeners to notify (except for global listeners that are always notified).
 * The second is an optional parameter that is supplied for the listeners.
 * 
 * <p>The {@link Notifier#notifyNow(int, Object)} method, immediately notifies all the global listeners and all 
 * the listeners that have been added for the given notification code.
 * 
 * <p>The {@link Notifier#notify(int, Object)} method, delays the notification of the matching listeners.<br/>
 * Instead of immediately notifying the listeners, an event is scheduled at current simulation time. This
 * behavior allows the caller to complete a given task before other tasks are executed. In another hand,
 * we avoid to overburden the simulator's call stack.<br/>
 * This method should be preferred over the {@link Notifier#notifyNow(int, Object)} method.
 * And if you need a given listener to be always immediately notified, then use {@link InstantNotificationListener}.
 * 
 * <p>Note that listeners can be discarded, and thus are never notified in the future.<br/>
 * The {@link NotificationListener#canBeNotified()} is used to determine if 
 * listeners can be notified (ie: not discarded), and if they cannot then they are removed.
 * 
 * <p>The {@link CoreNotificationCodes} interface contains all predefined notification codes.
 * 
 * @since 1.0
 */
public interface Notifier extends Cloneable {

	/**
	 * Creates a clone of this {@link Notifier}.
	 * 
	 * <p>The clone will contain a new empty listeners list and is independent from this object.
	 */
	public Notifier clone();

	/**
	 * Adds the given <tt>listener</tt> for the given <tt>notification_code</tt>.<br/>
	 * Nothing is done if the listener was already added for the same <tt>notification_code</tt>.
	 * 
	 * @param notification_code
	 * @param listener
	 *
	 * @throws NullPointerException if <tt>listener</tt> is <tt>null</tt>
	 */
	public void addListener(int notification_code, NotificationListener listener);

	/**
	 * Returns a collection containing all listeners for the given <tt>notification_code</tt>.
	 * 
	 * @param notification_code
	 * @return a collection containing all listeners for the given <tt>notification_code</tt>
	 */
	public Collection<NotificationListener> getListeners(int notification_code);

	/**
	 * Removes the given <tt>listener</tt> for the given <tt>notification_code</tt>
	 * and returns <tt>true</tt> on success.
	 * 
	 * @param notification_code
	 * @param listener
	 * @return <tt>true</tt> if the given <tt>listener</tt> was found and removed
	 */
	public boolean removeListener(int notification_code, NotificationListener listener);

	/**
	 * Removes all the listeners for the given <tt>notification_code</tt>
	 * and returns a collection containing the removed listeners.
	 * 
	 * @param notification_code
	 * @return a collection containing removed listeners
	 */
	public Collection<NotificationListener> removeAllListeners(int notification_code);

	/**
	 * Adds the given <tt>listener</tt> as a global listener that will be triggered for any notification code.<br/>
	 * Nothing is done if the listener was already added.
	 * 
	 * @param listener
	 *
	 * @throws NullPointerException if <tt>listener</tt> is <tt>null</tt>
	 */
	public void addGlobalListener(NotificationListener listener);

	/**
	 * Returns a collection containing all global listeners.
	 * 
	 * @return a collection containing all global listeners
	 */
	public Collection<NotificationListener> getGlobalListeners();

	/**
	 * Removes the given <tt>listener</tt> from the list of global listeners.
	 * 
	 * @param listener
	 */
	public boolean removeGlobalListener(NotificationListener listener);

	/**
	 * Removes all the global listeners and returns a collection containing the removed listeners.
	 * 
	 * @return a collection containing removed listeners
	 */
	public Collection<NotificationListener> removeAllGlobalListeners();

	/**
	 * Removes all added listeners, including global listeners, and
	 * returns a collection containing the removed listeners.
	 * 
	 * @return a collection containing removed listeners
	 */
	public Collection<NotificationListener> removeAllListeners();

	/**
	 * This method iterates through all listeners (including global listeners), and checks
	 * for discarded listeners and removes them.
	 * 
	 * <p>You are not required to call this method after discarding a {@link NotificationListener}, because, anyhow,
	 * this entity will not use them in the future.<br/>
	 * Call this method only if you need this entity to immediately remove discarded listeners.
	 * 
	 * <p>After calling this method, this entity is guaranteed to not reference any discarded listener.
	 */
	public void cleanupListeners();

	/**
	 * Executes the code of all listeners that were added for the given <tt>notification_code</tt>.
	 * The <tt>data</tt> parameter is supplied for the listener.
	 * 
	 * <p>The calling order of the listeners is undetermined. Thus a new listener may be called before an
	 * older listener and vice versa.
	 * 
	 * <p>Listeners of which {@link NotificationListener#canBeNotified()} method returns <tt>false</ff>
	 * are ignored and removed from the list of listeners.
	 * 
	 * @param notification_code
	 * @param data extra data supplied for the listener when its code is executed
	 */
	public void notifyNow(int notification_code, Object data);

	/**
	 * For each listener that was added using the given <tt>notification_code</tt>,
	 * this method does one of the following actions.
	 * 
	 * <p>If calling {@link NotificationListener#isInstantNotification()} returns <tt>true</tt> then
	 * the listener's code is executed immediately.
	 * 
	 * <p>If calling {@link NotificationListener#isInstantNotification()} returns <tt>false</tt> then
	 * this method schedules an event in order to execute the listener's code.
	 * 
	 * <p>Regarding the last case, there should be an individual event for each listener, 
	 * and each of them is scheduled at the current simulator's time.<br/>
	 * Also, this method will not schedule another event for a listener if it
	 * already has another scheduled and not yet processed event
	 * with the same <tt>notification_code</tt> and <tt>data</tt>.
	 * 
	 * <p>The calling order of the listeners is undetermined. Thus a new listener may be called before an
	 * older listener and vice versa.
	 * 
	 * <p>Use {@link Notifier#notifyNow(int, Object)} to notify listeners immediately.
	 * 
	 * @param notification_code
	 * @param data extra data supplied for the listener when its code is executed
	 */
	public void notify(int notification_code, Object data);

	/**
	 * Cancels all previously scheduled events using {@link Notifier#notify(int, Object)}.
	 */
	public void cancelNotifications();

	/**
	 * Returns <tt>true</tt> if notifications are disabled.
	 * 
	 * @return <tt>true</tt> if notifications are disabled
	 */
	public boolean isNotificationsDisabled();

	/**
	 * Further notifications are disabled. Which means that all further calls to 
	 * {@link Notifier#notifyNow(int, Object)} or {@link Notifier#notify(int, Object)}
	 * are ignored.
	 * 
	 * <p><b>Note:</b> If there were scheduled notifications using {@link Notifier#notify(int, Object)},
	 * then those are not canceled after calling this method. If you want to cancel them use 
	 * {@link Notifier#cancelNotifications()}.
	 */
	public void disableNotifications();

	/**
	 * Further notifications are enabled. Which means that all further calls to
	 * {@link Notifier#notifyNow(int, Object) notifyNow} or {@link Notifier#notify(int, Object) notify}
	 * are not ignored.
	 */
	public void enableNotifications();
}
