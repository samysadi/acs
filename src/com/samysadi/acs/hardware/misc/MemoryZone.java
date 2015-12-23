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

package com.samysadi.acs.hardware.misc;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.Bitmap;

/**
 * You can associate for each MemoryZone one or more bitmaps describing the 
 * modified space of the memory zone.
 * 
 * @since 1.0
 */
public interface MemoryZone extends Entity {

	@Override
	public MemoryZone clone();

	@Override
	public MemoryUnit<?> getParent();

	/**
	 * Returns the size (in number of {@link Simulator#BYTE}s) taken by this zone on the parent memory unit.
	 * 
	 * @return the size (in number of {@link Simulator#BYTE}s) taken by this zone on the parent memory unit
	 */
	public long getSize();

	/**
	 * Updates the size (in number of {@link Simulator#BYTE}s) of this zone and allocates/frees
	 * the difference in the parent memory unit.
	 * 
	 * <p>Throws a {@link NotificationCodes#MZ_SIZE_CHANGED} notification.
	 * 
	 * @param newSize new size of the zone
	 */
	public void setSize(long newSize);

	/**
	 * Returns a MetaData object representing the meta-data associated to this memory zone.
	 * 
	 * <p>This object may contain for example a filename, a file version_id, modification/creation dates etc..
	 * It is left to the user discretion to choose which meta-data is important and should be saved for the simulation.
	 * 
	 * @return a MetaData object representing the meta-data associated to this memory zone
	 */
	public MetaData getMetaData();

	/**
	 * Updates the meta-data associated with the current zone.<br/>
	 * A {@link NotificationCodes#MZ_METADATA_CHANGED} notification is also thrown.
	 * @param metaData
	 */
	public void setMetaData(MetaData metaData);

	/**
	 * Returns <tt>true</tt> if the given <tt>zone</tt> is the current zone or if it is a replica of the current zone.
	 * 
	 * <p>This method may be used, for instance, after migrations to find a replica for a particular memory zone in the new MemoryUnits of the new
	 * host.
	 * 
	 * <p>Default implementations uses {@link MemoryZone#getMetaData()} to check if the two zones contains the same data:<br/>
	 * <pre>{@code this.getMetaData().isSameData(zone.getMetaData());}</pre>
	 * A special case if one of the meta-data is <tt>null</tt>, is to return <tt>false</tt>.
	 * 
	 * @param zone
	 * @return <tt>true</tt> if the given <tt>zone</tt> is the current zone or if it is a replica of the current zone
	 */
	public boolean isReplicaOf(MemoryZone zone);

	/**
	 * Returns the user defined memory map with the given <tt>id</tt>.
	 * 
	 * <p>If none is set, then a new one is created such that it marks all the memory as dirty.
	 * 
	 * @param id
	 * @return the user defined memory map with the given <tt>id</tt>
	 */
	public Bitmap getMemoryMap(Object id);

	/**
	 * Removes the memory map that matches the given <tt>id</tt>.
	 * 
	 * @param id
	 */
	public void removeMemoryMap(Object id);

	/**
	 * Call this method to mark as modified the delimited area in this zone by <b><tt>pos</tt></b> (inclusive) and <b><tt>pos+size</tt></b> (exclusive).
	 * 
	 * <p>After calling this, all the memory maps inside of this memory zone are updated accordingly (the delimited memory is marked as dirty).<br/>
	 * Also, the {@link MetaData} related to this {@link MemoryZone} is updated accordingly (the zone's version is updated).
	 * 
	 * <p>A {@link NotificationCodes#MZ_MODIFIED} notification is also thrown.
	 * 
	 * @param pos the position from which the modification starts
	 * @param size the count of bytes that were modified (starting from <tt>pos</tt>)
	 */
	public void modify(long pos, long size);
}
