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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.IpAddress;
import com.samysadi.acs.utility.random.Uniform;


/**
 * This TopologyFactory can be used to generate a hierarchical tree topology, where
 * hosts (leafs of the tree) are connected to each other through a given number (and levels) of
 * switches.
 *
 * @since 1.0
 */
public class TopologyFactoryHierarchical extends TopologyFactory {
	public static final String CONTEXT = "Layer";

	private final List<Config> layerConfigurations = new ArrayList<Config>();

	public TopologyFactoryHierarchical(Config config, CloudProvider cloudProvider) {
		super(config, cloudProvider);
		loadConfig();
	}

	private void loadConfig() {
		layerConfigurations.clear();

		int i = 0;
		while (getConfig().hasContext(CONTEXT, i)) {
			Config cfg = getConfig().addContext(CONTEXT, i);
			layerConfigurations.add(cfg);
		}

		if (layerConfigurations.isEmpty())
			layerConfigurations.add(new Config());
	}

//	public static int numberOfBits(int v) {
//		return Integer.SIZE - Integer.numberOfLeadingZeros(v);
//	}

//	private void doPrintIt(int l, NetworkInterface ni) {
//		StringBuilder p = new StringBuilder();
//		for (int i=l+1; i<layerConfigurations.size(); i++)
//			p.append("\t");
//		p.append(ni.getParent().getId() + "-->");
//		System.out.print(p);
//
//		for (NetworkInterface nii: ni.getParent().getInterfaces())
//			if (nii.getIp() != null)
//				System.out.print(" (ip=" + nii.getIp() + ")");
//		/*if (ni.getParent() instanceof Host)
//			System.out.print("(d,c,r=" + ((Host) ni.getParent()).getDatacenterId() + "," +
//					((Host) ni.getParent()).getClusterId() + "," +
//					((Host) ni.getParent()).getRackId() + ")");*/
//		if (ni.getParent() instanceof Host)
//			System.out.print("(d,c,r=" + Integer.toHexString(System.identityHashCode(((Host) ni.getParent()).getRack().getCluster().getDatacenter())) + "," +
//					Integer.toHexString(System.identityHashCode(((Host) ni.getParent()).getRack().getCluster())) + "," +
//							Integer.toHexString(System.identityHashCode(((Host) ni.getParent()).getRack())) + ")");
//
//		System.out.println();
//		p.append(">");
//
//		((RoutingProtocolDefault)ni.getParent().getRoutingProtocol()).printHints(p.toString());
//	}

	private int total_adv = 0; //is the total accomplished percent * 100
	private int total_hosts = 0;

