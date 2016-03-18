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

package com.samysadi.acs.user;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachineDefault;

/**
 *
 * @since 1.0
 */
public class ThinClientVirtualMachineDefault extends VirtualMachineDefault implements ThinClientVirtualMachine {
	private NotificationListener autoStartListener;

	public ThinClientVirtualMachineDefault() {
		super();
	}

	@Override
	public ThinClientVirtualMachineDefault clone() {
		final ThinClientVirtualMachineDefault clone = (ThinClientVirtualMachineDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.autoStartListener = newAutoStartListener();
	}

	@Override
	public ThinClient getParent() {
		return (ThinClient) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof ThinClient))
				throw new IllegalArgumentException("The given entity cannot be a parent of this entity");

		super.setParent(parent);
	}

	@Override
	protected void ancestorChanged(EntityImpl ancestor) {
		checkStartVm();
		super.ancestorChanged(ancestor);
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);
		if (oldParent != null) {
			oldParent.removeListener(NotificationCodes.FAILURE_STATE_CHANGED, autoStartListener);
			oldParent.removeListener(NotificationCodes.POWER_STATE_CHANGED, autoStartListener);
		}
		if (getParent() != null) {
			getParent().addListener(NotificationCodes.FAILURE_STATE_CHANGED, autoStartListener);
			getParent().addListener(NotificationCodes.POWER_STATE_CHANGED, autoStartListener);
		}
	}

	private NotificationListener newAutoStartListener() {
		return new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				checkStartVm();
			}
		};
	}

	private boolean canStartVm() {
		return (this.canStart() || this.canRestart()) &&
				this.getParent().getFailureState() == FailureState.OK &&
				this.getParent().getPowerState() == PowerState.ON;
	}

	private void checkStartVm() {
		if (canStartVm()) {
			if (this.canStart())
				this.doStart();
			else if (this.canRestart())
				this.doRestart();
		}
	}

	@Override
	public void setUsableProcessingUnits(List<ProcessingUnit> processingUnits, boolean allocatePu) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setUsableNetworkInterfaces(List<NetworkInterface> networkInterfaces) {
		super.setUsableNetworkInterfaces(networkInterfaces);
	}

	@Override
	public void setVirtualRam(VirtualRam v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVirtualStorage(VirtualStorage v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setUser(User user) {
		//nothing is done, just ignore this
	}

	@Override
	public User getUser() {
		if (getParent() == null)
			return null;
		return getParent().getParent();
	}
}
