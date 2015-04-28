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

package com.samysadi.acs_test.utility.factory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs_test.Utils;


/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class FactoryTest {

	private static Object getDefaultPrimitive(Class<?> clazz) {
		if (clazz.equals(boolean.class))
			return false;
		else if (clazz.equals(byte.class))
			return (byte) 0;
		else if (clazz.equals(short.class))
			return (short) 0;
		else if (clazz.equals(int.class))
			return 0;
		else if (clazz.equals(long.class))
			return 1l;
		else if (clazz.equals(float.class))
			return 0f;
		else if (clazz.equals(double.class))
			return 0d;
		else
			throw new IllegalArgumentException("Unkown primitive class: "
					+ clazz);
	}

	private static Object getResult(String name, Simulator simulator) {
		for (Method m: Factory.getFactory(simulator).getClass().getMethods())
			if (Modifier.isPublic(m.getModifiers()) && m.getName().equals(name))
				return getResult(m, simulator);
		return null;
	}

	private static Object getResult(Method m, Simulator simulator) {
		Object[] params = new Object[m.getParameterTypes().length];
		Object o;
		try {
			int i = 0;
			for (Class<?> t : m.getParameterTypes()) {
				if (t.isPrimitive())
					params[i++] = getDefaultPrimitive(t);
				else if (t.equals(Simulator.class))
					params[i++] = simulator;
				else if (t.equals(Factory.class))
					params[i++] = Factory.getFactory(simulator);
				else if (t.equals(Config.class))
					params[i++] = simulator.getConfig();
				else if (t.equals(String.class))
					params[i++] = "";
				else if (!Entity.class.equals(t) && Entity.class.isAssignableFrom(t))
					params[i++] = getResult("new" + t.getSimpleName(), simulator);
				else
					params[i++] = null;
			}
			o = m.invoke(Factory.getFactory(simulator), params);
		} catch (Exception e) {
			System.err.println("Exception when invoking " + m.getName());
			e.printStackTrace();
			return null;
		}
		return o;
	}

	@Test
	public void test0() {
		HashSet<String> noTest = new HashSet<String>(Arrays.asList(
					"newSimulator", "newProbe", "newTrace"
				));

		Simulator simulator = Utils.newSimulator();
		for (Method m : Factory.class.getMethods()) {
			if (Modifier.isPublic(m.getModifiers())
					&& m.getName().startsWith("new")) {
				if (noTest.contains(m.getName()))
					continue;
				Object o = getResult(m, simulator);
				if (o == null) {
					System.out.println(m.getName() + " returned null");
					continue;
				}
				Assert.assertTrue(
						"Bad return type for " + m.getName(),
						m.getReturnType().isAssignableFrom(o.getClass())
					);
			}
		}

	}
}
