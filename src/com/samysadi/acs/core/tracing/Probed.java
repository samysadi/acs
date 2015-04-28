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

import java.util.Collection;

/**
 * This interface defines an object which may contain
 * one or multiple probes, and defines methods to add/remove probes and
 * to access those probes.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface Probed {

	/**
	 * Adds the given <tt>probe</tt> and returns <tt>null</tt>, and if
	 * there is already an existing probe with the same Key as the probe being added
	 * then returns the old probe.
	 * 
	 * <p>If there was already another probe with the same Key (see {@link Probe#getKey()})
	 * as the probe being added, then this probe will replace that probe.
	 * 
	 * <p>The added probe is setup if it was not. And, the replaced probe is discarded.
	 * 
	 * @param probe
	 * @return <tt>null</tt> or the old probe (ie: the probe that was replaced)
	 * @throws NullPointerException if the given <tt>probe</tt> is <tt>null</tt>
	 * @throws IllegalArgumentException if the given <tt>probe</tt>'s parent is not equal to this probed Object
	 */
	public Probe<?> addProbe(Probe<?> probe);

	/**
	 * This method is an alias for {@link Probed#getProbe(String, boolean) getProbe(probeKey, true)}.
	 */
	public Probe<?> getProbe(String probeKey);

	/**
	 * Returns an existing probe matching the given <tt>probeKey</tt>, a newly created probe
	 * or <tt>null</tt>.
	 * 
	 * <p>This method first searches for a probe that matches the given <tt>probeKey</tt> and returns it
	 * if found.
	 * 
	 * <p>Then if no such probe is found and <tt>create</tt> is <tt>true</tt>, then a new probe is created based
	 * on the given <tt>probeKey</tt> and current configuration.
	 * The newly created probe is returned and is also added to the list of probes inside this probed object (ie: it is added
	 * to the list returned by {@link Probed#getProbes()}.
	 * 
	 * <p>If the <tt>create</tt> parameter is <tt>false</tt>, then <tt>null</tt> is returned.
	 * 
	 * @param probeKey
	 * @param create whether to create a probe with the given <tt>probeKey</tt> if none is found
	 * @return an existing probe matching the given <tt>probeKey</tt>, a newly created probe
	 * or <tt>null</tt>
	 */
	public Probe<?> getProbe(String probeKey, boolean create);

	/**
	 * Returns an unmodifiable collection containing all probes in this object.
	 * 
	 * @return an unmodifiable collection containing all probes in this object
	 */
	public Collection<Probe<?>> getProbes();

	/**
	 * Removes the probe whose key is equal the given <tt>probeKey</tt> if it exists and returns it.
	 * 
	 * <p>The removed probe is discarded.
	 * 
	 * @param probeKey
	 * @return the removed probe
	 */
	public Probe<?> removeProbe(String probeKey);

	/**
	 * Removes the given <tt>probe</tt> if it exists and returns it.
	 * 
	 * <p>The removed probe is discarded.
	 * 
	 * @param probe
	 * @return the removed probe
	 */
	public Probe<?> removeProbe(Probe<?> probe);
}
