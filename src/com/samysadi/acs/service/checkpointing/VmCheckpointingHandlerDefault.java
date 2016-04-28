package com.samysadi.acs.service.checkpointing;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.service.checkpointing.checkpoint.VmCheckpoint;
import com.samysadi.acs.utility.factory.FactoryUtils;
import com.samysadi.acs.virtualization.VirtualMachine;

/**
 * This implementation relies on its configuration.<br/>
 * Following configuration values can be set:<ul>
 * <li><b>Interval</b> in {@link Simulator#SECOND}. Indicates the delay before automatically updating
 * the checkpoint. Default value is 10;
 * <li><b>UpdateErrorCountThreshold</b> indicates the number of checkpoint update tries before
 * abandoning the automatic update of the checkpoint and throwing the corresponding notification. Default value is 3;
 * <li><b>RecoveryErrorCountThreshold</b> indicates the number of automatic recovery tries before
 * abandoning to use the checkpoint for automatic recovery and throwing the corresponding notification. Default value is 3;
 * <li><b>BufferOutput</b> a boolean that indicates whether an output commit mechanism
 * should be used by automatically buffering and releasing output. Default value is false;
 * </ul>
 * @see VmCheckpointingHandlerAbstract
 * @since 1.2
 */
public class VmCheckpointingHandlerDefault extends VmCheckpointingHandlerAbstract {

	public VmCheckpointingHandlerDefault() {
		super();
	}

	@Override
	public VmCheckpointingHandlerDefault clone() {
		return (VmCheckpointingHandlerDefault) super.clone();
	}

	@Override
	protected long getDelayBeforeNextAutoUpdate(VirtualMachine vm) {
		VmCheckpoint c = selectCheckpointForRecovery(vm);

		if (c == null)
			return 0l;

		if (!c.isCheckpointBusy() && !c.canRecover(null,null))
			return 0l;

		long v = 10 * Simulator.SECOND;

		Double d = FactoryUtils.generateDouble("Interval", getConfig(), null);
		if (d != null)
			v = Math.round(d.doubleValue() * Simulator.SECOND);

		return Math.max(0, v - Simulator.getSimulator().getTime() + c.getCheckpointTime());
	}

	@Override
	protected int getUpdateErrorsThreshold(VirtualMachine vm) {
		return FactoryUtils.generateInt("UpdateErrorCountThreshold", getConfig(), 3);
	}

	@Override
	protected int getRecoverErrorsThreshold(VirtualMachine vm) {
		return FactoryUtils.generateInt("RecoveryErrorCountThreshold", getConfig(), 3);
	}


	@Override
	protected boolean isUseBufferOutput() {
		boolean v = false;
		if (getConfig() == null)
			return v;
		return getConfig().getBoolean("BufferOutput", v);
	}

	@Override
	protected boolean useSameHostForCheckpointAndRecoveredVm() {
		return true;
	}
}
