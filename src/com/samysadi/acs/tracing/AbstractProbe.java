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
		return this.registeredListeners.get(index);
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