	protected void generateLayer(int layer_index, List<NetworkDevice> topLayerDevices, Config topLayerCfg,
			int aff_adv) {
		final List<NetworkDevice> generatedDevices;
		final Config deviceCfg;
		//generating this layer devices
		//#####################################################################
		if (layer_index >= 0) {
			//generating switches
			deviceCfg = layerConfigurations.get(layer_index);

			Config switchCfg = deviceCfg.addContext(FactoryUtils.Switch_CONTEXT);

			final int switchCount = FactoryUtils.generateCount(switchCfg, 1);
			generatedDevices = new ArrayList<NetworkDevice>(switchCount);
			for (int i=0; i<switchCount; i++) {
				Switch s = FactoryUtils.generateSwitch(switchCfg, getCloudProvider());

				generatedDevices.add(s);
			}
		} else {
			//generating hosts
			Host h = FactoryUtils.generateHost(getHostGenerationMode().next(), getCloudProvider());
			deviceCfg = h.getConfig();

			generatedDevices = new ArrayList<NetworkDevice>(1);
			generatedDevices.add(h);

			total_adv+= aff_adv;
			total_hosts++;
			if (total_hosts % 1000 == 0)
				FactoryUtils.logAdvancement("Hosts", total_hosts, total_adv/ 100d);
		}

		final IpAddress firstIp = IpAddress.newIpAddress();

		//generate sub layers
		//#####################################################################
		if (layer_index >= 0) {
			final int nodes_count = FactoryUtils.generateCount(deviceCfg.addContext("Nodes"), 5);

			int new_adv = total_adv + aff_adv;
			aff_adv = aff_adv / nodes_count;

			for (int i=0; i<nodes_count; i++)
				generateLayer(layer_index-1, generatedDevices, deviceCfg, aff_adv);

			total_adv = new_adv;

			if (deviceCfg.getBoolean("NewDatacenter", false)) {
				getCloudProvider().addDatacenter();
			} else if (deviceCfg.getBoolean("NewCluster", false)) {
				getCloudProvider().addCluster();
			} else if (deviceCfg.getBoolean("NewRack", false))
				getCloudProvider().addRack();
		}

		final IpAddress lastIp = IpAddress.newIpAddress();

		//link each generated device with topLayerDevices
		//#####################################################################
		if (topLayerDevices != null && !topLayerDevices.isEmpty()) {
			final Uniform lossRateGenerator = new Uniform(
					topLayerCfg.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMin", 0.0d),
					topLayerCfg.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMax", 0.0d)
				);
			final Uniform distanceGenerator = new Uniform(
					topLayerCfg.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMin", 0l),
					topLayerCfg.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMax", 0l)
				);

			for (NetworkDevice device: generatedDevices) {
				for (NetworkDevice topDevice: topLayerDevices) {
					NetworkInterface[] ni = FactoryUtils.linkDevices(
							topLayerCfg,
							device, topDevice,
							distanceGenerator.nextLong() * Simulator.LATENCY_PER_KILOMETER / 1000,
							lossRateGenerator.nextDouble()
						);

					if (device instanceof Host) {
						IpAddress ip = IpAddress.newIpAddress();
						ni[0].setIp(ip);

						//topDevice.getRoutingProtocol().addRoutingHint(ip, ip, ni[1]);
					} else {
						topDevice.getRoutingProtocol().addRoutingHint(firstIp, lastIp, ni[1]);

						device.getRoutingProtocol().addRoutingHint(IpAddress.ipMin, firstIp, ni[0]);
						device.getRoutingProtocol().addRoutingHint(lastIp, IpAddress.ipMax, ni[0]);
					}

//					doPrintIt(layer_index, ni[0]);
//					System.out.println();
//					doPrintIt(layer_index, ni[1]);
//					System.out.println("#################################################################");
//					System.out.println();
				}
			}
		}
	}

	@Override
	public Object generate() {
		Simulator.getSimulator().setRandomGenerator(this.getClass());

		int lastLayerIndex = layerConfigurations.size()-1;

		total_adv = 0;
		total_hosts = 0;

		Config internetLayer = getConfig().addContext(INTERNETLAYER_CONTEXT);
		ArrayList<NetworkDevice> topLayerDevices = new ArrayList<NetworkDevice>();
		{
			final Uniform lossRateGenerator = new Uniform(
						internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMin", 0.0d),
						internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMax", 0.0d)
					);
			final Uniform distanceGenerator = new Uniform(
						internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMin", 0l),
						internetLayer.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMax", 0l)
					);
			final Config switchCfg = internetLayer.addContext(FactoryUtils.Switch_CONTEXT);
			for (int i=0; i<FactoryUtils.generateCount(switchCfg, 1); i++) {
				Switch s = FactoryUtils.generateSwitch(switchCfg, getCloudProvider());

				FactoryUtils.connectToInternet(
						internetLayer,
						s,
						distanceGenerator.nextLong() * Simulator.LATENCY_PER_KILOMETER / 1000,
						lossRateGenerator.nextDouble()
					);

				topLayerDevices.add(s);
			}
		}

		generateLayer(lastLayerIndex, topLayerDevices, internetLayer, 100 * 100);

		FactoryUtils.logAdvancement("Hosts", total_hosts, 100d);

		Simulator.getSimulator().restoreRandomGenerator();
		return null;
	}

	public Collection<Config> getConfigurations() {
		return layerConfigurations;
	}

}
