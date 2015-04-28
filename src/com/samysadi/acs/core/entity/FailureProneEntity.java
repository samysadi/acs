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

package com.samysadi.acs.core.entity;

import com.samysadi.acs.core.notifications.CoreNotificationCodes;

/**
 * Defines a failure-prone entity. Such entities can be declared as failed.
 * 
 * <p>Note that even if a class implements this interface, that does not mean that it's failure
 * state can be updated.<br/>
 * Use {@link FailureProneEntity#supportsFailureStateUpdate()} to see if this entity supports 
 * failure state update.
 * 
 * <p>When the failure state changes, a {@link CoreNotificationCodes#FAILURE_STATE_CHANGED} notification is thrown.
 * 
 * <p>Default failure state when instantiating a new object which implements this class is {@link FailureState#OK}.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface FailureProneEntity extends Entity {
	public enum FailureState {
		/**
		 * This entity has not failed. This is the default value.
		 */
		OK,
		/**
		 * This entity has failed.
		 */
		FAILED,
		USER1, USER2, USER3
	}

	@Override
	public FailureProneEntity clone();

	/**
	 * Returns <tt>true</tt> if the {@link FailureState} of this entity can be modified.
	 * 
	 * Note that even if a entity implements the {@link FailureProneEntity} interface,
	 * it does not mean it supports state changing.<br/>
	 * Thus you have to check the return value of this method to ensure that you can use
	 * {@link FailureProneEntity#setFailureState(FailureState)} safely.
	 * 
	 * <p>If this method returns <tt>false</tt> and you try to change the {@link FailureState} of this entity you will
	 * get a UnsupportedOperationException.
	 * 
	 * @return <tt>true</tt> if the {@link FailureState} of this entity can be modified
	 */
	public boolean supportsFailureStateUpdate();

	/**
	 * Returns the {@link FailureState} of this entity.
	 * 
	 * @return the {@link FailureState} of this entity
	 */
	public FailureState getFailureState();

	/**
	 * Changes the state of this entity to the new given <tt>state</tt>.
	 * 
	 * <p>Additionally, if the state has changed then a {@link CoreNotificationCodes#FAILURE_STATE_CHANGED} notification is thrown.
	 * 
	 * @param state the new {@link FailureState}
	 * @throws UnsupportedOperationException if this entity does not support this operation
	 */
	public void setFailureState(FailureState state);
}
