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
package com.samysadi.acs.service.checkpointing.checkpoint;

import java.util.Iterator;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.misc.MemoryUnit;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.utility.collections.Bitmap;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * This implementation relies on its configuration.<br/>
 * Following configuration values can be set:<ul>
 * <li><b>UpdateOverhead</b> in {@link Simulator#MI}. Indicates the computing overhead which
 * is added during the checkpoint update. Default value is 0;
 * <li><b>UpdateOverheadIsBlocking</b> a boolean indicating if the checkpointed virtual machine should
 * be paused when computing for the update overhead. Default value is <tt>true</tt>;
 * <li><b>CompressionRatio</b> a floating point value indicating the compression ratio. The greater the value
 * is, the more the compression is effective. Default value is 1.0 which indicates that
 * there is no applied compression (ie: {@code compressionRatio * compressedSize = uncompressedSize});
 * <li><b>IsLiveUpdate</b> a boolean indicating whether the checkpoint updates should be live. A live
 * update contains a memory pre-copy phase where the virtual machine is not paused. After,
 * the pre-copy phase, the virtual machine is paused and a final phase where it is paused is engaged.
 * Enabling live update, may reduce the pause time of the virtual machine during the checkpoint.
 * Default value is <tt>true</tt>;
 * <li><b>LiveUpdateMemoryBlockSize</b> in {@link Simulator#MEBIBYTE}. Indicates the memory block
 * size during the pre-copy phase of the update. Default value is 10 mebibytes;
 * <li><b>LiveUpdateMaximumIterations</b> indicates the maximum number of iterations before
 * the pre-copy phase ends, and the final phase is engaged. Default value is 5;
 * <li><b>LiveUpdateProgressThreshold</b> in {@link Simulator#MEBIBYTE}. During pre-copy phase, if no progress
 * is made, the final phase is engaged where the virtual machine is paused. This value
 * is used to determine if progress is done or not. Default value is 10 mebibytes;
 * <li><b>RecoveryOverhead</b> in {@link Simulator#MI}. Indicates the computing overhead which
 * is added during the recovery process. Default value is 0;
 * <li><b>EnableStorageOperations</b> a boolean indicating whether the checkpoint should simulate
 * storage operations when accessing disk files. Default value is <tt>false</tt>.
 * </ul>
 * @since 1.2
 */
public class VmCheckpointDefault extends VmCheckpointAbstract {
	private static final Object CHECKPOINTING_MAP = new Object();

	protected UpdateInfo updateInfo;

	protected static class UpdateInfo {
		//used only during updates
		public long iterationCount;
		public long dirtyMemory0;
		public long dirtyMemory1;
		public int nextIndex;
		public boolean inRam;
		public long progMaxIt;
		public long progSizeThreshold;
	}

	public VmCheckpointDefault() {
		super();
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.updateInfo = null;
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);

		markAllMemoryDirty();
	}

	@Override
	protected boolean isStorageOperationsEnabled() {
		boolean v = false;
		if (getConfig() == null)
			return v;
		return getConfig().getBoolean("EnableStorageOperations", v);
	}

	@Override
	protected long computeCompressedSize(long size) {
		Double ratio = FactoryUtils.generateDouble("CompressionRatio", getConfig(), null);
		if (ratio == null)
			return size;
		return Math.round(size / ratio);
	}

	@Override
	protected long getUpdateOverhead() {
		return FactoryUtils.generateLong("UpdateOverhead", getConfig(), 0l) * Simulator.MI;
	}

	@Override
	protected boolean isUpdateOverheadBlocking() {
		boolean v = true;
		if (getConfig() == null)
			return v;
		return getConfig().getBoolean("UpdateOverheadIsBlocking", v);
	}

	@Override
	protected boolean isLiveUpdateEnabled() {
		boolean v = true;
		if (getConfig() == null)
			return v;
		return getConfig().getBoolean("IsLiveUpdate", v);
	}

	@Override
	protected boolean isLiveUpdateProgressing() {
		if (updateInfo.dirtyMemory0 - this.updateInfo.dirtyMemory1 < this.updateInfo.progSizeThreshold)
			return false;

		if (updateInfo.iterationCount > this.updateInfo.progMaxIt)
			return false;

		return true;
	}

	private void markAllMemoryDirty() {
		if (getParent() == null)
			return;
		//mark all memory zones as modified
		if (getParent().getVirtualRam() != null)
			for (MemoryZone z : getParent().getVirtualRam().getMemoryZones())
				z.removeMemoryMap(CHECKPOINTING_MAP);
		if (getParent().getVirtualStorage() != null)
			for (MemoryZone z : getParent().getVirtualStorage().getMemoryZones())
				z.removeMemoryMap(CHECKPOINTING_MAP);
	}

	private long computeDirtyMemory() {
		long r = 0l;
		if (getParent() == null)
			return r;
		if (getParent().getVirtualRam() != null)
			for (MemoryZone z : getParent().getVirtualRam().getMemoryZones())
				r+=z.getMemoryMap(CHECKPOINTING_MAP).getMarkedSize();
		if (getParent().getVirtualStorage() != null)
			for (MemoryZone z : getParent().getVirtualStorage().getMemoryZones())
				r+=z.getMemoryMap(CHECKPOINTING_MAP).getMarkedSize();
		return r;
	}

	private Iterator<? extends MemoryZone> getMzIterator() {
		MemoryUnit<? extends MemoryZone> mu;
		if (getParent().getVirtualRam() == null) {
			mu = getParent().getVirtualStorage();
			if (mu == null)
				return null;
			if (updateInfo.inRam) {
				this.updateInfo.inRam = false;
				this.updateInfo.nextIndex = 0;
			}
		} else if (getParent().getVirtualStorage() == null) {
			mu = getParent().getVirtualRam();
			if (mu == null)
				return null;
			if (!updateInfo.inRam) {
				this.updateInfo.inRam = true;
				this.updateInfo.nextIndex = 0;
			}
		} else
			mu = this.updateInfo.inRam ? getParent().getVirtualRam() : getParent().getVirtualStorage();
		return mu.getMemoryZones().listIterator(updateInfo.nextIndex);
	}

	private long getUpdatePhaseCheckpointSize(long maxLength) {
		this.updateInfo.dirtyMemory0 = this.updateInfo.dirtyMemory1;

		Iterator<? extends MemoryZone> it = getMzIterator();
		if (it == null) {
			this.updateInfo.dirtyMemory1 = 0;
			return 0l;
		}
		long total = maxLength;
		int b = 0;
		while (maxLength > 0) {
			if (!it.hasNext()) {
				this.updateInfo.inRam = !updateInfo.inRam;
				if (updateInfo.inRam)
					this.updateInfo.iterationCount++;
				it = getMzIterator();
				if (++b==3)
					break;
			} else {
				MemoryZone m = it.next();
				Bitmap map = m.getMemoryMap(CHECKPOINTING_MAP);

				maxLength -= map.getMarkedSize();

				map.unmark();
			}
		}

		total -= maxLength;

		this.updateInfo.dirtyMemory1 = total;

		return total;
	}

	protected long getUpdatePhaseCheckpointMaximumLength() {
		return FactoryUtils.generateLong("LiveUpdateMemoryBlockSize", getConfig(), 10l) * Simulator.MEBIBYTE;
	}

	@Override
	protected long getNextUpdatePhaseCheckpointSize() {
		return getUpdatePhaseCheckpointSize(getUpdatePhaseCheckpointMaximumLength());
	}

	@Override
	protected long getFinalUpdatePhaseCheckpointSize() {
		return getUpdatePhaseCheckpointSize(Long.MAX_VALUE);
	}

	@Override
	protected long computeCheckpointSize(long oldSize, long transferSize) {
		return Math.min((getParent().getVirtualRam() == null ? 0l : getParent().getVirtualRam().getSize()) +
				(getParent().getVirtualStorage() == null ? 0l : getParent().getVirtualStorage().getSize()),
				oldSize + transferSize);
	}

	@Override
	protected void beforeUpdate() {
		this.updateInfo = new UpdateInfo();

		this.updateInfo.dirtyMemory0 = Long.MAX_VALUE;
		this.updateInfo.dirtyMemory1 = computeDirtyMemory();
		this.updateInfo.iterationCount = 0;
		this.updateInfo.inRam = true;
		this.updateInfo.nextIndex = 0;
		this.updateInfo.progMaxIt = FactoryUtils.generateInt("LiveUpdateMaximumIterations", getConfig(), 5);
		this.updateInfo.progSizeThreshold = FactoryUtils.generateLong("LiveUpdateProgressThreshold", getConfig(), 10l) * Simulator.MEBIBYTE;
	}

	@Override
	protected void afterUpdate() {
		this.updateInfo = null;
	}

	@Override
	protected void afterUpdateError() {
		markAllMemoryDirty();

		this.updateInfo = null;
	}

	@Override
	protected long getRecoveryOverhead() {
		return FactoryUtils.generateLong("RecoveryOverhead", getConfig(), 0l) * Simulator.MI;
	}

	@Override
	protected void beforeRecover(Entity parent, VirtualMachine toReplace) {
		//nothing
	}

	@Override
	protected void afterRecover() {
		//nothing
	}

	@Override
	protected void afterRecoverError() {
		//nothing
	}

	@Override
	protected void beforeCopy(MemoryZone zone) {
		//nothing
	}

	@Override
	protected void afterCopy() {
		//nothing
	}

	@Override
	protected void afterCopyError() {
		//nothing
	}

}
