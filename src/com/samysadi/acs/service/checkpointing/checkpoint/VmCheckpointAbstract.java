package com.samysadi.acs.service.checkpointing.checkpoint;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 *
 * @since 1.2
 */
public abstract class VmCheckpointAbstract extends CheckpointAbstract<VirtualMachine, Host> implements VmCheckpoint {
	private int epoch;
	private VirtualMachine vmSnapshot;
	private VirtualRam ramSnapshot;
	private VirtualStorage storageSnapshot;

	public VmCheckpointAbstract() {
		super();

		cleanCheckpoint();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof VirtualMachine))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");

		super.setParent(parent);
	}

	@Override
	public int getCheckpointEpoch() {
		return this.epoch;
	}

	protected void setCheckpointEpoch(int epoch) {
		this.epoch = epoch;
	}

	protected class CheckpointData {
		public int epoch;
		public VirtualMachine vmSnapshot;
		public VirtualRam ramSnapshot;
		public VirtualStorage storageSnapshot;
	}

	@Override
	protected CheckpointData takeCheckpoint() {
		CheckpointData d = new CheckpointData();

		d.vmSnapshot = getParent().clone();
		d.vmSnapshot.setUser(null);
		d.ramSnapshot = getParent().getVirtualRam() == null ? null : getParent().getVirtualRam().clone();
		d.storageSnapshot = getParent().getVirtualStorage() == null ? null : getParent().getVirtualStorage().clone();
		d.storageSnapshot.setUser(null);

		d.epoch = (getParent().getEpoch());
		getParent().setEpoch(d.epoch + 1);

		return d;
	}

	@Override
	protected void updateCheckpoint(Object _data) {
		if (!(_data instanceof CheckpointData))
			throw new IllegalStateException();

		CheckpointData data = (CheckpointData) _data;

		this.vmSnapshot = data.vmSnapshot;
		this.ramSnapshot = data.ramSnapshot;
		this.storageSnapshot = data.storageSnapshot;

		this.setCheckpointEpoch(data.epoch);
	}

	@Override
	protected boolean hasCheckpoint() {
		return this.vmSnapshot != null;
	}

	@Override
	protected void cleanCheckpoint() {
		this.epoch = 0;
		this.vmSnapshot = null;
		this.ramSnapshot = null;
		this.storageSnapshot = null;
	}

	@Override
	protected void unplaceRunnableEntity(
			Host parent,
			VirtualMachine toReplace,
			final CheckpointAbstract._CAMethodReturnSimple success,
			CheckpointAbstract._CAMethodReturnSimple error) {
		final Host recoveryHost = getRecoveryHost(parent);
		if (toReplace.getVirtualRam() != null)
			toReplace.getVirtualRam().setParent(null);
		if (toReplace.getVirtualStorage() != null && toReplace.getVirtualStorage().getParentHost() == recoveryHost)
			toReplace.getVirtualStorage().unplace();
		recoveryHost.getCloudProvider().getPowerManager().lockHost(recoveryHost);
		toReplace.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();

				recoveryHost.getCloudProvider().getPowerManager().unlockHost(recoveryHost);

				success.run();
			}
		});
		toReplace.setUser(null);
		toReplace.unplace();
	}

	@Override
	protected void recoverEntity(
			Host parent,
			final CheckpointAbstract._CAMethodReturn success,
			CheckpointAbstract._CAMethodReturnSimple error) {
		Host recoveryHost = getRecoveryHost(parent);
		VirtualMachine vm = this.vmSnapshot.clone();
		vm.setUser(getParent().getUser());
		if (this.ramSnapshot != null)
			vm.setVirtualRam(this.ramSnapshot.clone());
		if (this.storageSnapshot != null) {
			vm.setVirtualStorage(this.storageSnapshot.clone());
			vm.getVirtualStorage().setUser(getParent().getVirtualStorage() == null ? null : getParent().getVirtualStorage().getUser());
		}

		if (!recoveryHost.getCloudProvider().getVmPlacementPolicy().canPlaceVm(vm, recoveryHost)) {
			error.run();
			return;
		}

		vm.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();

				VirtualMachine vm = (VirtualMachine) notifier;
				success.run(vm);
			}
		});

		recoveryHost.getCloudProvider().getVmPlacementPolicy().placeVm(vm, recoveryHost);
	}

	@Override
	protected VmCheckpointAbstract copyCheckpoint() {

		//instantiate
		VmCheckpointAbstract checkpoint;

		//create an instance copy
		try {
			checkpoint = this.getClass().getConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}

		checkpoint.setParent(getParent());

		checkpoint.epoch = this.epoch;
		checkpoint.vmSnapshot = this.vmSnapshot.clone();
		checkpoint.ramSnapshot = this.ramSnapshot.clone();
		checkpoint.storageSnapshot = this.storageSnapshot.clone();

		return checkpoint;
	}

	@Override
	protected User getCheckpointUser() {
		return getParent().getUser();
	}

	@Override
	protected Host getUpdateHost() {
		return getParent().getParent();
	}

	@Override
	protected Host getRecoveryHost(Entity parent) {
		if (!(parent instanceof Host))
			throw new IllegalStateException("The parent entity for recovery should be a Host entity");
		return (Host) parent;
	}

}
