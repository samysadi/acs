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

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventChain;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.Checkpoint;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.utility.factory.TraceFactory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs_test.Utils;


/**
 * Following tests have been written assuming:<ul>
 * <li>recovery/checkpoint overhead == 0
 * <li>checkpoint compression == 0%
 * </ul>
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class CheckpointTest {
	Simulator simulator;
	CloudProvider cloudProvider;
	Host hPrimary;
	Host hCheckpoint;
	Host hCheckpoint2;
	Host hRecovery;
	final long[] finished = new long[]{0l};

	VirtualMachine vmPrimary;
	Job jobPrimary;

	long old_c = 0;
	long timeOffset = 0;

	long PU;
	long NETWORK;
	long STORAGE;

	private volatile AssertionError exc;

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
		cloudProvider = FactoryUtils.generateCloudProvider(simulator.getConfig());

		hPrimary = cloudProvider.getHosts().get(0); hPrimary.setName("HostPrime");
		hCheckpoint = cloudProvider.getHosts().get(1); hCheckpoint.setName("HostCheckpoint");
		hRecovery = cloudProvider.getHosts().get(cloudProvider.getHosts().size()-1); hRecovery.setName("HostRecovery");


		Host h2 = FactoryUtils.generateHost(hCheckpoint.getConfig(), cloudProvider);
		FactoryUtils.linkDevices(simulator.getConfig(), h2, cloudProvider.getSwitches().get(0), 100 * Simulator.MEBIBYTE, 100 * Simulator.MEBIBYTE, 0, 0);
		hCheckpoint2 = h2; hCheckpoint2.setName("HostCheckpoint2");

		PU = hPrimary.getProcessingUnits().get(0).getComputingProvisioner().getCapacity();
		NETWORK = hPrimary.getInterfaces().get(0).getUpLink().getNetworkProvisioner().getCapacity();
		STORAGE = hPrimary.getStorages().get(0).getStorageProvisioner().getCapacity();

		Assert.assertEquals("We need same up and down BW for this test", NETWORK, hPrimary.getInterfaces().get(0).getDownLink().getNetworkProvisioner().getCapacity());

		cloudProvider.getPowerManager().powerOn(hPrimary);
		cloudProvider.getPowerManager().powerOn(hCheckpoint);
		cloudProvider.getPowerManager().powerOn(hCheckpoint2);
		cloudProvider.getPowerManager().powerOn(hRecovery);

		vmPrimary = FactoryUtils.generateVirtualMachine(cloudProvider.getConfig(), null, null);
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				cloudProvider.getVmPlacementPolicy().placeVm(vmPrimary, hPrimary);
			}
		});
		jobPrimary = Factory.getFactory(simulator).newJob(null, vmPrimary);
	}

	private NotificationListener getOperationListener() {
		return new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				ComputingOperation n = (ComputingOperation) notifier;
//				if (n.getRunnableState() == RunnableState.PAUSED)
//					return;

				n.getLogger().log(n, "On " + n.getParent().getParent().getParent().getName() +
						": " + n.getRunnableState() + ": " + n.getAllocatedResource() + " comp=" + n.getCompletedLength());

				if (n.getRunnableState() == RunnableState.COMPLETED)
					finished[0] = Simulator.getSimulator().getTime();
			}
		};
	}

	private void assertLEquals(long expected, long time) {
		long d = expected - time;
		Assert.assertTrue("Expected <" + expected + "> but was: <" + time + ">", Math.abs(d) <= 1);
	}

	public void testWith(final long COMPUTE_LENGTH,
			final long CHECKPOINT_RAM_SIZE, final long CHECKPOINT_STORAGE_SIZE) {
		final Checkpoint c = Factory.getFactory(simulator).newCheckpoint(null, vmPrimary);
		c.setDestinationHost(hCheckpoint);

		//first launch computing
		simulator.schedule(0, new EventChain() {
			@Override
			public boolean processStage(int stageNum) {
				if (stageNum < 10)
					return CONTINUE;
				try {
					if (vmPrimary.canStart())
						vmPrimary.doStart();
					if (vmPrimary.canStart())
						jobPrimary.doStart();
	
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

					if (CHECKPOINT_RAM_SIZE>0)
						jobPrimary.allocateRam(CHECKPOINT_RAM_SIZE).modify(0, CHECKPOINT_RAM_SIZE);
					
					if (CHECKPOINT_STORAGE_SIZE>0)
						jobPrimary.createFile(CHECKPOINT_STORAGE_SIZE).modify(0, CHECKPOINT_STORAGE_SIZE);
				} catch(AssertionError e) {
					exc = e;
				}
				return STOP;
			}
		});

		//
		final long CHECKPOINT_START_SEC = 5;
		timeOffset = CHECKPOINT_START_SEC * Simulator.SECOND;

		simulator.schedule(CHECKPOINT_START_SEC * Simulator.SECOND, new EventImpl() {
			@Override
			public void process() {
				NotificationListener l = new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						try {
							long expected = timeOffset + (CHECKPOINT_RAM_SIZE + CHECKPOINT_STORAGE_SIZE) * Simulator.SECOND / NETWORK;
							simulator.getLogger().log(NotificationCodes.notificationCodeToString(notification_code));

							Assert.assertEquals(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, notification_code);
							assertLEquals(expected, Simulator.getSimulator().getTime());

							old_c = ((ComputingOperation)jobPrimary.getOperations().get(0)).getCompletedLength();

							c.transfer(hCheckpoint2);

						} catch (AssertionError e) {
							exc = e;
						}
					}
				};
				c.addListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, l);
				c.addListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, l);

				NotificationListener l2 = new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						try {
							simulator.getLogger().log(NotificationCodes.notificationCodeToString(notification_code));

							simulator.schedule(2 * Simulator.SECOND, new EventImpl() {
								@Override
								public void process() {
									hPrimary.setFailureState(FailureState.FAILED);
									c.recover(hRecovery, null);

									timeOffset = Simulator.getSimulator().getTime();
								}
							});

							timeOffset = Simulator.getSimulator().getTime();
						} catch (AssertionError e) {
							exc = e;
						}
					}
				};
				c.addListener(NotificationCodes.CHECKPOINT_TRANSFER_ERROR, l2);
				c.addListener(NotificationCodes.CHECKPOINT_TRANSFER_SUCCESS, l2);
				NotificationListener l3 = new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						try {
							VirtualMachine vm = ((VirtualMachine) data);
							Assert.assertNotNull(vm);
							vm.doStart();
							simulator.getLogger().log(NotificationCodes.notificationCodeToString(notification_code));

							assertLEquals(timeOffset + c.getCheckpointSize() * Simulator.SECOND / NETWORK, 
									Simulator.getSimulator().getTime());

							timeOffset = Simulator.getSimulator().getTime();
						} catch (AssertionError e) {
							exc = e;
						}
					}
				};
				c.addListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, l3);
				c.addListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, l3);
				c.update();
			}
		});



		simulator.start();
		if (exc != null)
            throw exc;

		old_c = (COMPUTE_LENGTH - old_c);
		if (old_c > 0)
			assertLEquals(timeOffset + old_c * Simulator.SECOND / PU
					, finished[0]);
	}



	@Test
	public void test5() {
		testWith(
				13 * 1000 * Simulator.MI,
				630 * Simulator.MEBIBYTE,
				510 * Simulator.MEBIBYTE
			);
	}

	@Test
	public void test0() {
		testWith(
				10 * 1000 * Simulator.MI,
				0 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE
			);
	}

	@Test
	public void test1() {
		testWith(
				11 * 1000 * Simulator.MI,
				33 * Simulator.MEBIBYTE,
				0 * Simulator.MEBIBYTE
			);
	}


	@Test
	public void test2() {
		testWith(
				7 * 1000 * Simulator.MI,
				0 * Simulator.MEBIBYTE,
				33 * Simulator.MEBIBYTE
			);
	}

	@Test
	public void test3() {
		testWith(
				13 * 1000 * Simulator.MI,
				33 * Simulator.MEBIBYTE,
				51 * Simulator.MEBIBYTE
			);
	}

	@Test
	public void test4() {
		testWith(
				13 * 1000 * Simulator.MI,
				330 * Simulator.MEBIBYTE,
				51 * Simulator.MEBIBYTE
			);
	}

	@Test
	public void test6() {
		testWith(
				13 * 1000 * Simulator.MI,
				0 * Simulator.MEBIBYTE,
				100 * Simulator.MEBIBYTE
			);
	}
}
