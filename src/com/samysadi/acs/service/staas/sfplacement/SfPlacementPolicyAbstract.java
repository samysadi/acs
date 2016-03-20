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

package com.samysadi.acs.service.staas.sfplacement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.ShuffledIterator;

/**
 *
 * @since 1.0
 */
public abstract class SfPlacementPolicyAbstract extends EntityImpl implements SfPlacementPolicy {

	public SfPlacementPolicyAbstract() {
		super();
	}

	@Override
	public SfPlacementPolicyAbstract clone() {
		final SfPlacementPolicyAbstract clone = (SfPlacementPolicyAbstract) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();


	}

	@Override
	public Staas getParent() {
		return (Staas) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Staas))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	/**
	 * Computes and returns a score indicating the level of compliancy of the given <tt>storage</tt>
	 * towards the storage file's constraints.
	 *
	 * <p>A return value of <tt>0.0d</tt> or less indicates that the host does not comply (at all). And the <tt>storageFile</tt>
	 * cannot be placed on that storage.
	 *
	 * <p>If this method returns {@link Double#POSITIVE_INFINITY} then the given storage produces the highest matching score for the given storageFile.
	 *
	 * <p>This method takes care of the {@link FailureState} of the storage and its parent host.
	 *
	 * <p>This method <b>does not</b> take care of the {@link PowerState} of the parent host of the storage.
	 *
	 * @param storageFile
	 * @param storage
	 * @return computed score for placing the given storageFile on the given storage
	 */
	public double computeStorageScore(StorageFile storageFile, Storage storage) {
		if (storage.getFailureState() != FailureState.OK)
			return 0.0d;
		if (storage.getParentHost() == null || storage.getParentHost().getFailureState() != FailureState.OK)
			return 0.0d;

		long fc = storage.getFreeCapacity();
		long s = storageFile.getSize();
		if (s > fc)
			return 0.0d;

		return (double) s / fc;
	}

	/**
	 * Returns the selected storage among all given hosts' storages.
	 *
	 * <p>Override this method in order to define your policy for selecting the storage.
	 *
	 * @param storageFile
	 * @param poweredOnHosts a list of powered on hosts. Cannot be <tt>null</tt>.
	 * @param excludedHosts a list of excluded hosts. May be <tt>null</tt>.
	 * @return the selected storage among all given hosts' storages
	 */
	protected abstract Storage _selectStorage(StorageFile storageFile, List<Host> poweredOnHosts, List<Host> excludedHosts);

	/**
	 * Returns the selected storage among all given hosts' storages.
	 *
	 * <p>This method is used when {@link SfPlacementPolicyAbstract#_selectStorage(StorageFile)} returns <tt>null</tt>.
	 *
	 * @param storageFile
	 * @param hosts a list of alternative hosts. Cannot be <tt>null</tt>.
	 * @param excludedHosts a list of excluded hosts. May be <tt>null</tt>.
	 * @return the selected host
	 */
	protected Storage _selectStorageAlternative(StorageFile storageFile, List<Host> hosts, List<Host> excludedHosts) {
		Iterator<Host> it = new ShuffledIterator<Host>(hosts);
		while (it.hasNext()) {
			final Host hostCandidate = it.next();
			if (excludedHosts != null && excludedHosts.contains(hostCandidate))
				continue;
			if (hostCandidate.getPowerState() == PowerState.ON ||
					hostCandidate.getCloudProvider().getPowerManager().canPowerOn(hostCandidate)) {
				final Iterator<Storage> it2 = new ShuffledIterator<Storage>(hostCandidate.getStorages());
				while (it2.hasNext()) {
					final Storage candidate = it2.next();
					final double s = computeStorageScore(storageFile, candidate);

					if (s > 0)
						return candidate;
				}
			}
		}
		return null;
	}

	@Override
	public Storage selectStorage(StorageFile storageFile, List<Host> hosts, List<Host> excludedHosts) {
		Storage bestStorage;

		List<Host> poweredOffHosts = null;
		{
			List<Host> poweredOnHosts;
			if (hosts == null) {
				poweredOnHosts = getParent().getParent().getPowerManager().getPoweredOnHosts();
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

			bestStorage = _selectStorage(storageFile, poweredOnHosts, excludedHosts);
		}

		if (bestStorage == null)
			bestStorage = _selectStorageAlternative(storageFile, poweredOffHosts == null ? getParent().getParent().getHosts() : poweredOffHosts, excludedHosts);

		if (bestStorage == null) {
			notify(NotificationCodes.SFP_STORAGESELECTION_FAILED, storageFile);
			return null;
		}

		notify(NotificationCodes.SFP_STORAGESELECTION_SUCCESS, storageFile);
		return bestStorage;
	}

	@Override
	public final Storage selectStorage(StorageFile storageFile, List<Host> hosts) {
		return selectStorage(storageFile, hosts, null);
	}

	@Override
	public final Storage selectStorage(StorageFile storageFile) {
		return selectStorage(storageFile, null);
	}

	@Override
	public boolean canPlaceStorageFile(StorageFile storageFile, Storage storage) {
		Host host = storage.getParentHost();
		if (host.getCloudProvider() != getParent().getParent()) {
			//delegate to the appropriate cloud provider
			return host.getCloudProvider().getStaas().getPlacementPolicy().canPlaceStorageFile(storageFile, storage);
		}
		if (storage.getFailureState() != FailureState.OK)
			return false;
		if (host.getFailureState() != FailureState.OK)
			return false;
		if (host.getPowerState() != PowerState.ON &&
					!host.getCloudProvider().getPowerManager().canPowerOn(host))
			return false;
		return storage.getFreeCapacity() >= storageFile.getSize();
	}

	@Override
	public void placeStorageFile(final StorageFile storageFile, final Storage storage) {
		Host host = storage.getParentHost();
		if (host.getCloudProvider() != getParent().getParent()) {
			//delegate to the appropriate cloud provider
			host.getCloudProvider().getStaas().getPlacementPolicy().placeStorageFile(storageFile, storage);
			return;
		}

		if (storageFile.getParent() != null)
			throw new IllegalArgumentException("The given StorageFile has already a defined parent");

		if (storage.getFreeCapacity() < storageFile.getSize())
			throw new IllegalArgumentException("Not enough space in the storage to place the file on it");

		//check host power state
		if (host.getPowerState() != PowerState.ON) {
			final long size = storageFile.getSize();
			storage.allocate(size);
			host.addListener(NotificationCodes.POWER_STATE_CHANGED, new PowerStateListener(storage, storageFile, size));

			host.getCloudProvider().getPowerManager().powerOn(host);
		} else {
			host.getCloudProvider().getPowerManager().lockHost(host);
			storageFile.setParent(storage);
		}
	}

	private static final class PowerStateListener extends NotificationListener {
		private final Storage storage;
		private final StorageFile storageFile;
		private final long size;

		private PowerStateListener(Storage storage, StorageFile storageFile,
				long size) {
			this.storage = storage;
			this.storageFile = storageFile;
			this.size = size;
		}

		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			Host host = (Host) notifier;
			if (host.getPowerState() != PowerState.ON)
				return;
			host.getCloudProvider().getPowerManager().lockHost(host);

			storage.free(size);

			storageFile.setParent(storage);

			this.discard();
		}
	}

	@Override
	public void unplaceStorageFile(StorageFile storageFile) {
		Storage storage = storageFile.getParent();
		if (storage == null || storageFile.getPlacementPolicy() != this)
			throw new IllegalArgumentException("The given StorageFile was not placed using this VmPlacementPolicy");

		//
		storageFile.unplace();
		storageFile.setPlacementPolicy(null);

		Host host = storage.getParentHost();
		host.getCloudProvider().getPowerManager().lockHost(host);
		//see if host can be powered off
		if (host.getCloudProvider().getPowerManager().canPowerOff(host))
			host.getCloudProvider().getPowerManager().powerOff(host);
	}
}
