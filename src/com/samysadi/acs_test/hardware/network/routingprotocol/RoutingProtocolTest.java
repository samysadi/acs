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

package com.samysadi.acs_test.hardware.network.routingprotocol;


import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol.RouteInfo;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.IpAddress;
import com.samysadi.acs_test.Utils;


/**
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class RoutingProtocolTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Simulator simulator = Utils.newSimulator();
		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		Utils.quiet(true);
		Utils.generateTopology0(simulator);
		Utils.quiet(false);
		Host h0 = cloudProvider.getHosts().get(0);
		System.out.println("RP = " + h0.getRoutingProtocol().getClass().getName());

		simulator.stop();
		simulator.free();
	}

	private void setIt(NetworkDevice d, Object e) {
		if (e == FailureState.FAILED)
			d.setFailureState((FailureState) e);
		else if (e == PowerState.OFF)
			d.setPowerState((PowerState) e);
		else
			throw new IllegalArgumentException();
	}

	public void testLoopback(Object e) {
		Simulator simulator = Utils.newSimulator();
		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology2(simulator);
		Host h0 = cloudProvider.getHosts().get(0);

		{
			RouteInfo r = h0.getRoutingProtocol().findRoute(h0, null);

			Assert.assertNotNull(r);
			Assert.assertNotNull(r.getRoute());
			Assert.assertEquals(0, r.getRoute().size());
		}

		{//Failure test for h0
			setIt(h0, e);
			RouteInfo r = h0.getRoutingProtocol().findRoute(h0, null);

			Assert.assertNull(r);
		}

		simulator.stop();
		simulator.free();
	}

	@Test
	public void testLoopback_a() {
		testLoopback(FailureState.FAILED);
	}

	@Test
	public void testLoopback_b() {
		testLoopback(PowerState.OFF);
	}

	public void testTopology0(Object e) {
		Simulator simulator = Utils.newSimulator();
		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology0(simulator);
		Host h0 = cloudProvider.getHosts().get(0);
		Host hl = cloudProvider.getHosts().get(1);
		Switch s0 = cloudProvider.getSwitches().get(0);

		{
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNotNull(r);
			Assert.assertNotNull(r.getRoute());
			Assert.assertEquals(2, r.getRoute().size());
			Iterator<NetworkInterface> it = r.getRoute().iterator(); NetworkInterface n;
			n = it.next(); Assert.assertEquals(h0, n.getParent());
			n = it.next(); Assert.assertEquals(s0, n.getParent());
			Assert.assertEquals(hl, n.getRemoteNetworkInterface().getParent());
		}

		{//Failure test for hl
			setIt(hl, e);
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNull(r);
		}

		simulator.stop();
		simulator.free();
	}


	@Test
	public void testTopology0_a() {
		testTopology0(FailureState.FAILED);
	}

	@Test
	public void testTopology0_b() {
		testTopology0(PowerState.OFF);
	}

	public void testTopology1(Object e) {
		Simulator simulator = Utils.newSimulator();
		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology1(simulator);
		Host h0 = cloudProvider.getHosts().get(0);
		Host hl = cloudProvider.getHosts().get(cloudProvider.getHosts().size()-1);
		Switch s0 = cloudProvider.getSwitches().get(0);
		Switch s1 = (Switch) h0.getInterfaces().get(0).getRemoteNetworkInterface().getParent();
		Switch sl = (Switch) hl.getInterfaces().get(0).getRemoteNetworkInterface().getParent();

		{
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNotNull(r);
			Assert.assertNotNull(r.getRoute());
			Assert.assertEquals(4, r.getRoute().size());
			Iterator<NetworkInterface> it = r.getRoute().iterator(); NetworkInterface n;
			n = it.next(); Assert.assertEquals(h0, n.getParent());
			n = it.next(); Assert.assertEquals(s1, n.getParent());
			n = it.next(); Assert.assertEquals(s0, n.getParent());
			n = it.next(); Assert.assertEquals(sl, n.getParent());
			Assert.assertEquals(hl, n.getRemoteNetworkInterface().getParent());
		}

		{//Failure test for h0
			setIt(h0, e);
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNull(r);
		}

		simulator.stop();
		simulator.free();
	}

	@Test
	public void testTopology1_a() {
		testTopology1(FailureState.FAILED);
	}

	@Test
	public void testTopology1_b() {
		testTopology1(PowerState.OFF);
	}

	public void testTopology2(Object e) {
		Simulator simulator = Utils.newSimulator();
		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology2(simulator);
		Host h0 = cloudProvider.getHosts().get(0);
		Host hl = cloudProvider.getHosts().get(cloudProvider.getHosts().size()-1);
		Switch s0 = cloudProvider.getSwitches().get(0);
		Switch sl = (Switch) hl.getInterfaces().get(0).getRemoteNetworkInterface().getParent();

		{
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNotNull(r);
			Assert.assertNotNull(r.getRoute());
			Assert.assertEquals(3, r.getRoute().size());
			Iterator<NetworkInterface> it = r.getRoute().iterator(); NetworkInterface n;
			n = it.next(); Assert.assertEquals(h0, n.getParent());
			n = it.next(); Assert.assertEquals(s0, n.getParent());
			n = it.next(); Assert.assertEquals(sl, n.getParent());
			Assert.assertEquals(hl, n.getRemoteNetworkInterface().getParent());
		}

		{//Failure test for s0
			setIt(s0, e);
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNull(r);
		}

		simulator.stop();
		simulator.free();
	}

	@Test
	public void testTopology2_a() {
		testTopology2(FailureState.FAILED);
	}

	@Test
	public void testTopology2_b() {
		testTopology2(PowerState.OFF);
	}

	public void testTopology2WithHints(Object e) {
		Simulator simulator = Utils.newSimulator();
		CloudProvider cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology2(simulator);
		Host h0 = cloudProvider.getHosts().get(0);
		Host hl = cloudProvider.getHosts().get(cloudProvider.getHosts().size()-1);
		Switch s0 = cloudProvider.getSwitches().get(0);
		Switch s1 = cloudProvider.getSwitches().get(1);
		Switch sl = (Switch) hl.getInterfaces().get(0).getRemoteNetworkInterface().getParent();

		IpAddress hlIp = IpAddress.newIpAddress();
		hl.getInterfaces().get(0).setIp(hlIp);
		NetworkInterface interfaceToS1 = null;
		for (NetworkInterface ni: h0.getInterfaces())
			if (ni.getRemoteNetworkInterface().getParent() == s1)
				interfaceToS1 = ni;
		Assert.assertNotNull(interfaceToS1);
		h0.getRoutingProtocol().addRoutingHint(hlIp, hlIp, interfaceToS1);

		{
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNotNull(r);
			Assert.assertNotNull(r.getRoute());
			Assert.assertEquals(4, r.getRoute().size());
			Iterator<NetworkInterface> it = r.getRoute().iterator(); NetworkInterface n;
			n = it.next(); Assert.assertEquals(h0, n.getParent());
			n = it.next(); Assert.assertEquals(s1, n.getParent());
			n = it.next(); Assert.assertEquals(s0, n.getParent());
			n = it.next(); Assert.assertEquals(sl, n.getParent());
			Assert.assertEquals(hl, n.getRemoteNetworkInterface().getParent());
		}

		{//Failure test for s1
			setIt(s1, e);
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNotNull(r);
			Assert.assertNotNull(r.getRoute());
			Assert.assertEquals(3, r.getRoute().size());
			Iterator<NetworkInterface> it = r.getRoute().iterator(); NetworkInterface n;
			n = it.next(); Assert.assertEquals(h0, n.getParent());
			n = it.next(); Assert.assertEquals(s0, n.getParent());
			n = it.next(); Assert.assertEquals(sl, n.getParent());
			Assert.assertEquals(hl, n.getRemoteNetworkInterface().getParent());
		}

		{//Failure test for s0
			setIt(s0, e);
			RouteInfo r = h0.getRoutingProtocol().findRoute(hl, null);

			Assert.assertNull(r);
		}

		simulator.stop();
		simulator.free();
	}

	@Test
	public void testTopology2WithHints_a() {
		testTopology2WithHints(FailureState.FAILED);
	}

	@Test
	public void testTopology2WithHints_b() {
		testTopology2WithHints(PowerState.OFF);
	}
}
