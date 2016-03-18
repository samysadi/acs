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

/**
 * Defines a virtual machine which is for temporary usage only.
 *
 * <p>Temporary VMs are automatically created by the simulator and are
 * used for hosting jobs for file transfers for example. Such
 * VMs are not explicitly deployed by users. But they are needed to correctly
 * measure resources that were used by a particular user.
 *
 * <p>Because they are not created by users, temporary VMs are not used by placement
 * policies when placing user jobs.
 *
 * @since 1.0
 */
public interface TemporaryVirtualMachine extends VirtualMachine {

}
