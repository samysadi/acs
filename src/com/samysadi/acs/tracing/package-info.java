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

/**
 * This package contains probes definitions of the simulator.
 *
 * <p>The naming convention for a probe's class is set so that each
 * probe's class name contains a prefixthat is completely lowercase except for the first letter.
 * Based on that prefix, probes are classified into different sub-packages of which names
 * are is that prefix (in lower case).
 *
 * <p>So for example, a probe name CpEnergyProbe will be put in the sub-package <i>cp</i>
 * with all probes that has the same prefix.
 *
 * <p>This package contains the following sub-packages:<ul>
 * 		<li>cp: contains cloud provider probes;
 * 		<li>entity: contains entity probes;
 * 		<li>host: contains host probes;
 * 		<li>job: contains job probes;
 * 		<li>mz: contains memory zones probes, whether it be StorageFiles or RamZones;
 * 		<li>sim: contains simulator probes;
 * 		<li>user: contains user probes;
 * 		<li>vm: contains virtual machine probes.
 * </ul>
 *
 * @since 1.0
 */
package com.samysadi.acs.tracing;
