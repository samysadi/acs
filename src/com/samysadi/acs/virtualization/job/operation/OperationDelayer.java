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

/**
 * This interface defines an operation delayer which is used to determine
 * when an operation should be delayed.
 * @param <O>
 *
 * @since 1.2
 */
public interface OperationDelayer<O extends LongOperation<?>> {
	/**
	 * Returns a long value indicating at which length the given operation
	 * should be delayed.
	 *
	 * <p>Returning a value which is equal to the operation's currently completed length,
	 * means that the operation should be delayed immediately.
	 *
	 * <p>Returning a negative value, a value strictly smaller than currently completed length,
	 * or a value greater than the operation length, means that the operation
	 * will never be delayed.
	 *
	 * @param operation
	 * @return a long value indicating at which length the given operation
	 * should be delayed.
	 */
	public long getNextLength(O operation);
}
