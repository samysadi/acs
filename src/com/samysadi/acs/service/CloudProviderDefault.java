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

package com.samysadi.acs.service;

import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.service.checkpointing.CheckpointingHandler;
import com.samysadi.acs.service.checkpointing.VmCheckpointingHandler;
import com.samysadi.acs.service.jobplacement.JobPlacementPolicy;
import com.samysadi.acs.service.migration.MigrationHandler;
import com.samysadi.acs.service.power.PowerManager;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicy;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;
import com.samysadi.acs.utility.collections.infrastructure.Cloud;
import com.samysadi.acs.utility.collections.infrastructure.CloudImpl;
import com.samysadi.acs.utility.collections.infrastructure.ClusterImpl;
import com.samysadi.acs.utility.collections.infrastructure.DatacenterImpl;
import com.samysadi.acs.utility.collections.infrastructure.Rack;
import com.samysadi.acs.utility.collections.infrastructure.RackImpl;


/**
 *
 * @since 1.0
 */
public class CloudProviderDefault extends EntityImpl implements CloudProvider {
	private RackImpl defaultRack;
	/**
	 * A structure that contains all hosts owned by this provider.
	 */
	private CloudImpl cloud;
	private List<Switch> switches;
	private List<User> users;
	private PowerManager powerManager;
	private JobPlacementPolicy jobPlacementPolicy;
	private VmPlacementPolicy vmPlacementPolicy;
	private Staas staas;
	private MigrationHandler migrationHandler;
	private VmCheckpointingHandler vmCheckpointingHandler;

	public CloudProviderDefault() {
		super();
	}

