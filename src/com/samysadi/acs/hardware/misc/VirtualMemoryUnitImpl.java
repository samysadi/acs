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
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.Bitmap;

/** 
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class VirtualMemoryUnitImpl<Zone extends MemoryZone, Unit extends MemoryUnit<Zone>> extends MemoryUnitImpl<Zone> implements VirtualMemoryUnit<Zone, Unit> {
	private MetaData metaData;
	private MemoryMaps memoryMap;
	private boolean isAllocatedFlag;
	private boolean isReserveCapacity;
//	/**
//	 * Listener for notifications on parent memory unit.<br/>
//	 * <br/>
//	 * We could get rid of this field and iterate on the parent's listeners when
//	 * we need to discard the listener.<br/>
//	 */
//	private NotificationListener parentListener;

	public VirtualMemoryUnitImpl(long capacity) {
		super(capacity);
		this.setMetaData(new MetaData());
		this.memoryMap = null;
		this.isReserveCapacity = false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public VirtualMemoryUnitImpl<Zone, Unit> clone() {
		final VirtualMemoryUnitImpl<Zone, Unit> clone = (VirtualMemoryUnitImpl<Zone, Unit>) super.clone();
		if (this.metaData != null)
			clone.metaData = this.metaData.clone();
		if (this.memoryMap != null)
			clone.memoryMap = this.memoryMap.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.isAllocatedFlag = false;
//		this.parentListener = null;
	}

	@Override
	public void setCapacity(long capacity) {
		if (getParent() != null && this.isReserveCapacity) {
			final long deltaSize = capacity - super.getCapacity();
			if (deltaSize > 0)
				getParent().allocate(deltaSize);
			else
				getParent().free(-deltaSize);
		}

		super.setCapacity(capacity);
	}

	@Override
	public boolean isCapacityReserved() {
		return this.isReserveCapacity;
	}

	@Override
	public void setIsCapacityReserved(boolean v) {
		if (this.isReserveCapacity == v)
			return;

		if (getParent() != null) {
			final long delta = this.getCapacity() - this.getSize();
			if (v)
				getParent().allocate(delta);
			else
				getParent().free(delta);
		}

		this.isReserveCapacity = v;
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);
		if (oldParent != null) {
//			//remove the global listener
//			this.parentListener.discard();
//			this.parentListener = null;
			((MemoryUnit<?>)oldParent).free(this.isCapacityReserved() ? getCapacity() : getSize());
		}
		if (getParent() != null) {
//			this.parentListener = new MyNotificationListener();
//			getParent().addGlobalListener(this.parentListener);
			getParent().allocate(this.isCapacityReserved() ? getCapacity() : getSize());
		}
	}

//	protected class MyNotificationListener extends NotificationListener {
//		public MyNotificationListener() {
//			super();
//		}
//
//		@Override
//		protected void notificationPerformed(Notifier notifier,
//				int notification_code, Object data) {
//			if (notifier != VirtualMemoryUnitImpl.this.getParent())
//				this.discard();
//			else
//				VirtualMemoryUnitImpl.this.notify(notification_code, data);
//		}
//
//		public VirtualMemoryUnitImpl<Zone, Unit> getVirtualMemoryUnit() {
//			return VirtualMemoryUnitImpl.this;
//		}
//	}

	@SuppressWarnings("unchecked")
	@Override
	public Unit getParent() {
		return (Unit) super.getParent();
	}

	@Override
	public Host getParentHost() {
		if (getParent() == null)
			return null;
		return getParent().getParentHost();
	}

	@Override
	public void setSize(long newSize) {
		throw new UnsupportedOperationException("You cannot change this VirtualMemoryUnit capacity after it is created. Maybe you wanted to use allocate and free?");
	}

	@Override
	public long getRealFreeCapacity() {
		long c = getFreeCapacity();
		if ((getParent() == null) || isCapacityReserved())
			return c;
		return Math.min(c, getParent().getFreeCapacity() +
				getSize() - (getCapacity() - c) //size that can be allocated inside virtual memory unit without growing its size on its parent
			);
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

	@Override
	public boolean supportsFailureStateUpdate() {
		return false;
	}

	@Override
	public boolean isAllocated() {
		return isAllocatedFlag;
	}

	@Override
	public void setAllocated(boolean b) {
		if (b == isAllocatedFlag)
			return;
		isAllocatedFlag = b;
		notify(NotificationCodes.ENTITY_ALLOCATED_FLAG_CHANGED, null);
	}
}
