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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * A list that gives a "read-only" view of multiple lists so that
 * they appear as one unique merged list without them being actually merged.<br/>
 * This list does not contain a merged copy of each of the underlying lists. So,
 * this has lesser memory usage, than creating a list and add all
 * the underlying lists.
 *
 * <p>Because this list is unmodifiable (read-only), all attempts to modify it, whether
 * directly or via its iterator, result in an UnsupportedOperationException.
 *
 * <p>You have to override the {@code lists()} and {@code size()} methods to use this list.<br/>
 * The {@code size()} method must return an integer that is equal to the sum of the sizes of each of the lists returned by
 * the {@code lists()} method.<br/>
 * The list returned by the {@code lists()} method may safely contain <tt>null</tt> lists.
 *
 * <p><b>Note</b> This list is optimized to be used with {@link RandomAccess} lists.
 * If {@code lists()} is not, or contains non-{@link RandomAccess} lists, then you will
 * probably experience important performance issues.
 *
 * @since 1.0
 */
public abstract class AbstractMultiListView<E> extends AbstractCollection<E> implements List<E>, RandomAccess {

	protected abstract List<? extends List<? extends E>> lists();

	/**
	 * Returns the list's modCount.
	 * It should be different after each modification of one of the underlying lists.
	 *
	 * <p>Override this method if you want to throw exceptions after co-modifications.
	 *
	 * <p>This is useful especially to avoid bugs when one of the underlying lists is changed during an iteration.
	 *
	 * @return the list's modCount
	 */
	protected int getModCount() {
		return 0;
	}

