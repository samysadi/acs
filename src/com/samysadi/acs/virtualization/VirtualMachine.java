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

package com.samysadi.acs.virtualization;

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.operation.provisioner.NetworkProvisioner;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.hardware.pu.operation.provisioner.ComputingProvisioner;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicy;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.Job;

/**
 * A VirtualMachine is a {@link RunnableEntity} that can have multiple jobs running inside of it.
 *
 * <p>Before starting a job inside a VM, ensure that the VM is running or you will get an IllegalStateException.
 *
 * <p>All the jobs inside the VM will share its allocated resources (computing/networking/storage).
 * Though, you can define specific rules on how resources are provisioned inside the VM.
 * For example, you can set a resource limit for this VM, and/or you can define your own rules
 * on how the resources are shared among jobs inside the VM.<br/>
 * To do so you have to instantiate a provisioner for the type of resource you want, and define this VM
 * as its parent. Just make sure the VM is not
 * running before doing so, or you will get an IllegalStateException.<br/>
 * You can only define one provisioner per resource type ({@link ComputingProvisioner}, {@link StorageProvisioner}, {@link NetworkProvisioner}).
 *
 * <p>{@link ComputingOperation}s in this VM will seek a {@link ProcessingUnit} using this VM's {@link PuAllocator}.
 *
 * <p>A {@link User} may own the VM.
 *
 * <p>
 * @since 1.0
 */
public interface VirtualMachine extends Entity, RunnableEntity {
	/**
	 * Creates a clone of current VM, and unsets any assigned resource on that clone (usable processing units, usable storages,
	 * usable network interfaces, virtual ram and virtual storage).
	 *
	 * <p>The same user owns the cloned VM and it has the same SLA constraints.
	 *
	 * <p>Other children entities are cloned just as specified by superclass clone method.
	 */
	@Override
	public VirtualMachine clone();

	@Override
	public Host getParent();

	/**
	 * {@inheritDoc}
	 *
	 * <p><b>Note</b> If you want to <u>unplace</u> rather than just update this VM's parent,
	 * then use {@link VirtualMachine#unplace()}. This method will only update current VM's parent without taking
	 * any further actions.
	 *
	 * @see VirtualMachine#unplace()
	 */
	@Override
	public void setParent(Entity parent);

	/**
	 * Returns the {@link VmPlacementPolicy} that was used when placing this VM in the current host or
	 * <tt>null</tt> if this VM has no defined {@link VmPlacementPolicy}.
	 *
	 * @return the {@link VmPlacementPolicy} that was used when placing this VM or <tt>null</tt>
	 */
	public VmPlacementPolicy getPlacementPolicy();

	/**
	 * This method is called by {@link VmPlacementPolicy} when placing this VM on a host.
	 *
	 * <p>You <b>should not</b> need to call this method if you are not implementing a placement policy.
	 *
	 * @param policy
	 */
	public void setPlacementPolicy(VmPlacementPolicy policy);

	/**
	 * Returns the {@link User} that owns this Virtual Machine.
	 *
	 * @return the {@link User} that owns this Virtual Machine
	 */
	public User getUser();

	/**
	 * Updates the {@link User} that owns this Virtual Machine.
	 *
	 * @param user
	 */
	public void setUser(User user);

	/**
	 * Returns the list of {@link Job}s that are running on top of this VirtualMachine.
	 *
	 * @return the list of {@link Job}s that are running on top of this VirtualMachine
	 */
	public List<Job> getJobs();

	/**
	 * Returns the processing units that this VM can use.
	 *
	 * <p><b>Default</b> is set so that all processing units on the parent host are usable.
	 *
	 * @return the processing units that this VM can use
	 */
	public List<ProcessingUnit> getUsableProcessingUnits();

	/**
	 * Same as {@link VirtualMachine#setUsableProcessingUnits(List, boolean)} with
	 * the <tt>allocatePu</tt> parameter set to <tt>true</tt>.
	 */
	public void setUsableProcessingUnits(List<ProcessingUnit> processingUnits);

	/**
	 * Updates the processing units that this VM can use.
	 *
	 * <p>If <tt>allocatePu</tt> parameter is <tt>true</tt>, then this method will also update the allocated flag
	 * for each of the processing units in the given list.
	 * And, if one of the given processing units is already allocated then an exception is thrown.
	 *
	 * <p>If <tt>null</tt> is given then this VM is allowed to use any processing unit in the parent host,
	 * even if it is allocated for another VM and independently from the <tt>allocatePu</tt> parameter.
	 *
	 * @param processingUnits usable processing units, or to <tt>null</tt> to let this vm use any processing unit on the parent host
	 * @throws IllegalStateException if this VM is running
	 * @throws IllegalArgumentException if <tt>allocatePu</tt> parameter is <tt>true</tt> and one of the PUs in the
	 * given <tt>processingUnits</tt> list is already allocated for another VM
	 */
	public void setUsableProcessingUnits(List<ProcessingUnit> processingUnits, boolean allocatePu);

	/**
	 * Returns the {@link PuAllocator} associated with this VM.
	 *
	 * @return the {@link PuAllocator} associated with this VM
	 */
	public PuAllocator getPuAllocator();

	/**
	 * Returns the virtual ram of this VM.
	 *
	 * @return the virtual ram of this VM
	 */
	public VirtualRam getVirtualRam();

	/**
	 * Updates the virtual ram associated with this VM.
	 *
	 * @param v
	 * @throws IllegalStateException if this VM is running
	 * @throws IllegalArgumentException if the given virtual ram is already allocated for another VM
	 */
	public void setVirtualRam(VirtualRam v);

	/**
	 * Returns the virtual storage associated with this VM.
	 *
	 * @return the virtual storage associated with this VM
	 */
	public VirtualStorage getVirtualStorage();

