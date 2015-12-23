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
public class FailureProneEntityImpl extends EntityImpl implements Entity, FailureProneEntity {
	private FailureState failureState;

	public FailureProneEntityImpl() {
		super();

		this.failureState = FailureState.OK;
	}

	@Override
	public FailureProneEntityImpl clone() {
		final FailureProneEntityImpl clone = (FailureProneEntityImpl) super.clone();
		return clone;
	}

	@Override
	public boolean supportsFailureStateUpdate() {
		return true;
	}

	@Override
	public FailureState getFailureState() {
		return failureState;
	}

	@Override
	public void setFailureState(FailureState failureState) {
		if (failureState == this.failureState)
			return;
		if (!supportsFailureStateUpdate())
			throw new UnsupportedOperationException();

		this.failureState = failureState;
		notify(CoreNotificationCodes.FAILURE_STATE_CHANGED, null);
	}
}
