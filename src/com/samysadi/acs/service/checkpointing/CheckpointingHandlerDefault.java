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

package com.samysadi.acs.service.checkpointing;

import java.util.Arrays;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.DispensableEventImpl;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.Checkpoint.CheckpointBusyState;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * This implementation relies on its configuration to 
 * determine checkpointing interval.
 * 
 * @since 1.0
 */
public class CheckpointingHandlerDefault extends EntityImpl implements CheckpointingHandler {
	private static final Object PROP_REGISTERED_CHECKPOINT = new Object();
	private static final Object PROP_CHECKPOINT_NEXT_EVENT = new Object();
	private static final Object PROP_CHECKPOINT_UPDATE_ERROR_COUNT = new Object();
	private static final Object PROP_CHECKPOINT_SCH_RECOVERY = new Object();
	private static final Object PROP_CHECKPOINT_VMTOREPLACE = new Object();
	private static final Object PROP_CHECKPOINT_BUF_EPOCH = new Object();

	private NotificationListener mainListener;

	public CheckpointingHandlerDefault() {
		super();
	}

	@Override
	public CheckpointingHandlerDefault clone() {
		final CheckpointingHandlerDefault clone = (CheckpointingHandlerDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.mainListener = null;
	}

	@Override
	public CloudProvider getParent() {
		return (CloudProvider) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof CloudProvider))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	public boolean register(VirtualMachine vm) {
		Host destinationHost = getParent().getVmPlacementPolicy().selectHost(vm, null, Arrays.asList(vm.getParent()));
		if (destinationHost == null)
			return false;
		register(vm, destinationHost);
		return false;
	}

