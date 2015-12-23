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

package com.samysadi.acs.core.tracing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import com.samysadi.acs.core.notifications.NotifierImpl;


/**
 * <b>Note:</b>This implementation also extends {@link NotifierImpl}.
 * 
 * @since 1.0
 */
public abstract class ProbedImpl extends NotifierImpl implements Probed {
	private HashMap<String, Probe<?>> probes;

	@Override
	public ProbedImpl clone() {
		final ProbedImpl clone = (ProbedImpl) super.clone();

		clone.probes = null;

		return clone;
	}

	@Override
	public Probe<?> addProbe(Probe<?> probe) {
		if (probe == null)
			throw new NullPointerException();
		if (this.probes == null)
			 this.probes = new HashMap<String, Probe<?>>();
		Probe<?> r = this.probes.put(probe.getKey(), probe);
		if (probe.isDiscarded())
			probe.setup(this);
		else if (probe.getParent() != this) {
			probe.discard();
			probe.setup(this);
		}
		return r;
	}

	@Override
	public final Probe<?> getProbe(String probeKey) {
		return getProbe(probeKey, true);
	}

	@Override
	public Probe<?> getProbe(String probeKey, boolean create) {
		Probe<?> probe = null;
		if (this.probes != null) {
			probe = this.probes.get(probeKey);
			if (probe != null && probe.getParent() != this) {
				removeProbe(probeKey);
				probe = null;
			}
		}
		if (probe == null && create) {
			probe = newProbe(probeKey);
			if (probe != null)
				addProbe(probe); //will also setup the probe
		}
		return probe;
	}

	@Override
	public Collection<Probe<?>> getProbes() {
		if (this.probes == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableCollection(this.probes.values());
	}

	@Override
	public Probe<?> removeProbe(String probeKey) {
		if (this.probes == null)
			return null;
		Probe<?> probe = this.probes.remove(probeKey);
		if (probe != null)
			probe.discard();
		if (this.probes.isEmpty())
			this.probes = null;
		return probe;
	}

	@Override
	public Probe<?> removeProbe(Probe<?> probe) {
		return removeProbe(probe.getKey());
	}

	/**
	 * Creates and returns a new probe instance based on the given <tt>probeKey</tt>.
	 * 
	 * <p>This method is used in {@link Probed#getProbe(String, boolean)} when creating a new Probe.
	 * 
	 * @return a new Probe instance based on the given <tt>probeKey</tt>
	 */
	protected abstract Probe<?> newProbe(String probeKey);

}
