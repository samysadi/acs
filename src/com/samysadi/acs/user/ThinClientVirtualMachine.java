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

package com.samysadi.acs.user;

import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * A thin client VM is different from regular VMs because you
 * cannot allocate some resources types for it (virtual ram, virtual storage, processing units).<br/>
 * If you try to allocate such resources you will get an UnsupportedOperationException.
 *
 * <p>Also, a thin client VM user is always the same as the {@link ThinClient}'s parent.<br/>
 * If you try to set a user for a thin client VM, then the method call is ignored.
 *
 * @since 1.0
 */
public interface ThinClientVirtualMachine extends VirtualMachine {

	@Override
	public ThinClient getParent();

	@Override
	public ThinClientVirtualMachine clone();
}
