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

package com.samysadi.acs.utility.collections.infrastructure;

import java.util.List;

import com.samysadi.acs.hardware.Host;


/**
 * A structure that contains a list of racks, and also offers
 * helper methods to build unmodifiable lists containing all
 * racks or hosts.
 *
 * @since 1.0
 */
public interface Cluster {

	/**
	 * Returns an unmodifiable list that contains all racks in this cluster.
	 *
	 * @return an unmodifiable list that contains all racks
	 */
	public List<Rack> getRacks();

	/**
	 * Returns an unmodifiable list that contains all hosts in all racks of this cluster.
	 *
	 * @return an unmodifiable list that contains all hosts
	 */
	public List<Host> getHosts();

	/**
	 * Returns the data-center that contains this cluster, or <tt>null</tt> if it is not set.
	 *
	 * @return the data-center that contains this cluster, or <tt>null</tt>
	 */
	public Datacenter getDatacenter();
}
