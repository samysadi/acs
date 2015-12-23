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

import com.samysadi.acs.core.notifications.NotifierImpl;

/**
 * This abstract class implements some methods of the 
 * {@link Probe} interface, and offers protected methods
 * in order to ease subclass implementations.
 * 
 * @since 1.0
 */
public abstract class ProbeImpl<ValueType> extends NotifierImpl implements Probe<ValueType> {
	private Probed parent;

	public ProbeImpl() {
		super();
	}

	@Override
	public void setup(Probed parent) {
		if (!isDiscarded())
			throw new IllegalStateException("This probe is already setup");
		if (parent == null)
			throw new NullPointerException();
		this.parent = parent;
	}

	@Override
	public boolean isDiscarded() {
		return getParent() == null;
	}

	@Override
	public void discard() {
		this.parent = null;
	}

	@Override
	public final ProbeImpl<ValueType> clone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Probed getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (this.getParent() == null)
			s.append("null#" + hashCode());
		else
			s.append(this.getParent().toString());
		s.append('.').append(this.getKey());
		return s.toString();
	}
}
