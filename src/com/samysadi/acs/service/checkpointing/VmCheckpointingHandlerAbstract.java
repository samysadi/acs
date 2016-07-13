package com.samysadi.acs.service.checkpointing;

import java.util.Arrays;
import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.misc.MemoryUnit;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.operation.NetworkOperationDelayer;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.checkpoint.Checkpoint;
import com.samysadi.acs.service.checkpointing.checkpoint.VmCheckpoint;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;

/**
 * This implementation defines a {@link CheckpointingHandlerAbstract} which
 * uses the parent {@link CloudProvider} policies to select a {@link Storage} where
 * to place checkpoints, and to select a host to use for recovered {@link VirtualMachine}s.
 *
 * <p>After that the {@link Storage} is selected a {@link VirtualStorage} is created on top
 * of the {@link Storage} which in turn holds the checkpoint's {@link StorageFile}.
 * The {@link VirtualStorage} is created in order to reserve space for the
 * checkpoint's {@link StorageFile}, and avoid any update error due to lack of space.
 * The reserved space is equal to the {@link VirtualMachine}'s {@link VirtualRam} size plus
 * the {@link VirtualMachine}'s {@link VirtualStorage} size.
 *
 * <p>This implementation will reserve in advance resources for the {@link VirtualMachine}
 * on a secondary host.
 * This way after a failure, there is immediately a ready host where to place the
 * recovered {@link VirtualMachine}.
 *
 * <p>The host where a recovered {@link VirtualMachine} is to be placed, might
 * be different from the host where the checkpoint is saved.
 * However, you can override the appropriate method (i.e. useSameHostForCheckpointAndRecoveredVm) so that this implementation will try to place the
 * checkpoint file in the same host as the host where a potentially recovered VM is
 * to be placed.
 *
 * @see VmCheckpointingHandler
 * @since 1.2
 */
public abstract class VmCheckpointingHandlerAbstract extends CheckpointingHandlerAbstract<VirtualMachine, VmCheckpoint> implements VmCheckpointingHandler {

	/**
	 * Used on:<ul>
	 * <li>MemoryZones to check if it was created by this CheckpointingHandler
	 * <li>Checkpoints to retrieve VmToReplace
	 * </ul>
	 */
	private Object PROP_KEY;

	private CheckpointingNetworkOperationDelayer networkOperationDelayer;

	/**
	 * An operation delayer which is used to automatically buffer network
	 * output whenever necessary, to simulate an output commit mechanism during the
	 * checkpointing process.
	 *
	 * @see VmCheckpointingHandlerAbstract#isUseBufferOutput()
	 */
	protected class CheckpointingNetworkOperationDelayer implements NetworkOperationDelayer {
		private Object PROP_EPOCH_KEY = new Object();

		/**
		 * Releases all output generated before epoch (operation.epoch <= epoch).
		 *
		 * @param vm
		 * @param epoch
		 */
		public void releaseEpoch(VirtualMachine vm, int epoch) {
			//update last committed state epoch
			vm.setProperty(PROP_EPOCH_KEY, Integer.valueOf(epoch));

			if (vm.isRunning()) {
				vm.doPause();
				vm.doStart();
			}
		}

		@Override
		public long getNextLength(NetworkOperation operation) {
			if (!isUseBufferOutput())
				return -1l;
			Job job = operation.getParent();
			if (job == null)
				return -1l;
			VirtualMachine vm = job.getParent();
			if (vm == null)
				return -1l;

			//get last committed state epoch
			Integer vmEpoch = (Integer) vm.getProperty(PROP_EPOCH_KEY);
			if (vmEpoch == null) {
				//no committed state yet, so buffer output
				return operation.getLength();
			}

			//operation epoch
			Integer opEpoch = (Integer) operation.getProperty(PROP_EPOCH_KEY);
			if (opEpoch == null) {
				if (operation.getCompletedLength() == operation.getLength()) {
					opEpoch = Integer.valueOf(vm.getEpoch());
					operation.setProperty(PROP_EPOCH_KEY, opEpoch);
				} else {
					return operation.getLength();
				}
			}

			//delay if last committed state was before the operation creation
			if (vmEpoch < opEpoch)
				return operation.getLength();
			else
				return -1l;
		}

		public void register(VirtualMachine vm) {
			if (!isUseBufferOutput())
				return;
			vm.setNetworkOperationDelayer(this);
		}

