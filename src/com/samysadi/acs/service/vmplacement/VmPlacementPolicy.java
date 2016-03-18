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

package com.samysadi.acs.service.vmplacement;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * Defines methods for selecting a host among all available hosts
 * when placing a Virtual Machine.
 *
 * <p>This interface also define a method for placing a VM after a host has been selected, and
 * a method to unplace it after it has been placed (see each method documentation for more information).
 *
 * @since 1.0
 */
public interface VmPlacementPolicy extends Entity {

	@Override
	public VmPlacementPolicy clone();

	@Override
	public CloudProvider getParent();

	/**
	 * Selects a host where to place the given <tt>vm</tt> and returns it.
	 *
	 * <p>A host among all given hosts will be selected such that it satisfies
	 * the virtual machine's defined SLA (defined through its configuration).<br/>
	 * Among other conditions, the selected host must be in {@link FailureState#OK} state.
	 * But, it can be in a state other than {@link PowerState#ON} if the cloud provider's
	 * power manager allows it to be powered on.
	 * In which case, it is left to the placement method to power it on.
	 *
	 * <p>Depending on whether a host was found or not, a {@link NotificationCodes#VMPLACEMENT_VMSELECTION_SUCCESS} or
	 * {@link NotificationCodes#VMPLACEMENT_VMSELECTION_FAILED} is thrown.
	 *
	 * @param vm
	 * @param hosts possible hosts. May be <tt>null</tt> in which case, all hosts in the cloud are considered.
	 * @param excludedHosts a list of hosts that should not be selected. May be <tt>null</tt>.
	 * @return the selected host or <tt>null</tt> if no host was found
	 */
	public Host selectHost(VirtualMachine vm, List<Host> hosts, List<Host> excludedHosts);

	/**
	 * Alias for {@link VmPlacementPolicy#selectHost(VirtualMachine, List, List)} where
	 * excluded hosts is <tt>null</tt>.
	 */
	public Host selectHost(VirtualMachine vm, List<Host> hosts);

	/**
	 * Alias for {@link VmPlacementPolicy#selectHost(VirtualMachine, List)} where
	 * possible hosts is <tt>null</tt>.
	 */
	public Host selectHost(VirtualMachine vm);

	/**
	 * Returns <tt>true</tt> if the vm can be placed on the given <tt>host</tt>.
	 *
	 * @param vm
	 * @param host
	 * @return <tt>true</tt> if the vm can be placed on the given <tt>host</tt>
	 */
	public boolean canPlaceVm(VirtualMachine vm, Host host);

	/**
	 * Takes all actions in order to place the given <tt>vm</tt> on the given <tt>host</tt>.
	 * When all actions were taken, the VM's parent is updated.<br/>
	 * You need to listen to the {@link NotificationCodes#ENTITY_PARENT_CHANGED} to know when the placement
	 * has ended, as it may not be the case when this method returns.
	 *
	 * <p>During the placement, any needed resources (example: {@link VirtualRam} and/or
	 * {@link VirtualStorage}) are instantiated and set accordingly to the <tt>vm</tt>'s
	 * SLA constraints (as defined in its configuration).
	 *
	 * <p>When the given <tt>vm</tt> already contains some or other resource, then
	 * implementations must use the already set resources and must not replace them.<br/>
	 * If the already set resources cannot comply to the SLA constraints, then an exception is thrown.
	 *
	 * <p>The given <tt>host</tt> may be in another state than {@link PowerState#ON}.
	 * If so then this method asks the cloud provider's power manger to power it on.
	 *
	 * <p>You need to check for the return value of {@link VmPlacementPolicy#canPlaceVm(VirtualMachine, Host)} to
	 * see if the <tt>vm</tt> can be placed on the given <tt>host</tt>.<br/>
	 * If you selected the <tt>host</tt> using {@link VmPlacementPolicy#selectHost(VirtualMachine)}
	 * then this method should succeed.
	 *
	 * @param vm the {@link VirtualMachine} to place
	 * @param host the {@link Host} where to place the <tt>vm</tt>
	 * @throws IllegalArgumentException if one of the following condition(s) are true:<ul>
	 * <li>the <tt>vm</tt> has already a defined parent;
	 * <li>the <tt>vm</tt> has already some resources set, but some of them do not comply to SLA constraints;
	 * <li>the <tt>host</tt> cannot satisfy the <tt>vm</tt> SLA (it has not enough resources for that).
	 * </ul>
	 */
	public void placeVm(VirtualMachine vm, Host host);

	/**
	 * Takes all actions in order to unplaces the given <tt>vm</tt>.<br/>
	 * When all actions were taken, the VM's parent is updated (set to <tt>null</tt>).
	 *
	 * <p>Any allocated resource during placement
	 * are detached from the parent host. This likely includes the {@link VirtualRam} and the {@link VirtualStorage} whose parents
	 * are then set to <tt>null</tt>.
	 *
	 * <p>The parent host of the VM should be powered off if
	 * the cloud provider's power manger allows it to.
	 *
	 * @param vm
	 * @throws IllegalArgumentException if the VM was not placed using this entity
	 */
	public void unplaceVm(VirtualMachine vm);
}
