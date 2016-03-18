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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.InstantNotificationListener;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.notifications.NotifierImpl;
import com.samysadi.acs_test.Utils;

/**
 *
 * @since 1.0
 */
public class NotifierTest {
	Simulator simulator;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@Before
	public void beforeTest() {
		simulator = Utils.newSimulator();
	}

	private volatile AssertionError exc;

	@Test
	public void test() {

		final String m = "Notifier.notify() behaviour is invalid.";
		final String o = m + " Invalid notification order.";
		final String f = m + " Invalid notification frequency.";

		//pair name=call_times
		final LinkedHashMap<String, Integer> h = new LinkedHashMap<String, Integer>();
		final NotifierImpl n = new NotifierImpl();
		simulator.schedule(new EventImpl() {
			private void doInc(String name) {
				Integer v = h.get(name);
				if (v == null)
					v = 0;
				v = v+1;
				h.put(name, v);
			}

			@Override
			public void process() {
				//listener b
				n.addListener(1, new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						doInc("b");
					}
				});
				//listener c
				n.addListener(2, new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						doInc("c");
					}
				});
				//listener d
				n.addListener(1, new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						doInc("d");

						n.notify(1, "recall_b");

						this.discard();
					}
				});
				//listener a
				n.addListener(1, new InstantNotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						doInc("a");
						this.discard();
					}
				});
				n.notify(1, null);
				try {
					@SuppressWarnings("unchecked")
					Entry<String, Integer>[] e = (Entry<String, Integer>[]) h.entrySet().toArray(new Entry<?,?>[0]);
					Assert.assertEquals(m, 1, e.length);
					Assert.assertEquals(o, "a", e[0].getKey());
					Assert.assertEquals(f, 1, e[0].getValue().intValue());
				} catch (AssertionError e) {
					exc = e;
				}
				n.notify(2, null);
				n.notify(2, "0");
				n.notify(2, "1");
				n.notify(1, null);
				//listener e
				n.addListener(1, new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						doInc("e");
					}
				});
			}

		});

		simulator.start();
		if (exc != null)
            throw exc;

		@SuppressWarnings("unchecked")
		Entry<String, Integer>[] e = (Entry<String, Integer>[]) h.entrySet().toArray(new Entry<?,?>[0]);

		Assert.assertEquals(m, 5, e.length);

		Assert.assertEquals(o, "a", e[0].getKey());
		Assert.assertEquals(f, 1, e[0].getValue().intValue());

		//either listener b or d must be second, and the other is third
		int ib = e[1].getKey().equals("b") ? 1 : 2;
		int id = ib == 1 ? 2 : 1;

		Assert.assertEquals(o, "b", e[ib].getKey());
		Assert.assertEquals(f, 2, e[ib].getValue().intValue());

		Assert.assertEquals(o, "d", e[id].getKey());
		Assert.assertEquals(f, 1, e[id].getValue().intValue());

		//listener c must be fourth to be called
		Assert.assertEquals(o, "c", e[3].getKey());
		Assert.assertEquals(f, 3, e[3].getValue().intValue());

		//listener e must be fifth to be called
		Assert.assertEquals(o, "e", e[4].getKey());
		Assert.assertEquals(f, 1, e[4].getValue().intValue());

	}

}
