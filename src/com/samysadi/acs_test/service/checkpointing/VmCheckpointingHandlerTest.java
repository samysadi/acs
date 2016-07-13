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

package com.samysadi.acs_test.service.checkpointing;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventChain;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.checkpoint.Checkpoint;
import com.samysadi.acs.service.checkpointing.checkpoint.VmCheckpoint;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.factory.TraceFactory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs_test.Utils;


/**
 *
 * @since 1.0
 */
public class VmCheckpointingHandlerTest {
	Simulator simulator;
	CloudProvider cloudProvider;
	Host hPrimary;
	Host hRecovery;
	Host hReceive;

	VirtualMachine vmPrimary;
	Job jobPrimary;

	private volatile AssertionError exc;
	protected long lastComp = -1;
	protected long lastSent = -1;


	protected long CHECKPOINT_FAILURE_SEC = 7;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@BeforeClass
	public static void beforeTests() {
		TraceFactory.IS_TRACING_DISABLED = true;
	}

	@Before
	public void beforeTest() {

		simulator = Utils.newSimulator();
		simulator.getConfig().setInt("Trace.Count", 0);
		cloudProvider = FactoryUtils.generateCloudProvider(simulator.getConfig().addContext(FactoryUtils.CloudProvider_CONTEXT, 0));
		cloudProvider.getConfig()
			.addContext(FactoryUtils.VmCheckpointingHandler_CONTEXT)
			.setInt("Interval", 2);

		hPrimary = cloudProvider.getHosts().get(0); hPrimary.setName("HostPrimary");

		cloudProvider.getPowerManager().powerOn(hPrimary);

		vmPrimary = FactoryUtils.generateVirtualMachine(cloudProvider.getConfig(), null, null);
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				cloudProvider.getVmPlacementPolicy().placeVm(vmPrimary, hPrimary);
			}
		});
		jobPrimary = Factory.getFactory(simulator).newJob(null, vmPrimary);

		cloudProvider.getVmCheckpointingHandler().addGlobalListener(new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				String s = "";
				if (notification_code == NotificationCodes.CHECKPOINT_STATE_CHANGED) {
					VmCheckpoint c = ((VmCheckpoint) notifier);
					if (c.isCheckpointBusy())
						s = " : busy : ";
					if (c.isCheckpointStateSet(Checkpoint.CHECKPOINT_STATE_UPDATING))
							s += "updating";
					else if (c.isCheckpointStateSet(Checkpoint.CHECKPOINT_STATE_RECOVERING))
						s += "recovering";
					else if (c.isCheckpointStateSet(Checkpoint.CHECKPOINT_STATE_COPYING))
						s += "copying";

				}
				simulator.getLogger().log(NotificationCodes.notificationCodeToString(notification_code) + s);
			}
		});
	}

	private NotificationListener getOperationListener() {
		return new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				ComputingOperation n = (ComputingOperation) notifier;
//				if (n.getRunnableState() == RunnableState.PAUSED)
//					return;

				n.getLogger().log(n, "On " + (n.hasParentRec() ? n.getParent().getParent().getParent().getName() : "?") +
						": " + n.getRunnableState() + ": " + n.getAllocatedResource() + " computed=" + n.getCompletedLength());

				if (!n.isTerminated() || n.getRunnableState()==RunnableState.COMPLETED)
					lastComp  = n.getLength() - n.getCompletedLength();
			}
		};
	}

	private NotificationListener getNetworkOperationListener(final List<Long> list) {
		return new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				NetworkOperation n = (NetworkOperation) notifier;
//				if (n.getRunnableState() == RunnableState.PAUSED)
//					return;


				n.getLogger().log(n, "On " + (n.hasParentRec() ? n.getParent().getParent().getParent().getName() : "?") +
						": " + n.getRunnableState() + ": " + n.getAllocatedResource() + " sent=" + n.getCompletedLength());

				if (!n.isTerminated() || n.getRunnableState()==RunnableState.COMPLETED)
					lastSent  = n.getLength() - n.getCompletedLength();

				if (list != null && n.getRunnableState() == RunnableState.COMPLETED)
					list.add(n.getId());
			}
		};
	}

	public void testWith(final long COMPUTE_LENGTH, final long SEND_LENGTH,
			final long CHECKPOINT_RAM_SIZE, final long CHECKPOINT_STORAGE_SIZE, final boolean OCM) {

		final List<Long> list = new ArrayList<Long>();

		Simulator.getSimulator().getLogger().log("Compute_LENGTH=" + COMPUTE_LENGTH);
		Simulator.getSimulator().getLogger().log("Send_LENGTH=" + SEND_LENGTH);

		if (OCM)
			cloudProvider.getConfig()
			.addContext(FactoryUtils.VmCheckpointingHandler_CONTEXT)
			.setBoolean("BufferOutput", true);

		//first launch computing
		simulator.schedule(0, new EventChain() {
			@Override
			public boolean processStage(int stageNum) {
				if (stageNum < 10)
					return CONTINUE;
				if (vmPrimary.canStart())
					vmPrimary.doStart();
				if (vmPrimary.canStart())
					jobPrimary.doStart();

				cloudProvider.getVmCheckpointingHandler().register(vmPrimary, null);

				//make sure receving host is not the recovery host
				for (Host h: cloudProvider.getHosts()) {
					if (h.getPowerState() == PowerState.OFF) {
						hReceive = h;
						hReceive.setName("HostReceive");
						cloudProvider.getPowerManager().powerOn(hReceive);
						break;
					}
				}

				Simulator.getSimulator().schedule(new EventImpl() {
					@Override
					public void process() {
						try {
							Operation<?> op0 = jobPrimary.compute(COMPUTE_LENGTH, getOperationListener());
							Assert.assertNotNull(op0);
							op0.addListener(NotificationCodes.ENTITY_CLONED, new NotificationListener() {
								@Override
								protected void notificationPerformed(Notifier notifier,
										int notification_code, Object data) {
									((Notifier)data).addListener(notification_code, this);
									((Notifier)data).addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getOperationListener());
								}
							});

							Operation<?> op1 = jobPrimary.sendData(hReceive, SEND_LENGTH, getNetworkOperationListener(list));
							Assert.assertNotNull(op1);
							op1.addListener(NotificationCodes.ENTITY_CLONED, new NotificationListener() {
								@Override
								protected void notificationPerformed(Notifier notifier,
										int notification_code, Object data) {
									((Notifier)data).addListener(notification_code, this);
									((Notifier)data).addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getNetworkOperationListener(list));
								}
							});

							if (CHECKPOINT_RAM_SIZE>0)
								jobPrimary.allocateRam(CHECKPOINT_RAM_SIZE).modify(0, CHECKPOINT_RAM_SIZE);

							if (CHECKPOINT_STORAGE_SIZE>0)
								jobPrimary.createFile(CHECKPOINT_STORAGE_SIZE).modify(0, CHECKPOINT_STORAGE_SIZE);
						} catch(AssertionError e) {
							exc = e;
						}
					}

				});
				return STOP;
			}
		});

		simulator.schedule(CHECKPOINT_FAILURE_SEC * Simulator.SECOND, new EventImpl() {
			@Override
			public void process() {
				hPrimary.setFailureState(FailureState.FAILED);
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;

		if (OCM)
			Assert.assertEquals(1, list.size()); //only one completion event should be thrown

		Assert.assertEquals(0, lastComp);
		Assert.assertEquals(0, lastSent);
	}

	@Test
	public void test0() {
		testWith(
				10 * 1000 * Simulator.MI,
				651 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test0t() {
		testWith(
				10 * 1000 * Simulator.MI,
				651 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				true
			);
	}

	@Test
	public void test1() {
		testWith(
				11 * 1000 * Simulator.MI,
				1100 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test1t() {
		testWith(
				11 * 1000 * Simulator.MI,
				1100 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				true
			);
	}

	@Test
	public void test2() {
		testWith(
				7 * 1000 * Simulator.MI,
				701 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test2t() {
		testWith(
				7 * 1000 * Simulator.MI,
				701 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE,
				true
			);
	}

	@Test
	public void test3() {
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE,
				51 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test3t() {
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE,
				51 * Simulator.MEBIBYTE,
				true
			);
	}

	@Test
	public void test4() {
		CHECKPOINT_FAILURE_SEC = 10;
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				330 * Simulator.MEBIBYTE,
				51 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test4t() {
		CHECKPOINT_FAILURE_SEC = 10;
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				330 * Simulator.MEBIBYTE,
				51 * Simulator.MEBIBYTE,
				true
			);
	}

	@Test
	public void test5() {
		CHECKPOINT_FAILURE_SEC = 12;
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				63 * Simulator.MEBIBYTE,
				510 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test5t() {
		CHECKPOINT_FAILURE_SEC = 12;
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				63 * Simulator.MEBIBYTE,
				510 * Simulator.MEBIBYTE,
				true
			);
	}

	@Test
	public void test6() {
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				100 * Simulator.MEBIBYTE,
				false
			);
	}

	@Test
	public void test6t() {
		testWith(
				13 * 1000 * Simulator.MI,
				1301 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE,
				100 * Simulator.MEBIBYTE,
				true
			);
	}
}

