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

import java.lang.ref.WeakReference;
import java.util.logging.Level;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.event.DispensableEventImpl;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.random.Exponential;

/**
 * This implementation generates failures and reparations with a {@link Exponential} probability
 * for each {@link FailureProneEntity} based the <tt>mtbf</tt> and <tt>mttr</tt> in their configuration.
 * 
 * @since 1.0
 */
public class FailuresFactoryDefault extends FailuresFactory {
	private static final Object PROP_FAILURE_EVENT = new Object();

	/**
	 * Weither failures should be generated for hosts only
	 */
	private boolean hostsOnly;

	protected abstract class FailureRepairEventImpl extends DispensableEventImpl implements FailureRepairEvent {
		private final WeakReference<FailureProneEntity> wfp;

		public FailureRepairEventImpl(FailureProneEntity entity) {
			super();
			wfp = new WeakReference<FailureProneEntity>(entity);
		}

		@Override
		public FailureProneEntity getEntity() {
			return wfp.get();
		}

		@Override
		public FailuresFactory getFactory() {
			return FailuresFactoryDefault.this;
		}
	}

	protected class RepairEventImpl extends FailureRepairEventImpl implements RepairEvent {
		public RepairEventImpl(FailureProneEntity entity) {
			super(entity);
		}

		@Override
		public void process() {
			FailureProneEntity fp = getEntity();
			if (fp == null)
				return;
			if (fp.getFailureState() == FailureState.FAILED) {
				getLogger().log(fp, "Repaired.");
				fp.setFailureState(FailureState.OK);
				FailuresFactoryDefault.this.enableFailures(fp);
			}
		}
	}

	protected class FailureEventImpl extends FailureRepairEventImpl implements FailureEvent {
		public FailureEventImpl(FailureProneEntity entity) {
			super(entity);
		}

		@Override
		public void process() {
			FailureProneEntity fp = getEntity();
			if (fp == null)
				return;
			if (fp.getFailureState() != FailureState.FAILED) {
				getLogger().log(fp, "Failed.");
				fp.setFailureState(FailureState.FAILED);
				FailuresFactoryDefault.this.enableRepairs(fp);
			}
		}
	}

	public FailuresFactoryDefault(Config config) {
		super(config);

		hostsOnly = false;
	}

	@Override
	public FailureRepairEvent getFutureEvent(Entity entity) {
		return (FailureRepairEvent) entity.getProperty(PROP_FAILURE_EVENT);
	}

	/**
	 * Schedules a repair event for the given entity.
	 * 
	 * @param fp
	 */
	protected void enableRepairs(FailureProneEntity fp) {
		if (!fp.supportsFailureStateUpdate())
			return;
		if (fp.getFailureState() != FailureState.FAILED)
			return;
		if (fp.getConfig() != null) {
			//configuration value is in hours
			long mttr = fp.getConfig().getLong("Mttr", 0l) * Simulator.HOUR;
			if (mttr != 0) {
				Event event = (Event) fp.getProperty(PROP_FAILURE_EVENT);
				if (event != null)
					event.cancel();
				event = new RepairEventImpl(fp);
				fp.setProperty(PROP_FAILURE_EVENT, event);
				Simulator.getSimulator().schedule((new Exponential(mttr)).nextLong(), event);
			}
		}
	}

	/**
	 * Schedules a failure event for the given entity.
	 * 
	 * @param fp
	 */
	protected void enableFailures(FailureProneEntity fp) {
		if (!fp.supportsFailureStateUpdate())
			return;
		if (fp.getFailureState() == FailureState.FAILED)
			return;
		if (fp.getConfig() != null) {
			//configuration value is in hours
			long mtbf = fp.getConfig().getLong("Mtbf", 0l) * Simulator.HOUR;
			if (mtbf != 0) {
				Event event = (Event) fp.getProperty(PROP_FAILURE_EVENT);
				if (event != null)
					event.cancel();
				event = new FailureEventImpl(fp);
				fp.setProperty(PROP_FAILURE_EVENT, event);
				Simulator.getSimulator().schedule((new Exponential(mtbf)).nextLong(), event);
			}
		}
	}