	@Override
	public void register(VirtualMachine vm, Host destinationHost) {
		if (isRegistered(vm))
			throw new IllegalArgumentException("The given vm is already registered using this CheckpointingHandler.");
		final Checkpoint c = generateCheckpoint(vm);
		c.setDestinationHost(destinationHost);
		vm.setProperty(PROP_REGISTERED_CHECKPOINT, c);

		c.addListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, getMainListener());
		c.addListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, getMainListener());
		c.addListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, getMainListener());
		c.addListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, getMainListener());

		VirtualMachine vmToReplace = vm.clone();
		if (destinationHost.getCloudProvider().getVmPlacementPolicy().canPlaceVm(vmToReplace, destinationHost))
			destinationHost.getCloudProvider().getVmPlacementPolicy().placeVm(vmToReplace, destinationHost);

		c.setProperty(PROP_CHECKPOINT_VMTOREPLACE, vmToReplace);

		//buffer notifications
		if (isUseBufferOutput()) {
			vm.setFlag(VirtualMachine.FLAG_BUFFER_NETWORK_OUTPUT);
			c.setProperty(PROP_CHECKPOINT_BUF_EPOCH, vm.getNotificationsBufferEpoch());
		}

		scheduleUpdateEvent(c, 0l);

		vm.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getMainListener());
	}

	@Override
	public void unregister(VirtualMachine vm) {
		Checkpoint c = getCheckpoint(vm);

		if (c == null)
			throw new IllegalArgumentException("The given vm is not registered using this CheckpointingHandler.");

		vm.unsetProperty(PROP_REGISTERED_CHECKPOINT);

		if (c.getParent() != vm)
			return;

		Event e = (Event) c.getProperty(PROP_CHECKPOINT_NEXT_EVENT);
		if (e != null)
			e.cancel();

		c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, getMainListener());
		c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, getMainListener());
		c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, getMainListener());
		c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, getMainListener());

		c.getParent().removeListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getMainListener());

		VirtualMachine vmToReplace = (VirtualMachine) c.getProperty(PROP_CHECKPOINT_VMTOREPLACE);
		if (vmToReplace != null)
			vmToReplace.unplace();

		if (isUseBufferOutput()) {
			vm.releaseBufferedNotifications(vm.getNotificationsBufferEpoch());
			vm.unsetFlag(VirtualMachine.FLAG_BUFFER_NETWORK_OUTPUT);
		}

		if (c.getBusyState() == CheckpointBusyState.IDLE)
			c.setParent(null);
		else
			c.addListener(NotificationCodes.CHECKPOINT_BUSY_STATE_CHANGED, new MyStaticListener0());
	}

	private static final class MyStaticListener0 extends
			NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			Checkpoint c = (Checkpoint) notifier;
			if (c.getBusyState() == CheckpointBusyState.IDLE) {
				c.setParent(null);
				this.discard();
			}
		}
	}

	@Override
	public final boolean isRegistered(VirtualMachine vm) {
		return getCheckpoint(vm) != null;
	}

	@Override
	public Checkpoint getCheckpoint(VirtualMachine vm) {
		return (Checkpoint) vm.getProperty(PROP_REGISTERED_CHECKPOINT);
	}

	private boolean isUseBufferOutput() {
		boolean v = false;
		if (getConfig() == null)
			return v;
		return getConfig().getBoolean("BufferOutput", v);
	}

	private Event scheduleUpdateEvent(final Checkpoint c, long delay) {
		Event e = new DispensableEventImpl() {
			@Override
			public void process() {
				if (c.canUpdate())
					c.update();
				else {
					unregister(c.getParent());
					register(c.getParent());
				}
			}
		};
		c.setProperty(PROP_CHECKPOINT_NEXT_EVENT, e);
		Simulator.getSimulator().schedule(delay, e);
		return e;
	}

	private NotificationListener getMainListener() {
		if (this.mainListener == null) {
			this.mainListener = new NotificationListener() {
				private boolean checkRecovery(Checkpoint c) {
					boolean recoveryScheduled = c.getProperty(PROP_CHECKPOINT_SCH_RECOVERY) != null;
					if (!recoveryScheduled)
						return false;

					VirtualMachine vmToReplace = (VirtualMachine) c.getProperty(PROP_CHECKPOINT_VMTOREPLACE);
					if (!c.canRecover(c.getDestinationHost(), vmToReplace))
						return false;

					c.unsetProperty(PROP_CHECKPOINT_SCH_RECOVERY);
					c.recover(c.getDestinationHost(), vmToReplace);
					return true;
				}

				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (notification_code == NotificationCodes.CHECKPOINT_UPDATE_SUCCESS) {
						Checkpoint c = (Checkpoint) notifier;
						c.setProperty(PROP_CHECKPOINT_UPDATE_ERROR_COUNT, Integer.valueOf(0));

						if (isUseBufferOutput()) {
							Integer oldEpoch = (Integer) c.getProperty(PROP_CHECKPOINT_BUF_EPOCH);
							if (oldEpoch != null)
								c.getParent().releaseBufferedNotifications(oldEpoch);
							c.setProperty(PROP_CHECKPOINT_BUF_EPOCH, Integer.valueOf(c.getCheckpointEpoch()));
						}

						if (checkRecovery(c))
							return;

						scheduleUpdateEvent(c, getCheckpointingInterval(c));
					} else if (notification_code == NotificationCodes.CHECKPOINT_UPDATE_ERROR) {
						Checkpoint c = (Checkpoint) notifier;

						int error_count = (Integer) c.getProperty(PROP_CHECKPOINT_UPDATE_ERROR_COUNT, Integer.valueOf(0));
						error_count++;
						c.setProperty(PROP_CHECKPOINT_UPDATE_ERROR_COUNT, error_count);

						if (checkRecovery(c))
							return;

						if (error_count >= getUpdateErrorCountThreshold()) {
							VirtualMachine vm = c.getParent();
							unregister(vm);
							register(vm);
						} else 
							scheduleUpdateEvent(c, getCheckpointingInterval(c));
					} else if (notification_code == NotificationCodes.CHECKPOINT_RECOVER_SUCCESS) {
						Checkpoint c = (Checkpoint) notifier;

						VirtualMachine vm = c.getParent();
						unregister(vm);

						VirtualMachine newVm = (VirtualMachine) data;
						if (newVm.canStart())
							newVm.doStart();
						if (isRegistered(newVm))
							unregister(newVm); //properties of the newVm have been cloned from the registered vm!
						register(newVm);
					} else if (notification_code == NotificationCodes.CHECKPOINT_RECOVER_ERROR) {
						Checkpoint c = (Checkpoint) notifier;

						c.setProperty(PROP_CHECKPOINT_SCH_RECOVERY, new Object());
						c.transfer(getParent().getVmPlacementPolicy().selectHost(c.getParent(), null, Arrays.asList(c.getParent().getParent(), c.getDestinationHost())));
					} else if (notification_code == NotificationCodes.CHECKPOINT_BUSY_STATE_CHANGED) {
						Checkpoint c = (Checkpoint) notifier;
						if (c.getBusyState() != CheckpointBusyState.IDLE)
							return;

						if (checkRecovery(c))
							return;
					} else if (notification_code == NotificationCodes.RUNNABLE_STATE_CHANGED) {
						VirtualMachine vm = (VirtualMachine) notifier;
						if (vm.getRunnableState() == RunnableState.FAILED) {
							Checkpoint c = (Checkpoint) vm.getProperty(PROP_REGISTERED_CHECKPOINT);
							if (c != null) {
								c.setProperty(PROP_CHECKPOINT_SCH_RECOVERY, new Object());
								if (checkRecovery(c))
									return;
							}
						}
					}
				}
			};
		}
		return this.mainListener;
	}

	protected Checkpoint generateCheckpoint(VirtualMachine vm) {
		Checkpoint c = Factory.getFactory(this).newCheckpoint(null, vm);
		c.setConfig(getConfig());
		return c;
	}

	protected int getUpdateErrorCountThreshold() {
		return FactoryUtils.generateInt("UpdateErrorCountThreshold", getConfig(), 3);
	}

	/**
	 * Returns the delay before next update.
	 * 
	 * @param c
	 * @return the delay before next update
	 */
	protected long getCheckpointingInterval(final Checkpoint c) {
		long v = 200 * Simulator.MILLISECOND;

		Double d = FactoryUtils.generateDouble("Interval", getConfig(), null);
		if (d != null)
			v = Math.round(d.doubleValue() * Simulator.SECOND);
		
		return Math.max(0, v - Simulator.getSimulator().getTime() + c.getCheckpointTime());
	}
}
