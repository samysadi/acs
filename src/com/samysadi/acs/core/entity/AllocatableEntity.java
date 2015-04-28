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

package com.samysadi.acs.core.entity;

import com.samysadi.acs.core.notifications.CoreNotificationCodes;

/**
 * An entity that can be allocated for sole and unique use by another entity.
 * 
 * <p>It offers two methods, one to check whether this entity is allocated or not,
 * and another to update that flag.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface AllocatableEntity extends Entity {
	/**
	 * Returns <tt>true</tt> if this Entity is allocated, usually for sole use by another {@link Entity}).
	 * 
	 * <p>By default this entity is not allocated (ie: this method returns false).
	 * 
	 * @return <tt>true</tt> if this Entity is allocated
	 * @see AllocatableEntity#setAllocated
	 */
	public boolean isAllocated();

	/**
	 * Updates the allocated flag of this entity.
	 * 
	 * <p>A {@link CoreNotificationCodes#ENTITY_ALLOCATED_FLAG_CHANGED} notification is thrown.
	 * 
	 * @param b
	 */
	public void setAllocated(boolean b);
}
