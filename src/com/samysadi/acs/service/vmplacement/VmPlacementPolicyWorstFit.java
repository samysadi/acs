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


/**
 * A placement policy that chooses the host that has enough resources for the virtual machine
 * among all available and powered on hosts.<br/>
 * The host is chosen according to the worst fit method.<br/>
 * If none is found then a new host is powered on.
 *
 * @since 1.0
 */
public class VmPlacementPolicyWorstFit extends VmPlacementPolicyBestFit {

	public VmPlacementPolicyWorstFit() {
		super();
	}

	@Override
	protected boolean isHostScoreBetter(double newScore, double compareToScore) {
		return Double.compare(newScore, compareToScore) > 0;
	}
}
