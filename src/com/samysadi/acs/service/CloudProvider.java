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

import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.service.checkpointing.VmCheckpointingHandler;
import com.samysadi.acs.service.jobplacement.JobPlacementPolicy;
import com.samysadi.acs.service.migration.MigrationHandler;
import com.samysadi.acs.service.power.PowerManager;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicy;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.collections.infrastructure.Cloud;
import com.samysadi.acs.utility.collections.infrastructure.Rack;


/**
 * A cloud provider is the entity that provides infrastructure and
 * services to the users.
 *
 * @since 1.0
 */
public interface CloudProvider extends Entity {

	/**
	 * Adds a new rack in the same cluster and data-center as the default rack.<br/>
	 * The added rack becomes the default rack.
	 */
	public void addRack();

	/**
	 * Adds a new cluster <b>with</b> an empty rack.<br/>
	 * The new cluster is placed in in the same data-center as
	 * the default rack.
	 *
	 * <p>After calling this method, the added rack in the new cluster becomes the default rack.
	 */
	public void addCluster();

	/**
	 * Adds a new data-center <b>with</b> one cluster containing one empty rack.<br/>
	 * After calling this method, the added rack in the new data-center becomes the default rack.
	 */
	public void addDatacenter();

	/**
	 * Returns the default rack where new hosts are added by default.
	 *
	 * <p><b>Important</b> please do not modify the returned instance. If you do so you are probably running into troubles.<br/>
	 * Use instead {@link CloudProvider#addDatacenter()}, {@link CloudProvider#addCluster()} and {@link CloudProvider#addRack()}.
	 *
	 * @return the default rack where new hosts are added by default
	 */
	public Rack getDefaultRack();

	/**
	 * Updates the default rack.
	 *
	 * <p>The default rack is where new hosts are put when this provider is defined as their parent.
	 *
	 * @param rack
	 * @throws IllegalArgumentException if the given rack is not contained within this cloud provider's infrastructure. Make sure the rack was returned by {@link CloudProvider#getDefaultRack()}.
	 */
	public void setDefaultRack(Rack rack);

	/**
	 * Returns the {@link Cloud} instance, which contains all hosts in this cloud provider.
	 *
	 * @return the {@link Cloud} instance
	 */
	public Cloud getCloud();

	/**
	 * Returns an unmodifiable list that contains all hosts of this cloud provider.
	 *
	 * @return an unmodifiable list that contains all hosts of this cloud provider
	 */
	public List<Host> getHosts();

	/**
	 * Returns an unmodifiable list that contains all switches of this cloud provider.
	 *
	 * @return an unmodifiable list that contains all switches of this cloud provider
	 */
	public List<Switch> getSwitches();

	/**
	 * Returns an unmodifiable list that contains all users of this cloud provider.
	 *
	 * @return an unmodifiable list that contains all users of this cloud provider
	 */
	public List<User> getUsers();

	public PowerManager getPowerManager();

	/**
	 * Returns the job placement policy of this cloud provider.
	 *
	 * @return the job placement policy of this cloud provider
	 */
	public JobPlacementPolicy getJobPlacementPolicy();

	/**
	 * Returns the vm placement policy of this cloud provider.
	 *
	 * @return the vm placement policy of this cloud provider
	 */
	public VmPlacementPolicy getVmPlacementPolicy();

	/**
	 * Returns the storage as a service instance of this cloud provider.
	 *
	 * @return the storage as a service instance of this cloud provider
	 */
	public Staas getStaas();

	/**
	 * Returns the migration handler of this cloud provider.
	 *
	 * @return the migration handler of this cloud provider
	 */
	public MigrationHandler getMigrationHandler();

	/**
	 * Returns the checkpointing handler of this cloud provider.
	 *
	 * @return the checkpointing handler of this cloud provider
	 */
	public VmCheckpointingHandler getVmCheckpointingHandler();
}
