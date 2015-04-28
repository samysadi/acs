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

package com.samysadi.acs.utility.factory.generation.flow;


/**
 * This interface contains method to define when to generate
 * entities during the simulation, and how many entities need to be generated
 * at that moment.
 * 
 * <p>Factories that need to generate entities gradually as the simulation runs
 * may use this interface.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface GenerationFlow extends Cloneable {
	public GenerationFlow clone();

	/**
	 * Returns the {@link GenerationFlowInfo} containing the simulation delay before generating
	 * next entities, and how many entities need to be generated at that moment.
	 * 
	 * @return the next {@link GenerationFlowInfo}
	 */
	public GenerationFlowInfo next();
}
