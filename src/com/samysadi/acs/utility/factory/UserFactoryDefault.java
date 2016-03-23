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

package com.samysadi.acs.utility.factory;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.generation.flow.GenerationFlowInfo;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 *
 * @since 1.0
 */
public class UserFactoryDefault extends UserFactory {

	public UserFactoryDefault(Config config, CloudProvider cloudProvider) {
		super(config, cloudProvider);
	}

	/*
	 * ThinClients generation
	 * ************************************************************************
	 */

	private void generateThinClient(User user, boolean init, int totalCount, int index, int count,
			NotificationListener l) {
		if (index >= count) {
			if (init)
				Simulator.getSimulator().restoreRandomGenerator();

			l.discard();
			user.removeListener(NotificationCodes.FACTORY_THINCLIENT_GENERATED, l);
			user.notify(NotificationCodes.FACTORY_ALL_THINCLIENTS_GENERATED, null);

			//generate the remaining entities
			int r = totalCount - count;
			if (r > 0)
				generateThinClients(user, r, false);

			//if init then continue generation
			if (init)
				_generate1(user);
			return;
		}

		FactoryUtils.generateThinClient(getThinClientGenerationMode().next(), user);
	}

	private void generateThinClients(final User user, final int totalCount, boolean _init) {
		GenerationFlowInfo n = getThinClientGenerationFlow().next();

		final int count = Math.min(Math.max(1, n.getCount()), totalCount);
		long delay = n.getDelay();

		final boolean init = _init && (delay == 0);

		Simulator.getSimulator().schedule(n.getDelay(), new EventImpl() {
			@Override
			public void process() {
				if (init)
					Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());

				final int[] indexTab = {0};
				NotificationListener l = new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						generateThinClient(user, init, totalCount, indexTab[0], count, this);
						indexTab[0]++;
					}
				};

				user.addListener(NotificationCodes.FACTORY_THINCLIENT_GENERATED, l);
				generateThinClient(user, init, totalCount, indexTab[0], count, l);
				indexTab[0]++;
			}
		});
	}

	//thin clients
	private void _generate0(User user) {
		final int totalCount = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.ThinClient_CONTEXT), 1);
		generateThinClients(user, totalCount, true);
	}

	/*
	 * VirtualMachines generation
	 * ************************************************************************
	 */

	private boolean generateVirtualMachine(User user, boolean init, boolean resubmitVms, int totalCount, int index, int count,
			NotificationListener l) {
		if (index >= count) {
			if (init)
				Simulator.getSimulator().restoreRandomGenerator();

			l.discard();
			user.removeListener(NotificationCodes.FACTORY_VIRTUALMACHINE_GENERATED, l);
			user.notify(NotificationCodes.FACTORY_ALL_VIRTUALMACHINES_GENERATED, null);

			//generate the remaining entities
			int r = totalCount - count;
			if (r > 0)
				generateVirtualMachines(user, r, false, resubmitVms);

			//if init then continue generation
			if (init)
				_generate2(user);
			return true;
		}

		VirtualMachine vm = FactoryUtils.generateVirtualMachine(getVmGenerationMode().next(), getCloudProvider(), user);
		return vm != null;
	}

	private void generateVirtualMachines(final User user, final int totalCount, boolean _init, final boolean resubmitVms) {
		GenerationFlowInfo n = getVmGenerationFlow().next();

		final int count = Math.min(Math.max(1, n.getCount()), totalCount);
		long delay = n.getDelay();

		final boolean init = _init && (delay == 0);

		Simulator.getSimulator().schedule(n.getDelay(), new EventImpl() {
			@Override
			public void process() {
				if (init)
					Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());

				final int[] indexTab = {0};
				final int[] resubmitCountTab = {0};
				NotificationListener l = new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						while (true) {
							boolean r = generateVirtualMachine(user, init, resubmitVms, totalCount + resubmitCountTab[0], indexTab[0], count, this);
							indexTab[0]++;
							if (r)
								break;
							resubmitCountTab[0]++;
						}
					}
				};

				user.addListener(NotificationCodes.FACTORY_VIRTUALMACHINE_GENERATED, l);
				while (true) {
					boolean r = generateVirtualMachine(user, init, resubmitVms, totalCount + resubmitCountTab[0], indexTab[0], count, l);
					indexTab[0]++;
					if (r)
						break;
					resubmitCountTab[0]++;
				}
			}
		});
	}

	//Virtual machines
	private void _generate1(User user) {
		final int totalCount = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.VirtualMachine_CONTEXT), 0);
		generateVirtualMachines(user, totalCount, true, false);
	}

	/*
	 * Workloads generation
	 * ************************************************************************
	 */

	private void generateWorkload(User user, boolean init, int totalCount, int index, int count,
			NotificationListener l) {
		if (index >= count) {
			if (init)
				Simulator.getSimulator().restoreRandomGenerator();

			l.discard();
			user.removeListener(NotificationCodes.FACTORY_WORKLOAD_GENERATED, l);
			user.notify(NotificationCodes.FACTORY_ALL_WORKLOADS_GENERATED, null);

			//generate the remaining entities
			int r = totalCount - count;
			if (r > 0)
				generateWorkloads(user, r, false);

			//if init then continue generation
			if (init)
				_generate3(user);
			return;
		}

		FactoryUtils.generateWorkload(getWorkloadGenerationMode().next(), user, init);
	}

	private void generateWorkloads(final User user, final int totalCount, boolean _init) {
		GenerationFlowInfo n = getWorkloadGenerationFlow().next();

		final int count = Math.min(Math.max(1, n.getCount()), totalCount);
		long delay = n.getDelay();

		final boolean init = _init && (delay == 0);

		Simulator.getSimulator().schedule(n.getDelay(), new EventImpl() {
			@Override
			public void process() {
				if (init)
					Simulator.getSimulator().setRandomGenerator(UserFactoryDefault.this.getClass());

				final int[] indexTab = {0};
				NotificationListener l = new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						generateWorkload(user, init, totalCount, indexTab[0], count, this);
						indexTab[0]++;
					}
				};

				user.addListener(NotificationCodes.FACTORY_WORKLOAD_GENERATED, l);
				generateWorkload(user, init, totalCount, indexTab[0], count, l);
				indexTab[0]++;
			}
		});
	}

	//workloads
	private void _generate2(User user) {
		final int totalCount = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.Workload_CONTEXT), 0);
		generateWorkloads(user, totalCount, true);
	}

	/*
	 * Misc
	 * ************************************************************************
	 */

	private void _generate3(User user) {
		FactoryUtils.generateTraces(getConfig(), user);

		Simulator.getSimulator().restoreRandomGenerator();

		if (getCloudProvider()!=null)
			getCloudProvider().notify(NotificationCodes.FACTORY_USER_GENERATED, user);
	}

	@Override
	public User generate() {
		Simulator.getSimulator().setRandomGenerator(this.getClass());

		final User user = newUser(null, getCloudProvider());
		user.setConfig(getConfig());
		user.setName(getConfig().getString("Name", null));

		_generate0(user);

		return user;
	}

}
