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

import java.util.List;

import com.samysadi.acs.core.Simulator;

/**
 * A trace gathers information from its parent probe, and offers
 * methods to access and save this information.
 * 
 * <p>When the trace is enabled, it keeps track of value changes on the parent probe.<br/>
 * However, the trace may not contain all value changes if these have happened
 * in short enough time delay (ie: a delay smaller than {@link Trace#getDelay()}).<br/>
 * Also, if the trace contains too much information (more than {@link Trace#getMaxLength()}) then
 * the trace delay may be increased and some value changes removed.<br/>
 * Though, in any circumstances, the trace will contain the first value change (when it was enabled)
 * and the last value change.
 * 
 * <p>If the trace is disabled then no history of the value changes is kept.
 * 
 * @since 1.0
 */
public interface Trace<T> {
	public static final int DEFAULT_MAXIMUM_LENGTH = 100;
	public static final long DEFAULT_DELAY = Simulator.MILLISECOND;

	/**
	 * @return the Probe used by this trace
	 */
	public Probe<T> getParent();

	/**
	 * Disables and discards this trace.
	 * 
	 * @throws IllegalStateException if this trace is already discarded
	 */
	public void discard();

	/**
	 * Returns <tt>true</tt> if this trace is enabled.
	 * 
	 * @return <tt>true</tt> if this trace is enabled
	 * @see Trace#setEnabled(boolean)
	 */
	public boolean isEnabled();

	/**
	 * Enables or disables this trace.
	 * 
	 * <p>If the trace is enabled then it will keep track of value changes on the parent probe. These values are then 
	 * accessible via {@link Trace#getValues()}.
	 * 
	 * <p>If the trace is disabled then value changes on the parent probe are ignored and are not included 
	 * in the list returned by {@link Trace#getValues()}.
	 * 
	 * @throws IllegalStateException if this trace is discarded
	 */
	public void setEnabled(boolean v);

	/**
	 * Returns the maximum trace length.
	 * 
	 * <p><b>Default</b> is {@link Trace#DEFAULT_MAXIMUM_LENGTH} if not specified otherwise.
	 * 
	 * @return maximum trace length
	 */
	public int getMaxLength();

	/**
	 * Updates the maximum length of the trace.<br/>
	 * When the trace contains too much information, then the trace
	 * delay may be increased.
	 * 
	 * @param length
	 * @throws IllegalArgumentException if you give a length smaller than 2
	 */
	public void setMaxLength(int length);

	/**
	 * Returns the minimum time delay before a probe value change is taken into account.
	 * In other words, the minumum time delay before a probe value change is included
	 * in the list returned by by {@link Trace#getValues()}.
	 * 
	 * <p><b>Default</b> is {@link Trace#DEFAULT_DELAY} if not specified otherwise.
	 * 
	 * @return the minimum time delay before a probe value change is taken into account
	 */
	public long getDelay();

	/**
	 * Updates the minimum time delay before a probe value change is taken into account.
	 * 
	 * <p>If the delay is increased, then some trace values may be removed.
	 * 
	 * @param value
	 * @throws IllegalArgumentException if you give negative delay
	 */
	public void setDelay(long value);

	/**
	 * Returns an unmodifiable list containing {@link TraceItem}s ordered chronologically.
	 * 
	 * <p>The returned list will not necessarily contain an item for every possible time, but instead
	 * it will contain a new item each time the value of this trace changes.<br/>
	 * Additionally, the minimum time difference between two items in the list is {@link Trace#getDelay()}.
	 * 
	 * @return an unmodifiable list containing {@link TraceItem}s ordered chronologically
	 */
	public List<TraceItem<T>> getValues();
}
