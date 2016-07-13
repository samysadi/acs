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

package com.samysadi.acs.tracing;

import java.util.ArrayList;
import java.util.List;

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.tracing.ProbeImpl;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.utility.NotificationCodes;


/**
 *
 * @param <ValueType>
 *
 * @since 1.0
 */
public abstract class AbstractProbe<ValueType> extends ProbeImpl<ValueType> {
	/**
	 * Notification listeners that were registered by this probe
	 */
	private List<NotificationListener> registeredListeners;

	private ValueType value;

	@Override
	public void setup(Probed parent) {
		super.setup(parent);
		this.registeredListeners = null;
		this.value = null;
	}

	/**
	 * Returns the listener at the given index as returned by {@link AbstractProbe#registeredListener(NotificationListener)}.
	 *
	 * @return the listener at the given index
	 */
	protected NotificationListener getRegisteredListener(int index) {
		if (this.registeredListeners == null)
			throw new IndexOutOfBoundsException();
		return this.registeredListeners.get(index);
	}

	/**
	 * Returns the first listener that is an instance of the given <tt>clazz</tt> or <tt>null</tt> if
	 * none does.
	 *
	 * @param clazz
	 * @return the first listener that is an instance of <tt>clazz</tt> or <tt>null</tt>
	 */
	@SuppressWarnings("unchecked")
	protected <T extends NotificationListener> T getRegisteredListener(Class<T> clazz) {
		if (clazz==null)
			throw new NullPointerException();
		if (this.registeredListeners == null)
			return null;
		for (NotificationListener l : this.registeredListeners)
			if (clazz.isAssignableFrom(l.getClass()))
				return (T) l;
		return null;
	}

	/**
	 * Unregisters and discards all the listeners that were registered using {@link AbstractProbe#registeredListener(NotificationListener)}.
	 */
	protected void unregisterListeners() {
		if (this.registeredListeners == null)
			return;
		for (NotificationListener n: this.registeredListeners)
			n.discard();
		this.registeredListeners = null;
	}

	/**
	 * Adds the given listener to an internal list and returns its index.
	 *
	 * <p>Use this method to keep track of newly added listeners in order to automatically unregister them if needed using {@link AbstractProbe#unregisterListeners()}.
	 *
	 * @param listener the listener that has been added.
	 * @return index of the registered listener
	 */
	protected int registeredListener(NotificationListener listener) {
		if (this.registeredListeners == null)
			this.registeredListeners = new ArrayList<NotificationListener>();
		this.registeredListeners.add(listener);
		return this.registeredListeners.size()-1;
	}

	/**
	 * Removes the given listener from the internal list containing registered listeners.
	 *
	 * @param listener the listener to remove
	 * @return <tt>true</tt> if the list contained the specified listener
	 */
	protected boolean unregisteredListener(NotificationListener listener) {
		if (this.registeredListeners == null)
			return false;
		return this.registeredListeners.remove(listener);
	}

	/**
	 * Removes the listener at the given index from the internal list containing registered listeners.
	 *
	 * @param index the index of the listener to remove
	 */
	protected NotificationListener unregisteredListener(int index) {
		if (this.registeredListeners == null)
			throw new IndexOutOfBoundsException();
		return this.registeredListeners.remove(index);
	}

	@Override
	public ValueType getValue() {
		if (this.isDiscarded())
			throw new IllegalStateException("This probe is discarded");
		return value;
	}

	protected void setValue(ValueType value) {
		if (this.isDiscarded())
			throw new IllegalStateException("This probe is discarded");
		if (this.value == null) {
			if (value == null)
				return;
		} else if (this.value.equals(value))
			return;

		this.value = value;

		notify(NotificationCodes.PROBE_VALUE_CHANGED, null);
	}

	@Override
	public void discard() {
		super.discard();
		unregisterListeners();
	}
}
