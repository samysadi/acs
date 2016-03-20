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

package com.samysadi.acs.service.staas.sfconsistency;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.collections.Bitmap;
import com.samysadi.acs.utility.collections.Bitmap.SubBitmap;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.OperationSynchronizer;
import com.samysadi.acs.virtualization.job.operation.OperationSynchronizer.RunnableStateChanged;

/**
 *
 * @since 1.0
 */
//FIXME set owner for files
public class SfConsistencyManagerDefault extends EntityImpl implements SfConsistencyManager {

	public SfConsistencyManagerDefault() {
		super();
	}

	@Override
	public SfConsistencyManagerDefault clone() {
		final SfConsistencyManagerDefault clone = (SfConsistencyManagerDefault) super.clone();
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

	private VirtualMachine newTemporaryVm(Host host, User user) {
		final VirtualMachine remoteVm = Factory.getFactory(this).newTemporaryVirtualMachine(null);
		remoteVm.setParent(host);
		remoteVm.setUser(user);
		return remoteVm;
	}

	private static void removeTemporaryVm(VirtualMachine vm) {
		vm.doTerminate();
		vm.setUser(null);
		vm.unplace();
	}

	private static final Object PROP_CONSIST_KEY = new Object();
	private static final Object PROP_UPD_KEY = new Object();
	private static final Object PROP_NEXTUPD_KEY = new Object();
	private static final Object SF_MAP = new Object();

	protected void updateReplicas(StorageFile primary) {
		Bitmap b = primary.getMemoryMap(SF_MAP);
		Iterator<SubBitmap> it = b.getMarkedSubBitmapsIterator();
		while (it.hasNext()) {
			SubBitmap sb = it.next();
			for (StorageFile sf: getParent().getReplicationManager().getReplicas(primary))
				updateReplica(sf, primary, sb.getStartIndex(), sb.getLength());
		}
		b.unmark();
	}

	protected void updateReplica(StorageFile replica, StorageFile primary,
			long pos, long size) {
		if (replica == primary)
			return; //nothing to do

		if (replica.getProperty(PROP_UPD_KEY) != null) {
			//already updating, so schedule this update for next time
			@SuppressWarnings("unchecked")
			LinkedList<long[]> l = (LinkedList<long[]>) replica.getProperty(PROP_NEXTUPD_KEY);
			if (l == null) {
				l = new LinkedList<long[]>();
				replica.setProperty(PROP_NEXTUPD_KEY, l);
			}
			l.add(new long[] {pos, size});
			return;
		}

		//make sure size is the same
		{
			long deltaSize = primary.getSize() - replica.getSize();
			if (deltaSize < 0)
				replica.setSize(primary.getSize());
			else if (deltaSize > 0) {
				if (replica.getParent().getFreeCapacity() < deltaSize) {
					//not enough capacity, let's delete this and let replication manager create a new replica if needed
					replica.unplace();
					return;
				}
				replica.setSize(primary.getSize());
			}
		}

		//read file from primary and write in replica

		VirtualMachine tempVm = newTemporaryVm(replica.getParent().getParentHost(), primary.getUser());

		Job j = Factory.getFactory(this).newJob(null, null);
		j.setParent(tempVm);

		if (!tempVm.canStart()) {
			getLogger().log(replica, "Consistency update failed, because we cannot start job on parent host.");
			removeTemporaryVm(tempVm);
			return;
		}
		tempVm.doStart();

		if (!j.isRunning()) {
			if (!j.canStart()) {
				getLogger().log(replica, "Consistency update failed, because we cannot start job on parent host.");
				removeTemporaryVm(tempVm);
				return;
			}
			j.doStart();
		}

		NotificationListener n_read = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> o = ((Operation<?>) notifier);
				if (o.isTerminated()) {
					this.discard();
					o.unplace();
				}
			}
		};

		StorageOperation read = j.readFile(primary, pos, size, n_read);
		if (read == null) {
			getLogger().log(replica, "Consistency update failed, because we cannot read primary file.");
			j.doCancel();
			j.unplace();
			return;
		}

		NotificationListener n_write = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				Operation<?> o = ((Operation<?>) notifier);
				if (o.isTerminated()) {
					this.discard();
					o.unplace();
				}
			}
		};

		StorageOperation write = j.writeFile(replica, pos, size, n_write);
		if (write == null) {
			getLogger().log(replica, "Consistency update failed, because we cannot write replica file.");
			j.doCancel();
			removeTemporaryVm(tempVm);
			return;
		}

		final OperationSynchronizer sync = OperationSynchronizer.synchronizeOperations(read, write, new RunnableStateChanged() {
			@Override
			public void run(OperationSynchronizer sync) {
				if (!sync.getOperation1().isTerminated())
					return;

				final StorageFile primary = ((StorageOperation)sync.getOperation1()).getStorageFile();
				final StorageFile replica = ((StorageOperation)sync.getOperation2()).getStorageFile();

				if (sync.getOperation1().getRunnableState() != RunnableState.COMPLETED) {
					getLogger().log(replica, "Consistency update failed, because we read/write operations failed.");
					removeTemporaryVm(sync.getOperation1().getParent().getParent());
					sync.discard();
					return;
				}

				removeTemporaryVm(sync.getOperation1().getParent().getParent());
				sync.discard();

				//ok
				replica.unsetProperty(PROP_UPD_KEY);

				//proceed next updates

				@SuppressWarnings("unchecked")
				LinkedList<long[]> l = (LinkedList<long[]>) replica.getProperty(PROP_NEXTUPD_KEY);
				if (l != null) {
					long[] next = l.pop();
					if (l.isEmpty())
						replica.unsetProperty(PROP_NEXTUPD_KEY);
					SfConsistencyManagerDefault.this.updateReplica(replica, primary, next[0], next[1]);
				}
			}
		});

		//
		replica.setProperty(PROP_UPD_KEY, sync);
	}

	@Override
	public void register(StorageFile storageFile) {
		final StorageFile primary = getParent().getReplicationManager().getReplicas(storageFile).get(0);

		if (primary.getProperty(PROP_CONSIST_KEY) != null)
			return; //already registered

		NotificationListener l = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				SfConsistencyManagerDefault.this.updateReplicas((StorageFile) notifier);
			}
		};

		primary.addListener(NotificationCodes.MZ_MODIFIED, l);
		primary.setProperty(PROP_CONSIST_KEY, l);

		//mark all primary files as modified, and update replicas
		primary.getMemoryMap(SF_MAP).mark(0, primary.getSize());
		updateReplicas(storageFile);
	}

	@Override
	public void unregister(StorageFile storageFile) {
		List<StorageFile> replicas = getParent().getReplicationManager().getReplicas(storageFile);

		StorageFile primary = replicas.get(0);

		NotificationListener l = (NotificationListener) primary.getProperty(PROP_CONSIST_KEY);
		if (l != null) {
			l.discard();
			primary.unsetProperty(PROP_CONSIST_KEY);
		}

		for (StorageFile replica: replicas) {
			OperationSynchronizer sync = (OperationSynchronizer) replica.getProperty(PROP_UPD_KEY);
			if (sync != null) {
				sync.getOperation1().getParent().doCancel();
				sync.getOperation1().getParent().unplace();

				sync.discard();

				replica.unsetProperty(PROP_UPD_KEY);
			}

			@SuppressWarnings("unchecked")
			LinkedList<long[]> nl = (LinkedList<long[]>) replica.getProperty(PROP_NEXTUPD_KEY);
			if (nl != null)
				replica.unsetProperty(PROP_NEXTUPD_KEY);
		}
	}

}
