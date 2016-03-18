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

package com.samysadi.acs.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.samysadi.acs.core.entity.EntityImpl;

/**
 * Use this class to change units precision.
 *
 * <p>{@link Simulator} must not be loaded when calling this class.
 *
 * @since 1.0
 */
public class SimulatorUnits extends EntityImpl {
	private static long Millisecond			= 1000;
	private static long Byte				= 1;
	private static long Mi					= 1;
	private static long CurrencyUnit		= 1000000000;
	private static String CurrencyCode		= "USD";
	private static String CurrencySymbol	= "$";
	private static long Watt				= 1000;

	private static void checkIsSimulatorLoaded() {
		boolean ok = false;

		Method m;
		try {
			m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);

			boolean old = m.isAccessible();
			m.setAccessible(true);
			try {
				if(m.invoke(ClassLoader.getSystemClassLoader(), SimulatorUnits.class.getPackage().getName() + ".Simulator") == null)
					ok = true;
			} finally {
				m.setAccessible(old);
			}
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InvocationTargetException e) {
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}

		if (!ok)
			throw new IllegalStateException("You cannot change simulator units once the Simulator class is initialized.");
	}


	public static long getMillisecond() {
		return Millisecond;
	}


	public static void setMillisecond(long millisecond) {
		checkIsSimulatorLoaded();
		Millisecond = millisecond;
	}


	public static long getByte() {
		return Byte;
	}


	public static void setByte(long b) {
		checkIsSimulatorLoaded();
		Byte = b;
	}


	public static long getMi() {
		return Mi;
	}


	public static void setMi(long mi) {
		checkIsSimulatorLoaded();
		Mi = mi;
	}


	public static long getCurrencyUnit() {
		return CurrencyUnit;
	}


	public static void setCurrencyUnit(long currencyUnit) {
		checkIsSimulatorLoaded();
		CurrencyUnit = currencyUnit;
	}


	public static String getCurrencyCode() {
		return CurrencyCode;
	}


	public static void setCurrencyCode(String currencyCode) {
		checkIsSimulatorLoaded();
		CurrencyCode = currencyCode;
	}


	public static String getCurrencySymbol() {
		return CurrencySymbol;
	}


	public static void setCurrencySymbol(String currencySymbol) {
		checkIsSimulatorLoaded();
		CurrencySymbol = currencySymbol;
	}


	public static long getWatt() {
		return Watt;
	}


	public static void setWatt(long watt) {
		checkIsSimulatorLoaded();
		Watt = watt;
	}
}
