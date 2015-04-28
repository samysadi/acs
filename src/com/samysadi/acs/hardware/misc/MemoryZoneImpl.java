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

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.Bitmap;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class MemoryZoneImpl extends EntityImpl implements MemoryZone {
	private MetaData metaData;
	private MemoryMaps memoryMap;
	private long size;

	/**
	 * Creates a new Memory Zone and allocates its initial size in the memory unit.
	 * 
	 * @param parent the ram
	 * @param size the initial size of this zone
	 */
	protected MemoryZoneImpl(long size) {
		super();
		this.size = 0l;
		this.setSize(size);
		this.setMetaData(new MetaData());
		this.memoryMap = null;
	}

	@Override
	public MemoryZoneImpl clone() {
		final MemoryZoneImpl clone = (MemoryZoneImpl) super.clone();
		if (this.metaData != null)
			clone.metaData = this.metaData.clone();
		if (this.memoryMap != null)
			clone.memoryMap = this.memoryMap.clone();
		return clone;
	}

	@Override
	public MetaData getMetaData() {
		return metaData;
	}

	@Override
	public void setMetaData(MetaData metaData) {
		if (this.metaData == metaData)
			return;

		this.metaData = metaData;
		notify(NotificationCodes.MZ_METADATA_CHANGED, null);
	}

	@Override
	public boolean isReplicaOf(MemoryZone zone) {
		if (zone == this)
			return true;
		if (this.getMetaData() == null || zone.getMetaData() == null)
			return false;
		return this.getMetaData().isSameData(zone.getMetaData());
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);
		if (oldParent != null)
			((MemoryUnit<?>)oldParent).free(getSize());
		if (getParent() != null)
			getParent().allocate(getSize());
	}

	@Override
	public MemoryUnit<?> getParent() {
		return (MemoryUnit<?>) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof MemoryUnit<?>))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public void setSize(long newSize) {
		if (this.size == newSize)
			return;
		else if (newSize < 0)
			throw new IllegalArgumentException("Negative size not allowed");
		if (getParent() != null) {
			long deltaSize = newSize - this.size;
			if (deltaSize > 0)
				getParent().allocate(deltaSize);
			else if (deltaSize < 0)
				getParent().free(-deltaSize);
		}

		this.size = newSize;
		notify(NotificationCodes.MZ_SIZE_CHANGED, null);
	}

	@Override
	public Bitmap getMemoryMap(Object id) {
		if (this.memoryMap == null)
			this.memoryMap = new MemoryMaps(1);
		return this.memoryMap.getBitmap(id, getSize());
	}

	@Override
	public void removeMemoryMap(Object id) {
		if (this.memoryMap == null)
			return;
		this.memoryMap.remove(id);
		if (this.memoryMap.size() == 0)
			this.memoryMap = null;
	}

	@Override
	public void modify(long pos, long size) {
		getMetaData().setVersionId(getMetaData().getVersionId() + 1);
		if (this.memoryMap != null)
			for (Bitmap b: this.memoryMap.values())
				b.mark(pos, size);
		notify(NotificationCodes.MZ_MODIFIED, null);
	}
}
