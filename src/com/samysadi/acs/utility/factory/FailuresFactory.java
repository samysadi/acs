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

package com.samysadi.acs.utility.factory;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.event.Event;

/**
 *
 * @since 1.0
 */
public abstract class FailuresFactory extends Factory {
	public FailuresFactory(Config config) {
		super(config);
		Simulator.getSimulator().setProperty(PROP_SIMULATOR_FAILURESFACTORY, this);
	}

	private static final Object PROP_SIMULATOR_FAILURESFACTORY = new Object();
	public static FailuresFactory getFailuresFactory() {
		return (FailuresFactory) Simulator.getSimulator().getProperty(PROP_SIMULATOR_FAILURESFACTORY);
	}

	public interface FailureRepairEvent extends Event {
		public FailureProneEntity getEntity();

		public FailuresFactory getFactory();
	}

	/**
	 * An event that is scheduled to generate a failure.
	 */
	public interface FailureEvent extends FailureRepairEvent {
	}

	/**
	 * An event that is scheduled to generate a repair.
	 */
	public interface RepairEvent extends FailureRepairEvent {
	}

	/**
	 * Returns the next scheduled event for the entity for handling failures/repairs.
	 * The event can either be scheduled to simulate a failure or a repair.
	 *
	 * @param entity
	 * @return the next scheduled event for the entity for handling failures/repairs
	 */
	public abstract FailureRepairEvent getFutureEvent(Entity entity);

	/**
	 * Disables simulation of the failures/repairs for the given <tt>entity</tt> if enabled.
	 *
	 * @param entity
	 */
	public abstract void disable(Entity entity);

	/**
	 * Disables simulation of the failures/repairs for the given <tt>entity</tt> and all of its children
	 * if enabled.
	 *
	 * @param entity
	 */
	public abstract void disableRec(Entity entity);

	/**
	 * Enables simulation of the failures/repairs (if not already) for the given <tt>entity</tt>
	 * if {@link FailureProneEntity#supportsFailureStateUpdate()} returns <tt>true</tt>.
	 *
	 * @param entity
	 */
	public abstract void enable(Entity entity);

	/**
	 * Enables simulation of the failures/repairs (if not already) for the given <tt>entity</tt> and all of its children
	 * which extend {@link FailureProneEntity} and
	 * if {@link FailureProneEntity#supportsFailureStateUpdate()} returns <tt>true</tt>.
	 *
	 * @param entity
	 */
	public abstract void enableRec(Entity entity);

	/**
	 * Enables failures/repairs simulation for the current Simulator and all of its children and returns <tt>null</tt>.
	 *
	 * @return <tt>null</tt>
	 */
	@Override
	public abstract Object generate();
}
