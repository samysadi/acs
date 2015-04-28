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

package com.samysadi.acs.core.event;

import com.samysadi.acs.core.Simulator;

/**
 * Events are processed by the simulator after they are scheduled when the simulation
 * time is equal to the event's scheduled time.
 * 
 * <p>If multiple events are scheduled at the same time, then they are processed in the order they
 * were scheduled.
 * 
 * @see EventChain
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface Event extends Cloneable {

	public Event clone();

	/**
	 * Returns the simulation time when this event will be processed or <tt>null</tt> if this event was not scheduled.
	 * 
	 * @return the simulation time when this event will be processed or <tt>null</tt>
	 */
	public Long getScheduledAt();

	/**
	 * Returns <tt>true</tt> if this event was scheduled and put in the simulation's event queue.
	 * <tt>false</tt> is returned if this event was not scheduled, or if it was canceled.
	 * 
	 * @return <tt>true</tt> if this event was scheduled
	 */
	public boolean isScheduled();

	/**
	 * This method is called by the simulator after that this event is scheduled.
	 * 
	 * <p><b>Note</b> that you should not need to call this method, instead use {@link Simulator#schedule(Event)}.
	 * 
	 * @param time the scheduled simulation time when this event will be processed
	 */
	public void scheduledAt(Long time);

	/**
	 * Cancels this event, and removes it from the simulator events queue.
	 * 
	 * <p>This method is a helper which calls the simulator's {@link Simulator#cancel(Event)} on this event.
	 * 
	 * <p>Nothing happens if this event is not scheduled.
	 */
	public void cancel();

	/**
	 * This method is called by the simulator when the time has come to process this event.
	 */
	public void process();
}
