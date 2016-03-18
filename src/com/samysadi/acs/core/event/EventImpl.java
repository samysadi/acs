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
 *
 * @since 1.0
 */
public abstract class EventImpl implements Event {
	//use Long since a Long is anyhow instantiated by the Simulator
	private Long scheduledAtTime = null;

	public EventImpl() {
		super();
	}

	@Override
	public EventImpl clone() {
		final EventImpl clone;
		try {
			clone = (EventImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

		clone.scheduledAtTime = null;

		return clone;
	}

	@Override
	public Long getScheduledAt() {
		return this.scheduledAtTime;
	}

	@Override
	public boolean isScheduled() {
		return this.scheduledAtTime != null;
	}

	@Override
	public void scheduledAt(Long time) {
		this.scheduledAtTime = time;
	}

	@Override
	public void cancel() {
		if (!isScheduled())
			return;

		Simulator.getSimulator().cancel(this);
	}
}