	/**
	 * Updates the virtual storage associated with this VirtualMachine.
	 *
	 * @param v
	 * @throws IllegalStateException if this VM is running
	 * @throws IllegalArgumentException if the given virtual storage is already allocated for another VM
	 */
	public void setVirtualStorage(VirtualStorage v);

	/**
	 * Returns the network interfaces that this virtual machine is allowed to use.
	 *
	 * <p><b>Default</b> is set so that all interfaces on the parent host are usable.
	 *
	 * @return the network interfaces that this virtual machine is allowed to use
	 */
	public List<NetworkInterface> getUsableNetworkInterfaces();

	/**
	 * Updates the network interfaces that this virtual machine is allowed to use.
	 *
	 * <p>If <tt>null</tt> is given then this VM is allowed to use any network interface in the parent host.
	 *
	 * @param networkInterfaces usable network interfaces, set to <tt>null</tt> to let this vm use any network interface on the parent host.
	 * @throws IllegalStateException if this VM is running
	 */
	public void setUsableNetworkInterfaces(List<NetworkInterface> networkInterfaces);

	/**
	 * Returns the ComputingProvisioner for computing operations at this virtual machine level, or <tt>null</tt> if none is set.
	 *
	 * <p>A ComputingProvisioner is not mandatory for VMs. It should be used only if you want to set specific MIPS limits for this particular VM. So,
	 * for example, you can set a maximum allowable MIPS for all operations inside this VM. You can also allow specific Jobs to have more MIPS than others.
	 *
	 * <p><tt>null</tt> can be returned if there is no need for special strategy at this VM level.
	 *
	 * @return this virtual machine's computing provisioner or <tt>null</tt> if no computing provisioner was set
	 */
	public ComputingProvisioner getComputingProvisioner();

	/**
	 * Returns the StorageProvisioner for storage operations at this virtual machine level, or <tt>null</tt> if none is set.
	 *
	 * <p>A StorageProvisioner is not mandatory for VMs. It should be used only if you want to set specific transfer rate limits for this particular VM. So,
	 * for example, you can set a maximum allowable transfer rate for all operations inside this VM. You can also allow specific Jobs to have more transfer rate than others.
	 *
	 * <p><tt>null</tt> can be returned if there is no need for special strategy at this VM level.
	 *
	 * @return this virtual machine's storage provisioner or <tt>null</tt> if no storage provisioner was set
	 */
	public StorageProvisioner getStorageProvisioner();

	/**
	 * Returns the NetworkProvisioner for network operations at this virtual machine level, or <tt>null</tt> if none is set.
	 *
	 * <p>A NetworkProvisioner is not mandatory for VMs. It should be used only if you want to set specific BW limits for this particular VM. So,
	 * for example, you can set a maximum allowable BW for all operations inside this VM. You can also allow specific Jobs to have more BW than others.
	 *
	 * <p><tt>null</tt> can be returned if there is no need for special strategy at this VM level.
	 *
	 * @return this virtual machine's network provisioner or <tt>null</tt> if no network provisioner was set
	 */
	public NetworkProvisioner getNetworkProvisioner();

	/**
	 * Indicates that {@link NetworkOperation} entities that run on top of this VM must buffer their {@link NotificationCodes#RUNNABLE_STATE_CHANGED} notifications
	 * using this VM's buffer.
	 */
	public static final long FLAG_BUFFER_NETWORK_OUTPUT		= 0x00001000;

	/**
	 * Indicates that this VM is being migrated to another host.
	 */
	public static final long FLAG_IS_MIGRATING				= 0X00010000;

	/**
	 * Returns <tt>true</tt> if this flag is set
	 *
	 * @param flag the flag id
	 * @return <tt>true</tt> if this flag is set
	 * @see VirtualMachine#setFlag(long)
	 */
	public boolean getFlag(long flag);

	/**
	 * Sets the flag. And a {@link NotificationCodes#VM_FLAG_CHANGED} notification is thrown.
	 *
	 * @param flag
	 */
	public void setFlag(long flag);

	/**
	 * Unsets the flag.
	 *
	 * @param flag
	 */
	public void unsetFlag(long flag);

	/**
	 * Adds the given notification information into a buffer in this VM level.
	 *
	 * <p>Buffered notifications can then be released using the {@link VirtualMachine#releaseBufferedNotifications(int)} or
	 * discarded using {@link VirtualMachine#clearBufferedNotifications(int)}.
	 *
	 * @param notifier
	 * @param notification_code
	 * @param data
	 */
	public void bufferNotification(Notifier notifier, int notification_code, Object data);

	/**
	 * Clears all buffered notifications that were added before the given epoch.
	 *
	 * @param epoch all notifications that were added before this epoch (exclusive) are cleared. Use {@link VirtualMachine#getNotificationsBufferEpoch()} to
	 * get current epoch.
	 */
	public void clearBufferedNotifications(int epoch);

	/**
	 * Returns the current epoch of the VM's notifications buffer.
	 * This is also equal to the epoch of the next notification that will be buffered.
	 *
	 * <p>You can use this to selectively release or clear buffered notifications.
	 *
	 * <p>Note that after buffering a new notification this value is incremented.
	 *
	 * @return the current epoch of the VM's notifications buffer
	 */
	public int getNotificationsBufferEpoch();

	/**
	 * Releases and clears all buffered notifications before the given epoch, and calls the {@link Notifier#notify(int, Object)} on
	 * each released <tt>notifier</tt>.
	 *
	 * @param epoch all notifications that were added before this epoch (exclusive) are released. Use {@link VirtualMachine#getNotificationsBufferEpoch()} to
	 * get current epoch.
	 */
	public void releaseBufferedNotifications(int epoch);
}
