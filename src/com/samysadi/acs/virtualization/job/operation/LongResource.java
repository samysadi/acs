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

import com.samysadi.acs.core.Simulator;


/**
 * Defines a resource that contains a long as its main value.
 * 
 * <p>This resource maybe used to describe BW, MIPS, TransferRate etc.. When it does, then
 * the long value describes how much length (bytes, instructions ..) can be processed
 * per one Unit of Time which is equal to {@link Simulator#SECOND}.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class LongResource implements Cloneable {
	/**
	 * Returns a long representation of this resource.
	 * 
	 * @return a long representation of this resource
	 */
	public abstract long getLong();

	@Override
	public LongResource clone() {
		final LongResource clone;
		try {
			clone = (LongResource) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		return clone;
	}

	/**
	 * Creates and returns a clone of this resource.
	 * The created clone's value is updated accordingly to the new given <tt>value</tt>.
	 * 
	 * @param value
	 * @return Cloned resource with given <tt>value</tt>
	 */
	public abstract LongResource clone(long value);

	/**
	 * Returns the unit of time associated with this resource.
	 * 
	 * <p>This is used to attach a time unit for resources (to represent BW, MIPS, TransferRate etc..).
	 * 
	 * <p><b>Default</b> is {@link Simulator#SECOND}.
	 * 
	 * @return the unit of time associated with this resource
	 */
	public final long getUnitOfTime() {
		return Simulator.SECOND;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(getLong()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LongResource))
			return false;
		return Long.valueOf(getLong()).equals(((LongResource)obj).getLong());
	}

	@Override
	public String toString() {
		return String.valueOf(getLong());
	}
}
