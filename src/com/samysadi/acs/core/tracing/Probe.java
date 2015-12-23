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

package com.samysadi.acs.core.tracing;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.notifications.CoreNotificationCodes;
import com.samysadi.acs.core.notifications.Notifier;

/**
 * A probe is used to access and watch modifications on a given information (value of the probe).<br/>
 * Each probe has an associated <i>Key</i> that defines what information it contains, and uniquely identifies
 * it inside its parent.
 * 
 * <p>A probe must be initialized using {@link Probe#setup(Probed)} after it is created to be usable.<br/>
 * And, when the probe is not needed anymore, the {@link Probe#discard()} should be called.
 * The probe will then perform all necessary cleanup.
 * 
 * <p>This interface extends the {@link Notifier} interface.<br/>
 * You can watch probe value changes by adding a listener for the appropriate
 * notification code ({@link CoreNotificationCodes#PROBE_VALUE_CHANGED}).
 * 
 * <p>As a design recommendation, the value of the probe should
 * directly depend on the parent of the probe.
 * In particular, when the parent of the probe is an {@link Entity},
 * then the value of the probe should be computed independently from the parent of that {@link Entity}.<br/>
 * The value of the probe, can however depend on the children of that {@link Entity}.
 * 
 * <p>Another design recommendation is that probe implementations should register all necessary listeners 
 * to update their value automatically. Thus, they should not rely on explicit updates from external code.
 * For instance, their value should not be updated through other entities or probes.<br/>
 * Nevertheless, such implementations may exist in some use case (as they may result in better performances),
 * the probe should then implement the {@link ModifiableProbe} interface.
 * 
 * @since 1.0
 */
public interface Probe<ValueType> extends Notifier {

	/**
	 * Returns the Probed object which this probe belongs to or <tt>null</tt>
	 * if this probe is discarded.
	 * 
	 * @return the Probed object which this probe belongs to or <tt>null</tt>
	 * if this probe is discarded
	 * @see Probe#isDiscarded()
	 */
	public Probed getParent();

	/**
	 * Returns a key that defines what kind of information this probe contains.
	 * The key is case sensitive and also uniquely identifies this probe inside its parent.
	 * 
	 * @return a key that defines what kind of information this probe contains
	 */
	public String getKey();

	/**
	 * Returns current value of this probe.
	 * 
	 * @return current value of this probe
	 * @throws IllegalStateException if the probe is discarded
	 * @see Probe#isDiscarded()
	 */
	public ValueType getValue();

	/**
	 * Setups the probe and sets its parent to the given one.
	 * 
	 * @param parent
	 * @throws IllegalStateException if the probe is not discarded (and thus has already a defined parent)
	 * @throws NullPointerException if the given <tt>parent</tt> probe is <tt>null</tt>
	 * @see Probe#isDiscarded()
	 */
	public void setup(Probed parent);

	/**
	 * Returns <tt>true</tt> if this probe was discarded.
	 * 
	 * @return <tt>true</tt> if this probe was discarded
	 * @see Probe#discard()
	 */
	public boolean isDiscarded();

	/**
	 * This method discards and performs cleanup on this probe.
	 * 
	 * <p>After calling this method, this probe is not usable anymore in the sense that 
	 * no value change notifications are thrown, and any call to {@link Probe#getValue()} will
	 * throw an exception.<br/>
	 * If you want to use the probe again you need to call {@link Probe#setup(Probed)} on the probe.
	 * 
	 * @throws IllegalStateException if the probe is already discarded
	 * @see Probe#isDiscarded()
	 */
	public void discard();
}
