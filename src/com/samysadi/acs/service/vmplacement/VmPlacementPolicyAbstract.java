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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.ShuffledIterator;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public abstract class VmPlacementPolicyAbstract extends EntityImpl implements VmPlacementPolicy {

	public VmPlacementPolicyAbstract() {
		super();
	}

	@Override
	public VmPlacementPolicyAbstract clone() {
		final VmPlacementPolicyAbstract clone = (VmPlacementPolicyAbstract) super.clone();
		return clone;
	}

	@Override
	public CloudProvider getParent() {
		return (CloudProvider) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof CloudProvider))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	/**
	 * Computes and returns a score indicating the level of compliancy of the given <tt>host</tt>
	 * towards the vm's constraints.
	 * 
	 * <p>A return value of <tt>0.0d</tt> or less indicates that the host does not comply (at all). And the <tt>vm</tt>
	 * cannot be placed on that host.
	 * 
	 * <p>If this method returns {@link Double#POSITIVE_INFINITY} then the given  host produces the highest matching score for current vm's constraints.
	 * 
	 * <p>This method takes care of the {@link FailureState} of the host and its components.
	 * 
	 * <p>This method <b>does not</b> take care of the {@link PowerState} of the host.
	 * 
	 * @param vm
	 * @param host
	 * @return computed score for placing the given vm on the given host
	 */
	protected double computeHostScore(VirtualMachine vm, Host host) {
		if (host.getFailureState() != FailureState.OK)
			return 0.0d;

		double score = 0.0d;

		Config vmConfig = vm.getConfig();
		if (vmConfig == null)
			vmConfig = new Config();

		//VirtualRam
		{
			final long slaRamCapacity = vmConfig.getLong("Ram_Capacity", 1024l) * Simulator.MEBIBYTE;

			long neededSize = slaRamCapacity;
			if (vm.getVirtualRam() != null && neededSize < vm.getVirtualRam().getCapacity())
				neededSize = vm.getVirtualRam().getCapacity();
			if (host.getRam().getFreeCapacity() < neededSize)
				return 0.0d;
			score+= (double) host.getRam().getFreeCapacity() / slaRamCapacity;
		}

		//ProcessingUnit
		{
			final int slaPuCount = vmConfig.getInt("Pu_Count", 1);
			final long slaPuMips = vmConfig.getLong("Pu_Mips", 1000l) * Simulator.MI;

			int totalAllocated = 0;
			int totalOk = 0;
			for (ProcessingUnit pu:host.getProcessingUnits()) {
				if (pu.getFailureState() != FailureState.OK)
					continue;
				if (!pu.isAllocated()) {
					if (slaPuMips <= pu.getComputingProvisioner().getCapacity())
						totalOk++;
				} else
					totalAllocated++;
			}

			if (totalOk < slaPuCount)
				return 0.0d;

			score+= (double) host.getProcessingUnits().size() / (host.getProcessingUnits().size() - totalAllocated - totalOk + 1);
		}
	
		//VirtualStorage
		{
			final long slaStorageCapacity = vmConfig.getLong("Storage_Capacity", 10000l) * Simulator.MEBIBYTE;

			long neededSize = slaStorageCapacity;
			if (vm.getVirtualStorage() != null) {
				neededSize = Math.max(
						neededSize,
						vm.getVirtualStorage().getCapacity() - vm.getVirtualStorage().getFreeCapacity()
					);
			}
	
			if (vm.getVirtualStorage() != null && neededSize < vm.getVirtualStorage().getCapacity())
				neededSize = vm.getVirtualStorage().getCapacity();

			long mostFreeSize = 0l;
			for (Storage s: host.getStorages()) {
				if (s.getFailureState() != FailureState.OK)
					continue;
				long m = s.getFreeCapacity();
				if (m >= neededSize) {
					mostFreeSize = m;
					break;
				}
			}

			if (mostFreeSize < neededSize)
				return 0.0d;
		}

		//Network
		{
			final long slaDownBw = vmConfig.getLong("Network_DownloadBw", 0l) * Simulator.MEBIBYTE;
			final long slaUpBw = vmConfig.getLong("Network_UploadBw", 0l) * Simulator.MEBIBYTE;

			if ((slaUpBw != 0l) || (slaDownBw != 0l)) {
				long sumUpBw = 0l;
				long sumDownBw = 0l;
				for (NetworkInterface ni: host.getInterfaces()) {
					if (ni.getFailureState() != FailureState.OK)
						continue;
					sumUpBw+= ni.getUpLink().getNetworkProvisioner().getCapacity();
					sumDownBw+= ni.getDownLink().getNetworkProvisioner().getCapacity();
				}
		
				if ((slaUpBw != 0l && sumUpBw < slaUpBw) || (slaDownBw != 0l && sumDownBw < slaDownBw))
					return 0.0d;
			}
		}

		return score;
	}

	/**
	 * Returns the selected host among all given hosts.
	 * 
	 * <p>Override this method in order to define your policy for selecting the host.
	 * 
	 * @param vm
	 * @param poweredOnHosts a list of powered on hosts. Cannot be <tt>null</tt>.
	 * @param excludedHosts a list of excluded hosts. May be <tt>null</tt>.
	 * @return the selected host among all given hosts
	 */
	protected abstract Host _selectHost(VirtualMachine vm, List<Host> poweredOnHosts, List<Host> excludedHosts);

	/**
	 * Returns the selected host among all given hosts.
	 * 
	 * <p>This method is used when {@link VmPlacementPolicyAbstract#_selectHost(VirtualMachine)} returns <tt>null</tt>.
	 * 
	 * @param vm
	 * @param hosts a list of alternative hosts. Cannot be <tt>null</tt>.
	 * @param excludedHosts a list of excluded hosts. May be <tt>null</tt>.
	 * @return the selected host
	 */
	protected Host _selectHostAlternative(VirtualMachine vm, List<Host> hosts, List<Host> excludedHosts) {
		Iterator<Host> it = new ShuffledIterator<Host>(hosts);
		while (it.hasNext()) {
			final Host candidate = it.next();
			if (excludedHosts != null && excludedHosts.contains(candidate))
				continue;
			if (candidate.getPowerState() == PowerState.ON ||
					candidate.getCloudProvider().getPowerManager().canPowerOn(candidate)) {	
				final double s = computeHostScore(vm, candidate);
				if (s > 0)
					return candidate;
			}
		}
		return null;
	}

	@Override
	public Host selectHost(VirtualMachine vm, List<Host> hosts, List<Host> excludedHosts) {
		Host bestHost;

		List<Host> poweredOffHosts = null;
		{
			List<Host> poweredOnHosts;
			if (hosts == null) {
				poweredOnHosts = getParent().getPowerManager().getPoweredOnHosts();
			} else {
				poweredOnHosts = new ArrayList<Host>(hosts.size());
				poweredOffHosts = new ArrayList<Host>(hosts.size());
				for (Host h: hosts) {
					if (excludedHosts != null && excludedHosts.contains(h))
						continue;
					if (h.getPowerState() == PowerState.ON)
						poweredOnHosts.add(h);
					else
						poweredOffHosts.add(h);
				}
				excludedHosts = null;
			}
	
			bestHost = _selectHost(vm, poweredOnHosts, excludedHosts);
		}

		if (bestHost == null)
			bestHost = _selectHostAlternative(vm, poweredOffHosts == null ? getParent().getHosts() : poweredOffHosts, excludedHosts);

		if (bestHost == null) {
			notify(NotificationCodes.VMPLACEMENT_VMSELECTION_FAILED, vm);
			return null;
		}

		notify(NotificationCodes.VMPLACEMENT_VMSELECTION_SUCCESS, vm);
		return bestHost;
	}

	@Override
	public final Host selectHost(VirtualMachine vm, List<Host> hosts) {
		return selectHost(vm, hosts, null);
	}

	@Override
	public final Host selectHost(VirtualMachine vm) {
		return selectHost(vm, null);
	}

	@Override
	public boolean canPlaceVm(VirtualMachine vm, Host host) {
		if (host.getFailureState() != FailureState.OK)
			return false;
		if (host.getPowerState() != PowerState.ON &&
					!host.getCloudProvider().getPowerManager().canPowerOn(host))
			return false;
		return computeHostScore(vm, host) > 0.0d;
	}

	//TODO we need a flag/property to make distinction between VMs being placed and others (ie: be able to say: I'm already placing this vm, but I just didn't finish yet because host is not on yet). This applies for Files placement too. Does this applies for unplacing?
	@Override
	public void placeVm(final VirtualMachine vm, final Host host) {
		if (vm.getParent() != null)
			throw new IllegalArgumentException("The given VM has already a defined parent");

		if (host.getFailureState() != FailureState.OK)
			throw new IllegalArgumentException("Cannot place the vm on the given host: host is failed");

		Config vmConfig = vm.getConfig();
		if (vmConfig == null)
			vmConfig = new Config();

		//VirtualRam
		{
			final long slaRamCapacity = vmConfig.getLong("Ram_Capacity", 1024l) * Simulator.MEBIBYTE;

			if (host.getRam().getFreeCapacity() < slaRamCapacity)
				throw new IllegalArgumentException("Cannot place the vm on the given host: not enough Ram");

			VirtualRam vRam = vm.getVirtualRam();
			if (vRam == null) {
				vRam = Factory.getFactory(this).newVirtualRam(null, host.getRam(), slaRamCapacity);
				vm.setVirtualRam(vRam);
			} else {
				//check capacity and force the virtual ram to be in the same host as the VM
				vRam.setParent(null);
				if (vRam.getCapacity() < slaRamCapacity)
					vRam.setCapacity(slaRamCapacity);
				vRam.setParent(host.getRam());
			}
			vRam.setIsCapacityReserved(true);
		}

		//ProcessingUnit
		{
			final int slaPuCount = vmConfig.getInt("Pu_Count", 1);
			final long slaPuMips = vmConfig.getLong("Pu_Mips", 1000l) * Simulator.MI;

			List<ProcessingUnit> pus = new ArrayList<ProcessingUnit>();
			for (ProcessingUnit pu:host.getProcessingUnits()) {
				if (pus.size() >= slaPuCount)
					break;
				if (!pu.isAllocated() && slaPuMips <= pu.getComputingProvisioner().getCapacity())
					pus.add(pu);
			}

			if (pus.size() < slaPuCount)
				throw new IllegalArgumentException("Cannot place the vm on the given host: not enough Computing power");

			vm.setUsableProcessingUnits(pus);
		}
	
		//VirtualStorage
		{	
			final long slaStorageCapacity = vmConfig.getLong("Storage_Capacity", 10000l) * Simulator.MEBIBYTE;

			long neededSize = slaStorageCapacity;
			if (vm.getVirtualStorage() != null) {
				neededSize = Math.max(
						neededSize,
						vm.getVirtualStorage().getCapacity() - vm.getVirtualStorage().getFreeCapacity()
					);
			}

			long mostFreeSize = 0l;
			Storage best = null;
			for (Storage s: host.getStorages()) {
				long m = s.getFreeCapacity();
				if ((best == null) ||  m > mostFreeSize) {
					best = s;
					mostFreeSize = m;
				}
			}

			if (best == null || (mostFreeSize < neededSize))
				throw new IllegalArgumentException("Cannot place the vm on the given host: not enough Storage capacity");

			VirtualStorage vStorage = vm.getVirtualStorage();
			if (vStorage == null) {
				vStorage = Factory.getFactory(this).newVirtualStorage(null, best, neededSize);
				vStorage.setUser(vm.getUser());
				vm.setVirtualStorage(vStorage);
			} else {
				//check capacity and force the virtual storage to be in the same host as the VM
				vStorage.setParent(null);
				if (vStorage.getCapacity() < slaStorageCapacity)
					vStorage.setCapacity(slaStorageCapacity);
				vStorage.setParent(best);
			}
			vStorage.setIsCapacityReserved(true);
		}

		//Network
		{
			final long slaDownBw = vmConfig.getLong("Network_DownloadBw", 0l) * Simulator.MEBIBYTE;
			final long slaUpBw = vmConfig.getLong("Network_UploadBw", 0l) * Simulator.MEBIBYTE;

			if ((slaUpBw != 0l) || (slaDownBw != 0l)) {
				long sumUpBw = 0l;
				long sumDownBw = 0l;
				for (NetworkInterface ni: host.getInterfaces()) {
					sumUpBw+= ni.getUpLink().getNetworkProvisioner().getCapacity();
					sumDownBw+= ni.getDownLink().getNetworkProvisioner().getCapacity();
				}
		
				if ((slaUpBw != 0l && sumUpBw < slaUpBw) || (slaDownBw != 0l && sumDownBw < slaDownBw))
					throw new IllegalArgumentException("Cannot place the vm on the given host: not enough Bw");
	
				vm.setUsableNetworkInterfaces(null); //all interfaces
			}
		}

		vm.setPlacementPolicy(VmPlacementPolicyAbstract.this);

		//check host power state
		if (host.getPowerState() != PowerState.ON) {
			host.addListener(NotificationCodes.POWER_STATE_CHANGED, new PowerStateListener(vm));

			host.getCloudProvider().getPowerManager().powerOn(host);
		} else {
			host.getCloudProvider().getPowerManager().lockHost(host);
			vm.setParent(host);
		}
	}

	private static final class PowerStateListener extends NotificationListener {
		private final VirtualMachine vm;

		private PowerStateListener(VirtualMachine vm) {
			this.vm = vm;
		}

		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			Host host = (Host) notifier;
			if (host.getPowerState() != PowerState.ON)
				return;
			host.getCloudProvider().getPowerManager().lockHost(host);

			vm.setParent(host);

			this.discard();
		}
	}

	@Override
	public void unplaceVm(VirtualMachine vm) {
		Host host = vm.getParent();
		if (host == null || vm.getPlacementPolicy() != this)
			throw new IllegalArgumentException("The given VM was not placed using this VmPlacementPolicy");

		//the vm cannot use host's resources anymore
		if (vm.getVirtualRam() != null)
			vm.getVirtualRam().setParent(null);
		vm.setUsableProcessingUnits(null);
		if (vm.getVirtualStorage() != null)
			vm.getVirtualStorage().setParent(null);
		vm.setUsableNetworkInterfaces(null);

		//
		vm.setParent(null);
		vm.setPlacementPolicy(null);

		host.getCloudProvider().getPowerManager().unlockHost(host);

		//see if host can be powered off
		if (host.getCloudProvider().getPowerManager().canPowerOff(host))
			host.getCloudProvider().getPowerManager().powerOff(host);
	}
}
