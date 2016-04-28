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
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 *
 * @since 1.0
 */
public class VirtualMachineFactoryDefault extends VirtualMachineFactory {

	public VirtualMachineFactoryDefault(Config config,
			CloudProvider cloudProvider, User user) {
		super(config, cloudProvider, user);
	}

	private static void _notifyDone(VirtualMachine vm) {
		if (vm.getUser() != null)
			vm.getUser().notify(NotificationCodes.FACTORY_VIRTUALMACHINE_GENERATED, vm);
	}

	@Override
	public VirtualMachine generate() {
		VirtualMachine vm = newVirtualMachine(null);
		vm.setConfig(getConfig());
		vm.setName(getConfig().getString("Name", null));

		newPuAllocator(null, vm);

		vm.setUser(getUser());

		if (getCloudProvider() == null) {
			_notifyDone(vm);
			return vm;
		}

		Host host = getCloudProvider().getVmPlacementPolicy().selectHost(vm);
		if (host == null) {
			vm.setUser(null);
			return null;
		}

		vm.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();

				VirtualMachine vm = (VirtualMachine) notifier;

				vm.doStart();

				FactoryUtils.generateTraces(getConfig(), vm);

				Config cfg = vm.getConfig().addContext(FactoryUtils.VmCheckpoint_CONTEXT);

				if (cfg.getBoolean("Enabled", false))
					getCloudProvider().getVmCheckpointingHandler().register(vm, cfg);

				_notifyDone(vm);
			}
		});

		getCloudProvider().getVmPlacementPolicy().placeVm(vm, host);

		return vm;
	}

}