	@Override
	public void enable(Entity entity) {
		if (!(entity instanceof FailureProneEntity))
			return;
		if (!((FailureProneEntity)entity).supportsFailureStateUpdate())
			return;

		if (hostsOnly && (!(entity instanceof Host)))
			return;

		if (entity instanceof PoweredEntity) {
			if (!entity.getListeners(NotificationCodes.POWER_STATE_CHANGED).contains(getPowerListener()))
				entity.addListener(NotificationCodes.POWER_STATE_CHANGED, getPowerListenerSingle());
		}

		if (((FailureProneEntity)entity).getFailureState() == FailureState.FAILED)
			enableRepairs(((FailureProneEntity)entity));
		else
			enableFailures(((FailureProneEntity)entity));
	}

	@Override
	public void enableRec(Entity entity) {
		if (entity instanceof PoweredEntity) {
			entity.removeListener(NotificationCodes.POWER_STATE_CHANGED, getPowerListenerSingle());
			entity.addListener(NotificationCodes.POWER_STATE_CHANGED, getPowerListener());

			if (((PoweredEntity) entity).getPowerState() != PowerState.ON)
				return;
		}

		enable(entity);

		if (hostsOnly) {
			if (entity instanceof Simulator ||
					entity instanceof CloudProvider) {
				//ok continue
			} else {
				return; //we said only hosts, and hosts are children of CloudProvider so let's return
			}
		}

		for (Entity e: entity.getEntities()) {
			enableRec(e);
		}
	}

	@Override
	public void disable(Entity entity) {
		if (entity instanceof PoweredEntity) {
			entity.removeListener(NotificationCodes.POWER_STATE_CHANGED, getPowerListenerSingle());
			entity.removeListener(NotificationCodes.POWER_STATE_CHANGED, getPowerListener());
		}

		if (!(entity instanceof FailureProneEntity))
			return;
		if (!((FailureProneEntity)entity).supportsFailureStateUpdate())
			return;

		Event old = (Event) entity.getProperty(PROP_FAILURE_EVENT);
		if (old != null)
			old.cancel();
		entity.unsetProperty(PROP_FAILURE_EVENT);
	}

	@Override
	public void disableRec(Entity entity) {
		disable(entity);
		for (Entity e: entity.getEntities())
			disableRec(e);
	}

	@Override
	public Object generate() {
		Simulator.getSimulator().setRandomGenerator(this.getClass());

		hostsOnly = getConfig().getBoolean("HostsOnly", false);

		if (getConfig().getBoolean("Enabled", true)) {
			getLogger().log(Level.INFO, "Enabling failures and repairs " + (hostsOnly ? "for hosts only " : "") + "...");
			enableRec(Simulator.getSimulator());
		} else
			getLogger().log(Level.INFO, "Failures and repairs are disabled");

		Simulator.getSimulator().restoreRandomGenerator();
		return null;
	}

	protected class PowerStateNotificationListener extends NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (notification_code == NotificationCodes.POWER_STATE_CHANGED) {
				PoweredEntity e = (PoweredEntity) notifier;
				if (e.getPowerState() == PowerState.ON) {
					enableRec(e);
				} else {
					disableRec(e);
					e.addListener(NotificationCodes.POWER_STATE_CHANGED, this); //re-add the listener because disableRec will remove it
				}
			}
		}
	}

	private NotificationListener powerListener = null;
	protected NotificationListener getPowerListener() {
		if (powerListener != null)
			return powerListener;
		powerListener = new PowerStateNotificationListener();
		return powerListener;
	}

	protected class PowerStateNotificationListenerSingle extends NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			if (notification_code == NotificationCodes.POWER_STATE_CHANGED) {
				PoweredEntity e = (PoweredEntity) notifier;
				if (e.getPowerState() == PowerState.ON) {
					enable(e);
				} else {
					disable(e);
					e.addListener(NotificationCodes.POWER_STATE_CHANGED, this); //re-add the listener because disable will remove it
				}
			}
		}
	}

	private NotificationListener powerListenerSingle = null;
	protected NotificationListener getPowerListenerSingle() {
		if (powerListenerSingle != null)
			return powerListenerSingle;
		powerListenerSingle = new PowerStateNotificationListenerSingle();
		return powerListenerSingle;
	}

}
