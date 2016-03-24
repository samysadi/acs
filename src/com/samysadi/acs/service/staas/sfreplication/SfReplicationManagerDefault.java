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

package com.samysadi.acs.service.staas.sfreplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.utility.NotificationCodes;

/**
 *
 * @since 1.0
 */
public class SfReplicationManagerDefault extends EntityImpl implements SfReplicationManager {

	private NotificationListener replica_deleted_listener;

	public SfReplicationManagerDefault() {
		super();
	}

	@Override
	public SfReplicationManagerDefault clone() {
		final SfReplicationManagerDefault clone = (SfReplicationManagerDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.replica_deleted_listener = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				StorageFile replica = (StorageFile) notifier;
				if (replica.getParent() != null)
					return;

				@SuppressWarnings("unchecked")
				List<StorageFile> r = (List<StorageFile>) replica.getProperty(PROP_REPLICAS_KEY);

				if (r == null)
					return;

				if (r.get(0) == replica) {
					getParent().deleteFile(r.get(0)); //primary is deleted
					return;
				}

				//secondary replica is deleted, so let's remove it and replace it with a new one
				removeReplica(replica);
				r.remove(replica);

				addReplica(r.get(0), r);
			}
		};
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

	private static final Object PROP_REPLICAS_KEY = new Object();

	protected void addReplica(StorageFile storageFile, List<StorageFile> r) {
		List<Host> excludedHosts = getStorageHosts(r);

		StorageFile replica = storageFile.clone();
		r.add(replica);
		replica.setProperty(PROP_REPLICAS_KEY, r);

		replica.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, replica_deleted_listener);

		Storage storage = getParent().getPlacementPolicy().selectStorage(replica, null, excludedHosts);
		getParent().getPlacementPolicy().placeStorageFile(replica, storage);
	}

	private List<Host> getStorageHosts(List<StorageFile> r) {
		ArrayList<Host> l = newArrayList(r.size());
		for (StorageFile sf: r) {
			if (sf.hasParentRec())
				l.add(sf.getParent().getParentHost());
		}
		return l;
	}

	/**
	 * You still need to remove it from the list.
	 *
	 * @param replica
	 */
	private void removeReplica(StorageFile replica) {
		replica.unsetProperty(PROP_REPLICAS_KEY);
		replica.removeListener(NotificationCodes.ENTITY_PARENT_CHANGED, replica_deleted_listener);
	}

	@Override
	public void register(StorageFile storageFile) {
		if (getReplicas(storageFile).size() > 1)
			return; //already replicated

		final int replicasCount = getReplicasCount(storageFile);

		final List<StorageFile> r = newArrayList(replicasCount);
		r.add(storageFile);
		storageFile.setProperty(PROP_REPLICAS_KEY, r);

		for (int i=0; i<replicasCount - 1; i++)
			addReplica(storageFile, r);
	}

	@Override
	public void unregister(StorageFile storageFile) {
		for (StorageFile replica: getReplicas(storageFile))
			removeReplica(replica);
	}

	@Override
	public List<StorageFile> getReplicas(StorageFile storageFile) {
		@SuppressWarnings("unchecked")
		List<StorageFile> r = (List<StorageFile>) storageFile.getProperty(PROP_REPLICAS_KEY);

		if (r == null)
			return Collections.unmodifiableList(Arrays.asList(storageFile));

		return r;
	}

	protected int getReplicasCount(StorageFile storageFile) {
		//Consistency Manager implementation is buggy
		//until it is fixed this method will return 1
		return 1;
		//return 3;
	}

}
