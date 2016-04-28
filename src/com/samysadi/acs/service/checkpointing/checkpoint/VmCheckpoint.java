package com.samysadi.acs.service.checkpointing.checkpoint;

import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 *
 * @see	Checkpoint
 * @since 1.2
 */
public interface VmCheckpoint extends Checkpoint<VirtualMachine, Host> {
	/**
	 * Returns the epoch value corresponding to this checkpoint.
	 *
	 * <p>The epoch value is returned by {@link VirtualMachine#getEpoch()} when
	 * the checkpoint was taken.
	 *
	 * @return the epoch value related to the checkpoint
	 */
	public int getCheckpointEpoch();
}
