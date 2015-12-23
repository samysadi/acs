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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.EventImpl;

/**
 * 
 * @since 1.0
 */
public class NotifierImpl implements Notifier, Cloneable {
	private static final Integer GLOBAL_LISTENER_KEY = Integer.valueOf(0);
	private Map<Integer, HashSet<NotificationListener>> notificationListeners = null;
	private boolean notificationsDisabled = false;
	//note: a same notification (same code and data) happens once, even after multiple call to notify(int, Object)
	//this next Set keeps track of all scheduled notification events
	private HashSet<NotifyEvent> notificationEvents = null;

	@Override
	public NotifierImpl clone() {
		final NotifierImpl clone;
		try {
			clone = (NotifierImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

		clone.notificationListeners = null;
		clone.notificationEvents = null;

		return clone;
	}

	private void addListener(Integer notification_code, NotificationListener listener) {
		if (listener == null)
			throw new NullPointerException();

		if (notificationListeners == null) {
			//HashMap will give better performances, but needs more memory usage
			//TreeMap will give worse performances, but needs less memory usage
			notificationListeners = new TreeMap<Integer, HashSet<NotificationListener>>();
		}

		HashSet<NotificationListener> nlist = notificationListeners.get(notification_code);
		if (nlist == null) {
			//HashSet will give best performances, but needs more memory usage
			//These next collections could be used.
			//Warning: If you go with one of these next, please review all code (especially iterations) as current implementation is optimized for HashSet
			//LinkedList will give worst performances, but needs less memory usage.
			//TreeSet is a tradeoff solution, but we need to make NotificationListener implement Comparable
			nlist = new HashSet<NotificationListener>();
			notificationListeners.put(notification_code, nlist);
		}
//		if (!nlist.contains(listener)) //uncomment this line if nlist is not a Set
		nlist.add(listener);
	}

	@Override
	public void addGlobalListener(NotificationListener listener) {
		addListener(GLOBAL_LISTENER_KEY, listener);
	}

	@Override
	public void addListener(int notification_code, NotificationListener listener) {
		if (notification_code == 0)
			throw new IllegalArgumentException("Given notification_code(" + notification_code + ") is not allowed");
		addListener(Integer.valueOf(notification_code), listener);
	}

	private Collection<NotificationListener> getListeners(Integer notification_code) {
		if (notificationListeners == null)
			return Collections.emptyList();
		Collection<NotificationListener> listeners = notificationListeners.get(notification_code);
		if (listeners == null)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(listeners);
	}

	@Override
	public Collection<NotificationListener> getGlobalListeners() {
		return getListeners(GLOBAL_LISTENER_KEY);
	}

	@Override
	public Collection<NotificationListener> getListeners(int notification_code) {
		if (notification_code == 0)
			throw new IllegalArgumentException("Given notification_code(" + notification_code + ") is not allowed");
		return getListeners(Integer.valueOf(notification_code));
	}

	private Collection<NotificationListener> removeNotificationsList(Integer key) {
		final Collection<NotificationListener> r = notificationListeners.remove(key);
		if (notificationListeners.isEmpty())
			notificationListeners = null;
		return Collections.unmodifiableCollection(r);
	}

	private boolean removeListener(Integer notification_code, NotificationListener listener) {
		if (notificationListeners == null)
			return false;

		Collection<NotificationListener> nlist = notificationListeners.get(notification_code);
		if (nlist == null)
			return false;

		final boolean r = nlist.remove(listener);
		if (nlist.isEmpty())
			removeNotificationsList(notification_code);
		return r;
	}

	@Override
	public boolean removeGlobalListener(NotificationListener listener) {
		return removeListener(GLOBAL_LISTENER_KEY, listener);
	}

	@Override
	public boolean removeListener(int notification_code, NotificationListener listener) {
		if (notification_code == 0)
			throw new IllegalArgumentException("Given notification_code(" + notification_code + ") is not allowed");
		return removeListener(Integer.valueOf(notification_code), listener);
	}

	private Collection<NotificationListener> removeAllListeners(Integer notification_code) {
		if (notificationListeners == null)
			return Collections.emptyList();
	
		return removeNotificationsList(notification_code);
	}

	@Override
	public Collection<NotificationListener> removeAllGlobalListeners() {
		return removeAllListeners(GLOBAL_LISTENER_KEY);
	}

	@Override
	public Collection<NotificationListener> removeAllListeners(int notification_code) {
		if (notification_code == 0)
			throw new IllegalArgumentException("Given notification_code(" + notification_code + ") is not allowed");
		return removeAllListeners(Integer.valueOf(notification_code));
	}

	@Override
	public Collection<NotificationListener> removeAllListeners() {
		if (notificationListeners == null)
			return Collections.emptyList();
		Collection<NotificationListener> r = new HashSet<NotificationListener>();
		for (Collection<NotificationListener> s: notificationListeners.values())
			if (s != null)
				r.addAll(s);
		notificationListeners = null;
		return r;
	}

	@Override
	public void cleanupListeners() {
		if (notificationListeners == null)
			return;

		final Iterator<Entry<Integer, HashSet<NotificationListener>>> it = notificationListeners.entrySet().iterator();
		while (it.hasNext()) {
			Collection<NotificationListener> l = it.next().getValue();
			final Iterator<NotificationListener> it2 = l.iterator();
			while (it2.hasNext()) {
				NotificationListener n = it2.next();
				if (!n.canBeNotified())
					it2.remove();
			}
			if (l.isEmpty())
				it.remove();
		}

		if (notificationListeners.isEmpty())
			notificationListeners = null;
	}

	@Override
	public boolean isNotificationsDisabled() {
		return this.notificationsDisabled;
	}

	@Override
	public void disableNotifications() {
		this.notificationsDisabled  = true;
	}

	@Override
	public void enableNotifications() {
		this.notificationsDisabled = false;
	}

	private void notifyList(boolean allNow,
			Integer listKey, int notification_code, Object data) {
		if (notificationListeners == null)
			return;

		HashSet<NotificationListener> olist = notificationListeners.get(listKey);

		if (olist == null)
			return;

		ArrayList<NotificationListener> nowListeners;

		if (allNow) {
			nowListeners = new ArrayList<NotificationListener>(olist);
		} else {
			if (this.notificationEvents == null) {
				this.notificationEvents = new HashSet<NotifyEvent>();
			}

			nowListeners = new ArrayList<NotificationListener>(olist.size());

			Iterator<NotificationListener> it = olist.iterator();
			while (it.hasNext()) {
				NotificationListener listener = it.next();
				if (listener.canBeNotified()) {
					if (listener.isInstantNotification()) {
						nowListeners.add(listener);
					} else {
						NotifyEvent e = new NotifyEvent(listener, this, notification_code, data);
						if (this.notificationEvents.add(e))
							Simulator.getSimulator().schedule(e);
					}
				} else
					it.remove();
			}

			if (this.notificationEvents.isEmpty())
				this.notificationEvents = null;
		}

		for (NotificationListener listener: nowListeners) {
			if (listener.canBeNotified()) //we need to re-test, because of: if (allNow) ..
				listener.notificationPerformed0(this, notification_code, data);
			if (!listener.canBeNotified())
				olist.remove(listener); //this method is O(1) with HashSet, so no need to make another iteration to remove discarded listeners
		}

		if (olist.isEmpty())
			removeNotificationsList(listKey);
	}

	@Override
	final public void notifyNow(int notification_code, Object data) {
		if (notification_code == 0)
			throw new IllegalArgumentException("Given notification_code(" + notification_code + ") is not allowed");

		if (this.isNotificationsDisabled())
			return;

		notifyList(true, notification_code, notification_code, data);
		notifyList(true, GLOBAL_LISTENER_KEY, notification_code, data);
	}

	@Override
	public void notify(int notification_code, Object data) {
		if (notification_code == 0)
			throw new IllegalArgumentException("Given notification_code(" + notification_code + ") is not allowed");

		if (this.isNotificationsDisabled())
			return;

		notifyList(false, notification_code, notification_code, data);
		notifyList(false, GLOBAL_LISTENER_KEY, notification_code, data);
	}

	@Override
	public void cancelNotifications() {
		if (this.notificationEvents == null)
			return;
		Iterator<NotifyEvent> it = this.notificationEvents.iterator();
		while (it.hasNext()) {
			NotifyEvent e = it.next();
			e.cancel();
		}
		this.notificationEvents = null;
	}

	public static class NotifyEvent extends EventImpl {
		private NotificationListener listener;
		private NotifierImpl notifier;
		private int notification_code;
		private Object data;

		public NotifyEvent(
				NotificationListener listener,
				NotifierImpl notifier,
				int notification_code, Object data) {
			super();
			this.listener = listener;
			this.notifier = notifier;
			this.notification_code = notification_code;
			this.data = data;
		}

		@Override
		public int hashCode() {
			int hash;
			hash = 23 + this.notification_code;
			hash = hash * 31 + this.listener.hashCode();
			if (this.data != null)
				hash = hash * 31 + this.data.hashCode();
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof NotifyEvent))
				return false;
			NotifyEvent o = (NotifyEvent) obj;
			if (o.listener != this.listener)
				return false;
			if (o.notifier != this.notifier)
				return false;
			if (o.notification_code != this.notification_code)
				return false;
			if (this.data == null)
				return o.data == null;
			return this.data.equals(o.data);
		}

		private void removeSelf() {
			if (this.notifier.notificationEvents != null) {
				this.notifier.notificationEvents.remove(this);
				if (this.notifier.notificationEvents.isEmpty())
					this.notifier.notificationEvents = null;
			}
		}

		private void cleanup() {
			if (!this.listener.canBeNotified()) {
				this.notifier.removeGlobalListener(this.listener);
				this.notifier.removeListener(this.notification_code, this.listener);
			}
			this.listener = null;
			this.notifier = null;
			this.data = null;
		}

		protected void performNotification() {
			if (this.listener.canBeNotified())
				this.listener.notificationPerformed0(this.notifier, this.notification_code, this.data);
		}

		@Override
		public void cancel() {
			removeSelf();
			cleanup();
			super.cancel();
		}

		@Override
		public void process() {
			removeSelf(); //allow re-notification with same parameters
			performNotification();
			cleanup();
		}
	}
}
