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

package com.samysadi.acs.core.tracing;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.notifications.CoreNotificationCodes;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;

/**
 *
 * @param <T>
 *
 * @since 1.0
 */
public class TraceDefault<T> implements Trace<T> {
	private Probe<T> parent;
	private NotificationListener changeListener;
	private NotificationListener tickListener;
	private int maxLength;
	private long delay;
	private LinkedList<TraceItem<T>> values;

	public TraceDefault(Probe<T> parent) {
		super();
		if (parent == null)
			throw new NullPointerException();
		this.parent = parent;
		this.changeListener = null;
		this.tickListener = null;
		this.maxLength = Trace.DEFAULT_MAXIMUM_LENGTH;
		if (this.maxLength < 2)
			throw new IllegalArgumentException("Maximum length cannot be smaller than 2");
		this.delay = Trace.DEFAULT_DELAY;
		this.values = null;
	}

	@Override
	public Probe<T> getParent() {
		return this.parent;
	}

	@Override
	public void discard() {
		setEnabled(false);
		this.parent = null;
	}

	@Override
	public boolean isEnabled() {
		return this.changeListener != null;
	}

	@Override
	public void setEnabled(boolean v) {
		if (v == isEnabled())
			return;

		if (!v) {
			if (getParent() != null)
				getParent().removeListener(CoreNotificationCodes.PROBE_VALUE_CHANGED, this.changeListener);
			this.changeListener.discard();
			this.changeListener = null;
			if (this.tickListener != null) {
				this.tickListener.discard();
				this.tickListener = null;
			}
		} else {
			if (getParent() == null)
				throw new IllegalStateException("This trace has been discarded");
			this.changeListener = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					TraceDefault.this.scheduleValueUpdate();
				}
			};
			getParent().addListener(CoreNotificationCodes.PROBE_VALUE_CHANGED, this.changeListener);
			scheduleValueUpdate();
		}
	}

	/**
	 * Removes values so that the trace contains a maximum of this.maxLength values
	 */
	private void shrink() {
		if (this.values == null)
			return;

		long m = getDelay();
		while (this.values.size() > this.maxLength) {
			long d = m >> 1;
			if (d == 0l)
				d = 1l;
			m = removeExtraValues(m + d);
		}

		if (m > getDelay()) {
			removeExtraValues(m);
			this.delay = m;
		}
	}

	/**
	 * Removes Extra values and returns min delta time between two values which is at least {@code 1l}.
	 */
	private long removeExtraValues(long delay) {
		if (this.values == null)
			return 1l;
		if (delay <= this.delay)
			return 1l;

		long min = delay;
		long lastTime = -delay-1;
		Iterator<TraceItem<T>> it = this.values.iterator();
		boolean hasNext = it.hasNext();
		while (hasNext) {
			TraceItem<T> v = it.next();
			hasNext = it.hasNext();
			if (!hasNext)
				break; //ignore last element
			long d = v.getTime() - lastTime;
			if (d < delay)
				it.remove();
			else {
				lastTime = v.getTime();
				if (d < min)
					min = d;
			}
		}

		return min;
	}

	@Override
	public int getMaxLength() {
		return this.maxLength;
	}

	@Override
	public void setMaxLength(int length) {
		if (length == this.maxLength)
			return;
		if (length < 2)
			throw new IllegalArgumentException("Maximum length cannot be smaller than 2");
		boolean wasEnabled = isEnabled();
		if (isEnabled())
			setEnabled(false);
		this.maxLength = length;
		shrink();
		setEnabled(wasEnabled);
	}

	@Override
	public long getDelay() {
		return this.delay;
	}

	@Override
	public void setDelay(long delay) {
		if (this.delay == delay)
			return;
		if (delay < 0)
			throw new IllegalArgumentException("Delay cannot be negative");
		boolean wasEnabled = isEnabled();
		if (isEnabled())
			setEnabled(false);
		removeExtraValues(delay);
		this.delay = delay;
		setEnabled(wasEnabled);
	}

	@Override
	public List<TraceItem<T>> getValues() {
		if (this.values == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.values);
	}

	/**
	 * This methods adds the given <tt>value</tt> to the list of values returned by {@link Trace#getValues()}
	 * and verifies that the {@link Trace} contract is respected.
	 */
	protected boolean addValue(long time, T value) {
		TraceItem<T> item = new TraceItem<T>(time, value);

		if (this.values == null) {
			//add first item
			this.values = new LinkedList<TraceItem<T>>();
			this.values.add(item);
			return true;
		}

		TraceItem<T> last = this.values.getLast();
		long deltaTime = time - last.getTime();

		if (deltaTime == 0) {
			//replace last item with the new one
			this.values.removeLast();
			this.values.add(item);
			return true;
		}

		if (value == last.getValue()
				|| (value != null && value.equals(last.getValue()))) {
			//value has not changed since last time, no need to keep item
			return false;
		}

		//check if last item must be retained or replaced
		boolean dontRetain = this.values.size() != 1; //always retain first element
		if (dontRetain) {
			TraceItem<T> beforeLast = this.values.get(this.values.size()-2);
			long deltaTime2 = last.getTime() - beforeLast.getTime();
			if (deltaTime2 >= this.delay) {
				//if delay has passed then last item should be retained and not replaced
				dontRetain = false;
			}
		}

		if (dontRetain)
			this.values.removeLast();

		this.values.add(item);
		shrink();
		return true;
	}

	protected void scheduleValueUpdate() {
		if (this.tickListener != null)
			return;

		this.tickListener = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				TraceDefault.this.addValue(Simulator.getSimulator().getTime(), TraceDefault.this.getParent().getValue());
				this.discard();
				TraceDefault.this.tickListener = null;
			}
		};
		Simulator.getSimulator().addListener(CoreNotificationCodes.SIMULATOR_TICK_PASSED, this.tickListener);
	}

	@Override
	public String toString() {
		if (this.getParent() == null)
			return "null#" + hashCode();
		else
			return this.getParent().toString();
	}
}
