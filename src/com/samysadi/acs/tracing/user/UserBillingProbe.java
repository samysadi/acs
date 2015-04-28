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

package com.samysadi.acs.tracing.user;

import java.util.ArrayList;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.ModifiableProbe;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.probetypes.PriceProbe;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.cp.CpUserBillingProbe;
import com.samysadi.acs.tracing.vm.VmRunningTimeProbe;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.Pair;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class UserBillingProbe extends AbstractProbe<Long> implements PriceProbe {
	public static final String KEY = UserBillingProbe.class.getSimpleName().substring(0, 
									UserBillingProbe.class.getSimpleName().length() - 5);

	private ArrayList<Pair<Probe<?>, Long>> lastTime;
	private double vmsPrice;
	private long maxStorage;

	@Override
	public void setup(Probed parent) {
		if (!(parent instanceof User))
			throw new IllegalArgumentException("Illegal Parent");
		super.setup(parent);

		this.lastTime = new ArrayList<Pair<Probe<?>,Long>>();
		this.vmsPrice = 0d;
		this.maxStorage = 0l;

		//register listeners
		{
			NotificationListener l = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (notification_code == NotificationCodes.USER_VM_ATTACHED) {
						Probe<?> p = ((Probed) data).getProbe(VmRunningTimeProbe.KEY);
						UserBillingProbe.this.lastTime.add(new Pair<Probe<?>, Long>(p, (Long) p.getValue()));
						p.addListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					} else if (notification_code == NotificationCodes.USER_VM_DETACHED) {
						Probe<?> p = ((Probed) data).getProbe(VmRunningTimeProbe.KEY);
						UserBillingProbe.this.lastTime.remove(new Pair<Probe<?>, Long>(p, (Long) p.getValue()));
						p.removeListener(NotificationCodes.PROBE_VALUE_CHANGED, this);
					}
					UserBillingProbe.this.recomputeValue();
				}
			};

			registeredListener(l);

			((Notifier) getParent()).addListener(NotificationCodes.USER_VM_ATTACHED, l);
			((Notifier) getParent()).addListener(NotificationCodes.USER_VM_DETACHED, l);

			for (VirtualMachine vm: ((User)getParent()).getVirtualMachines()) {
					Probe<?> p = vm.getProbe(VmRunningTimeProbe.KEY);
					p.addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);
				}

			getParent().getProbe(UserFilesSizeProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);

			getParent().getProbe(UserDownBwCloudTotalProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);

			getParent().getProbe(UserUpBwCloudTotalProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);

			getParent().getProbe(UserDownBwInternetTotalProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);

			getParent().getProbe(UserUpBwInternetTotalProbe.KEY).addListener(NotificationCodes.PROBE_VALUE_CHANGED, l);

			recomputeValue();
		}
	}

	private void recomputeValue() {
		double v = 0;
		for (Pair<Probe<?>, Long> o: this.lastTime) {
			VirtualMachine vm = (VirtualMachine) o.getValue1().getParent();
			if (vm.getParent() instanceof ThinClient)
				continue;

			double price_per_hour = vm.getConfig() == null ? 0.0d :
				vm.getConfig().getDouble("Price_FlatPerHour", 0.01d) * Simulator.CURRENCY_UNIT;

			Long newP = ((Long) o.getValue1().getValue());

			v+= (double)(newP - o.getValue2()) * price_per_hour / Simulator.HOUR;
			o.setValue2(newP);
		}
		this.vmsPrice += v;

		v = this.vmsPrice;

		Config cloudConfig = ((User)getParent()).getParent().getConfig();
		if (cloudConfig == null)
			cloudConfig = new Config();

		maxStorage = Math.max(((Long)getParent().getProbe(UserFilesSizeProbe.KEY).getValue()).longValue(), maxStorage);
		v+= maxStorage *
				cloudConfig.getDouble("Price_1GBStorage", 0.100d) *
				Simulator.CURRENCY_UNIT / Simulator.GIBIBYTE;

		v+= ((Long)getParent().getProbe(UserDownBwCloudTotalProbe.KEY).getValue()).doubleValue() *
				cloudConfig.getDouble("Price_1GBBwFromCloud", 0.000d) *
				Simulator.CURRENCY_UNIT / Simulator.GIBIBYTE;

		v+= ((Long)getParent().getProbe(UserUpBwCloudTotalProbe.KEY).getValue()).doubleValue() *
				cloudConfig.getDouble("Price_1GBBwToCloud", 0.010d) *
				Simulator.CURRENCY_UNIT / Simulator.GIBIBYTE;

		v+= ((Long)getParent().getProbe(UserDownBwInternetTotalProbe.KEY).getValue()).doubleValue() *
				cloudConfig.getDouble("Price_1GBBwFromInternet", 0.000d) *
				Simulator.CURRENCY_UNIT / Simulator.GIBIBYTE;

		v+= ((Long)getParent().getProbe(UserUpBwInternetTotalProbe.KEY).getValue()).doubleValue() *
				cloudConfig.getDouble("Price_1GBBwToInternet", 0.100d) *
				Simulator.CURRENCY_UNIT / Simulator.GIBIBYTE;

		setValue(Long.valueOf(Math.round(v)));
	}

	@Override
	public void discard() {
		super.discard();
		lastTime = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setValue(Long value) {
		if (value != null) {
			if (value < 0l)
				throw new IllegalStateException("Negative price");

			long delta = value.longValue() - (getValue() == null ? 0l : getValue().longValue());

			super.setValue(value);

			{ //update Cp probes if needed
				Entity e = ((User)this.getParent()).getParent();
				if (e != null) {
					Probe<?> cp = e.getProbe(CpUserBillingProbe.KEY);
					if (cp instanceof ModifiableProbe<?>)
						((ModifiableProbe<Long>) cp).setValue(((ModifiableProbe<Long>) cp).getValue() + delta);
				}
			}
		} else
			super.setValue(null);
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
