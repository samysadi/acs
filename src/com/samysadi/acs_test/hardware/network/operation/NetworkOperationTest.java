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

package com.samysadi.acs_test.hardware.network.operation;

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
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.NetworkLink;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
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
public class NetworkOperationTest {
	Simulator simulator;
	CloudProvider cloudProvider;
	Host h0, h1, h2, hl;
	Switch s0, s1, sl;
	Job j0, j1, j2, jl;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@Before
	public void beforeTest() {
		simulator = Utils.newSimulator();
		cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology2(simulator);
		h0 = cloudProvider.getHosts().get(0);
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

	private static final long LINK_BW = 100 * Simulator.MEBIBYTE;
	private static final long OP_DATA_LENGTH = 1000 * Simulator.MEBIBYTE;

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

	@Test
	public void test0() {
		final long BW = LINK_BW;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load (upload from j0 to j1)
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				//add network load (download from j1 to j0)
				final NetworkOperation o01_1 = j1.sendData(j0, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(BW, o01_0.getAllocatedResource().getBw());
							Assert.assertTrue(o01_1.isRunning());
							Assert.assertNotNull(o01_1.getAllocatedResource());
							Assert.assertEquals(BW, o01_1.getAllocatedResource().getBw());
							//same speed (symmetrical links)
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

		Assert.assertEquals((long) Math.ceil((double)OP_DATA_LENGTH * Simulator.SECOND / BW), simulator.getTime());
	}


	@Test
	public void test1() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_2 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(BW, o01_0.getAllocatedResource().getBw());
							Assert.assertTrue(o01_1.isRunning());
							Assert.assertNotNull(o01_1.getAllocatedResource());
							Assert.assertEquals(BW, o01_1.getAllocatedResource().getBw());
							Assert.assertTrue(o01_2.isRunning());
							Assert.assertNotNull(o01_2.getAllocatedResource());
							Assert.assertEquals(BW, o01_2.getAllocatedResource().getBw());
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

		Assert.assertEquals((long) Math.ceil((double)OP_DATA_LENGTH * Simulator.SECOND / BW),
				simulator.getTime());
	}

	@Test
	public void test2() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				o01_0.doPause(); o01_0.setResourceMax(BW / 2); o01_0.doStart();
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_2 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(BW / 2, o01_0.getAllocatedResource().getBw());
							Assert.assertTrue(o01_1.isRunning());
							Assert.assertNotNull(o01_1.getAllocatedResource());
							Assert.assertTrue(o01_2.isRunning());
							Assert.assertNotNull(o01_2.getAllocatedResource());
							//make sure remaining bw is equally distributed
							Assert.assertEquals(o01_1.getAllocatedResource().getBw(), o01_2.getAllocatedResource().getBw());
							assertLEquals(LINK_BW, o01_0.getAllocatedResource().getBw() + o01_1.getAllocatedResource().getBw() + o01_2.getAllocatedResource().getBw());
						} catch(AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals((long) Math.ceil((double)OP_DATA_LENGTH * Simulator.SECOND / (BW / 2)), simulator.getTime());
	}


	@Test
	public void test2b() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				o01_0.doPause(); o01_0.setResourceMax(BW / 2); o01_0.doStart();
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_2 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o0l_0 = j0.sendData(jl, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(BW / 2, o01_0.getAllocatedResource().getBw());
							Assert.assertTrue(o01_1.isRunning());
							Assert.assertNotNull(o01_1.getAllocatedResource());
							Assert.assertTrue(o01_2.isRunning());
							Assert.assertNotNull(o01_2.getAllocatedResource());
							//make sure remaining bw is equally distributed
							Assert.assertEquals(o01_1.getAllocatedResource().getBw(), o01_2.getAllocatedResource().getBw());
							assertLEquals(LINK_BW, o01_0.getAllocatedResource().getBw() + o01_1.getAllocatedResource().getBw() + o01_2.getAllocatedResource().getBw());
							Assert.assertEquals(LINK_BW, o0l_0.getAllocatedResource().getBw());
						} catch(AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals((long) Math.ceil((double)OP_DATA_LENGTH * Simulator.SECOND / (BW / 2)), simulator.getTime());
	}



