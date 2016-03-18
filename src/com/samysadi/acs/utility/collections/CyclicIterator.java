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

package com.samysadi.acs.utility.collections;

import java.util.Iterator;
import java.util.List;

/**
 * This iterator will return elements in the same order as they are in the underlying list starting from a given
 * index. When reaching the end of the list, this iterator will return remaining elements starting from the beginning
 * of the list.
 *
 * <p>For example, if the list has 10 elements and the initial index is set to 4, then this iterator will return the
 * elements in the following order: {@code 4,5,6,7,8,9,0,1,2,3}.
 *
 * <p>A ConcurrentModificationException is thrown if the underlying list is modified during the iteration.
 *
 * @since 1.0
 */
public class CyclicIterator<T> implements Iterator<T> {
	private List<T> list;
	private Iterator<T> iterator;
	private int cursor;

	public CyclicIterator(List<T> list, int startIndex) {
		super();

		this.cursor = 0;
		this.list = list;

		this.iterator = this.list.listIterator(startIndex);
	}

	@Override
	public boolean hasNext() {
		return cursor < list.size();
	}

	@Override
	public T next() {
		cursor++;

		if (!this.iterator.hasNext())
			this.iterator = this.list.listIterator();

		return this.iterator.next();
	}

	@Override
	public void remove() {
		this.iterator.remove();
		cursor--;
	}
}
