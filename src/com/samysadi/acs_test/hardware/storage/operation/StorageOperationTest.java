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

package com.samysadi.acs_test.hardware.storage.operation;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs_test.Utils;


/**
 * 
 * @since 1.0
 */
@SuppressWarnings("unused")
public class StorageOperationTest {
	Simulator simulator;
	CloudProvider cloudProvider;
	Host h0, h1, h2, hl;
	Switch s0, s1, sl;
	Job j0, j1, j2, jl;

	private long TR_CAPACITY;

	private static final long SF_SIZE = 1000 * Simulator.MEBIBYTE;
	StorageFile sf0, sf1, sf2;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@Before
	public void prepareTest() {
		simulator = Utils.newSimulator();
		cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology2(simulator);
		h0 = cloudProvider.getHosts().get(0);
		sf0 = Factory.getFactory(simulator).newStorageFile(null, h0.getStorages().get(0), SF_SIZE);
		sf0.getMemoryMap(DEFAULT_MAP).unmark();
		sf1 = Factory.getFactory(simulator).newStorageFile(null, h0.getStorages().get(0), SF_SIZE);
		sf1.getMemoryMap(DEFAULT_MAP).unmark();
		sf2 = Factory.getFactory(simulator).newStorageFile(null, h0.getStorages().get(0), SF_SIZE);
		sf2.getMemoryMap(DEFAULT_MAP).unmark();
		TR_CAPACITY = sf0.getParent().getStorageProvisioner().getCapacity();
		
		h1 = cloudProvider.getHosts().get(1);
		h2 = cloudProvider.getHosts().get(2);
		hl = cloudProvider.getHosts().get(cloudProvider.getHosts().size()-1);
		s0 = cloudProvider.getSwitches().get(0);
		s1 = (Switch) h0.getInterfaces().get(0).getRemoteNetworkInterface().getParent();
		sl = (Switch) hl.getInterfaces().get(0).getRemoteNetworkInterface().getParent();

		j0 = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
		j0.setParent(Utils.getVmFor(h0));
		j0.doStart();

		j1 = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
		j1.setParent(Utils.getVmFor(h1));
		j1.doStart();

		j2 = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
		j2.setParent(Utils.getVmFor(h2));
		j2.doStart();

		jl = Factory.getFactory(Simulator.getSimulator()).newJob(null, null);
		jl.setParent(Utils.getVmFor(hl));
		jl.doStart();
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

	private static final long LENGTH = 100 * Simulator.MEBIBYTE;
	private static final Object DEFAULT_MAP = new Object();

	@Test
	public void test0() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j0.readFile(sf0, 0, LENGTH, getOperationListener());
				s0.doPause();s0.setResourceMax(TR_CAPACITY / 6); s0.doStart();
				final StorageOperation s1 = j0.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j0.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(s0.isRunning());
							Assert.assertNotNull(s0.getAllocatedResource());
							Assert.assertEquals(TR_CAPACITY / 6, s0.getAllocatedResource().getLong());
							Assert.assertTrue(s1.isRunning());
							Assert.assertNotNull(s1.getAllocatedResource());
							Assert.assertTrue(s2.isRunning());
							Assert.assertNotNull(s2.getAllocatedResource());
							Assert.assertEquals(s1.getAllocatedResource().getLong(), s2.getAllocatedResource().getLong());
							assertLEquals(TR_CAPACITY - TR_CAPACITY / 6, s1.getAllocatedResource().getLong() + s2.getAllocatedResource().getLong());
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

		Assert.assertEquals(SF_SIZE, sf0.getSize());
		Assert.assertEquals(0, sf0.getMemoryMap(DEFAULT_MAP).getMarkedSize());
		Assert.assertEquals(SF_SIZE, sf1.getSize());
		Assert.assertEquals(LENGTH, sf1.getMemoryMap(DEFAULT_MAP).getMarkedSize());
		Assert.assertEquals(SF_SIZE + LENGTH, sf2.getSize());
		Assert.assertEquals(LENGTH, sf2.getMemoryMap(DEFAULT_MAP).getMarkedSize());

		Assert.assertEquals((long) Math.ceil((double)LENGTH * Simulator.SECOND / (TR_CAPACITY / 6)), simulator.getTime());
	}

