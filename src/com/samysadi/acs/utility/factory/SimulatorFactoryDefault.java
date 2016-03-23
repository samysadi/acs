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

import java.util.logging.Level;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;


/**
 *
 * @since 1.0
 */
public class SimulatorFactoryDefault extends SimulatorFactory {

	public SimulatorFactoryDefault(Config config) {
		super(config);
	}

	private void generateCloudProvider(long tick, GenerationMode g, int index, int count, NotificationListener l) {
		if (index >= count) {
			l.discard();
			Simulator.getSimulator().removeListener(NotificationCodes.FACTORY_CLOUDPROVIDER_GENERATED, l);
			Simulator.getSimulator().notify(NotificationCodes.FACTORY_ALL_CLOUDPROVIDERS_GENERATED, null);
			_generate1(tick);
			return;
		}

		getLogger().log(Level.FINER, "Generating CloudProvider " + index + "/" + count);
		FactoryUtils.generateCloudProvider(g.next());
	}

	private void _generate0(final long tick) {
		getLogger().log(Level.INFO, "Generating infrastructure ...");
		final GenerationMode g = newGenerationMode(null, FactoryUtils.CloudProvider_CONTEXT);
		final int count = FactoryUtils.generateCount(getConfig().addContext(FactoryUtils.CloudProvider_CONTEXT), 1);
		final int[] indexTab = {0};

		NotificationListener l = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				generateCloudProvider(tick, g, indexTab[0], count, this);
				indexTab[0]++;
			}
		};

		Simulator.getSimulator().addListener(NotificationCodes.FACTORY_CLOUDPROVIDER_GENERATED, l);
		generateCloudProvider(tick, g, indexTab[0], count, l);
		indexTab[0]++;
	}

	private void _generate1(final long tick) {
		NotificationListener l = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();
				Simulator.getSimulator().removeListener(NotificationCodes.FACTORY_FAILURES_GENERATED, this);

				_generate2(tick);
			}
		};

		Simulator.getSimulator().addListener(NotificationCodes.FACTORY_FAILURES_GENERATED, l);
		FactoryUtils.generateFailures(getConfig().addContext(FactoryUtils.Failures_CONTEXT));
	}

	private void _generate2(long tick) {
		FactoryUtils.generateTraces(getConfig(), Simulator.getSimulator());

		getLogger().log(Level.INFO, "Simulator was initialized. Initialization took: " +
				Simulator.formatTime((System.nanoTime()-tick) * Simulator.MILLISECOND / 1000000) +
				"."
			);

		Simulator.getSimulator().notify(NotificationCodes.FACTORY_SIMULATOR_GENERATED, null);
	}

	@Override
	public Simulator generate() {
		Simulator simulator = newSimulator(null, getConfig());

		Simulator.getSimulator().schedule(new EventImpl() {
			@Override
			public void process() {
				getLogger().log(Level.INFO, "Simulator is being initialized ...");
				_generate0(System.nanoTime());
			}
		});

		return simulator;
	}
}