		public void unregister(VirtualMachine vm) {
			if (vm.getNetworkOperationDelayer() != this)
				return;
			vm.setNetworkOperationDelayer(null);
		}
	}

	public VmCheckpointingHandlerAbstract() {
		super();
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.PROP_KEY = new Object();
		this.networkOperationDelayer = new CheckpointingNetworkOperationDelayer();
	}

	@Override
	public VmCheckpointingHandlerAbstract clone() {
		return (VmCheckpointingHandlerAbstract) super.clone();
	}

	@Override
	public void register(VirtualMachine vm, Config checkpointConfig) {
		if (isUseBufferOutput() && vm.getNetworkOperationDelayer() != null)
			throw new IllegalStateException("Cannot register this VM because it is registered using another handler which uses an output commit mechanism");

		super.register(vm, checkpointConfig);

		//enable network buffering
		this.networkOperationDelayer.register(vm);
	}

	@Override
	public void unregister(VirtualMachine vm) {
		super.unregister(vm);

		//disable network buffering and release output
		this.networkOperationDelayer.unregister(vm);
	}

	@Override
	protected void cleanupEntityForRecovery(VirtualMachine vm) {
		super.cleanupEntityForRecovery(vm);

		//disable network buffering and release output
		this.networkOperationDelayer.unregister(vm);
	}

	@Override
	protected void generateCheckpoint(final VirtualMachine vm, final Config checkpointConfig,
			final _CHMethodReturn<VmCheckpoint> success, final _CHMethodReturnSimple error) {
		if (!vm.hasParentRec()) {
			error.run();
			return;
		}

		Host vmToReplaceDestinationHost = VmCheckpointingHandlerAbstract.this.getParent().getVmPlacementPolicy().selectHost(vm, null, Arrays.asList(vm.getParent()));
		if (vmToReplaceDestinationHost == null) {
			error.run();
			return;
		}

		VirtualMachine vmToReplace = vm.clone();
		if (!VmCheckpointingHandlerAbstract.this.getParent().getVmPlacementPolicy().canPlaceVm(vmToReplace, vmToReplaceDestinationHost)) {
			error.run();
			return;
		}

		vmToReplace.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();

				final VirtualMachine vmToReplace = (VirtualMachine) notifier;
				vmToReplace.removeListener(NotificationCodes.ENTITY_PARENT_CHANGED, this);

				long size = vm.getVirtualRam().getCapacity() + vm.getVirtualStorage().getSize();
				VirtualStorage vs = Factory.getFactory(VmCheckpointingHandlerAbstract.this).newVirtualStorage(null, null, size);
				vs.setCapacity(size);
				vs.setIsCapacityReserved(true);
				Storage s = null;

				if (useSameHostForCheckpointAndRecoveredVm()) {
					//try to use same host
					s = VmCheckpointingHandlerAbstract.this.getParent().getStaas().getPlacementPolicy().selectStorage(vs, Arrays.asList(vmToReplace.getParent()), null);
				}

				if (s == null) {
					s = VmCheckpointingHandlerAbstract.this.getParent().getStaas().getPlacementPolicy().selectStorage(vs, null, Arrays.asList(vm.getParent()));
					if (s == null ||
							!VmCheckpointingHandlerAbstract.this.getParent().getStaas().getPlacementPolicy().canPlaceStorageFile(vs, s)) {
						vmToReplace.setUser(null);
						vmToReplace.unplace();
						error.run();
						return;
					}
				}

				vs.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new NotificationListener() {
					@Override
					protected void notificationPerformed(Notifier notifier,
							int notification_code, Object data) {
						this.discard();

						VirtualStorage vs = (VirtualStorage) notifier;
						vs.removeListener(NotificationCodes.ENTITY_PARENT_CHANGED, this);

						StorageFile sf = Factory.getFactory(VmCheckpointingHandlerAbstract.this).newStorageFile(null, vs, 0l);

						Config cfg = checkpointConfig == null ? VmCheckpointingHandlerAbstract.this.getConfig() :
							checkpointConfig;
						VmCheckpoint c = Factory.getFactory(cfg).newVmCheckpoint(null, vm);
						c.setConfig(cfg);

						sf.setProperty(PROP_KEY, true);
						c.setMemoryZone(sf);

						c.setProperty(PROP_KEY, vmToReplace);

						c.setCheckpointingHandler(VmCheckpointingHandlerAbstract.this);

						success.run(c);
					}
				});

