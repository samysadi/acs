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

package com.samysadi.acs.hardware.network;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntityImpl;
import com.samysadi.acs.utility.IpAddress;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class NetworkInterfaceDefault extends FailureProneEntityImpl implements NetworkInterface {
	private NetworkLink upLink;
	private NetworkLink downLink;

	private IpAddress ipAddress;

	public NetworkInterfaceDefault() {
		super();
		this.upLink = null;
		this.downLink = null;
		this.ipAddress = null;
	}

	@Override
	public NetworkInterfaceDefault clone() {
		final NetworkInterfaceDefault clone = (NetworkInterfaceDefault) super.clone();
		return clone;
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

	private void ensureLinkIsValid(NetworkLink link) {
		NetworkInterface r = getRemoteNetworkInterface();

		if (this == link.getFirstInterface()) {
			if (r == null || (r == link.getSecondInterface()))
				return;
		} else if (this == link.getSecondInterface()) {
			if (r == null || (r == link.getFirstInterface()))
				return;
		}

		throw new IllegalArgumentException("The given link cannot be added for the current interface.");
	}

	@Override
	public NetworkLink getUpLink() {
		return upLink;
	}

	@Override
	public void setUpLink(NetworkLink link) {
		if (link == upLink)
			return;

		upLink = null;
		ensureLinkIsValid(link);

		upLink = link;
		getRemoteNetworkInterface().setDownLink(link);

		notify(NotificationCodes.NI_LINKING_UPDATED, null);
	}

	@Override
	public NetworkLink getDownLink() {
		return downLink;
	}

	@Override
	public void setDownLink(NetworkLink link) {
		if (link == downLink)
			return;

		downLink = null;
		ensureLinkIsValid(link);

		downLink = link;
		getRemoteNetworkInterface().setUpLink(link);

		notify(NotificationCodes.NI_LINKING_UPDATED, null);
	}

	@Override
	public NetworkInterface getRemoteNetworkInterface() {
		NetworkLink link = this.getUpLink();
		if (link == null) {
			link = this.getDownLink();
			if (link == null)
				return null;
		}

		if (link.getFirstInterface() == this)
			return link.getSecondInterface();
		else
			return link.getFirstInterface();
	}

	@Override
	public IpAddress getIp() {
		return ipAddress;
	}

	@Override
	public void setIp(IpAddress ip) {
		if (ip == ipAddress || (ip != null && ip.equals(ipAddress)))
			return;

		ipAddress = ip;
		notify(NotificationCodes.NI_IPADDRESS_CHANGED, null);
	}
}