	@Test
	public void test0b() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j0.readFile(sf0, 0, LENGTH, getOperationListener());
				s0.doPause();s0.setResourceMax(TR_CAPACITY / 6); s0.doStart();
				final StorageOperation s1 = j0.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j0.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(100, new EventImpl() {
					@Override
					public void process() {
						sf0.getParent().setFailureState(FailureState.FAILED);
						sf1.getParent().setFailureState(FailureState.FAILED);
						sf2.getParent().setFailureState(FailureState.FAILED);
						simulator.getLogger().log("Storages all failed");
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals(100, simulator.getTime());
	}

	private static final long LINK_BW = 100 * Simulator.MEBIBYTE;

	@Test
	public void test1() {
		final long s0Max = TR_CAPACITY / 7;

		final long BW0 = Math.min(s0Max, LINK_BW); //s0 uses down link

		final long BW1 = Math.min((TR_CAPACITY - BW0) / 2, LINK_BW / 2); //s1 shares down link with s2

		final long BW2 = BW1;
		
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j1.readFile(sf0, 0, LENGTH, getOperationListener());
				s0.doPause();s0.setResourceMax(s0Max); s0.doStart();
				final StorageOperation s1 = j1.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j1.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(s0.isRunning());
							Assert.assertNotNull(s0.getAllocatedResource());
							Assert.assertEquals(BW0, s0.getAllocatedResource().getLong());
							Assert.assertTrue(s1.isRunning());
							Assert.assertNotNull(s1.getAllocatedResource());
							Assert.assertTrue(s2.isRunning());
							Assert.assertNotNull(s2.getAllocatedResource());
							Assert.assertEquals(s1.getAllocatedResource().getLong(), s2.getAllocatedResource().getLong());
							assertLEquals(Math.min(TR_CAPACITY, LINK_BW),
									s1.getAllocatedResource().getLong() + s2.getAllocatedResource().getLong());
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

		Assert.assertEquals(SF_SIZE, sf0.getSize());
		Assert.assertEquals(0, sf0.getMemoryMap(DEFAULT_MAP).getMarkedSize());
		Assert.assertEquals(SF_SIZE, sf1.getSize());
		Assert.assertEquals(LENGTH, sf1.getMemoryMap(DEFAULT_MAP).getMarkedSize());
		Assert.assertEquals(SF_SIZE + LENGTH, sf2.getSize());
		Assert.assertEquals(LENGTH, sf2.getMemoryMap(DEFAULT_MAP).getMarkedSize());

		long timeForS0 = (long) Math.ceil((double)LENGTH * Simulator.SECOND / (BW0));

		long timeForS12 = (long) Math.ceil((double)LENGTH * Simulator.SECOND / (BW2));

		Assert.assertEquals(Math.max(timeForS0, timeForS12), simulator.getTime());
	}

	@Test
	public void test1b() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j1.readFile(sf0, 0, LENGTH, getOperationListener());
				final StorageOperation s1 = j1.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j1.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(100, new EventImpl() {
					@Override
					public void process() {
						StorageOperationTest.this.s1.setFailureState(FailureState.FAILED);
						simulator.getLogger().log(StorageOperationTest.this.s1, "Failed");
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
		Assert.assertEquals(100, simulator.getTime());
	}

	@Test
	public void test1c() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j1.readFile(sf0, 0, LENGTH, getOperationListener());
				final StorageOperation s1 = j1.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j1.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(100, new EventImpl() {
					@Override
					public void process() {
						StorageOperationTest.this.s1.setPowerState(PowerState.OFF);
						simulator.getLogger().log(StorageOperationTest.this.s1, "Failed");
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
		Assert.assertEquals(100, simulator.getTime());
	}

	@Test
	public void test1d() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j1.readFile(sf0, 0, LENGTH, getOperationListener());
				final StorageOperation s1 = j1.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j1.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(100, new EventImpl() {
					@Override
					public void process() {
						sf0.getParent().setFailureState(FailureState.FAILED);
						sf1.getParent().setFailureState(FailureState.FAILED);
						sf2.getParent().setFailureState(FailureState.FAILED);
						simulator.getLogger().log("Storages all failed");
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
		Assert.assertEquals(100, simulator.getTime());
	}

	@Test
	public void test1e() {
		final long s0Max = TR_CAPACITY / 6;

		final long BW0 = Math.min(s0Max, LINK_BW); //s0 uses down link

		final long BW1 = Math.min((TR_CAPACITY - BW0) / 2, LINK_BW / 2); //s1 shares down link with s2

		final long BW2 = BW1;

		final long[] mdfy = new long[1];
		
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j1.readFile(sf0, 0, LENGTH, getOperationListener());
				s0.doPause();s0.setResourceMax(s0Max); s0.doStart();
				final StorageOperation s1 = j1.writeFile(sf1, 0, LENGTH, getOperationListener());
				final StorageOperation s2 = j1.appendFile(sf2, LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(s0.isRunning());
							Assert.assertNotNull(s0.getAllocatedResource());
							Assert.assertEquals(BW0, s0.getAllocatedResource().getLong());
							Assert.assertTrue(s1.isRunning());
							Assert.assertNotNull(s1.getAllocatedResource());
							Assert.assertTrue(s2.isRunning());
							Assert.assertNotNull(s2.getAllocatedResource());
							Assert.assertEquals(s1.getAllocatedResource().getLong(), s2.getAllocatedResource().getLong());
							assertLEquals(Math.min(TR_CAPACITY, LINK_BW),
									s1.getAllocatedResource().getLong() + s2.getAllocatedResource().getLong());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});

				Simulator.getSimulator().schedule(500, new EventImpl() {
					@Override
					public void process() {
						s1.doPause();
						mdfy[0] = s1.getCompletedLength();
						s1.setStorageFile(sf2);
						s1.doStart();
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals(SF_SIZE, sf0.getSize());
		Assert.assertEquals(0, sf0.getMemoryMap(DEFAULT_MAP).getMarkedSize());
		Assert.assertEquals(SF_SIZE, sf1.getSize());
		Assert.assertEquals(mdfy[0], sf1.getMemoryMap(DEFAULT_MAP).getMarkedSize());
		Assert.assertEquals(SF_SIZE + LENGTH, sf2.getSize());
		Assert.assertEquals(LENGTH * 2 - mdfy[0], sf2.getMemoryMap(DEFAULT_MAP).getMarkedSize());

		long timeForS0 = (long) Math.ceil((double)LENGTH * Simulator.SECOND / (BW0));

		long timeForS12 = (long) Math.ceil((double)LENGTH * Simulator.SECOND / (BW2));

		assertLEquals(Math.max(timeForS0, timeForS12), simulator.getTime() - 1);
	}
	

	@Test
	public void aaaaa() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final StorageOperation s0 = j0.readFile(sf0, 1, 1, getOperationListener());
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

	}
}
