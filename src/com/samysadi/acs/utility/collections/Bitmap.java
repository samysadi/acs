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
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import com.samysadi.acs.utility.Pair;


/**
 * This class keeps an optimized internal bitmap.
 *
 * <p>It offers basic operations like marking (ie: setting to 1) and unmarking (ie: setting to 0) bits. It also,
 * offers bitwise operations (NOT, AND, OR, XOR).
 *
 * <p>This implementation uses a {@link TreeMap} to keep a cheap representation of the bitmap.
 * It can contain a maximum of {@link Bitmap#MAX_BITMAP_SIZE} bits. So, if you try to mark
 * or unmark bits starting from an index that is beyond this limit you will get an exception.
 *
 * @since 1.0
 */
public class Bitmap implements Cloneable {

	public static final long MAX_BITMAP_SIZE = Long.MAX_VALUE;

	/**
	 * Keys are start indexes of marked zones,
	 * values are end indexes of marked zones (inclusive).<br/>
	 * (key, value) = (0, 9) means that bits number 0 until 9 (inclusive) are marked (or set to <tt>1</tt>)
	 */
	private TreeMap<Long, Long> map = null;

	/**
	 * Checks that length and start parameters are valid and returns a valid length.
	 *
	 * @return the old length, or the new length if it was too big
	 * @throws IllegalArgumentException if start or length are negative or if start is greater than or equal to {@link Bitmap#MAX_BITMAP_SIZE}
	 */
	private long boundaryCheck(long start, long length) {
		if (length != 0) {
			if (start < 0)
				throw new IllegalArgumentException("Negative indexes not allowed");
			else if (start >= MAX_BITMAP_SIZE)
				throw new IllegalArgumentException("Too big index");
			if (length < 0)
				throw new IllegalArgumentException("Length must be postive");
			else {
				long maxLength = MAX_BITMAP_SIZE - start;
				if (length > maxLength)
					length = maxLength;
			}
		}
		return length;
	}

	/**
	 * Marks (ie: sets to <tt>1</tt>) all the bits starting from the bit indexed
	 * with <b>{@code start}</b> (inclusive) to the bit indexed with <b>{@code start + length - 1}</b> (inclusive).
	 *
	 * @param start the index of the first bit to be marked
	 * @param length how many bits will be marked after the given <tt>start</tt> index.
	 * @throws IllegalArgumentException if start or length are negative or if start is greater than or equal to {@link Bitmap#MAX_BITMAP_SIZE}.
	 */
	public void mark(long start, long length) {
		length = boundaryCheck(start, length);

		if (length == 0)
			return;

		if (map == null)
			map = new TreeMap<Long, Long>();

		boolean noPut = false;
		long end = start + length - 1;

		final Long old = map.get(start);

		if (old == null) { //if there was not an old entry there before, then we have to check for the prev entry
			final Entry<Long, Long> prev = map.lowerEntry(start);
			if (prev != null) {
				if (prev.getValue() + 1 >= start) {
					noPut = true;
					if (prev.getValue() < end)
						map.put(prev.getKey(), end);
					else
						end = prev.getValue();
					start = prev.getKey();
				}
			}
		} else {
			if (old >= end)
				return;
		}

		if (!noPut)
			map.put(start, end);

		boolean more = true;
		while (more) {
			more = false;
			Entry<Long, Long> next = map.higherEntry(start);
			if (next != null) {
				if (end + 1 >= next.getKey()) {
					if (end < next.getValue())
						map.put(start, next.getValue());
					else
						more = true;
					map.remove(next.getKey());
				}
			}
		}
	}

	/**
	 * Marks all the bits that are marked in the given bitmap.
	 *
	 * <p>This is equivalent to making a bitwise OR of current bitmap with the given bitmap.<br/>
	 * {@code newBitmap = this.bitwiseOr(m);}
	 *
	 * @param m
	 */
	public void mark(Bitmap m) {
		this.bitwiseOr(m);
	}

