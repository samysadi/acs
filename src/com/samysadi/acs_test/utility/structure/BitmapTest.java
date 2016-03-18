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

package com.samysadi.acs_test.utility.structure;

import org.junit.Assert;
import org.junit.Test;

import com.samysadi.acs.utility.collections.Bitmap;


/**
 *
 * @since 1.0
 */
public class BitmapTest {

	@Test
	public void test() {
		Bitmap m = new Bitmap();

		Assert.assertEquals(0, m.getMarkedSize());
		Assert.assertEquals(Bitmap.MAX_BITMAP_SIZE, m.getUnmarkedSize());
		Assert.assertFalse(m.isMarked(150));

		m.mark(100, 300);
		Assert.assertEquals(300, m.getMarkedSize());
		Assert.assertEquals(Bitmap.MAX_BITMAP_SIZE - 300, m.getUnmarkedSize());

		Assert.assertFalse(m.isMarked(99));
		Assert.assertFalse(m.isMarked(50));
		Assert.assertTrue(m.isMarked(100));
		Assert.assertTrue(m.isMarked(150));
		Assert.assertTrue(m.isMarked(399));
		Assert.assertFalse(m.isMarked(400));
		Assert.assertFalse(m.isMarked(450));

		m.mark(200, 300);
		m.unmark(0, 100);
		m.unmark(500, 100);
		Assert.assertEquals(400, m.getMarkedSize());
		Assert.assertEquals(Bitmap.MAX_BITMAP_SIZE - 400, m.getUnmarkedSize());
		Assert.assertFalse(m.isMarked(99));
		Assert.assertFalse(m.isMarked(50));
		Assert.assertTrue(m.isMarked(100));
		Assert.assertTrue(m.isMarked(150));
		Assert.assertTrue(m.isMarked(399));
		Assert.assertTrue(m.isMarked(400));
		Assert.assertTrue(m.isMarked(450));

		m.mark(0, 99);
		Assert.assertTrue(m.isMarked(0));
		Assert.assertTrue(m.isMarked(98));
		Assert.assertFalse(m.isMarked(99));
		Assert.assertTrue(m.isMarked(100));
		Assert.assertEquals(499, m.getMarkedSize());

		m.mark(501, 99);
		Assert.assertTrue(m.isMarked(501));
		Assert.assertFalse(m.isMarked(500));
		Assert.assertTrue(m.isMarked(499));
		Assert.assertEquals(598, m.getMarkedSize());

		m.unmark(50, 100);
		Assert.assertEquals(499, m.getMarkedSize());

		m.mark();
		Assert.assertEquals(Bitmap.MAX_BITMAP_SIZE, m.getMarkedSize());

		m.unmark();
		m.mark(10, 10);
		m.mark(20, 10);
		Assert.assertEquals(20, m.getMarkedSize());
		m.mark(10, 30);
		Assert.assertEquals(30, m.getMarkedSize());
		m.mark(0, 20);
		Assert.assertEquals(40, m.getMarkedSize());

		m.unmark();
		Assert.assertEquals(0, m.getMarkedSize());

		m.mark(50, 10);
		m.mark(10, 20);

		Bitmap m2 = new Bitmap();
		m2.mark(10, 30);

		m.mark(m2);
		Assert.assertEquals(40, m.getMarkedSize());

		m.unmark(m2);
		Assert.assertEquals(10, m.getMarkedSize());

		m2.unmark();
		m2.mark(40, 15);
		m.bitwiseOr(m2);
		Assert.assertEquals(20, m.getMarkedSize());

		m.mark(); m.unmark(30, 15); m.unmark(50, 5);
		m.bitwiseNot();
		Assert.assertEquals(20, m.getMarkedSize());

		m.unmark(); m.mark(40, 20);
		m2.unmark(); m2.mark(30, 15); m2.mark(50, 5);
		m.bitwiseAnd(m2);
		Assert.assertEquals(10, m.getMarkedSize());

		m.unmark(); m.mark(50, 20);
		m2.unmark(); m2.mark(40, 20);
		m.bitwiseXor(m2);
		Assert.assertEquals(20, m.getMarkedSize());
	}

	@Test
	public void test2() {
		final long P = 8;

		Bitmap m = new Bitmap();

		Assert.assertEquals(0, m.getMarkedPagesSize(P));

		m.mark(1,1);
		Assert.assertEquals(P, m.getMarkedPagesSize(P));

		m.mark(4,4);
		Assert.assertEquals(P, m.getMarkedPagesSize(P));

		m.mark(16,P+1);
		Assert.assertEquals(P*3, m.getMarkedPagesSize(P));

		m.mark(160,1);
		Assert.assertEquals(P*4, m.getMarkedPagesSize(P));

		System.out.println(m.toString());
	}

}
