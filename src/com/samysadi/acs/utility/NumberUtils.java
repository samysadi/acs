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
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class NumberUtils {
	private NumberUtils() { }

	/**
	 * Use it! so you don't mess around with Double precision thing
	 */
	public static final double EPSILON = 1E-6;

	private static final double epsilon_zero_double = EPSILON * Double.MIN_NORMAL;
	/**
	 * Returns -1 if {@code a < b}, 0 if {@code a == b} and 1 if {@code a > b}.
	 * 
	 * Don't use this in Comparable. The relation is not transitive
	 * you can find x,y,z such as {@code compareDoubles(x,y)==0}, {@code compareDoubles(y,z)==0} but 
	 * {@code compareDoubles(x,z)>0}
	 * 
	 * @return -1 if {@code a < b}, 0 if {@code a == b} and 1 if {@code a > b}
	 */
	public static int compareDoubles(double a, double b) {
		if (a == b)
			return 0;
		double sdiff = a - b;
		double diff = Math.abs(sdiff);
		if ((a == 0 || b == 0 || diff < Double.MIN_NORMAL) 
				&& (diff < epsilon_zero_double))
			return 0;
		else if (diff/(Math.abs(a) + Math.abs(b)) < EPSILON)
			return 0;
		else if (sdiff < 0)
			return -1;
		else
			return 1;
	}

}
