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
 * This interface defines an {@link Operation} that uses a {@link LongResource}.
 *
 * @since 1.2
 */
public interface LongOperation<Resource extends LongResource> extends Operation<Resource> {
	/**
	 * Returns the length of the operation in simulator units (see {@link Simulator} for details).
	 *
	 * @return the length of the operation
	 */
	public long getLength();

	/**
	 * Returns the maximum resource value that this operation can use (inclusive).
	 *
	 * <p><b>Default</b> value is {@code Long.MAX_VALUE}.
	 *
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates, computation speed etc..), the value returned here
	 * is assumed to be the maximum length that can be processed in one {@link Simulator#SECOND}.
	 *
	 * @return the maximum resource value that this operation can use (inclusive)
	 */
	public long getResourceMax();

	/**
	 * Sets the maximum resource value that this operation can use (inclusive).
	 *
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates, computation speed etc..), the value set here
	 * is assumed to be the maximum length that can be processed in one {@link Simulator#SECOND}.
	 *
	 * @param resourceMax
	 * @throws IllegalStateException if this operation is running
	 */
	public void setResourceMax(long resourceMax);

	/**
	 * Returns the minimum resource value that is needed by this operation to start (inclusive).
	 *
	 * <p>When starting the operation, if this operation receives a smaller promise than this value from its provisioner, then it fails.
	 *
	 * <p><b>Default</b> value is {@code 1l}.
	 *
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates, computation speed etc..), the value returned here
	 * is assumed to be the minimum length that can be processed in one {@link Simulator#SECOND}.
	 *
	 * @return the minimum resource value that is needed by this operation to start (inclusive).
	 */
	public long getResourceMin();

	/**
	 * Sets the minimum resource value needed by the operation to start (inclusive).
	 *
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates, computation speed etc..), the value set here
	 * is assumed to be the minimum length that can be processed in one {@link Simulator#SECOND}.
	 *
	 * @param resourceMin
	 * @throws IllegalStateException if this operation is running
	 */
	public void setResourceMin(long resourceMin);

	/**
	 * Returns the total length that was processed by the operation.
	 *
	 * <p>If the operation is running, then this does not include the length that
	 * was processed since the operation was started.
	 * Only the length that has been processed during previous runs is included.
	 *
	 * @return the total length that was processed by the operation
	 */
	public long getCompletedLength();
}
