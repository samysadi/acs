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

package com.samysadi.acs.hardware.network.routingprotocol;

import java.util.HashSet;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.utility.IpAddress;
import com.samysadi.acs.utility.NotificationCodes;


/**
 * A RoutingProtocol is used to select a {@link Route} between the parent device and another device.
 *
 * <p>If calling {@link NetworkDevice#isRoutingEnabled()} on a given device returns <tt>false</tt>
 * and if is not the parent device,
 * then any of its interfaces can be included in a route generated by this {@link RoutingProtocol}.
 *
 * <p>The route selection process is left to the implementation discretion. However,
 * the selected route cannot contain a failed interface or an interface of which parent has failed.<br/>
 * Furthermore, the route must be invalidated by throwing a {@link NotificationCodes#RP_ROUTING_UPDATED} notification
 * whenever the route becomes invalid (for example, if an interface in the route fails, or a parent of an interface in the route fails).<br/>
 * Implementations should not care about the route end-points (jobs and virtual machines) state. This is left for the
 * NetworkOperation to care about.<br/>
 * Doubtlessly, in order to achieve previous goals, implementations need to listen to these following notifications:<ul>
 * 		<li>{@link NotificationCodes#FAILURE_STATE_CHANGED}
 *			Check if the network device has failed. Or if any network interface of a network device has failed.
 * 		<li>{@link NotificationCodes#POWER_STATE_CHANGED}
 *			Check if the network device has been powered off.
 * 		<li>{@link NotificationCodes#ENTITY_ADDED}
 *			check if a new Network Interface is added.
 * 		<li>{@link NotificationCodes#ENTITY_REMOVED}
 *			check if an existing Network interface is removed.
 * 		<li>{@link NotificationCodes#NI_LINKING_UPDATED}
 *			The Network Interface linking has changed.
 * </ul>
 *
 * @since 1.0
 */
public interface RoutingProtocol extends Entity {
	public static class RouteConstraints {
		public HashSet<NetworkDevice> excludedDevices;
		public double minimumMetric;

		public RouteConstraints(HashSet<NetworkDevice> excludedDevices,
				double minimumMetric) {
			super();
			this.excludedDevices = excludedDevices;
			this.minimumMetric = minimumMetric;
		}

		public RouteConstraints() {
			this(new HashSet<NetworkDevice>(), Double.POSITIVE_INFINITY);
		}
	}

	public static class RouteInfo {
		private final Route route;
		private double metric;

		public RouteInfo(Route route, double metric) {
			super();
			this.route = route;
			this.metric = metric;
		}

		public RouteInfo(Route route) {
			this(route, 0.0d);
		}

		public RouteInfo() {
			this(new Route());
		}

		public double getMetric() {
			return metric;
		}

		public void setMetric(double metric) {
			this.metric = metric;
		}

		public Route getRoute() {
			return route;
		}
	}

	@Override
	public RoutingProtocol clone();

	@Override
	public NetworkDevice getParent();

	/**
	 * Adds a hint for the routing protocol.<br/>
	 * Implementations can perform faster routing when looking for a route to a device whose
	 * IP has already an associated routing hint. This may considerably reduce
	 * the simulation performance for big topologies.
	 *
	 * <p>Implementations are not constrained to use this hint, nor they should rely on this hint to
	 * perform routing.<br/>
	 * In fact, an implementation may leak routing hints for some IP addresses, the hints may be deprecated after
	 * some devices fails, or it may receive requests to find routes for network devices that do not have
	 * an associated {@link IpAddress}.<br/>
	 * A best practice should be to try to find a route firstly using a hint if it is possible, and if not, use other
	 * methods to find a route. If a route has been found using a hint it is assumed optimal.
	 *
	 * @param destinationIpStart the first IP that is concerned by this hint. All IPs between this value and <tt>destinationIpEnd</tt> are also concerned by this hint.
	 * @param destinationIpEnd the last IP that is concerned by this hint
	 * @param nextInterface the next interface that will be used when looking for a route to an IP that matches the <tt>destinationIp</tt> and the <tt>destinationMask</tt>
	 */
	public void addRoutingHint(IpAddress destinationIpStart, IpAddress destinationIpEnd, NetworkInterface nextInterface);

	/**
	 * Removes all routing hints that were previously added.
	 */
	public void clearRoutingHints();

	/**
	 * Returns a network route, to be used for network communications
	 * between the parent device and the given <tt>destinationDevice</tt>.
	 *
	 * <p>If {@code getParent() == destinationDevice} then the returned RouteInfo will contain an empty route.
	 *
	 * <p>If no route was found, <tt>null</tt> is returned.
	 *
	 * @param destinationDevice the destination device
	 * @param constraints a {@link RouteConstraints} that may be passed to exclude some devices from the route, or to add
	 * metric constraints for the returned route. Can be <tt>null</tt>.
	 * @return A {@link RouteInfo} containing the selected route, an empty route or <tt>null</tt>
	 */
	public RouteInfo findRoute(NetworkDevice destinationDevice, RouteConstraints constraints);
}