	/**
	 * Marks (ie: sets to <tt>1</tt>) at least <tt>length</tt> bits starting from the bit
	 * indexed with <tt>0</tt>.
	 *
	 * <p>This method will start from the index <tt>0</tt> looking for unmarked bits and it marks them
	 * until it marks all the given <tt>length</tt>.
	 *
	 * <p>After calling this method the {@link Bitmap#getMarkedSize()} will be equal to:<br/>
	 * {@code oldMarkedSize+length}
	 *
	 * @param length
	 */
	public void mark(long length) {
		if (map == null)
			map = new TreeMap<Long, Long>();
		LinkedList<Pair<Long, Long>> toMark = new LinkedList<Pair<Long,Long>>();
		long s = 0;
		Iterator<Entry<Long, Long>> it = this.map.entrySet().iterator();
		while (it.hasNext() && length >= 0) {
			Entry<Long, Long> n = it.next();
			long l = n.getKey() - s;
			final long oldS = s;
			s = n.getValue() + 1;
			if (l<=0)
				continue;

			if (l >= length) {
				toMark.add(new Pair<Long, Long>(oldS, length));
				length = 0;
			} else {
				length-= l;
				toMark.add(new Pair<Long, Long>(oldS, l));
			}
		}

		if (length > 0)
			toMark.add(new Pair<Long, Long>(s, length));

		for (Pair<Long, Long> p: toMark)
			this.map.put(p.getValue1(), p.getValue1() + p.getValue2() - 1);
	}

	/**
	 * Marks (ie: sets to <tt>1</tt>) all bits in this bitmap and returns the old bitmap (as it was before modifications).
	 *
	 * @return {@link Bitmap} old bitmap, as it was before being fully marked.
	 */
	public Bitmap mark() {
		Bitmap old = new Bitmap();
		old.map = this.map;
		this.map = null;
		this.mark(0, MAX_BITMAP_SIZE);
		return old;
	}

	/**
	 * Unmarks (ie: sets to <tt>0</tt>) all the bits starting from the
	 * bit indexed with <b>{@code start}</b> (inclusive) until the bit indexed with <b>{@code start + length - 1}</b> (inclusive).
	 *
	 * @param start the index of the first bit to be unmarked
	 * @param length how many bits will be unmarked after the given <tt>start</tt> index.
	 * @throws IllegalArgumentException if start or length are negative or if start is greater than or equal to {@link Bitmap#MAX_BITMAP_SIZE}.
	 */
	public void unmark(long start, long length) {
		length = boundaryCheck(start, length);

		if (length == 0)
			return;

		if (map == null || map.isEmpty())
			return;

		long end = start + length - 1;

		Entry<Long, Long> prev = map.floorEntry(start);
		if (prev != null && prev.getValue() >= start) {
			if (start == prev.getKey()) {
				map.remove(prev.getKey());
			} else {
				map.put(prev.getKey(), start-1);
			}

			if (end < prev.getValue()) {
				map.put(end+1, prev.getValue());
			} else if (end > prev.getValue()) {
				unmark(prev.getValue() + 1, end - prev.getValue());
			}
			return;
		}

		Entry<Long, Long> next = map.higherEntry(start);
		if (next != null && next.getKey() <= end) {
			map.remove(next.getKey());
			if (next.getValue() > end) {
				map.put(end+1, next.getValue());
			} else {
				unmark(next.getValue() + 1, end - next.getValue());
			}
			return;
		}
	}

	/**
	 * Unmarks all the bits that are marked in the given bitmap.
	 *
	 * <p>This is equivalent to the bitwise AND NOT of current bitmap with the given bitmap.<br/>
	 * {@code newBitmap = this.bitwiseAnd(m.clone().bitwiseNot());}
	 *
	 * @param m
	 */
	public void unmark(Bitmap m) {
		if (m.map == null)
			return;
		for (Entry<Long, Long> e: m.map.entrySet())
			this.unmark(e.getKey(), e.getValue() - e.getKey() + 1);
	}

