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
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;

/**
 * The strategy employed is to choose the {@link ProcessingUnit} that gives a 
 * higher promise (ie: the less used PU).
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class PuAllocatorDefault extends EntityImpl implements PuAllocator {

	public PuAllocatorDefault() {
		super();
	}

	@Override
	public PuAllocatorDefault clone() {
		final PuAllocatorDefault clone = (PuAllocatorDefault) super.clone();
		return clone;
	}

	@Override
	public VirtualMachine getParent() {
		return (VirtualMachine) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof VirtualMachine))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public ProcessingUnit chooseProcessingUnit(ComputingOperation op) {
		ProcessingUnit bestPu = null;
		long bestMips = -1;
		for (ProcessingUnit pu:getParent().getUsableProcessingUnits()) {
			long avg = pu.getComputingProvisioner().getResourcePromise(op).getMips();
			if (avg > bestMips) {
				bestPu = pu;
				bestMips = avg;
			}
		}
		return bestPu;
	}
}
