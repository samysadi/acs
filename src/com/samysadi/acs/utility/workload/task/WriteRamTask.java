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

package com.samysadi.acs.utility.workload.task;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.workload.Workload;

/**
 * Writes a maximum of the given <i>Size</i> in {@link Simulator#MEBIBYTE}
 * starting at the given <i>Pos</i> (ie: offset in {@link Simulator#MEBIBYTE})
 * in the Ram.<br/>
 * The <i>Pos</i> configuration can be omitted, in which case it is assumed to be the last
 * modified index from last call to this method (or <tt>0</tt> if it's the first call, or if the end of the ram is reached).
 *
 * <p>If the size cannot be allocated (not enough ram) then this task fails.<br/>
 * Use the <i>TrySize</i> configuration instead of <i>Size</i>
 * so that this task will not try to allocate more than available Ram.
 *
 * @since 1.0
 */
public class WriteRamTask extends TaskImpl {
	public WriteRamTask(Workload workload, Config config) {
		super(workload, config);
	}

	private static final Object PROP_LAST_ID = new Object();

	@Override
	public void execute() {
		if (this.isExecuting())
			return;

		if (this.isDone()) {
			success();
			return;
		}

		final RamZone zone = getWorkload().getRamZone();
		if (zone == null) {
			fail("RamZone could not be allocated.");
			return;
		}

		boolean canChgPos = false;
		long pos;
		{
			Long v = FactoryUtils.generateLong("Pos", getConfig(), null);
			if (v == null) {
				v = (Long) getWorkload().getProperty(PROP_LAST_ID);
				pos = (v == null) ? 0l : v;
				canChgPos = true;
			} else
				pos = v * Simulator.MEBIBYTE;
		}

		boolean canTrimSize = false;
		Long size = FactoryUtils.generateLong("Size", getConfig(), null);
		if (size == null) {
			size = FactoryUtils.generateLong("TrySize", getConfig(), null);
			if (size == null) {
				fail("Ram size was not given.");
				return;
			}
			canTrimSize = true;
		}

		size = size * Simulator.MEBIBYTE;

		long lastOffset = pos + size;
		long d = lastOffset - zone.getSize();
		if (canChgPos && zone.getParent().getFreeCapacity() < d) {
			pos = 0l;
			lastOffset = size;
			d = lastOffset - size;
		}
		if (zone.getParent().getFreeCapacity() < d) {
			if (canTrimSize) {
				lastOffset = zone.getSize() + zone.getParent().getFreeCapacity();
			} else {
				fail("Not enough free ram (pos=" + Simulator.formatSize(pos) +", size=" + Simulator.formatSize(size) + ").");
				return;
			}
		}

		if (pos >= lastOffset) {
			success(); //avoid 0 length operation
			return;
		}

		zone.setSize(lastOffset);
		zone.modify(pos, lastOffset - pos);

		getWorkload().setProperty(PROP_LAST_ID, lastOffset);

		success();
	}

	@Override
	public void interrupt() {
		//nothing
	}

	@Override
	public boolean isExecuting() {
		return false;
	}

}
