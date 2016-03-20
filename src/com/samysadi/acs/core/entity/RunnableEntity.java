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
 * A {@link RunnableEntity} describes a entity that can be started/paused/restarted/canceled or
 * declared as failed.
 *
 * <p>When the runnable state changes, a {@link CoreNotificationCodes#RUNNABLE_STATE_CHANGED} notification is thrown.
 *
 * <p>Default runnable state when instantiating a new object which implements this class is {@link RunnableState#PAUSED}.
 *
 * <p>{@link RunnableEntity}'s children, when they also implement {@link RunnableEntity},
 * must not be running if their parent is not running.
 * Thus, trying to start a {@link RunnableEntity} when its {@link RunnableEntity} parent is not running
 * will result in an exception.
 * In the same logic:<ul>
 * <li>when a {@link RunnableEntity} is paused, then all of its {@link RunnableEntity} children are paused;
 * <li>when a {@link RunnableEntity} is started, then all of its {@link RunnableEntity} children are started (if they can be);
 * <li>when a {@link RunnableEntity} is restarted, then all of its {@link RunnableEntity} children are canceled;
 * <li>when a {@link RunnableEntity} is canceled, then all of its {@link RunnableEntity} children are canceled;
 * <li>when a {@link RunnableEntity} fails, then all of its {@link RunnableEntity} children fail;
 * <li>when a {@link RunnableEntity} is terminated (other states), then all of its {@link RunnableEntity} children are terminated.
 * </ul>
 * @since 1.0
 */
public interface RunnableEntity extends Entity {
	public enum RunnableState {
		/** The entity is paused. This is the default value. */
		PAUSED,
		/** The entity is running, and not terminated yet (ie: not completed/canceled/failed). */
		RUNNING,
		/** The entity is successfully completed. */
		COMPLETED,
		/** The entity is canceled. */
		CANCELED,
		/** The entity is failed. */
		FAILED,
		USER1, USER2, USER3
	}

	@Override
	public RunnableEntity clone();

	/**
	 * Returns <tt>true</tt> if this entity can be restarted safely, and calling {@link RunnableEntity#doRestart()} on
	 * this entity will not throw an exception.
	 *
	 * <p>Usually, this methods returns <tt>true</tt>, if these next conditions are verified:<ul>
	 * <li>{@link RunnableEntity#isRunning()} returns <tt>false</tt>;
	 * <li>Calling {@link Entity#hasParentRec()} on this entity returns <tt>true</tt>;
	 * <li>If the parent of this entity is a {@link RunnableEntity}, then it must be running;
	 * <li>Other implementation-specific conditions may also apply.
	 * </ul>
	 *
	 * @return <tt>true</tt> if this entity can be restarted safely
	 */
	public boolean canRestart();

	/**
	 * Returns <tt>true</tt> if this entity can be started safely, and calling {@link RunnableEntity#doStart()} on
	 * this entity will not throw an exception.
	 *
	 * <p>This methods returns <tt>true</tt>, if these next conditions are verified:<ul>
	 * <li>{@link RunnableEntity#isTerminated()} returns <tt>false</tt>;
	 * <li>{@link RunnableEntity#isRunning()} returns <tt>false</tt>;
	 * <li>Calling {@link Entity#hasParentRec()} on this entity returns <tt>true</tt>;
	 * <li>If the parent of this entity is a {@link RunnableEntity}, then it must be running;
	 * <li>Other implementation-specific conditions may also apply.
	 * </ul>
	 *
	 * @return <tt>true</tt> if this entity can be started safely
	 */
	public boolean canStart();

	/**
	 * Stops the entity's activity and declares it as canceled.
	 *
	 * <p>If this entity owns {@link RunnableEntity} children, then they must also be canceled.
	 *
	 * <p>Nothing is done if this entity is already terminated (if {@link RunnableEntity#isTerminated()} returns <tt>true</tt>).
	 */
	public void doCancel();

	/**
	 * Stops the entity's activity and declares it as failed.
	 *
	 * <p>If this entity owns {@link RunnableEntity} children, then they must also be declared as failed.
	 *
	 * <p>Nothing is done if this entity is already terminated (if {@link RunnableEntity#isTerminated()} returns <tt>true</tt>).
	 */
	public void doFail();

	/**
	 * Stops the entity's activity and declares it as paused.
	 *
	 * <p>If this entity owns {@link RunnableEntity} children, then they must also be paused.
	 *
	 * @throws IllegalStateException if this Entity is not running
	 */
	public void doPause();

	/**
	 * Reinitializes the entity and restarts it from the beginning.
	 *
	 * <p>Use this method to start again an entity after it is terminated.
	 * So, unlike {@link RunnableEntity#doStart()} this method will not throw an exception if this
	 * entity is already terminated.
	 *
	 * <p>Make sure that {@link RunnableEntity#canRestart()} returns <tt>true</tt> before calling
	 * this method or you will get an exception.
	 *
	 * <p>If this entity owns {@link RunnableEntity} children, then they must be canceled.
	 *
	 * @throws IllegalStateException if {@link RunnableEntity#canRestart()} returns <tt>false</tt>.
	 */
	public void doRestart();

	/**
	 * Starts or continues the entity's activity and declares it as running.
	 *
	 * <p>Make sure that {@link RunnableEntity#canStart()} returns <tt>true</tt> before calling
	 * this method or you will get an exception.
	 *
	 * <p>If this entity owns {@link RunnableEntity} children, then they must also be started.
	 *
	 * @throws IllegalStateException if {@link RunnableEntity#canStart()} returns <tt>false</tt>.
	 */
	public void doStart();

	/**
	 * Terminates the entity's activity.<br/>
	 * This means either cancel it, or declare it as failed or declare it as completed.
	 *
	 * <p>Calling {@link RunnableEntity#isTerminated()} after this should return <tt>true</tt>.
	 *
	 * <p>If this entity owns {@link RunnableEntity} children, then they must also be terminated.
	 */
	public void doTerminate();

	/**
	 * Returns the {@link RunnableState} of this entity.
	 *
	 * @return the {@link RunnableState} of this entity
	 */
	public RunnableState getRunnableState();

	/**
	 * Returns <tt>true</tt> if this entity is activated and running. This happens after a successful call to
	 * {@link RunnableEntity#doStart()} or {@link RunnableEntity#doRestart()}.
	 *
	 * @return <tt>true</tt> if this entity is activated and running
	 */
	public boolean isRunning();

	/**
	 * Returns <tt>true</tt> if this entity's activity is terminated and cannot be started again using {@link RunnableEntity#doStart()}. Especially, this
	 * should return <tt>true</tt> after a successful call to {@link RunnableEntity#doFail()} or {@link RunnableEntity#doCancel()}.
	 *
	 * @return <tt>true</tt> if this entity is terminated
	 */
	public boolean isTerminated();

	/**
	 * Takes all actions to unplace this entity using the appropriate placement strategy if any, then
	 * sets the <tt>null</tt> parent to this entity.
	 *
	 * <p>Because this may take time, you need to listen to {@link CoreNotificationCodes#ENTITY_PARENT_CHANGED} to know when
	 * all actions have been taken.
	 *
	 * <p>If you only want to set a <tt>null</tt> parent for this entity then use {@link Entity#setParent(Entity)}.
	 * Which will set the <tt>null</tt> parent immediately.
	 */
	public void unplace();
}
