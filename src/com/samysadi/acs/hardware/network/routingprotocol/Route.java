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

import java.util.Iterator;
import java.util.LinkedList;

import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.NetworkLink;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.operation.NetworkResource;
import com.samysadi.acs.utility.NotificationCodes;

/**
 *
 * @since 1.0
 */
public class Route implements Iterable<NetworkInterface> {
	private final LinkedList<NetworkInterface> interfaces;

	public Route() {
		super();
		interfaces = new LinkedList<NetworkInterface>();
	}

	public Route(Route src) {
		this();
		this.interfaces.addAll(src.interfaces);
	}

	/**
	 * Adds the given interface to this route and returns <tt>true</tt>.
	 *
	 * @param networkInterface
	 * @return <tt>true</tt> (as specified by {@link LinkedList#add(Object)})
	 * @throws NullPointerException if the given interface is <tt>null</tt>
	 * @throws IllegalArgumentException if the given interface parent device is different from current route's last device (if any)
	 */
	public boolean add(NetworkInterface networkInterface) {
		if (networkInterface == null)
			throw new NullPointerException("You cannot add null interfaces");
		if (!this.interfaces.isEmpty()) {
			if (this.getDestinationDevice() != networkInterface.getParent())
				throw new IllegalArgumentException("You cannot add the given interface to this route (it would result in an inconsistent route).");
		}
		return interfaces.add(networkInterface);
	}

	/**
	 * Merges the given route with this route and returns <tt>true</tt> if the route has changed.
	 * The new route is added to the end of the current route.
	 *
	 * @param route
	 * @return <tt>true</tt> if the route changed as result of the call
	 * @throws NullPointerException if the given route is <tt>null</tt>
	 * @throws IllegalArgumentException if the first device of the given route is different from current route's last device (if any)
	 */
	public boolean add(Route route) {
		if (route == null)
			throw new NullPointerException("You cannot add null routes");
		if (!this.interfaces.isEmpty()) {
			if (this.getDestinationDevice() != route.getSourceDevice())
				throw new IllegalArgumentException("You cannot add the given route to this route (it would result in an inconsistent route).");
		}
		return interfaces.addAll(route.interfaces);
	}

	/**
	 * Adds the given interface in the beginning of the current route and returns <tt>true</tt>.
	 *
	 * @param networkInterface
	 * @return <tt>true</tt>
	 * @throws NullPointerException if the given interface is <tt>null</tt>
	 * @throws IllegalArgumentException if the parent device of the remote interface that is connected to the given interface
	 * is different from current route's first device (if any)
	 */
	public boolean prepend(NetworkInterface networkInterface) {
		if (networkInterface == null)
			throw new NullPointerException("You cannot add null interfaces");
		if (!this.interfaces.isEmpty()) {
			if (this.getSourceDevice() != networkInterface.getRemoteNetworkInterface().getParent())
				throw new IllegalArgumentException("You cannot add the given interface to this route (it would result in an inconsistent route).");
		}
		interfaces.addFirst(networkInterface);
		return true;
	}

	/**
	 * Returns the number of interfaces this route contains.
	 *
	 * @return the number of interfaces this route contains
	 */
	public int size() {
		return interfaces.size();
	}

	protected NetworkInterface getFirst() {
		return interfaces.getFirst();
	}

	/**
	 * Returns the last (receiving) interface in this route that will receive this data.
	 * This is NOT the last interface in the route but: lastInterface.getRemoteNetworkInterface()
	 *
	 * @return the last (receiving) interface in this route that will receive this data
	 */
	protected NetworkInterface getLastRemoteNetworkInterface() {
		return interfaces.getLast().getRemoteNetworkInterface();
	}

	protected NetworkDevice getSourceDevice() {
		return this.getFirst().getParent();
	}

	protected NetworkDevice getDestinationDevice() {
		return this.getLastRemoteNetworkInterface().getParent();
	}

	/**
	 * Call this method to register a listener for {@link NotificationCodes#RP_ROUTING_UPDATED} notifications
	 * on each device in this route including the parent of the remote interface that is linked to the last interface in this route (ie: destination device).
	 *
	 * <p>Prefer using this method rather than to iterate through network interfaces of this route.
	 *
	 * @param listener
	 */
	public void registerListenerForRoutingUpdates(NotificationListener listener) {
		for (NetworkInterface next: this.interfaces)
			next.getParent().getRoutingProtocol().addListener(NotificationCodes.RP_ROUTING_UPDATED, listener);

		//add listener for the last remote device
		if (!this.interfaces.isEmpty())
			this.interfaces.getLast().getRemoteNetworkInterface().getParent().getRoutingProtocol().addListener(NotificationCodes.RP_ROUTING_UPDATED, listener);
	}