	@Override
	public CloudProviderDefault clone() {
		final CloudProviderDefault clone = (CloudProviderDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.cloud = new CloudImpl();
		this.defaultRack = null;
		addDatacenter();

		this.switches = newArrayList();
		this.users = newArrayList();
		this.powerManager = null;
		this.jobPlacementPolicy = null;
		this.vmPlacementPolicy = null;
		this.staas = null;
		this.migrationHandler = null;
		this.vmCheckpointingHandler = null;
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof ThinClient) {
			throw new IllegalArgumentException("You cannot add such entities to this entity");
		} else if (entity instanceof Host) {
//			this is always false
//			if (this.defaultRack == null)
//				throw new NullPointerException("Please add at least one rack before adding hosts.");
			if (!this.defaultRack.add((Host) entity))
				return;
		} else if (entity instanceof Switch) {
			if (!this.switches.add((Switch) entity))
				return;
		} else if (entity instanceof User) {
			if (!this.users.add((User) entity))
				return;
		} else if (entity instanceof PowerManager) {
			if (this.powerManager == entity)
				return;
			if (this.powerManager != null)
				this.powerManager.setParent(null);
			this.powerManager = (PowerManager) entity;
		} else if (entity instanceof JobPlacementPolicy) {
			if (this.jobPlacementPolicy == entity)
				return;
			if (this.jobPlacementPolicy != null)
				this.jobPlacementPolicy.setParent(null);
			this.jobPlacementPolicy = (JobPlacementPolicy) entity;
		} else if (entity instanceof VmPlacementPolicy) {
			if (this.vmPlacementPolicy == entity)
				return;
			if (this.vmPlacementPolicy != null)
				this.vmPlacementPolicy.setParent(null);
			this.vmPlacementPolicy = (VmPlacementPolicy) entity;
		} else if (entity instanceof Staas) {
			if (this.staas == entity)
				return;
			if (this.staas != null)
				this.staas.setParent(null);
			this.staas = (Staas) entity;
		} else if (entity instanceof MigrationHandler) {
			if (this.migrationHandler == entity)
				return;
			if (this.migrationHandler != null)
				this.migrationHandler.setParent(null);
			this.migrationHandler = (MigrationHandler) entity;
		} else if (entity instanceof CheckpointingHandler) {
			if (this.vmCheckpointingHandler == entity)
				return;
			if (this.vmCheckpointingHandler != null)
				this.vmCheckpointingHandler.setParent(null);
			this.vmCheckpointingHandler = (VmCheckpointingHandler) entity;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof ThinClient) {
			return;
		} else if (entity instanceof Host) {
			final RackImpl l = (RackImpl) ((Host) entity).getRack();
			if (!l.remove(entity))
				return;
		} else if (entity instanceof Switch) {
			if (!this.switches.remove(entity))
				return;
		} else if (entity instanceof User) {
			if (!this.users.remove(entity))
				return;
		} else if (entity instanceof PowerManager) {
			if (this.powerManager != entity)
				return;
			this.powerManager = null;
		} else if (entity instanceof JobPlacementPolicy) {
			if (this.jobPlacementPolicy != entity)
				return;
			this.jobPlacementPolicy = null;
		} else if (entity instanceof VmPlacementPolicy) {
			if (this.vmPlacementPolicy != entity)
				return;
			this.vmPlacementPolicy = null;
		} else if (entity instanceof Staas) {
			if (this.staas != entity)
				return;
			this.staas = null;
		} else if (entity instanceof MigrationHandler) {
			if (this.migrationHandler != entity)
				return;
			this.migrationHandler = null;
		} else if (entity instanceof CheckpointingHandler) {
			if (this.vmCheckpointingHandler != entity)
				return;
			this.vmCheckpointingHandler = null;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<Host> h = this.getHosts();

		List<Entity> l = newArrayList(6);
		if (this.powerManager != null)
			l.add(this.powerManager);
		if (this.jobPlacementPolicy != null)
			l.add(this.jobPlacementPolicy);
		if (this.vmPlacementPolicy != null)
			l.add(this.vmPlacementPolicy);
		if (this.staas != null)
			l.add(this.staas);
		if (this.migrationHandler != null)
			l.add(this.migrationHandler);
		if (this.vmCheckpointingHandler != null)
			l.add(this.vmCheckpointingHandler);

		List<List<? extends Entity>> r = newArrayList(5);
		r.add(s);
		r.add(l);
		r.add(this.users);
		r.add(this.switches);
		r.add(h);
		return new MultiListView<Entity>(r);
	}

	@Override
	public Rack getDefaultRack() {
		return this.defaultRack;
	}

	@Override
	public void setDefaultRack(Rack rack) {
		if (rack == null)
			throw new NullPointerException();
		if (rack.getCluster() == null || rack.getCluster().getDatacenter() == null ||
				rack.getCluster().getDatacenter().getCloud() != this.cloud)
			throw new IllegalArgumentException("You cannot define an external rack as the default rack");
		this.defaultRack = (RackImpl) rack;
	}

	protected static RackImpl _addRack(ClusterImpl cluster) {
		RackImpl rack = new RackImpl();
		rack.setCluster(cluster);
		cluster.add(rack);
		return rack;
	}

	protected static ClusterImpl _addCluster(DatacenterImpl datacenter) {
		ClusterImpl cluster = new ClusterImpl();
		cluster.setDatacenter((DatacenterImpl) datacenter);
		datacenter.add(cluster);
		return cluster;
	}

	protected static DatacenterImpl _addDatacenter(CloudImpl cloud) {
		DatacenterImpl datacenter = new DatacenterImpl();
		datacenter.setCloud(cloud);
		cloud.add(datacenter);
		return datacenter;
	}

	@Override
	public void addRack() {
//		this should always be false
//		if (this.defaultRack == null)
//			throw new NullPointerException("There is no defined cluster where to add the rack, please use addCluster instead.");
		this.defaultRack = _addRack((ClusterImpl) this.defaultRack.getCluster());
	}

	@Override
	public void addCluster() {
//		this should always be false
//		if (this.defaultRack == null)
//			throw new NullPointerException("There is no defined datacenter where to add the cluster, please use addDatacenter instead.");
		this.defaultRack = _addRack(_addCluster((DatacenterImpl) this.defaultRack.getCluster().getDatacenter()));
	}

	@Override
	public void addDatacenter() {
		this.defaultRack = _addRack(_addCluster(_addDatacenter(this.cloud)));
	}

	@Override
	public Cloud getCloud() {
		return this.cloud;
	}

	@Override
	public final List<Host> getHosts() {
		return this.cloud.getHosts();
	}

	@Override
	public List<Switch> getSwitches() {
		return Collections.unmodifiableList(this.switches);
	}

	@Override
	public List<User> getUsers() {
		return Collections.unmodifiableList(this.users);
	}

	@Override
	public PowerManager getPowerManager() {
		return this.powerManager;
	}

	@Override
	public JobPlacementPolicy getJobPlacementPolicy() {
		return this.jobPlacementPolicy;
	}

	@Override
	public VmPlacementPolicy getVmPlacementPolicy() {
		return this.vmPlacementPolicy;
	}

	@Override
	public Staas getStaas() {
		return this.staas;
	}

	@Override
	public MigrationHandler getMigrationHandler() {
		return this.migrationHandler;
	}

	@Override
	public VmCheckpointingHandler getVmCheckpointingHandler() {
		return this.vmCheckpointingHandler;
	}
}