	/**
	 * Unmarks (ie: sets to <tt>0</tt>) at least <tt>length</tt> bits starting from the
	 * bit indexed with <tt>0</tt>.<br/>
	 * <b>Note</b> If the given <tt>length</tt> is greater than the value returned by
	 * {@link Bitmap#getMarkedSize()}, then a maximum of that value is unmarked.
	 *
	 * <p>This method will start from the index <tt>0</tt> looking for marked bits and it unmarks them
	 * until it unmarks all the given <tt>length</tt>.
	 *
	 * <p>After calling this method the {@link Bitmap#getMarkedSize()} will be equal to:<br/>
	 * {@code Math.max(oldMarkedSize-length, 0)}
	 *
	 * @param length
	 */
	public void unmark(long length) {
		if (map == null)
			return;
		Iterator<Entry<Long, Long>> it = this.map.descendingMap().entrySet().iterator();
		while (it.hasNext() && length >= 0) {
			Entry<Long, Long> n = it.next();
			long l = n.getValue() - n.getKey() + 1;
			if (length<l) {
				it.remove();
				this.map.put(n.getKey(), n.getValue() - length);
				break;
			} else {
				it.remove();
				length-=l;
			}
		}
	}

	/**
	 * Unmarks (sets to <tt>0</tt>) all the bits in this bitmap and returns the old bitmap (as it was before modifications).
	 *
	 * @return a {@link Bitmap} containing old bitmap.
	 */
	public Bitmap unmark() {
		Bitmap old = new Bitmap();
		old.map = map;
		map = null;
		return old;
	}

	/**
	 * Returns <tt>true</tt> if the bit at the given index is marked (ie: equal to <tt>1</tt>).
	 *
	 * @param index
	 * @return <tt>true</tt> if the bit at the given index is marked
	 */
	public boolean isMarked(long index) {
		if (this.map == null || this.map.isEmpty())
			return false;
		Entry<Long, Long> e = this.map.floorEntry(index);
		if (e == null)
			return false;
		return e.getValue() >= index;
	}

	/**
	 * Returns the total number of bits that are marked (ie: set to 1).
	 *
	 * @return the total number of bits that are marked
	 */
	public long getMarkedSize() {
		if (map == null)
			return 0;
		long t = 0;
		for (Entry<Long, Long> e: map.entrySet())
			t+=e.getValue() - e.getKey() + 1;
		return t;
	}

	/**
	 * Returns the total number of bits that are unmarked (ie: set to 0).
	 *
	 * @return the total number of bits that are unmarked
	 */
	public long getUnmarkedSize() {
		return MAX_BITMAP_SIZE - getMarkedSize();
	}

	/**
	 * Returns the marked size based on a page boundary, where page size is given by <tt>pageSize</tt>.
	 *
	 * <p>This method assumes that if a bit at a given page is marked then all the other bits in the page are marked too.
	 *
	 * @return the marked size based on a page boundary
	 */
	public long getMarkedPagesSize(long pageSize) {
		if (map == null)
			return 0;
		if (pageSize <= 0)
			throw new IllegalArgumentException("Negative or null page size given");
		long lastCountedPage = -1l;
		long t = 0;
		for (Entry<Long, Long> e: map.entrySet()) {
			long lastPage = e.getValue() / pageSize;
			if (lastPage <= lastCountedPage)
				continue;
			long firstPage = e.getKey() / pageSize;
			if (firstPage <= lastCountedPage)
				firstPage = lastCountedPage + 1;
			t+= (lastPage - firstPage + 1) * pageSize;
			lastCountedPage = lastPage;
		}
		return t;
	}

	/**
	 * Returns the unmarked size based on a page boundary, where page size is given by <tt>pageSize</tt>.
	 *
	 * <p>This method assumes that if a bit at a given page is marked then all the other bits in the page are marked too.
	 *
	 * @return the unmarked size based on a page boundary
	 */
	public long getUnMarkedPagesSize(long pageSize) {
		return MAX_BITMAP_SIZE - getMarkedPagesSize(pageSize);
	}

