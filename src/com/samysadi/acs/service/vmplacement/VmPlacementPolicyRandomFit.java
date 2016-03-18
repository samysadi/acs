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

package com.samysadi.acs.service.vmplacement;

import java.util.Iterator;
import java.util.List;

import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.utility.collections.ShuffledIterator;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * A placement policy that chooses the host that has enough resources for the virtual machine
 * among all available and powered on hosts.<br/>
 * The host is chosen according to the random fit method.<br/>
 * If none is found then a new host is powered on.
 *
 * @since 1.0
 */
public class VmPlacementPolicyRandomFit extends VmPlacementPolicyAbstract {
	public VmPlacementPolicyRandomFit() {
		super();
	}

	@Override
	protected Host _selectHost(VirtualMachine vm, List<Host> poweredOnHosts, List<Host> excludedHosts) {
		Iterator<Host> it = new ShuffledIterator<Host>(poweredOnHosts);
		while (it.hasNext()) {
			final Host candidate = it.next();
			if (excludedHosts != null && excludedHosts.contains(candidate))
				continue;
			if (candidate.getPowerState() == PowerState.ON) {
				final double s = computeHostScore(vm, candidate);
				if (s>0)
					return candidate;
			}
		}
		return null;
	}
}
