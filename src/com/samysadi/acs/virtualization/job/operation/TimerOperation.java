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

package com.samysadi.acs.virtualization.job.operation;

import com.samysadi.acs.core.entity.Entity;


/**
 * A operation that runs for a given delay, then becomes completed.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public final class TimerOperation extends LongOperationImpl<TimeResource> {
	private static final TimeResource timePromise = new TimeResource();
	
	/**
	 * Empty constructor that creates a zero-length operation.
	 * 
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.
	 * You should use {@link TimerOperation#TimerOperation(long)} though.
	 */
	public TimerOperation() {
		this(0);
	}

	public TimerOperation(long delay) {
		super(delay);
	}

	@Override
	public TimerOperation clone() {
		final TimerOperation clone = (TimerOperation) super.clone();
		return clone;
	}

	@Override
	protected TimeResource getProvisionerPromise() {
		if (timePromise.getLong() < getResourceMin() || timePromise.getLong() > getResourceMax())
			throw new IllegalStateException("Min / Max resource restrictions cannot be set for a TimerOperation.");
		if (timePromise.getLong() > getSynchronizedResource())
			throw new IllegalStateException("TimerOperation cannot be synchronized.");
		return timePromise;
	}

	@Override
	protected void grantAllocatedResource() {
		//nothing
	}

	@Override
	protected void revokeAllocatedResource() {
		//nothing
	}

	@Override
	protected void prepareActivation() {
		//nothing
	}

	@Override
	protected TimeResource computeSynchronizedResource(long delay) {
		return null;
	}
}
