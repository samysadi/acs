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

package com.samysadi.acs.utility.collections.infrastructure;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @since 1.0
 */
abstract class MyArrayList<E> extends ArrayList<E> {
	private static final long serialVersionUID = 1L;
	private int childModCount = 0;

	public MyArrayList() {
		super();
	}

	public MyArrayList(Collection<? extends E> c) {
		super(c);
	}

	public MyArrayList(int initialCapacity) {
		super(initialCapacity);
	}

	public abstract MyArrayList<?> getParent();

	@Override
	public E set(int index, E element) {
		_modified();
		return super.set(index, element);
	}

	@Override
	public boolean add(E e) {
		_modified();
		return super.add(e);
	}

	@Override
	public void add(int index, E element) {
		_modified();
		super.add(index, element);
	}

	@Override
	public E remove(int index) {
		_modified();
		return super.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		_modified();
		return super.remove(o);
	}

	@Override
	public void clear() {
		_modified();
		super.clear();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		_modified();
		return super.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		_modified();
		return super.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		_modified();
		return super.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		_modified();
		return super.retainAll(c);
	}

	private void _modified() {
		if (getParent() != null)
			getParent().childUpdated(1);
	}

	private static final int MODIF_BITS_PER_LEVEL = 7;
	private static final int MODIF_BITS_MAX_LEVEL = 32 / 7;

	protected void childUpdated(int inc) {
		int new_inc = inc << MODIF_BITS_PER_LEVEL;
		childModCount = ((childModCount + inc) & (new_inc-1)) |
				childModCount &  (~(new_inc-1));
		if (getParent() != null)
			getParent().childUpdated(new_inc);
	}

	/**
	 * Returns a mod count that changes every time a child at the given level
	 * or lesser level is modified.
	 *
	 * <p>Level 0 contains direct child elements and level 1 contains grandchildren.
	 *
	 * <p>Basically, every time a child is modified, it notifies its parent, which in turn notifies its own parents and so forth.<br/>
	 * So if you call this method with a level of 2, then the returned mod_count will only reflect modifications that were done on children
	 * at level 0, 1 and 2. If a child is modified at level 3 or more, this mod_count should not change.
	 *
	 * <p><b>Note</b> this is a best effort mod_count that you may use to detect concurrent modifications on children
	 * elements (ie: modification on the elements themselves, not the structure of this list). But you should not rely on this.
	 *
	 * @param level
	 * @return current mod count
	 */
	protected int getChildModCount(int level) {
		if (level >= MODIF_BITS_MAX_LEVEL)
			return childModCount;
		final int n = MODIF_BITS_PER_LEVEL * (level + 1);
		return childModCount & (n >= 32 ? 0xffffffff : ((1 << n) - 1));
	}

	/**
	 * Same as getChildModCount(Integer.MAX_VALUE)
	 */
	protected int getChildModCount() {
		return childModCount;
	}

}
