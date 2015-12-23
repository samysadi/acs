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

package com.samysadi.acs.hardware;

import java.util.List;

import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.ram.Ram;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.utility.collections.infrastructure.Rack;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public interface Host extends NetworkDevice {

	@Override
	public Host clone();

	/**
	 * Returns the rack to whom this Host belongs to.
	 * 
	 * @return the rack to whom this Host belongs to
	 */
	public Rack getRack();

	/**
	 * Returns the ram of this host.
	 * 
	 * @return the ram of this host
	 */
	public Ram getRam();

	/**
	 * Returns the list of virtual machines that are contained within this host.
	 * 
	 * @return the list of virtual machines that are contained within this host
	 */
	public List<VirtualMachine> getVirtualMachines();

	/**
	 * Returns the list of processing units contained within this host.
	 * 
	 * @return the list of processing units contained within this host
	 */
	public List<ProcessingUnit> getProcessingUnits();

	/**
	 * Returns the list of storages that this host contains.
	 * 
	 * @return the list of storages that this host contains
	 */
	public List<Storage> getStorages();
}
