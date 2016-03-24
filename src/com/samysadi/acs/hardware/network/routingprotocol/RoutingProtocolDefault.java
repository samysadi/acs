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
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.utility.IpAddress;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.Pair;
import com.samysadi.acs.utility.collections.Bitmap;
import com.samysadi.acs.utility.collections.ShuffledIterator;

/**
 *
 * @since 1.0
 */
public class RoutingProtocolDefault extends EntityImpl implements RoutingProtocol {
	private List<Pair<NetworkInterface, Bitmap>> routingHints;

	private NotificationListener listenerForStopRouting;

	public RoutingProtocolDefault() {
		super();
	}

//	public void printHints(String p) {
//		if (routingHints == null || routingHints.isEmpty())
//			System.out.println(p + " nothing");
//		else
//			for (Pair<NetworkInterface, Bitmap> r : routingHints)
//				System.out.println(p + r.getValue2() + " ==> " + r.getValue1().getRemoteNetworkInterface().getParent().getId());
//	}

	@Override
	public RoutingProtocolDefault clone() {
		final RoutingProtocolDefault clone = (RoutingProtocolDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.listenerForStopRouting = null;
		this.routingHints = null;
	}

	@Override
	public NetworkDevice getParent() {
		return (NetworkDevice) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof NetworkDevice))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);
		if (getParent() != null) {
			registerListener();

			//make sure hints are applicable
			routingHintsCleanup();
		} else {
			unregisterListener();
		}
		notify(NotificationCodes.RP_ROUTING_UPDATED, null);
	}

	protected void registerListener() {
		unregisterListener();
		listenerForStopRouting = new NotificationListener() {
			@Override
			public void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				if (notification_code == NotificationCodes.ENTITY_ADDED || notification_code == NotificationCodes.ENTITY_REMOVED) {
					if (!(data instanceof NetworkInterface))
						return;
					NetworkInterface ni = (NetworkInterface) data;
					if (notification_code == NotificationCodes.ENTITY_ADDED) {
						ni.addListener(NotificationCodes.FAILURE_STATE_CHANGED, this);
						ni.addListener(NotificationCodes.NI_LINKING_UPDATED, this);
					} else {
						ni.removeListener(NotificationCodes.FAILURE_STATE_CHANGED, this);
						ni.removeListener(NotificationCodes.NI_LINKING_UPDATED, this);
						routingHintsCleanup();
					}
				}
				RoutingProtocolDefault.this.notify(NotificationCodes.RP_ROUTING_UPDATED, null);
			}
		};

		getParent().addListener(NotificationCodes.FAILURE_STATE_CHANGED, listenerForStopRouting);
		getParent().addListener(NotificationCodes.POWER_STATE_CHANGED, listenerForStopRouting);
		getParent().addListener(NotificationCodes.ENTITY_ADDED, listenerForStopRouting);
		getParent().addListener(NotificationCodes.ENTITY_REMOVED, listenerForStopRouting);

		for (NetworkInterface ni: getParent().getInterfaces()) {
			ni.addListener(NotificationCodes.FAILURE_STATE_CHANGED, listenerForStopRouting);
			ni.addListener(NotificationCodes.NI_LINKING_UPDATED, listenerForStopRouting);
		}
	}

	protected void unregisterListener() {
		if (listenerForStopRouting == null)
			return;
		listenerForStopRouting.discard();
		listenerForStopRouting = null;
	}

	private void routingHintsCleanup() {
		if (this.routingHints != null) {
			Iterator<Pair<NetworkInterface, Bitmap>> it = this.routingHints.iterator();
			while (it.hasNext()) {
				Pair<NetworkInterface, Bitmap> n = it.next();
				if (n.getValue1().getParent() != getParent())
					it.remove();
			}
		}
	}

	@Override
	public void addRoutingHint(IpAddress destinationIpStart, IpAddress destinationIpEnd,
			NetworkInterface nextInterface) {
		if (destinationIpStart.getValue() > destinationIpEnd.getValue())
			throw new IllegalArgumentException("first IP must be smaller than the last IP");
		if (getParent() != null && nextInterface.getParent() != getParent())
			return; // do nothing

		Bitmap m = null;
		if (this.routingHints == null)
			this.routingHints = newArrayList(getParent() != null ? getParent().getInterfaces().size() : 2);
		else {
			for (Pair<NetworkInterface, Bitmap> p: this.routingHints)
				if (p.getValue1().equals(nextInterface))
					m = p.getValue2();
		}
		if (m == null) {
			m = new Bitmap();
			this.routingHints.add(new Pair<NetworkInterface, Bitmap>(nextInterface, m));
		}

		m.mark(destinationIpStart.getValue(), destinationIpEnd.getValue() - destinationIpStart.getValue() + 1);
	}

	@Override
	public void clearRoutingHints() {
		this.routingHints = null;
	}

	private boolean isReady() {
		return listenerForStopRouting != null;
	}

	/**
	 * Returns a new constraints based on the given constraints for use by next RoutingProtocol
	 * or <tt>null</tt> if no constraints are set.
	 *
	 * <p>Subclasses can use this method to improve performances by excluding some interfaces from being
	 * analyzed when searching a route by returning <tt>null</tt>.
	 *
	 * <p>This method should return <tt>null</tt> only if there exist no route to <tt>destinationDevice</tt> through <tt>nextInterface</tt>.
	 *
	 * @param destinationDevice the destination device
	 * @param nextInterface the next interface that is being analyzed
	 * @param constraints as set in findRoute
	 * @return <tt>null</tt> if the <tt>nextInterface</tt> should not be used, or the new constraints to apply if <tt>nextInterface</tt> must be used
	 */
	protected RouteConstraints getNextConstraintsForInterface(NetworkDevice destinationDevice, NetworkInterface nextInterface, RouteConstraints constraints) {
		if (nextInterface.getFailureState() != FailureState.OK)
			return null;
		if (nextInterface.getRemoteNetworkInterface().getFailureState() != FailureState.OK)
			return null;

		return constraints;
	}

	protected double getMetricCost() {
		return 1.0d;
	}

	/**
	 * Choose the best route to transmit data,
	 * by choosing the shortest route (with less nodes in it).
	 *
	 * <p>Of course, this may not be the optimal route.
	 * You are free to override this method to do a better routing.
	 *
	 * <p>If many equivalent routes are found, one is chosen randomly.
	 */
	@Override
	public RouteInfo findRoute(NetworkDevice destinationDevice, RouteConstraints constraints) {
		if (!isReady())
			return null;

		if (destinationDevice.getFailureState() != FailureState.OK)
			return null;

		if (destinationDevice.getPowerState() != PowerState.ON)
			return null;

		final NetworkDevice nextDevice = getParent();

		if (nextDevice == destinationDevice)
			return new RouteInfo();

		if (nextDevice.getFailureState() != FailureState.OK)
			return null;

		if (nextDevice.getPowerState() != PowerState.ON)
			return null;

		if (constraints == null)
			constraints = new RouteConstraints();

		RouteInfo best = null;

		final double MC = getMetricCost();
		double minimumMetric = constraints.minimumMetric;
		List<NetworkInterface> interfaces = null;

		boolean usingHints = true;
		Iterator<NetworkInterface> niIterator = null;

		constraints.excludedDevices.add(nextDevice);
		while (true) {
			NetworkInterface networkInterface = null;

			//look if we have a hint for this
			if (usingHints) {
				if (this.routingHints != null && !this.routingHints.isEmpty()) {
					if (niIterator == null) {
						final List<NetworkInterface> l = destinationDevice.getInterfaces();
						niIterator = new ShuffledIterator<NetworkInterface>(l);
					}
					if (niIterator.hasNext()) {
						IpAddress destIp = niIterator.next().getIp();
						if (destIp == null)
							continue;
						//look for a hint
						Iterator<Pair<NetworkInterface, Bitmap>> it = new ShuffledIterator<Pair<NetworkInterface, Bitmap>>(this.routingHints);
						while (it.hasNext()) {
							Pair<NetworkInterface, Bitmap> p = it.next();
							if (p.getValue2().isMarked(destIp.getValue())) { //found interface
								networkInterface = p.getValue1();
								if (interfaces == null)
									interfaces = new LinkedList<NetworkInterface>(nextDevice.getInterfaces());
								interfaces.remove(networkInterface); //make sure this interface is not viewed again
								break;
							}
						}
						if (networkInterface == null)
							continue;
					} else {
						niIterator = null;
						usingHints = false;
						continue;
					}
				} else {
					niIterator = null;
					usingHints = false;
					continue;
				}
			} else {
				if (niIterator == null) {
					if (interfaces == null)
						interfaces = nextDevice.getInterfaces();
					niIterator = new ShuffledIterator<NetworkInterface>(interfaces);
				}

				if (niIterator.hasNext())
					networkInterface = niIterator.next();
				else
					break;
			}

			final RouteConstraints nextConstraints = getNextConstraintsForInterface(destinationDevice, networkInterface, constraints);
			if (nextConstraints == null)
				continue;
			final NetworkDevice remoteDevice = networkInterface.getRemoteNetworkInterface().getParent();

			if (remoteDevice != destinationDevice && !remoteDevice.isRoutingEnabled())
				continue;
			if (minimumMetric < Double.POSITIVE_INFINITY)
				constraints.minimumMetric=minimumMetric - MC; //no need to create a new constraints object (this implementation does not modify this object).
			if (constraints.excludedDevices.contains(remoteDevice))
				continue;
			final RouteInfo ri = remoteDevice.getRoutingProtocol().findRoute(destinationDevice, nextConstraints);
			if (ri == null)
				continue;

			ri.getRoute().prepend(networkInterface);
			ri.setMetric(ri.getMetric() + MC);

			final int metric_c = Double.compare(ri.getMetric(), minimumMetric);
			//If new route has bigger or equal metric then continue.
			//No need to make extra random selection when equal because we already are iterating starting from a random position
			if (metric_c >= 0)
				continue;

			best = ri;

			minimumMetric = ri.getMetric();

			if (usingHints) //we found this route using a hint, so trust and assume it's best route
				break;
		}
		constraints.excludedDevices.remove(nextDevice);

		return best;
	}
}
