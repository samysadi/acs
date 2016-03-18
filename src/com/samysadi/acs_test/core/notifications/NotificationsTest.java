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

package com.samysadi.acs_test.core.notifications;

import java.lang.reflect.Field;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import com.samysadi.acs.utility.NotificationCodes;


/**
 *
 * @since 1.0
 */
public class NotificationsTest {

	@Test
	public void test() throws IllegalArgumentException, IllegalAccessException {
		//make sure notification codes are unique
		HashSet<Integer> h = new HashSet<Integer>();
		for (Field f: NotificationCodes.class.getFields()) {
			if (f.getType() == int.class) {
				Integer i = Integer.valueOf(f.getInt(null));
				Assert.assertFalse("The value of the field: " + f.getName() + " is already used", h.contains(i));
				h.add(i);
			}
		}
	}

}
