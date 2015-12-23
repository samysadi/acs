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
import com.samysadi.acs.core.entity.Entity;

/**
 * This class extends the {@link Event} class and can be used if you want
 * to schedule multiple events at a given moment of the simulation, and make sure
 * that each event is <u>processed</u> before the next event is <u>scheduled</u>.
 * 
 * <p>More precisely after scheduling this event, the {@link EventChain#processStage(int)} method is
 * scheduled and called repetitively as long as it returns {@link EventChain#CONTINUE}.<br/>
 * At the first call, the {@link EventChain#processStage(int)} method is called with the <tt>0</tt> parameter using
 * current event. Then, if that method returns {@link EventChain#CONTINUE}, this event
 * is cloned and scheduled to run at the same simulation time, but with a <tt>1</tt> parameter. The parameter
 * keeps incrementing and the method is re-called as long as it returns the {@link EventChain#CONTINUE} value.
 * 
 * <p>This class is particularly useful if you need to call multiple {@link Entity}'s methods in a sequential order,
 * and make sure the generated notifications when calling each of these methods will be processed <b>before</b> the next
 * method is run.
 * 
 * @since 1.0
 */
public abstract class EventChain extends EventImpl {
	public static final boolean CONTINUE	= true;
	public static final boolean STOP		= false;

	private int stageNum = 0;

	public EventChain() {
		super();
	}

	@Override
	public EventChain clone() {
		EventChain clone = (EventChain) super.clone();
		return clone;
	}

	/**
	 * This method is repetitively called with an incremented <tt>stageNum</tt>, starting from <tt>0</tt>, as
	 * long as it returns <tt>true</tt>.<br/>
	 * This is done by transparently scheduling a clone of this event after each stage.
	 * 
	 * <p><b>Note</b> make sure that this method can return <tt>false</tt> or the simulator will be stuck in a dead loop.
	 * 
	 * @param stageNum
	 * @return {@link EventChain#CONTINUE} if this event has to be re-scheduled and this method recalled.
	 * {@link EventChain#STOP} otherwise.
	 */
	public abstract boolean processStage(int stageNum);

	@Override
	final public void process() {
		if (processStage(stageNum) == EventChain.CONTINUE) {
			stageNum++;
			Simulator.getSimulator().schedule(this.clone());
		}
	}
}
