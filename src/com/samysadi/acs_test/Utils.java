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

package com.samysadi.acs_test;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * Set system property acs.config if you want to use your own configuration.
 * 
 * @since 1.0
 */
public class Utils {

	public static Simulator newSimulator() {
		hname = 0;
		sname = 0;
		Simulator simulator = new Simulator(new Config(Config.DEFAULT_CONFIG_FILENAME));
		Factory.getFactory(simulator).newCloudProvider(null, simulator);
		return simulator;
	}

	public static boolean quiet = false;
	public static int hname;
	public static int sname;

	/**
	 * Returns old value.
	 * 
	 * @param b
	 * @return old value
	 */
	public static boolean quiet(boolean b) {
		if (quiet == b)
			return quiet;
		quiet = b;
		return !b;
	}

	/**
	 * Very simple Topology.
	 * 
	 * <p>3 hosts -- switch<br/>
	 * up/down link = 100 mb/s<br/>
	 * link latency = 0<br/>
	 * link loss rate = 0
	 */
	public static void generateTopology0(Simulator simulator) {
		if (!quiet) {
			simulator.getLogger().log("Generating topology0:\t3 hosts -- 1 switch");
		}

		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);

		Config hostCfg = hostsConfig(simulator);
		hostCfg.setInt("Trace.Count", 0);

		Host h0 = FactoryUtils.generateHost(hostCfg, cloudProvider);h0.setName("H" + (hname++));
		Host h1 = FactoryUtils.generateHost(hostCfg, cloudProvider);h1.setName("H" + (hname++));
		Host h2 = FactoryUtils.generateHost(hostCfg, cloudProvider);h2.setName("H" + (hname++));

		simulator.getConfig().setInt("Trace.Count", 0);

		Switch s0 = FactoryUtils.generateSwitch(simulator.getConfig().addContext(FactoryUtils.Switch_CONTEXT), cloudProvider);s0.setName("S" + (sname++));
		FactoryUtils.linkDevices(simulator.getConfig(), h0, s0, 100 * Simulator.MEBIBYTE, 100 * Simulator.MEBIBYTE, 0, 0);
		FactoryUtils.linkDevices(simulator.getConfig(), h1, s0, 100 * Simulator.MEBIBYTE, 100 * Simulator.MEBIBYTE, 0, 0);
		FactoryUtils.linkDevices(simulator.getConfig(), h2, s0, 100 * Simulator.MEBIBYTE, 100 * Simulator.MEBIBYTE, 0, 0);
	}

	private static Config hostsConfig(Simulator simulator) {
		final Config cfg = (new Config());

		cfg.setString("Name", "H"); //need to set something in that context to enable it

		cfg.setBoolean("PowerState", true);
		
		return cfg;
	}

	/**
	 * Hierarchical Topology.
	 * 
	 * <p><table style="text-align:center; width:400px;">
	 * <tr><td colspan="4">switch0</td></tr>
	 * <tr><td>switch1</td><td>switch2</td><td>switch3</td><td>switch4</td></tr>
	 * <tr><td>3hosts</td><td>3hosts</td><td>3hosts</td><td>3hosts</td></tr>
	 * </table>
	 * <br/>
	 * up/down link = 100 mb/s | 300 mb/s switch to switch<br/>
	 * link latency = 0<br/>
	 * link loss rate = 0
	 */
	public static void generateTopology1(Simulator simulator) {
		final boolean oldQuiet = quiet;
		if (!quiet) {
			simulator.getLogger().log("Generating topology1:\t(3 hosts -- 1 switch) * 4 -- 1 switch");
		}
		quiet = true;

		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);

		simulator.getConfig().setInt("Trace.Count", 0);

		Switch s0 = FactoryUtils.generateSwitch(simulator.getConfig().addContext(FactoryUtils.Switch_CONTEXT), cloudProvider); s0.setName("S" + (sname++));
		
		generateTopology0(simulator);
		generateTopology0(simulator);
		generateTopology0(simulator);
		generateTopology0(simulator);

		Switch s1 = cloudProvider.getSwitches().get(1);
		Switch s2 = cloudProvider.getSwitches().get(2);
		Switch s3 = cloudProvider.getSwitches().get(3);
		Switch s4 = cloudProvider.getSwitches().get(4);


		FactoryUtils.linkDevices(simulator.getConfig(), s0, s1, 300 * Simulator.MEBIBYTE, 300 * Simulator.MEBIBYTE, 0, 0);
		FactoryUtils.linkDevices(simulator.getConfig(), s0, s2, 300 * Simulator.MEBIBYTE, 300 * Simulator.MEBIBYTE, 0, 0);
		FactoryUtils.linkDevices(simulator.getConfig(), s0, s3, 300 * Simulator.MEBIBYTE, 300 * Simulator.MEBIBYTE, 0, 0);
		FactoryUtils.linkDevices(simulator.getConfig(), s0, s4, 300 * Simulator.MEBIBYTE, 300 * Simulator.MEBIBYTE, 0, 0);

		quiet = oldQuiet;
	}

	/**
	 * Fat Topology.<br/>
	 * same as {@link Utils#generateTopology1(Simulator)} + a link for h0 directly to switch 0
	 * 
	 * <p>up/down link = 100 mb/s | 300 mb/s switch to switch<br/>
	 * link latency = 0<br/>
	 * link loss rate = 0
	 */
	public static void generateTopology2(Simulator simulator) {
		final boolean oldQuiet = quiet;
		if (!quiet) {
			simulator.getLogger().log("Generating topology2:\t(3 hosts -- 1 switch) * 4 -- 1 switch + h0 -- switch0");
		}
		quiet = true;

		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		
		generateTopology1(simulator);

		Host h0 = cloudProvider.getHosts().get(0);
		Switch s0 = cloudProvider.getSwitches().get(0);

		FactoryUtils.linkDevices(simulator.getConfig(), s0, h0, 100 * Simulator.MEBIBYTE, 100 * Simulator.MEBIBYTE, 0, 0);

		quiet = oldQuiet;
	}

	private static final Object VM_FOR = new Object();
	public static VirtualMachine getVmFor(Host h0) {
		VirtualMachine vm = (VirtualMachine) h0.getProperty(VM_FOR);
		if (vm == null) {
			vm = FactoryUtils.generateVirtualMachine(h0.getConfigRec(), null);
			vm.setParent(h0);

			if (!vm.isRunning() && vm.canStart())
				vm.doStart();

			h0.setProperty(VM_FOR, vm);
		}
		return vm;
	}
}