	/**
	 * Performs a bitwise NOT on the current bitmap.
	 */
	public void bitwiseNot() {
		long min = 0;
		Bitmap m = new Bitmap();
		if (this.map != null) {
			for (Entry<Long, Long> e: this.map.entrySet()) {
				m.mark(min, e.getKey() - min);
				min = e.getValue() + 1;
			}
		}
		if (min < MAX_BITMAP_SIZE)
			m.mark(min, MAX_BITMAP_SIZE);
		this.map = m.map;
	}

	/**
	 * Performs a bitwise OR on the current bitmap using the given bitmap.
	 * @param m
	 */
	public void bitwiseOr(Bitmap m) {
		if (m.map == null)
			return;
		for (Entry<Long, Long> e: m.map.entrySet())
			this.mark(e.getKey(), e.getValue() - e.getKey() + 1);
	}

	/**
	 * Performs a bitwise AND on the current bitmap using the given bitmap.
	 * @param m
	 */
	public void bitwiseAnd(Bitmap m) {
		if (this.map == null || this.map.isEmpty())
			return;
		long min = this.map.firstKey();
		if (m.map != null) {
			for (Entry<Long, Long> e: m.map.entrySet()) {
				final long l = e.getKey() - min;
				if (l > 0)
					this.unmark(min, l);
				min = e.getValue() + 1;
			}
		}
		this.unmark(min, MAX_BITMAP_SIZE);
	}

	/**
	 * Performs a bitwise XOR on the current bitmap using the given bitmap.
	 * @param m
	 */
	public void bitwiseXor(Bitmap m) {
		Bitmap t2 = m.clone();
		t2.bitwiseNot(); // ~m
		t2.bitwiseAnd(this); // this & (~m)

		this.bitwiseNot(); // ~this
		this.bitwiseAnd(m); // (~this) & m
		this.bitwiseOr(t2); // ((~this) & m) | (this & (~m))
	}

	/**
	 * A SubBitmap is a contiguous bitmap that is defined
	 * by a starting index, and a length.
	 */
	public static class SubBitmap {
		private long startIndex;
		private long length;

		public SubBitmap(long startIndex, long length) {
			super();
			this.startIndex = startIndex;
			this.length = length;
		}

		public long getStartIndex() {
			return startIndex;
		}

		public long getLength() {
			return length;
		}
	}

	/**
	 * Returns an iterator through all marked {@link SubBitmap}s of the current Bitmap.
	 *
	 * @return an iterator through all marked {@link SubBitmap}s
	 */
	public Iterator<SubBitmap> getMarkedSubBitmapsIterator() {
		final TreeMap<Long, Long> map = this.map;
		return new SubBitmapsIterator(map);
	}

	private static final class SubBitmapsIterator implements
			Iterator<SubBitmap> {
		Iterator<Entry<Long, Long>> it;

		private SubBitmapsIterator(TreeMap<Long, Long> map) {
			it = map == null ? null : map.entrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			if (it == null)
				return false;
			return it.hasNext();
		}

		@Override
		public SubBitmap next() {
			if (it == null)
				throw new NoSuchElementException();
			Entry<Long, Long> n = it.next();
			return new SubBitmap(n.getKey(), n.getValue() - n.getKey() + 1);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public String toString() {
		if (map == null)
			return "";
		StringBuilder b = new StringBuilder();
		for (Entry<Long, Long> e: map.entrySet())
			b.append("[" + e.getKey() + "," + e.getValue() + "]");
		return b.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Bitmap))
			return false;
		if (((Bitmap) obj).map == this.map)
			return true;
		if (this.map != null)
			return this.map.equals(((Bitmap) obj).map);
		return false;
	}

	@Override
	public int hashCode() {
		return this.map == null ? super.hashCode() : this.map.hashCode();
	}

	/**
	 * Returns a deep clone of this {@link Bitmap}.
	 *
	 * <p>You can modify the newly created instance safely and independently from the original.
	 *
	 * @return a deep clone of this {@link Bitmap}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Bitmap clone() {
		Bitmap clone = null;
		try {
			clone = (Bitmap) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

		if (map != null)
			clone.map = (TreeMap<Long, Long>) map.clone();

		return clone;
	}
}
