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
 *
 * @since 1.0
 */
public abstract class RunnableEntityImpl extends EntityImpl implements RunnableEntity {
	private RunnableState runnableState;

	public RunnableEntityImpl() {
		super();

		this.runnableState = RunnableState.PAUSED;
	}

	@Override
	public RunnableEntityImpl clone() {
		final boolean wasRunning = this.isRunning();
		if (wasRunning)
			this.doPause();
		final RunnableEntityImpl clone = (RunnableEntityImpl) super.clone();
		if (wasRunning)
			this.doStart();
		return clone;
	}

	@Override
	public void setParent(Entity parent) {
		if (parent == getParent())
			return;
		if (this.isRunning()) {
			getLogger().log(this, "Canceled because a new parent is set.");
			this.doCancel();
		}
		super.setParent(parent);
	}

	@Override
	public boolean canRestart() {
		if (isRunning())
			return false;
		if (!hasParentRec())
			return false;
		if ((getParent() instanceof RunnableEntity) && !((RunnableEntity) getParent()).isRunning())
			return false;
		return true;
	}

	protected String getCannotRestartReason() {
		if (isRunning())
			return "This entity (" + this + ") cannot be restarted because it is already running.";
		if (!hasParentRec())
			return "This entity (" + this + ") cannot be restarted because it has no parent, or one of its ancestors has a null parent.";
		if ((getParent() instanceof RunnableEntity) && !((RunnableEntity) getParent()).isRunning())
			return "This entity (" + this + ") cannot be restarted because its parent is not running.";
		return "This entity (" + this + ") cannot be restarted.";
	}

	@Override
	public boolean canStart() {
		if (isTerminated())
			return false;
		if (isRunning())
			return false;
		if (!hasParentRec())
			return false;
		if ((getParent() instanceof RunnableEntity) && !((RunnableEntity) getParent()).isRunning())
			return false;
		return true;
	}

	protected String getCannotStartReason() {
		if (isRunning())
			return "This entity (" + this + ") cannot be started because it is already running.";
		if (isTerminated())
			return "This entity (" + this + ") cannot be started because it is terminated (" + this.getRunnableState() + "). Use doRestart() instead.";
		if (!hasParentRec())
			return "This entity (" + this + ") cannot be started because it has no parent, or one of its ancestors has a null parent.";
		if ((getParent() instanceof RunnableEntity) && !((RunnableEntity) getParent()).isRunning())
			return "This entity (" + this + ") cannot be started because its parent is not running.";
		return "This entity (" + this + ") cannot be started.";
	}

	@Override
	public RunnableState getRunnableState() {
		return this.runnableState;
	}

	protected void setRunnableState(RunnableState state) {
		if (state == this.runnableState)
			return;
		final RunnableState old = this.runnableState;
		this.runnableState = state;
		notify(CoreNotificationCodes.RUNNABLE_STATE_CHANGED, null);

		if (state == RunnableState.RUNNING) {
			//lock parent change
			this.lockParentRec();
		} else if (old == RunnableState.RUNNING)
			this.unlockParentRec();
	}

	/**
	 * Returns <tt>true</tt> if current state is {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#RUNNING}
	 *
	 * @return <tt>true</tt> if this entity is in running state
	 */
	@Override
	public boolean isRunning() {
		return getRunnableState() == RunnableState.RUNNING;
	}

	/**
	 * Returns <tt>true</tt> if this entity's state is equal to one of these values:<ul>
	 * 	<li>{@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#COMPLETED};
	 * 	<li>{@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#CANCELED};
	 * 	<li>{@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#FAILED}.
	 * </ul>
	 *
	 * @return <tt>true</tt> if this entity is terminated
	 */
	@Override
	public boolean isTerminated() {
		return
				getRunnableState() == RunnableState.COMPLETED ||
				getRunnableState() == RunnableState.CANCELED ||
				getRunnableState() == RunnableState.FAILED
			;
	}

	@Override
	public void unplace() {
		setParent(null);
	}
}
