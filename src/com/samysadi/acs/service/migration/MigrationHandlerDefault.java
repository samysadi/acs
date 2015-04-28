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

package com.samysadi.acs.service.migration;

import java.util.ListIterator;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.misc.MemoryUnit;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.NotificationCodes.MigrationRequest;
import com.samysadi.acs.utility.NotificationCodes.MigrationResult;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.JobDefault;
import com.samysadi.acs.virtualization.job.operation.OperationSynchronizer;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class MigrationHandlerDefault extends EntityImpl implements MigrationHandler {

	private static final Object MIGRATION_MAP = new Object();

	public MigrationHandlerDefault() {
		super();
	}

	@Override
	public MigrationHandlerDefault clone() {
		final MigrationHandlerDefault clone = (MigrationHandlerDefault) super.clone();
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

	@Override
	public boolean supportsLiveMigration() {
		return false;
	}

	@Override
	public boolean supportsStorageMigration() {
		return true;
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
		vm.setParent(null);
	}

	@Override
	public void migrate(VirtualMachine vm, Host destinationHost) {
		if (vm.getFlag(VirtualMachine.FLAG_IS_MIGRATING))
			throw new IllegalArgumentException("The given vm is being migrated.");

		//create the vm that will handle the migration job, in local host
		final VirtualMachine migrationVm = newTemporaryVm(vm.getParent(), vm.getUser());

		//create the migration job
		final MigrationJob job = new MigrationJob(new MigrationRequest(vm, destinationHost));
		job.setParent(migrationVm);

		if (!migrationVm.canStart()) {
			job.fail();
			return;
		}
		migrationVm.doStart();

		if (!job.isRunning()) {
			if (!job.canStart()) {
				job.fail();
				return;
			}
	
			job.doStart();
		}

		job.runJob();
	}

	protected long getPageSize() {
		return Long.MAX_VALUE; //no paging
	}

	protected long getIterationsThreshold() {
		return 3;
	}

	protected long getSizeThreshold() {
		return 1 * Simulator.MEBIBYTE; //100MB
	}

	protected long getSizeAdvancementThreshold() {
		return 0;
	}

	protected class MigrationJob extends JobDefault {

		MigrationRequest migrationRequest;
		/**
		 * Remote job that will receive vm
		 */
		Job destinationJob = null;
		/**
		 * Whether the vm was running
		 */
		boolean vmWasRunning = false;
		/**
		 * Number of times we iterated through the memory
		 */
		long numberOfIterations;
		/**
		 * Remaining size to send. Used to see if there is progress
		 */
		long remainingSize;
		/**
		 * Count of zones left in Ram in current iteration
		 */
		int zonesLeftInRam;
		/**
		 * Count of zones left in Ram in current iteration
		 */
		int zonesLeftInStorage;
		/**
		 * True if this job will also migrate the storage
		 */
		boolean migrateStorage;

		public MigrationJob(MigrationRequest migrationRequest) {
			super();
			this.migrationRequest = migrationRequest;
		}

		@Override
		public void beforeSetParent(Entity newParent) {
			//discard and reset everything, as runJob() will do the initialization

			if (destinationJob != null) {
				removeTemporaryVm(destinationJob.getParent());
				destinationJob = null;
			}

			if (vmWasRunning && migrationRequest.getVm().canStart()) {
				migrationRequest.getVm().doStart();
				vmWasRunning = false;
			}

			//
			zonesLeftInRam = 0;
			zonesLeftInStorage = 0;
			if (migrationRequest.getVm().getVirtualRam() != null)
				for (RamZone z : migrationRequest.getVm().getVirtualRam().getRamZones())
					z.removeMemoryMap(MIGRATION_MAP);

			if (migrationRequest.getVm().getVirtualStorage() != null)
				for (StorageFile z : migrationRequest.getVm().getVirtualStorage().getStorageFiles())
					z.removeMemoryMap(MIGRATION_MAP);


			if (getParent() != null && newParent != getParent())
				removeTemporaryVm(getParent());

			super.beforeSetParent(newParent);
		}

		/**
		 * Fails, discards the job and throws appropriate notifications
		 */
		private void fail() {
			this.doFail();
			this.setParent(null);

			notify(NotificationCodes.MIGRATION_ERROR, migrationRequest);
			migrationRequest.getVm().notify(NotificationCodes.VM_CANNOT_BE_MIGRATED, migrationRequest);
		}

		private void updateRemainingZonesCounts() {
			if (migrationRequest.getVm().getVirtualRam() != null)
				zonesLeftInRam = migrationRequest.getVm().getVirtualRam().getMemoryZones().size();
			else
				zonesLeftInRam = 0;
			if (migrateStorage)
				zonesLeftInStorage = migrationRequest.getVm().getVirtualStorage().getMemoryZones().size();
			else
				zonesLeftInStorage = 0;
		}

		public void runJob() {
			vmWasRunning = false;
			numberOfIterations = 0;
			remainingSize = 0;
			migrateStorage = supportsStorageMigration() && migrationRequest.getVm().getVirtualStorage()!=null;

			updateRemainingZonesCounts();

			//mark all memory zones as dirty (ie: needs to be copied)
			if (migrationRequest.getVm().getVirtualRam() != null) {
				for (MemoryZone z : migrationRequest.getVm().getVirtualRam().getMemoryZones()) {
					z.getMemoryMap(MIGRATION_MAP).mark(0, z.getSize());
					remainingSize+= z.getSize();
				}
			}

			if (migrateStorage) {
				for (MemoryZone z : migrationRequest.getVm().getVirtualStorage().getMemoryZones()) {
					z.getMemoryMap(MIGRATION_MAP).mark(0, z.getSize());
					remainingSize+= z.getSize();
				}
			}


			//create the vm that will handle the migration job, in local host
			final VirtualMachine migrationDestinationVm = newTemporaryVm(migrationRequest.getDestinationHost(), migrationRequest.getVm().getUser());

			destinationJob = Factory.getFactory(this).newJob(null, null);
			destinationJob.setParent(migrationDestinationVm);

			if (!migrationDestinationVm.canStart()) {
				getLogger().log(migrationRequest.getVm(), "Migration failed because the migration job cannot be started on destination host.");
				MigrationJob.this.fail();
				return;
			}
			migrationDestinationVm.doStart();

			if (!destinationJob.isRunning()) {
				if (!destinationJob.canStart()) {
					getLogger().log(migrationRequest.getVm(), "Migration failed because the migration job cannot be started on destination host.");
					MigrationJob.this.fail();
					return;
				}
				destinationJob.doStart();
			}

			if (!supportsLiveMigration() && migrationRequest.getVm().isRunning()) {
				vmWasRunning = true;
				migrationRequest.getVm().doPause();
			}

			//1: start sending VM state, but leave the VM running
			MigrationJob.this.continueSync();
		}

		private void continueSync() {
			MemoryUnit<? extends MemoryZone> mu = null;

			int nextZoneId = -1;

			if (zonesLeftInRam > 0) {
				mu = migrationRequest.getVm().getVirtualRam();
				if (zonesLeftInRam > mu.getMemoryZones().size())
					zonesLeftInRam = mu.getMemoryZones().size();
				nextZoneId = zonesLeftInRam - 1;
			} else if (zonesLeftInStorage > 0) {
				mu = migrationRequest.getVm().getVirtualStorage();
				if (zonesLeftInStorage > mu.getMemoryZones().size())
					zonesLeftInStorage = mu.getMemoryZones().size();
				nextZoneId = zonesLeftInStorage - 1;
			}

			if (mu == null || nextZoneId < 0) {
				//reached the end of one iteration (fully iterated through ram and storage)
				//let's see how many new data need to be sent
				long s = 0;
				for (RamZone z : migrationRequest.getVm().getVirtualRam().getRamZones())
					s+= z.getMemoryMap(MIGRATION_MAP).getMarkedSize();

				if (migrateStorage) {
					for (StorageFile z : migrationRequest.getVm().getVirtualStorage().getStorageFiles())
						s+= z.getMemoryMap(MIGRATION_MAP).getMarkedSize();
				}

				numberOfIterations++;
				if (s == 0) {
					//no more data to send
					//3: unplace the VM from old host and place it in destination host, and launch it (if needed)
					if (migrationRequest.getVm().isRunning()) {
						//we need to pause it first
						vmWasRunning = true;
						migrationRequest.getVm().doPause();
					}

					if (migrateStorage && migrationRequest.getVm().getVirtualStorage() != null) {
						//unlink virtual storage from its parent, so that it is moved to the destination host during placement
						migrationRequest.getVm().getVirtualStorage().setParent(null);
					}

					migrationRequest.getVm().addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
						boolean placeNext = true;
						@Override
						protected void notificationPerformed(Notifier notifier,
								int notification_code, Object data) {
							if (placeNext) {
								//vm was unplaced, now place it on the destination host
								placeNext = false;
								if (!MigrationHandlerDefault.this.getParent().getVmPlacementPolicy().canPlaceVm(migrationRequest.getVm(), migrationRequest.getDestinationHost())) {
									this.discard();
									getLogger().log(migrationRequest.getVm(), "Migration failed because the VM cannot be placed on destination host.");
									MigrationJob.this.fail();
									return;
								}
								MigrationHandlerDefault.this.getParent().getVmPlacementPolicy().placeVm(migrationRequest.getVm(), migrationRequest.getDestinationHost());
								return;
							}

							//vm was placed on the destination host
							this.discard();

							if (vmWasRunning && migrationRequest.getVm().canStart())
								migrationRequest.getVm().doStart();

							MigrationJob.this.doCancel();
							MigrationJob.this.setParent(null);
							final MigrationResult migrationResult = new MigrationResult(migrationRequest);
							MigrationHandlerDefault.this.notify(NotificationCodes.MIGRATION_SUCCESS, migrationResult);
							migrationRequest.getVm().notify(NotificationCodes.VM_MIGRATED, migrationResult);
						}
					});

					migrationRequest.getVm().unplace();

					return;
				} else if (s <= getSizeThreshold() || numberOfIterations >= getIterationsThreshold()
						|| remainingSize - s <= getSizeAdvancementThreshold()) {
					//memory is dirtied too fast etc.. and we want to pause VM for last iteration
					//2: we pause the VM for last copy stage
					if (migrationRequest.getVm().isRunning()) {
						vmWasRunning = true;
						migrationRequest.getVm().doPause();
					}
				}

				updateRemainingZonesCounts();
				remainingSize = s;
				MigrationJob.this.continueSync();
				return;
			}

			//compute amount of data to send now
			final long maxToSend = getPageSize();
			long remaining = maxToSend;
			do {
				ListIterator<? extends MemoryZone> it = mu.getMemoryZones().listIterator(nextZoneId + 1);
				while (it.hasPrevious() && remaining > 0) {
					MemoryZone mz = it.previous();

					long dirtySize = mz.getMemoryMap(MIGRATION_MAP).getMarkedSize();
					long toUnmark = Math.min(remaining, dirtySize);

					mz.getMemoryMap(MIGRATION_MAP).unmark(toUnmark);

					remaining -= toUnmark;
				}

				int count = nextZoneId - it.previousIndex();
				nextZoneId = it.previousIndex();

				if (zonesLeftInRam > 0) {
					zonesLeftInRam -= count;
					if (remaining > 0) {
						mu = migrationRequest.getVm().getVirtualStorage();
						if (zonesLeftInStorage > mu.getMemoryZones().size())
							zonesLeftInStorage = mu.getMemoryZones().size();
						nextZoneId = zonesLeftInStorage - 1;
					}
				} else
					zonesLeftInStorage -= count;

				if (nextZoneId < 0)
					break;

				if (remaining <= 0)
					break;
			} while (true);

			remaining = maxToSend - remaining;
			if (remaining == 0) {
				MigrationJob.this.continueSync();
				return;
			}
			final NetworkOperation nextOperation = this.sendData(destinationJob, remaining, new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					NetworkOperation operation = (NetworkOperation) notifier;
					
					if (!operation.isTerminated())
						return;
	
					operation.setParent(null);
					this.discard();

					if (operation.getRunnableState() != RunnableState.COMPLETED) {
						MigrationJob.this.fail();
						return;
					}

					MigrationJob.this.continueSync();
				}
			});
			if (nextOperation == null) {
				getLogger().log(this, "Migration failed because we cannot sync data.");
				MigrationJob.this.fail();
				return;
			}

			//if we are reading data from a Storage then we need to create a read operation
			if (mu instanceof VirtualStorage) {
				VirtualMachine muVm = newTemporaryVm(mu.getParentHost(), migrationRequest.getVm().getUser());
				final Job muJob = Factory.getFactory(this).newJob(null, null);
				muJob.setParent(muVm);

				if (!muVm.canStart()) {
					getLogger().log(this, "Migration failed because we cannot read/write from remote storage.");
					removeTemporaryVm(muJob.getParent());
					nextOperation.doPause();
					nextOperation.setParent(null);
					MigrationJob.this.fail();
					return;
				}
				muVm.doStart();

				if (!muJob.isRunning()) {
					if (!muJob.canStart()) {
						getLogger().log(this, "Migration failed because we cannot read from remote storage.");
						removeTemporaryVm(muJob.getParent());
						nextOperation.doPause();
						nextOperation.setParent(null);
						MigrationJob.this.fail();
						return;
					}
					muJob.doStart();
				}

				StorageOperation read = muJob.readFile((VirtualStorage)mu, 0, remaining, null);
				StorageOperation write = null;
				for (Storage s: destinationJob.getParent().getParent().getStorages())
					if (s.getFreeCapacity() >= remaining) {
						StorageFile writeFile = Factory.getFactory(destinationJob).newStorageFile(null, s, remaining);
						write = destinationJob.writeFile(writeFile, 0, remaining, null);
					}

				if (read == null || write == null) {
					if (write != null) {
						getLogger().log(this, "Migration failed because we cannot read from source storage.");
						write.doCancel();
						write.setParent(null);
					} else
						getLogger().log(this, "Migration failed because we cannot write to destination storage.");

					removeTemporaryVm(muJob.getParent());
					nextOperation.doPause();
					nextOperation.setParent(null);
					MigrationJob.this.fail();
					return;
				}

				final OperationSynchronizer sync0 = OperationSynchronizer.synchronizeOperations(nextOperation, read);
				OperationSynchronizer.synchronizeOperations(read, write, new MyStaticRsc0(sync0));
			}
		}
	}

	private static final class MyStaticRsc0 extends OperationSynchronizer.RunnableStateChanged {
		private final OperationSynchronizer sync0;
		
		private MyStaticRsc0(OperationSynchronizer sync0) {
			this.sync0 = sync0;
		}
		
		@Override
		public void run(OperationSynchronizer sync) {
			if (!sync.getOperation2().isTerminated())
				return;
		
			//discard the file, it was here only to add storage overhead
			((StorageOperation) sync.getOperation2()).getStorageFile().setParent(null);
		
			//discard the write operation
			((StorageOperation) sync.getOperation2()).setParent(null);
		
			//discard the read job (and thus read operation)
			Job muJob = sync.getOperation1().getParent();
			removeTemporaryVm(muJob.getParent());
		
			sync.discard();
			sync0.discard();
		}
	}
}
