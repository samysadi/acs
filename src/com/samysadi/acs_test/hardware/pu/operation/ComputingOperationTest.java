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

package com.samysadi.acs_test.hardware.pu.operation;

import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.OperationSynchronizer;
import com.samysadi.acs_test.Utils;

/**
 *
 * @since 1.0
 */
public class ComputingOperationTest {
	Simulator simulator;
	CloudProvider cloudProvider;
	Host h0;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@Before
	public void prepareTest() {
		simulator = Utils.newSimulator();
		cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology0(simulator);
		h0 = cloudProvider.getHosts().get(0);
		Utils.getVmFor(h0).doPause();
		Utils.getVmFor(h0).setUsableProcessingUnits(Arrays.asList(h0.getProcessingUnits().get(0)));
		Utils.getVmFor(h0).doStart();

		Factory.getFactory(simulator).newComputingProvisioner(null,
				Utils.getVmFor(h0).getUsableProcessingUnits().get(0),
				MIPS_CAPACITY);
	}

	private volatile AssertionError exc;

	private NotificationListener getOperationListener() {
		return new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> n = (Operation<?>) notifier;
				if (n.getRunnableState() == RunnableState.PAUSED)
					return;

				n.getLogger().log(n, n.getRunnableState() + ":" + n.getAllocatedResource());
			}
		};
	}

	private void assertLEquals(long max, long v) {
		if (max == v + 1)
			return;
		Assert.assertEquals(max, v);
	}

	private Job newJob() {
		Job j = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
		j.setParent(Utils.getVmFor(h0));
		j.doStart();
		return j;
	}

	private Job newJob(int i) {
		Job j = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
		j.setParent(Utils.getVmFor(cloudProvider.getHosts().get(i)));
		j.doStart();
		return j;
	}

	private static final long MIPS_CAPACITY = 1000 * Simulator.MI;
	private static final long LENGTH = 10000 * Simulator.MI;

	@Test
	public void test0length() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				try {
					newJob().compute(0, getOperationListener());
				} catch (IllegalArgumentException e) {
					return;
				}

				Assert.assertTrue("Exception needs to be thrown for 0 length operation.", false);
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

	}

	@Test
	public void test0() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final ComputingOperation c0 = newJob().compute(LENGTH, getOperationListener());
				c0.doPause();c0.setResourceMax(MIPS_CAPACITY / 6); c0.doStart();
				final ComputingOperation c1 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c2 = newJob().compute(LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(c0.isRunning());
							Assert.assertNotNull(c0.getAllocatedResource());
							Assert.assertEquals(MIPS_CAPACITY / 6, c0.getAllocatedResource().getLong());
							Assert.assertTrue(c1.isRunning());
							Assert.assertNotNull(c1.getAllocatedResource());
							Assert.assertTrue(c2.isRunning());
							Assert.assertNotNull(c2.getAllocatedResource());
							Assert.assertEquals(c1.getAllocatedResource().getLong(), c2.getAllocatedResource().getLong());
							assertLEquals(MIPS_CAPACITY - MIPS_CAPACITY / 6, c1.getAllocatedResource().getLong() + c2.getAllocatedResource().getLong());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals((long) Math.ceil((double)LENGTH * Simulator.SECOND / (MIPS_CAPACITY / 6)), simulator.getTime());

	}

	@Test
	public void testSync() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final ComputingOperation c0 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c1 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c2 = newJob().compute(LENGTH, getOperationListener());
				c2.doPause();c2.setResourceMax(100);c2.doStart();
				c0.doPause();c1.doPause();c2.doPause();
				OperationSynchronizer.synchronizeOperations(c0, c1);
				OperationSynchronizer.synchronizeOperations(c1, c2);
				c0.doStart();c1.doStart();c2.doStart();

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(c0.isRunning());
							Assert.assertNotNull(c0.getAllocatedResource());
							Assert.assertEquals(100, c0.getAllocatedResource().getLong());
							Assert.assertTrue(c1.isRunning());
							Assert.assertNotNull(c1.getAllocatedResource());
							Assert.assertEquals(100, c1.getAllocatedResource().getLong());
							Assert.assertTrue(c2.isRunning());
							Assert.assertNotNull(c2.getAllocatedResource());
							Assert.assertEquals(100, c2.getAllocatedResource().getLong());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals((long) Math.ceil((double)LENGTH * Simulator.SECOND / 100), simulator.getTime());
	}

	@Test
	public void testSync2() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final ComputingOperation c0 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c1 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c2 = newJob().compute(LENGTH, getOperationListener());
				c2.doPause();c2.setResourceMax(100);c2.doStart();
				final ComputingOperation c3 = newJob(1).compute(LENGTH, getOperationListener());
				final ComputingOperation c4 = newJob(2).compute(LENGTH, getOperationListener());
				c0.doPause();c1.doPause();c2.doPause();c3.doPause();c4.doPause();
				OperationSynchronizer.synchronizeOperations(c0, c1);
				OperationSynchronizer.synchronizeOperations(c1, c2);
				OperationSynchronizer.synchronizeOperations(c0, c3);
				OperationSynchronizer.synchronizeOperations(c2, c4);
				c0.doStart();c1.doStart();c2.doStart();c3.doStart();c4.doStart();

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(c0.isRunning());
							Assert.assertNotNull(c0.getAllocatedResource());
							Assert.assertEquals(100, c0.getAllocatedResource().getLong());
							Assert.assertTrue(c1.isRunning());
							Assert.assertNotNull(c1.getAllocatedResource());
							Assert.assertEquals(100, c1.getAllocatedResource().getLong());
							Assert.assertTrue(c2.isRunning());
							Assert.assertNotNull(c2.getAllocatedResource());
							Assert.assertEquals(100, c2.getAllocatedResource().getLong());
							Assert.assertTrue(c3.isRunning());
							Assert.assertNotNull(c3.getAllocatedResource());
							Assert.assertEquals(100, c3.getAllocatedResource().getLong());
							Assert.assertNotNull(c4.getAllocatedResource());
							Assert.assertEquals(100, c4.getAllocatedResource().getLong());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals((long) Math.ceil((double)LENGTH * Simulator.SECOND / 100), simulator.getTime());
	}

	@Test
	public void testSync3() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final ComputingOperation c0 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c1 = newJob().compute(LENGTH, getOperationListener());
				final ComputingOperation c2 = newJob().compute(LENGTH, getOperationListener());
				c2.doPause();c2.setResourceMax(100);c2.doStart();
				final ComputingOperation c3 = newJob(1).compute(LENGTH, getOperationListener());
				final ComputingOperation c4 = newJob(2).compute(LENGTH, getOperationListener());
				c0.doPause();c1.doPause();c2.doPause();c3.doPause();c4.doPause();
				OperationSynchronizer.synchronizeOperations(c0, c1);
				OperationSynchronizer.synchronizeOperations(c1, c2);
				OperationSynchronizer.synchronizeOperations(c0, c3);
				OperationSynchronizer.synchronizeOperations(c2, c4);
				c0.doStart();c1.doStart();c2.doStart();c3.doStart();c4.doStart();

				Simulator.getSimulator().schedule(500, new EventImpl() {
					@Override
					public void process() {
						try {
							c0.doFail();
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals(500, simulator.getTime());
	}
}