				VmCheckpointingHandlerAbstract.this.getParent().getStaas().getPlacementPolicy().placeStorageFile(vs, s);
			}
		});

		VmCheckpointingHandlerAbstract.this.getParent().getVmPlacementPolicy().placeVm(vmToReplace, vmToReplaceDestinationHost);
	}

	private void _freeVmToReplace(Checkpoint<?,?> c) {
		VirtualMachine vmToReplace = (VirtualMachine) c.getProperty(PROP_KEY);
		c.unsetProperty(PROP_KEY);
		if (vmToReplace == null || !vmToReplace.hasParentRec())
			return;
		vmToReplace.setUser(null);
		vmToReplace.unplace();
	}

	@Override
	protected void freeCheckpoint(VmCheckpoint c) {
		_freeVmToReplace(c);

		MemoryZone z = c.getMemoryZone();

		MemoryUnit<?> vs = z == null ? null : z.getParent();

		c.delete();
		c.setParent(null);

		if (z != null && z.getProperty(PROP_KEY) != null && vs != null) {
			if (vs instanceof VirtualStorage) {
				((VirtualStorage) vs).unplace();
			}
			if (z instanceof StorageFile)
				((StorageFile) z).unplace();
			else
				z.setParent(null);
		}
	}

//	private void deleteUselessCheckpoints(List<VmCheckpoint> l, VmCheckpoint selected) {
//		for (VmCheckpoint c : l) {
//			if (c == selected)
//				continue;
//			if (!isAutomaticCheckpoint(c))
//				continue;
//			deleteCheckpoint(c);
//		}
//	}

	@Override
	protected VmCheckpoint selectCheckpointForUpdate(VirtualMachine entity) {
		List<VmCheckpoint> l = getCheckpoints(entity);

		VmCheckpoint c = null;
		for (VmCheckpoint c2 : l) {
			if (!isAutomaticCheckpoint(c2))
				continue;
			if (isCheckpointMarkedToBeDeleted(c2))
				continue;
			if (c == null || c2.getCheckpointTime() > c.getCheckpointTime()) {
				if (!c2.canUpdate())
					continue;
				c = c2;
			}
		}

		//deleteUselessCheckpoints(l, c);

		return c;
	}

	@Override
	protected VmCheckpoint selectCheckpointForRecovery(VirtualMachine entity) {
		List<VmCheckpoint> l = getCheckpoints(entity);

		VmCheckpoint c = null;
		for (VmCheckpoint c2 : l) {
			if (!isAutomaticCheckpoint(c2))
				continue;
			if (isCheckpointMarkedToBeDeleted(c2))
				continue;
			if (c == null || c2.getCheckpointTime() > c.getCheckpointTime()) {
				if (!c2.canRecover(null,null))
					continue;
				c = c2;
			}
		}

		//deleteUselessCheckpoints(l, c);

		return c;
	}

	@Override
	protected boolean recoverUsingCheckpoint(VmCheckpoint c) {
		VirtualMachine vm = c.getParent();
		VirtualMachine toReplace = (VirtualMachine) c.getProperty(PROP_KEY);
		Host parent = null;
		if (toReplace != null)
			parent = toReplace.getParent();
		if (parent == null) {
			toReplace = null;
			parent = getParent().getVmPlacementPolicy().selectHost(vm);
			if (parent == null)
				return false;
		}

		if (!c.canRecover(parent, toReplace))
			return false;

		c.recover(parent, toReplace);

		return true;
	}

	@Override
	protected void afterAutoUpdate(VmCheckpoint c) {
		VirtualMachine vm = c.getParent();

		//checkpoint committed, handle output commit mechanism
		if (isUseBufferOutput())
			this.networkOperationDelayer.releaseEpoch(vm, c.getCheckpointEpoch());

		super.afterAutoUpdate(c);
	}

	@Override
	protected List<VmCheckpoint> getCheckpoints(VirtualMachine entity) {
		return entity.getCheckpoints();
	}

	protected abstract boolean isUseBufferOutput();

	/**
	 * If this method returns <tt>true</tt> then this implementation will
	 * try to use as recovery host, the same host as the host where
	 * the checkpoint file is placed.
	 *
	 * @return <tt>true</tt> if we should try to use the same host for the checkpoint file
	 * and the recovered {@link VirtualMachine}
	 */
	protected abstract boolean useSameHostForCheckpointAndRecoveredVm();
}