	@Override
	public Iterator<E> iterator() {
		return listIterator();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E get(int index) {
		if (index < 0 || index >= this.size())
			throw new IndexOutOfBoundsException();
		for (List<? extends E> l : this.lists())
			if (l != null) {
				if (index >= l.size())
					index -= l.size();
				else
					return l.get(index);
			}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		int i = 0;
		for (List<? extends E> l : this.lists())
			if (l != null) {
				final int j = l.indexOf(o);
				if (j >= 0)
					return i + j;
				else
					i += l.size();
			}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		int i = this.size();
		for (ListIterator<? extends List<? extends E>> iterator = this.lists()
				.listIterator(this.lists().size()); iterator.hasPrevious();) {
			final List<? extends E> cl = iterator.previous();
			if (cl == null)
				continue;
			final int j = cl.lastIndexOf(o);
			i -= cl.size();
			if (j >= 0)
				return i + j;
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new MyListIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new MySubList<E>(this, fromIndex, toIndex);
	}

	private class MyListIterator implements ListIterator<E> {
		protected int listId = -1;
		protected int listElemId;
		protected int index;

		protected int modCount = AbstractMultiListView.this.getModCount();

		public MyListIterator(int index) {
			if (index < 0 || index > AbstractMultiListView.this.size())
				throw new IndexOutOfBoundsException();

			this.index = index;

			this.listId = 0;
			while (this.listId < AbstractMultiListView.this.lists().size()) {
				final List<? extends E> l = AbstractMultiListView.this.lists().get(this.listId);
				if (l != null && !l.isEmpty()) {
					final int d = index - l.size();
					if (d >= 0)
						index = d;
					else
						break;
				}
				this.listId++;
			}

			if (this.listId > AbstractMultiListView.this.lists().size())
				throw new IndexOutOfBoundsException();

			this.listElemId = index;
		}

		@Override
		public boolean hasNext() {
			return this.nextIndex() < AbstractMultiListView.this.size();
		}

		@Override
		public E next() {
			this.checkForComodification();
			final List<? extends E> l = AbstractMultiListView.this.lists().get(this.listId);
			final E e = l.get(this.listElemId);
			this.listElemId++;
			if (this.listElemId >= l.size()) {
				this.listId++;
				while (this.listId < AbstractMultiListView.this.lists().size()) {
					final List<? extends E> ll = AbstractMultiListView.this.lists().get(this.listId);
					if (ll != null && !ll.isEmpty()) {
						this.listElemId = 0;
						break;
					}
					this.listId++;
				}
			}
			this.index++;
			return e;
		}

		@Override
		public boolean hasPrevious() {
			return this.previousIndex() >= 0;
		}

		@Override
		public E previous() {
			this.checkForComodification();
			this.listElemId--;
			if (this.listElemId < 0) {
				this.listId--;
				while (this.listId >= 0) {
					final List<? extends E> ll = AbstractMultiListView.this.lists().get(this.listId);
					if (ll != null && !ll.isEmpty()) {
						this.listElemId = ll.size() - 1;
						break;
					}
					this.listId--;
				}
			}
			this.index--;
			final List<? extends E> l = AbstractMultiListView.this.lists().get(this.listId);
			final E e = l.get(this.listElemId);
			return e;
		}

		@Override
		public int nextIndex() {
			return this.index;
		}

		@Override
		public int previousIndex() {
			return this.index - 1;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(E e) {
			throw new UnsupportedOperationException();
		}

        private void checkForComodification() {
            if (AbstractMultiListView.this.getModCount() != this.modCount)
                throw new ConcurrentModificationException();
        }
	}

	private static class MySubList<E> extends AbstractCollection<E> implements
			List<E> {
		private final List<E> l;
		private final int offset;
		private int size;

		MySubList(List<E> list, int fromIndex, int toIndex) {
			if (fromIndex < 0)
				throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
			if (toIndex > list.size())
				throw new IndexOutOfBoundsException("toIndex = " + toIndex);
			if (fromIndex > toIndex)
				throw new IllegalArgumentException("fromIndex(" + fromIndex
						+ ") > toIndex(" + toIndex + ")");
			l = list;
			offset = fromIndex;
			size = toIndex - fromIndex;
		}

		@Override
		public E set(int index, E element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public E get(int index) {
			rangeCheck(index);
			return l.get(index + offset);
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public void add(int index, E element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public E remove(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<E> iterator() {
			return listIterator();
		}

		@Override
		public ListIterator<E> listIterator(final int index) {
			return new ListIterator<E>() {
				private final ListIterator<E> i = l
						.listIterator(index + offset);

				@Override
				public boolean hasNext() {
					return nextIndex() < size;
				}

				@Override
				public E next() {
					if (hasNext())
						return i.next();
					else
						throw new NoSuchElementException();
				}

				@Override
				public boolean hasPrevious() {
					return previousIndex() >= 0;
				}

				@Override
				public E previous() {
					if (hasPrevious())
						return i.previous();
					else
						throw new NoSuchElementException();
				}

				@Override
				public int nextIndex() {
					return i.nextIndex() - offset;
				}

				@Override
				public int previousIndex() {
					return i.previousIndex() - offset;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void set(E e) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void add(E e) {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			return new MySubList<E>(this, fromIndex, toIndex);
		}

		private void rangeCheck(int index) {
			if (index < 0 || index >= size)
				throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
		}

		private String outOfBoundsMsg(int index) {
			return "Index: " + index + ", Size: " + size;
		}

		@Override
		public int indexOf(Object o) {
	        ListIterator<E> it = listIterator();
	        if (o==null) {
	            while (it.hasNext())
	                if (it.next()==null)
	                    return it.previousIndex();
	        } else {
	            while (it.hasNext())
	                if (o.equals(it.next()))
	                    return it.previousIndex();
	        }
	        return -1;
		}

		@Override
		public int lastIndexOf(Object o) {
	        ListIterator<E> it = listIterator(size());
	        if (o==null) {
	            while (it.hasPrevious())
	                if (it.previous()==null)
	                    return it.nextIndex();
	        } else {
	            while (it.hasPrevious())
	                if (o.equals(it.previous()))
	                    return it.nextIndex();
	        }
	        return -1;
		}

		@Override
		public ListIterator<E> listIterator() {
			return listIterator(0);
		}
	}
}