	/**
	 * Returns the resource promise for the given <tt>operation</tt> through this route (all interfaces).
	 *
	 * <p>The returned promise can be safely granted as it is checked against all interfaces (links) that
	 * the route contains.
	 *
	 * @param operation the operation for whom a resource promise will be returned
	 * @return the resource promise for the given <tt>operation</tt> through this route
	 */
	public NetworkResource getResourcePromise(NetworkOperation operation) {
		long latency = 0;
		double lossRate = 0;
		long bw = Long.MAX_VALUE;

		//check bw promise of the source VM
		if (operation.getParent().getParent().getNetworkProvisioner()!=null)
			bw = Math.min(
					bw,
					operation.getParent().getParent().getNetworkProvisioner().getResourcePromise(operation).getBw()
				);

		//check bw promise of the destination VM
		if (operation.getDestinationJob().getParent().getNetworkProvisioner()!=null)
			bw = Math.min(
					bw,
					operation.getDestinationJob().getParent().getNetworkProvisioner().getResourcePromise(operation).getBw()
				);

		for (NetworkInterface next: this.interfaces) {
			NetworkLink link = next.getUpLink();
			NetworkResource r = link.getNetworkProvisioner().getResourcePromise(operation);

			latency += r.getLatency();

			lossRate = Math.max(lossRate, r.getLossRate());

			bw = Math.min(bw, r.getBw());
		}

		return new NetworkResource(bw, latency, lossRate);
	}

	/**
	 * Grants the <tt>operation</tt> allocated resource through this route.
	 *
	 * @param operation allocated resource of this operation is granted through the current route
	 */
	public void grantAllocatedResource(NetworkOperation operation) {
		//grant in source VM provisioner
		if (operation.getParent().getParent().getNetworkProvisioner() != null)
			operation.getParent().getParent().getNetworkProvisioner().grantAllocatedResource(operation);
		//grant in destination VM provisioner
		if (operation.getDestinationJob().getParent().getNetworkProvisioner() != null)
			operation.getDestinationJob().getParent().getNetworkProvisioner().grantAllocatedResource(operation);
		//grant in each upLink from the first interface to the last one
		for (NetworkInterface next: this.interfaces)
			next.getUpLink().getNetworkProvisioner().grantAllocatedResource(operation);
	}

	/**
	 * Revokes the <tt>operation</tt> allocated resource through this route.
	 *
	 * @param operation allocated resource of this operation is revoked through the current route
	 */
	public void revokeAllocatedResource(NetworkOperation operation) {
		//revoke in source VM provisioner
		if (operation.getParent().getParent().getNetworkProvisioner() != null)
			operation.getParent().getParent().getNetworkProvisioner().revokeAllocatedResource(operation);
		//revoke in destination VM provisioner
		if (operation.getDestinationJob().getParent().getNetworkProvisioner() != null)
			operation.getDestinationJob().getParent().getNetworkProvisioner().revokeAllocatedResource(operation);
		//revoke in each upLink from the first interface to the last one
		for (NetworkInterface next: this.interfaces)
			next.getUpLink().getNetworkProvisioner().revokeAllocatedResource(operation);
	}

	protected class RouteIterator implements Iterator<NetworkInterface> {
		private Iterator<NetworkInterface> iterator;

		public RouteIterator() {
			super();
			iterator = Route.this.interfaces.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public NetworkInterface next() {
			return iterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * An iterator over the interfaces of this route in proper order.
	 *
	 * <p>The first interface is the interface of the source device.<br/>
	 * The last is the interface of the penultimate device and not the destination device. This last interface
	 * is linked to the destination device (use {@link NetworkInterface#getRemoteNetworkInterface()}.getParent() to get the destination device.
	 */
	@Override
	public Iterator<NetworkInterface> iterator() {
		return new RouteIterator();
	}

	@Override
	public String toString() {
        Iterator<NetworkInterface> it = iterator();
        if (!it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            NetworkInterface e = it.next();
            sb.append(e.getParent());
            sb.append(" --> ");
            if (!it.hasNext())
                return sb.append(e.getRemoteNetworkInterface().getParent()).append(']').toString();
        }
	}

	public static Route toRoute(NetworkInterface... networkInterfaces) {
		Route route = new Route();
		for (NetworkInterface n: networkInterfaces)
			route.add(n);
		return route;
	}
}
