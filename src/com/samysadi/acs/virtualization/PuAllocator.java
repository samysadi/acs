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

package com.samysadi.acs.virtualization;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;

/**
 * This interface defines the strategy to use when a new {@link ComputingOperation} is 
 * being activated in order to select a {@link ProcessingUnit} among all available ones
 * inside the VM.
 * 
 * @since 1.0
 */
public interface PuAllocator extends Entity {

	@Override
	public PuAllocator clone();

	@Override
	public VirtualMachine getParent();

	/**
	 * Returns the best ProcessingUnit that should be used for the given computing operation.
	 * 
	 * @return the best ProcessingUnit that should be used for the given computing operation
	 */
	public ProcessingUnit chooseProcessingUnit(ComputingOperation com);
}
