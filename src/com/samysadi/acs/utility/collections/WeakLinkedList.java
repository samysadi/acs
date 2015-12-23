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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A linked list that keeps weak references of its elements.<br/>
 * This list provides best effort access to its elements, in the sense that if you
 * are iterating over the list and an element has been garbage-collected, then the
 * iterator will not fail, and will just skip those elements.
 * 
 * <p>This implementation is not synchronized.
 * 
 * @since 1.0
 */
public class WeakLinkedList<E> extends AbstractSequentialList<E> implements
		Deque<E> {
	transient int size = 0;
	transient Node<E> first;
	transient Node<E> last;
	private ReferenceQueue<E> queue;

	private static class Node<E> extends WeakReference<E> {
		Node<E> next;
		Node<E> prev;
		boolean removed = false;

		public Node(Node<E> prev, E element, ReferenceQueue<E> queue,
				Node<E> next) {
			super(element, queue);
			this.next = next;
			this.prev = prev;
		}

		public boolean isRemoved() {
			return removed;
		}

		public void setRemoved(boolean removed) {
			this.removed = removed;
		}
	}

	public WeakLinkedList() {
		super();
		queue = new ReferenceQueue<E>();
	}

	public WeakLinkedList(Collection<? extends E> c) {
		this();
		this.addAll(c);
	}

	@SuppressWarnings("unchecked")
	private void expungeStaleRefs() {
		for (Node<E> n; (n = (Node<E>) queue.poll()) != null;)
			unlink(n);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new WeakListIterator(index);
	}

	@Override
	public int size() {
		expungeStaleRefs();
		return size;
	}

	private class WeakListIterator implements ListIterator<E> {
		private Node<E> lastReturned = null;
		private Node<E> next;
		private int nextIndex;
		private int expectedModCount = modCount;

		public WeakListIterator(int index) {
			if (index < 0 || index > size)
				throw new IndexOutOfBoundsException();
			next = (index == size) ? null : node(index);
			nextIndex = index;
		}

		@Override
		public boolean hasNext() {
			expungeStaleRefs();
			return nextIndex < size;
		}

		@Override
		public E next() {
			checkForComodification();
			if (!hasNext())
				throw new NoSuchElementException();

			while (next.isRemoved())
				next = next.next;

			lastReturned = next;
			next = next.next;
			nextIndex++;
			return lastReturned.get();
		}

		@Override
		public boolean hasPrevious() {
			expungeStaleRefs();
			return nextIndex > 0;
		}

		@Override
		public E previous() {
			checkForComodification();
			if (!hasPrevious())
				throw new NoSuchElementException();

			if (next != null) {
				while (next.isRemoved())
					next = next.next;
			}

			lastReturned = next = (next == null) ? last : next.prev;
			nextIndex--;
			return lastReturned.get();
		}

		@Override
		public int nextIndex() {
			return nextIndex;
		}

		@Override
		public int previousIndex() {
			return nextIndex - 1;
		}

		@Override
		public void remove() {
			checkForComodification();
			if (lastReturned == null)
				throw new IllegalStateException();

			Node<E> lastNext = lastReturned.next;
			if (unlink(lastReturned)) {
				modCount++;
				expectedModCount++;
				if (next == lastReturned)
					next = lastNext;
				else
					nextIndex--;
			}
			lastReturned = null;
		}

		@Override
		public void set(E e) {
			if (lastReturned == null)
				throw new IllegalStateException();
			checkForComodification();

			if (lastReturned.isRemoved())
				add(e);

			Node<E> n = newNode(lastReturned.prev, e, lastReturned.next);
			if (n.prev != null)
				n.prev.next = n;
			if (n.next != null)
				n.next.prev = n;

			if (first == lastReturned)
				first = n;

			if (last == lastReturned)
				last = n;

			lastReturned.setRemoved(true);
			//let other instance of this iterator update their own next field
			lastReturned.next = n;

			lastReturned = n;
		}

		@Override
		public void add(E e) {
			checkForComodification();
			lastReturned = null;
			if (next == null)
				linkLast(e);
			else
				linkBefore(e, next);
			modCount++;
			nextIndex++;
			expectedModCount++;
		}

		final void checkForComodification() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}
	}

	private Node<E> node(int index) {
		if (index < (size >> 1)) {
			Node<E> x = first;
			for (int i = 0; i < index; i++)
				x = x.next;
			return x;
		} else {
			Node<E> x = last;
			for (int i = size - 1; i > index; i--)
				x = x.prev;
			return x;
		}
	}

	private Node<E> newNode(Node<E> pred, E e, Node<E> succ) {
		return new Node<E>(pred, e, queue, succ);
	}

	private Node<E> linkLast(E e) {
		final Node<E> l = last;
		final Node<E> newNode = newNode(l, e, null);
		last = newNode;
		if (l == null)
			first = newNode;
		else
			l.next = newNode;
		size++;
		return newNode;
	}

	private Node<E> linkBefore(E e, Node<E> succ) {
		final Node<E> pred = succ.prev;
		final Node<E> newNode = newNode(pred, e, succ);
		succ.prev = newNode;
		if (pred == null)
			first = newNode;
		else
			pred.next = newNode;
		size++;
		return newNode;
	}

	private boolean unlink(Node<E> x) {
		if (x.isRemoved())
			return false;

		final Node<E> next = x.next;
		final Node<E> prev = x.prev;

		if (prev == null) {
			first = next;
		} else
			prev.next = next;

		if (next == null) {
			last = prev;
		} else
			next.prev = prev;

		x.setRemoved(true);

		size--;
		return true;
	}

	@Override
	public void addFirst(E e) {
		add(0, e);
	}

	@Override
	public void addLast(E e) {
		add(size(), e);
	}

	@Override
	public boolean offerFirst(E e) {
		addFirst(e);
		return true;
	}

	@Override
	public boolean offerLast(E e) {
		addLast(e);
		return true;
	}

	@Override
	public E removeFirst() {
		return remove(0);
	}

	@Override
	public E removeLast() {
		return remove(size() - 1);
	}

	@Override
	public E pollFirst() {
		if (this.size() == 0)
			return null;
		return removeFirst();
	}

	@Override
	public E pollLast() {
		if (this.size() == 0)
			return null;
		return removeLast();
	}

	@Override
	public E getFirst() {
		return get(0);
	}

	@Override
	public E getLast() {
		return get(size() - 1);
	}

	@Override
	public E peekFirst() {
		if (this.size() == 0)
			return null;
		return getFirst();
	}

	@Override
	public E peekLast() {
		if (this.size() == 0)
			return null;
		return getLast();
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		return remove(o);
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		Iterator<E> it = descendingIterator();
		while (it.hasNext()) {
			E e = it.next();
			if (e == o || (e != null && e.equals(o))) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean offer(E e) {
		return offerLast(e);
	}

	@Override
	public E remove() {
		return removeFirst();
	}

	@Override
	public E poll() {
		return pollFirst();
	}

	@Override
	public E element() {
		return getFirst();
	}

	@Override
	public E peek() {
		return peekFirst();
	}

	@Override
	public void push(E e) {
		addFirst(e);
	}

	@Override
	public E pop() {
		return removeFirst();
	}

	@Override
	public Iterator<E> descendingIterator() {
		return new DescendingIterator();
	}

	private class DescendingIterator implements Iterator<E> {
		private final ListIterator<E> itr = listIterator(size());

		@Override
		public boolean hasNext() {
			return itr.hasPrevious();
		}

		@Override
		public E next() {
			return itr.previous();
		}

		@Override
		public void remove() {
			itr.remove();
		}
	}
}
