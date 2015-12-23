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

package com.samysadi.acs.utility;

/**
 * This generic class defines a pair containing two Objects.
 * 
 * @since 1.0
 */
public class Pair<O1, O2> {
	private O1 value1;
	private O2 value2;

	public Pair(O1 value1, O2 value2) {
		super();
		this.value1 = value1;
		this.value2 = value2;
	}

	public O1 getValue1() {
		return value1;
	}

	public void setValue1(O1 value1) {
		this.value1 = value1;
	}

	public O2 getValue2() {
		return value2;
	}

	public void setValue2(O2 value2) {
		this.value2 = value2;
	}

	@Override
	public int hashCode() {
		int hash = 23;
		hash = hash * 31 + ((getValue1() != null) ? getValue1().hashCode() : 0);
		hash = hash * 31 + ((getValue2() != null) ? getValue2().hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		else if (!(obj instanceof Pair))
			return false;
		else {
			Pair<?,?> p = (Pair<?,?>) obj;
			return
					((getValue1() != null) ? getValue1().equals(p.getValue1()) : p.getValue1() == null) &&
					((getValue2() != null) ? getValue2().equals(p.getValue2()) : p.getValue2() == null)
				;
		}
	}

	@Override
	public String toString() {
		return '{' + 
				((getValue1() != null) ? getValue1().toString() : "null") + ',' +
				((getValue2() != null) ? getValue2().toString() : "null") +
				'}';
	}
	
}
