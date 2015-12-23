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
import java.util.NoSuchElementException;
import java.util.Random;

import com.samysadi.acs.core.Simulator;

/**
 * This iterator sets a shuffled order to the elements of the underlying list using 
 * a supplied {@link Random}.
 * 
 * <p>This implementation does not make any verification regarding if the underlying list is modified.
 * 
 * @since 1.0
 */
public class ShuffledIterator<T> implements Iterator<T> {
	private int[] indices;
	private int size;
	private List<T> collection;
	private Random generator;

	public ShuffledIterator(List<T> collection) {
		this(collection, Simulator.getSimulator().getRandomGenerator());
	}

	public ShuffledIterator(List<T> collection, Random generator) {
		super();

		this.collection = collection;
		this.size = collection.size();
		this.generator = generator;

		this.indices = new int[this.size];
		for (int i=0; i<this.size; i++)
			this.indices[i] = i;
	}

	@Override
	public boolean hasNext() {
		return this.size > 0;
	}

	@Override
	public T next() {
		if (this.size == 0)
			throw new NoSuchElementException();
		int n = this.generator.nextInt(this.size);
		this.size--;
		int z = this.indices[n];
		this.indices[n] = this.indices[this.size];
		return collection.get(z);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
