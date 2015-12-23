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

package com.samysadi.acs.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.notifications.InstantNotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.MultiListView;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * 
 * @since 1.0
 */
public class UserDefault extends EntityImpl implements User {
	private static final InstantNotificationListener instantListener = new InstantNotificationListener() {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			UserDefault user = (UserDefault) notifier;
			if (notification_code == NotificationCodes.USER_VM_ATTACHED) {
				VirtualMachine vm = (VirtualMachine) data;
				if (vm instanceof ThinClientVirtualMachine)
					return;
				if (vm.getUser() != user)
					return;
				if (user.virtualMachines == null)
					user.virtualMachines = new ArrayList<VirtualMachine>();//new WeakLinkedList<VirtualMachine>();
				user.virtualMachines.add(vm);
			} else if (notification_code == NotificationCodes.USER_VM_DETACHED) {
				VirtualMachine vm = (VirtualMachine) data;
				if (user.virtualMachines != null) {
					user.virtualMachines.remove(vm);
				}
			} else if (notification_code == NotificationCodes.USER_STORAGEFILE_ATTACHED) {
				StorageFile sf = (StorageFile) data;
				if (sf.getUser() != user)
					return;
				if (user.storageFiles == null)
					user.storageFiles = new ArrayList<StorageFile>();//new WeakLinkedList<StorageFile>();
				user.storageFiles.add(sf);
			} else if (notification_code == NotificationCodes.USER_STORAGEFILE_DETACHED) {
				StorageFile sf = (StorageFile) data;
				if (user.storageFiles != null) {
					user.storageFiles.remove(sf);
				}
			}
		}
	};

	private List<ThinClient> thinClients;
	private List<VirtualMachine> virtualMachines;
	private List<StorageFile> storageFiles;

	public UserDefault() {
		super();
	}

	@Override
	public UserDefault clone() {
		final UserDefault clone = (UserDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.thinClients = null;
		this.virtualMachines = null;
		this.storageFiles = null;

		this.addListener(NotificationCodes.USER_VM_ATTACHED, instantListener);
		this.addListener(NotificationCodes.USER_VM_DETACHED, instantListener);

		this.addListener(NotificationCodes.USER_STORAGEFILE_ATTACHED, instantListener);
		this.addListener(NotificationCodes.USER_STORAGEFILE_DETACHED, instantListener);
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
		if (entity instanceof ThinClient) {
			if (this.thinClients == null)
				this.thinClients = new ArrayList<ThinClient>();
			if (!this.thinClients.add((ThinClient) entity))
				return;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof ThinClient) {
			if (this.thinClients == null)
				return;
			if (!this.thinClients.remove(entity))
				return;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(NotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		//List<Entity> l = new ArrayList<Entity>(4);

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		//r.add(l);
		if (this.thinClients != null)
			r.add(this.thinClients);
		return new MultiListView<Entity>(r);
	}

	@Override
	public List<ThinClient> getThinClients() {
		if (this.thinClients == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.thinClients);
	}

	@Override
	public List<VirtualMachine> getVirtualMachines() {
		if (this.virtualMachines == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.virtualMachines);
	}

	@Override
	public List<StorageFile> getStorageFiles() {
		if (this.storageFiles == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.storageFiles);
	}
}
