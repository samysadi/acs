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

package com.samysadi.acs.service.staas;

import java.util.ArrayList;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.staas.sfconsistency.SfConsistencyManager;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.service.staas.sfreplicaselection.SfReplicaSelectionPolicy;
import com.samysadi.acs.service.staas.sfreplication.SfReplicationManager;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;
import com.samysadi.acs.utility.factory.Factory;

/**
 * This implementation only creates one replica per file (which is that file).
 *
 * <p>You should create your own implementation if you need more replicas per file, and
 * add listeners in order to keep consistency.
 *
 * @since 1.0
 */
public class StaasDefault extends EntityImpl implements Staas {
	private SfConsistencyManager consistencyManager;
	private SfPlacementPolicy placementPolicy;
	private SfReplicaSelectionPolicy replicaSelectionPolicy;
	private SfReplicationManager replicationManager;

	public StaasDefault() {
		super();
	}

	@Override
	public StaasDefault clone() {
		final StaasDefault clone = (StaasDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.consistencyManager = null;
		this.placementPolicy = null;
		this.replicaSelectionPolicy = null;
		this.replicationManager = null;
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

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof SfConsistencyManager) {
			if (this.consistencyManager == entity)
				return;
			if (this.consistencyManager != null)
				this.consistencyManager.setParent(null);
			this.consistencyManager = (SfConsistencyManager) entity;
		} else if (entity instanceof SfPlacementPolicy) {
			if (this.placementPolicy == entity)
				return;
			if (this.placementPolicy != null)
				this.placementPolicy.setParent(null);
			this.placementPolicy = (SfPlacementPolicy) entity;
		} else if (entity instanceof SfReplicaSelectionPolicy) {
			if (this.replicaSelectionPolicy == entity)
				return;
			if (this.replicaSelectionPolicy != null)
				this.replicaSelectionPolicy.setParent(null);
			this.replicaSelectionPolicy = (SfReplicaSelectionPolicy) entity;
		} else if (entity instanceof SfReplicationManager) {
			if (this.replicationManager == entity)
				return;
			if (this.replicationManager != null)
				this.replicationManager.setParent(null);
			this.replicationManager = (SfReplicationManager) entity;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof SfConsistencyManager) {
			if (this.consistencyManager != entity)
				return;
			this.consistencyManager = null;
		} else if (entity instanceof SfPlacementPolicy) {
			if (this.placementPolicy != entity)
				return;
			this.placementPolicy = null;
		} else if (entity instanceof SfReplicaSelectionPolicy) {
			if (this.replicaSelectionPolicy != entity)
				return;
			this.replicaSelectionPolicy = null;
		} else if (entity instanceof SfReplicationManager) {
			if (this.replicationManager != entity)
				return;
			this.replicationManager = null;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<Entity> l = new ArrayList<Entity>(4);
		if (this.consistencyManager != null)
			l.add(this.consistencyManager);
		if (this.placementPolicy != null)
			l.add(this.placementPolicy);
		if (this.replicaSelectionPolicy != null)
			l.add(this.replicaSelectionPolicy);
		if (this.replicationManager != null)
			l.add(this.replicationManager);

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		r.add(l);
		return new MultiListView<Entity>(r);
	}

	@Override
	public SfConsistencyManager getConsistencyManager() {
		return this.consistencyManager;
	}

	@Override
	public SfPlacementPolicy getPlacementPolicy() {
		return this.placementPolicy;
	}

	@Override
	public SfReplicaSelectionPolicy getReplicaSelectionPolicy() {
		return this.replicaSelectionPolicy;
	}

	@Override
	public SfReplicationManager getReplicationManager() {
		return this.replicationManager;
	}

	private static final Object PROP_CREATING = new Object();

	@Override
	public StorageFile createFile(long size, User user) {
		StorageFile storageFile = Factory.getFactory(this).newStorageFile(null, null, size);
		storageFile.setUser(user);

		Storage storage = getPlacementPolicy().selectStorage(storageFile);

		if (storage == null) {
			storageFile.setUser(null);
			return null;
		}

		NotificationListener n = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				StorageFile storageFile = (StorageFile) notifier;
				if (storageFile.getParent() == null)
					return;

				this.discard();

				if (getReplicationManager() != null && getConsistencyManager() != null) {
					getReplicationManager().register(storageFile);
					getConsistencyManager().register(storageFile);
				}

				storageFile.unsetProperty(PROP_CREATING);
			}
		};
		storageFile.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, n);
		storageFile.setProperty(PROP_CREATING, n);

		getPlacementPolicy().placeStorageFile(storageFile, storage);

		return storageFile;
	}

	@Override
	public void deleteFile(StorageFile storageFile) {
		if (getReplicationManager() != null && getConsistencyManager() != null) {
			List<StorageFile> replicas = getReplicationManager().getReplicas(storageFile);

			StorageFile primary = replicas.get(0);
			getConsistencyManager().unregister(primary);
			getReplicationManager().unregister(primary);

			for (StorageFile replica: replicas) {
				NotificationListener n = (NotificationListener) replica.getProperty(PROP_CREATING);
				if (n != null) {
					replica.unsetProperty(PROP_CREATING);
					n.discard();
				}

				replica.unplace();
			}
		} else {
			storageFile.unplace();
		}
	}

}
