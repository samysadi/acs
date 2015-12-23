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

package com.samysadi.acs.core.event;

/**
 * Dispensable events can, unlike other events, be ignored by the simulator
 * <b>if</b> there are only dispensable events remaining in the simulator queue.<br/>
 * In other words, simulation can be stopped if only dispensable events remain in the
 * simulator's events queue.
 * 
 * <p><b>Use case example:</b> You can use dispensable events, to schedule future device failures. Such failures are
 * important to the simulation only if there are operations that depend on it. So if only
 * failure events remain in the simulator's event queue, there is no need to process them and simulation
 * can be terminated.
 * 
 * <p>Make your events implement this interface if you want to make them dispensable.
 * 
 * @since 1.0
 */
public interface DispensableEvent extends Event {

}
