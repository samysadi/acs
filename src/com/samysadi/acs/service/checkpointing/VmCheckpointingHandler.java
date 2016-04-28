package com.samysadi.acs.service.checkpointing;

import com.samysadi.acs.service.checkpointing.checkpoint.VmCheckpoint;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * This interface defines a {@link CheckpointingHandler} which
 * handles the checkpointing process for {@link VirtualMachine}s.
 *
 * @see CheckpointingHandler
 * @since 1.2
 */
public interface VmCheckpointingHandler extends CheckpointingHandler<VirtualMachine, VmCheckpoint> {

	@Override
	public VmCheckpointingHandler clone();
}