	@Test
	public void test2c() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				o01_0.doPause(); o01_0.setResourceMax(BW / 2); o01_0.doStart();
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_2 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(Simulator.SECOND, new EventImpl() {
					@Override
					public void process() {
						s1.setFailureState(FailureState.FAILED);
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals(Simulator.SECOND, simulator.getTime());
	}

	@Test
	public void test2d() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				o01_0.doPause(); o01_0.setResourceMax(BW / 2); o01_0.doStart();
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_2 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(Simulator.SECOND, new EventImpl() {
					@Override
					public void process() {
						s1.setPowerState(PowerState.OFF);
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals(Simulator.SECOND, simulator.getTime());
	}

	@Test
	public void test3() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				o01_0.doPause(); o01_0.setResourceMax(BW / 2); o01_0.doStart();
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_2 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_3 = j0.sendData(j1, 5 * OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(BW / 2, o01_0.getAllocatedResource().getBw());
							Assert.assertTrue(o01_1.isRunning());
							Assert.assertNotNull(o01_1.getAllocatedResource());
							Assert.assertTrue(o01_2.isRunning());
							Assert.assertNotNull(o01_2.getAllocatedResource());
							Assert.assertTrue(o01_3.isRunning());
							Assert.assertNotNull(o01_3.getAllocatedResource());
							//make sure remaining bw is equally distributed
							Assert.assertEquals(o01_1.getAllocatedResource().getBw(), o01_2.getAllocatedResource().getBw());
							Assert.assertEquals(o01_1.getAllocatedResource().getBw(), o01_3.getAllocatedResource().getBw());
							assertLEquals(LINK_BW, o01_0.getAllocatedResource().getBw() + o01_1.getAllocatedResource().getBw() + o01_2.getAllocatedResource().getBw()
									 + o01_3.getAllocatedResource().getBw());
						} catch(AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		long timeFor2Op = (long) Math.ceil((double)OP_DATA_LENGTH * Simulator.SECOND / ((LINK_BW - BW / 2) / 3));
		long remainingTimeForOp0 = (long) Math.ceil((double)(OP_DATA_LENGTH - timeFor2Op * (BW / 2) / Simulator.SECOND) * Simulator.SECOND / (BW / 2));
		long remainingTimeForOp3 = (4 * OP_DATA_LENGTH - remainingTimeForOp0 * (LINK_BW - BW / 2) / Simulator.SECOND) * Simulator.SECOND / LINK_BW;
		Assert.assertEquals(timeFor2Op + remainingTimeForOp0 + remainingTimeForOp3, simulator.getTime());
	}

	@Test
	public void test3b() {
		final long BW = LINK_BW / 3;

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				o01_0.doPause(); o01_0.setResourceMax(BW / 2); o01_0.doStart();
				final NetworkOperation o01_1 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o02_0 = j0.sendData(j2, OP_DATA_LENGTH, getOperationListener());
				final NetworkOperation o01_3 = j0.sendData(j1, 5 * OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(BW / 2, o01_0.getAllocatedResource().getBw());
							Assert.assertTrue(o01_1.isRunning());
							Assert.assertNotNull(o01_1.getAllocatedResource());
							Assert.assertTrue(o02_0.isRunning());
							Assert.assertNotNull(o02_0.getAllocatedResource());
							Assert.assertTrue(o01_3.isRunning());
							Assert.assertNotNull(o01_3.getAllocatedResource());
							//make sure remaining bw is equally distributed
							Assert.assertEquals(o01_1.getAllocatedResource().getBw(), o02_0.getAllocatedResource().getBw());
							Assert.assertEquals(o01_1.getAllocatedResource().getBw(), o01_3.getAllocatedResource().getBw());
							assertLEquals(LINK_BW, o01_0.getAllocatedResource().getBw() + o01_1.getAllocatedResource().getBw() + o02_0.getAllocatedResource().getBw()
									 + o01_3.getAllocatedResource().getBw());
						} catch(AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		long timeFor2Op = (long) Math.ceil((double)OP_DATA_LENGTH * Simulator.SECOND / ((LINK_BW - BW / 2) / 3));
		long remainingTimeForOp0 = (long) Math.ceil((double)(OP_DATA_LENGTH - timeFor2Op * (BW / 2) / Simulator.SECOND) * Simulator.SECOND / (BW / 2));
		long remainingTimeForOp3 = (4 * OP_DATA_LENGTH - remainingTimeForOp0 * (LINK_BW - BW / 2) / Simulator.SECOND) * Simulator.SECOND / LINK_BW;
		Assert.assertEquals(timeFor2Op + remainingTimeForOp0 + remainingTimeForOp3, simulator.getTime());
	}

	@Test
	public void test4() {

		s0.setFailureState(FailureState.FAILED);

		NetworkLink link_h0s1 = null;
		for (NetworkInterface i: h0.getInterfaces())
			if (i.getRemoteNetworkInterface().getParent() == s1) {
				link_h0s1 = i.getUpLink();
				break;
			}
		Assert.assertNotNull(link_h0s1);

		NetworkLink link_s1h1 = null;
		for (NetworkInterface i: s1.getInterfaces())
			if (i.getRemoteNetworkInterface().getParent() == h1) {
				link_s1h1 = i.getUpLink();
				break;
			}
		Assert.assertNotNull(link_s1h1);

		final long lat0 = 100; final long lat1 = 500;
		final double loss0 = 0.1; final double loss1 = 0.5;

		Factory.getFactory(simulator).newNetworkProvisioner(null, link_h0s1, LINK_BW, lat0, loss0);
		Factory.getFactory(simulator).newNetworkProvisioner(null, link_s1h1, LINK_BW, lat1, loss1);

		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				//add network load
				final NetworkOperation o01_0 = j0.sendData(j1, OP_DATA_LENGTH, getOperationListener());

				Simulator.getSimulator().schedule(1, new EventImpl() {
					@Override
					public void process() {
						try {
							Assert.assertTrue(o01_0.isRunning());
							Assert.assertNotNull(o01_0.getAllocatedResource());
							Assert.assertEquals(LINK_BW, o01_0.getAllocatedResource().getBw());
						} catch(AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		Assert.assertEquals(lat0 + lat1 + (long) Math.ceil((double)OP_DATA_LENGTH * (1+ Math.max(loss0, loss1)) * Simulator.SECOND / LINK_BW), simulator.getTime());
	}
}
