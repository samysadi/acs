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
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.utility.IpAddress;

/**
 *
 * @since 1.0
 */
public interface NetworkInterface extends Entity, FailureProneEntity {
	/**
	 * The clone will have the same up and down links and ip / mask.
	 */
	@Override
	public NetworkInterface clone();

	@Override
	public NetworkDevice getParent();

	/**
	 * Returns <tt>null</tt> or the remote interface that is connected to this interface through up-link or down-link.
	 *
	 * @return <tt>null</tt> or the remote interface that is connected to this interface through up-link or down-link
 	 */
	public NetworkInterface getRemoteNetworkInterface();

	/**
	 * Returns the up-link that is associated with this interface.
	 *
	 * <p>This link is used when uploading (ie: sending data) to the remote interface.
	 *
	 * @return the up-link that is associated with this interface
	 *
	 */
	public NetworkLink getUpLink();

	/**
	 * Update the <tt>up-link</tt> associated with this interface.<br/>
	 * This link is used when uploading (ie: sending data) to the remote interface.
	 *
	 * <p>Will also ensure that the down-link of the remote network interface is the same as this interface's up-link. Thus,
	 * you don't need to call {@link NetworkInterface#setDownLink(NetworkLink)} on the remote interface after calling this.
	 *
	 * @param link
	 *
	 * @throws IllegalArgumentException if the given <tt>link</tt> cannot be assigned to this interface either because this interface
	 * is not one of the end-points of the <tt>link</tt>, or if the the remote interface of the down-link is not the same as the one
	 * that is specified in the given <tt>link</tt>
	 */
	public void setUpLink(NetworkLink link);

	/**
	 * Returns the down-link that is associated with this interface.
	 *
	 * <p>This link is used when downloading (ie: receiving data) from the remote interface.
	 *
	 * @return the down-link that is associated with this interface
	 */
	public NetworkLink getDownLink();

	/**
	 * Update the <tt>down-link</tt> associated with this interface.<br/>
	 * This link is used when downloading (ie: receiving data) from the remote interface.
	 *
	 * <p>Will also ensure that the up-link of the remote network interface is the same as this interface's down-link. Thus,
	 * you don't need to call {@link NetworkInterface#setUpLink(NetworkLink)} on the remote interface after calling this.
	 *
	 * @param link
	 *
	 * @throws IllegalArgumentException if the given <tt>link</tt> cannot be assigned to this interface either because this interface
	 * is not one of the end-points of the <tt>link</tt>, or if the the remote interface of the up-link is not the same as the one
	 * that is specified in the given <tt>link</tt>
	 */
	public void setDownLink(NetworkLink link);

	/**
	 * Returns the IP address of this networkInterface or <tt>null</tt> if no IP is set for this interface.
	 *
	 * @return the IP address of this networkInterface or <tt>null</tt>
	 */
	public IpAddress getIp();

	/**
	 */
	public void setIp(IpAddress ip);
}
