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

package com.samysadi.acs.virtualization.job.operation;

import java.util.List;
import java.util.logging.Level;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.FailureProneEntity.FailureState;
import com.samysadi.acs.core.entity.PoweredEntity;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.Factory;

/**
 * This is the default implementation of the {@link LongOperation} interface.
 *
 * @since 1.0
 */
public abstract class LongOperationImpl<Resource extends LongResource> extends OperationImpl<Resource> implements LongOperation<Resource> {
	private long resourceMax;
	private long resourceMin;
	private long lastActivated;
	private long length;

	private long oldTotalRunningTime;
	private long completedLength;

	private long maxActivationLength;
	private Event endOfOperationEvent;

	/**
	 * The operation is delayed
	 */
	private static final int DELAYED				= 0x00000001;

	/**
	 * The operation needs to be delayed at a given moment before completion
	 */
	private static final int DELAY_BEFORE_COMPLETION	= 0x00000002;

	private int delayedState;

	private long synchronizedResource;
	private long synchronizedTimeAdjust;
	private boolean synchronizedNeedsDelayingBeforeCompletion;
	private OperationSynchronizer operationSynchronizer;

	/**
	 * Empty constructor that creates a zero-length operation.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.
	 * You should use {@link LongOperationImpl#LongOperationImpl(long)} though.
	 */
	public LongOperationImpl() {
		this(0l);
	}

	/**
	 *
	 * @param operationLength a long value indicating the length of this operation,
	 * needs to be strictly positive
	 */
	public LongOperationImpl(long operationLength) {
		super();

		this.resourceMax = Long.MAX_VALUE;
		this.resourceMin = 1l;
		this.lastActivated = Long.MAX_VALUE;
		this.oldTotalRunningTime = 0l;
		this.completedLength = 0l;
		this.maxActivationLength = 0l;
		this.endOfOperationEvent = null;
		this.delayedState = 0;

		if (operationLength <= 0)
			throw new IllegalArgumentException("Operation length needs to be strictly positive");
		setLength(operationLength);
	}

	@Override
	public LongOperationImpl<Resource> clone() {
		final LongOperationImpl<Resource> clone = (LongOperationImpl<Resource>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.synchronizedResource = Long.MAX_VALUE;
		this.synchronizedTimeAdjust = 0l;
		this.synchronizedNeedsDelayingBeforeCompletion = false;
		this.operationSynchronizer = null;
	}

	@Override
	public long getLength() {
		return this.length;
	}

	private void setLength(long length) {
		this.length = length;
	}

	@Override
	public long getResourceMax() {
		return resourceMax;
	}

	@Override
	public void setResourceMax(long resourceMax) {
		if (this.resourceMax == resourceMax)
			return;
		if (this.isRunning())
			throw new IllegalStateException("The operation is activated.");
		this.resourceMax = resourceMax;
	}

	@Override
	public long getResourceMin() {
		return resourceMin;
	}

	@Override
	public void setResourceMin(long resourceMin) {
		if (this.resourceMin == resourceMin)
			return;
		if (this.isRunning())
			throw new IllegalStateException("The operation is activated.");
		this.resourceMin = resourceMin;
	}

	@Override
	public long getTotalRunningTime() {
		long r = this.oldTotalRunningTime;
		if (isRunning())
			r+= Simulator.getSimulator().getTime() - getLastActivated();
		return r;
	}

	@Override
	public long getCompletedLength() {
		return completedLength;
	}

	/**
	 * Returns the simulation time when this operation was activated if it is running, or {@code Long.MAX_VALUE} if it is not running.
	 *
	 * @return the simulation time when this operation was activated if it is running, or {@code Long.MAX_VALUE} if it is not running
	 */
	protected long getLastActivated() {
		return this.lastActivated;
	}

	/**
	 * Updates the completed length.
	 *
	 * <p>Will also set the operation state to {@link OperationState#COMPLETED} if the completed length is greater or equal
	 * to the the operation length.
	 *
	 * @param completedLength
	 */
	protected final void setCompletedLength(long completedLength) {
		if (this.completedLength != completedLength) {
			if (completedLength<0)
				throw new IllegalArgumentException("Negative length");
			if (completedLength > this.getLength())
				completedLength = this.getLength();
			this.completedLength = completedLength;
		}
		if (completedLength == this.getLength()) {
			if (isDelayedBeforeCompletion())
				return;

			setRunnableState(RunnableState.COMPLETED);
		}
	}

	protected long _getRemainingDelay(long remainingLength) {
		return Math.round(Math.ceil((double) remainingLength * this.getAllocatedResource().getUnitOfTime() / this.getAllocatedResource().getLong()));
	}

	protected final long getRemainingDelay(long remainingLength) {
		if (this.isDelayed())
			return -1l;

		if (this.getSynchronizationTimeAdjust() < 0)
			return -1l;

		if (remainingLength <= 0l)
			return this.getSynchronizationTimeAdjust();

		if (this.getAllocatedResource() == null)
			throw new IllegalStateException("Not allowed when there is no resource allocated");

		if (this.getAllocatedResource().getLong() == 0)
			return -1l;

		long total = 0l;

		total += _getRemainingDelay(remainingLength);

		total += this.getSynchronizationTimeAdjust();

		if (isRunning()) {
			total -= Simulator.getSimulator().getTime() - this.getLastActivated();
			if (total <= 0l)
				return 0l;
		}

		return total;
	}

	@Override
	public final long getRemainingDelay() {
		return getRemainingDelay(this.getLength() - this.getCompletedLength());
	}

	protected long _getCompletedLengthAfterDelay(long delay) {
		return Math.round(Math.floor((double) delay * getAllocatedResource().getLong() / this.getAllocatedResource().getUnitOfTime()));
	}

	/**
	 * Returns the length completed after computing for the given <tt>delay</tt> using the
	 * current allocated resource. This should <b>not</b> include old Completed Length if any.
	 *
	 * @param delay
	 * @return the length completed after computing for the given <tt>delay</tt>
	 */
	protected final long getCompletedLengthAfterDelay(long delay) {
		if (this.getSynchronizationTimeAdjust() < 0)
			return 0l;

		delay -= this.getSynchronizationTimeAdjust();
		if (delay <= 0) {
			this.setSynchronizationTimeAdjust(-delay);
			return 0l;
		} else {
			this.setSynchronizationTimeAdjust(0l);
		}

		return _getCompletedLengthAfterDelay(delay);
	}

	/**
	 * Checks the given resource promise for validity, and if it is not valid
	 * this method first tries to return a valid value for it, otherwise it returns <tt>null</tt>.
	 *
	 * <p>If the given promise's value is greater than the value returned by {@link LongOperationImpl#getResourceMax()}, then
	 * this function returns a new resource with that value. Because, if the provisioner promised a value greater than {@link LongOperationImpl#getResourceMax()}, then
	 * we can safely grant a lesser value.
	 *
	 * <p>If the given promise is lesser than the value returned by {@link LongOperationImpl#getResourceMin()}, then
	 * this function returns <tt>null</tt>. Because, there is no guarantee that the provisioner can grant {@link LongOperationImpl#getResourceMin()} if
	 * we returned that value.
	 *
	 * @param r
	 * @return the new validated resource or <tt>null</tt>
	 */
	@SuppressWarnings("unchecked")
	protected Resource validateResourcePromise(Resource r) {
		if (r == null)
			return null;

		long value = r.getLong();

		if (value > getSynchronizedResource())
			value = getSynchronizedResource();

		if (value > getResourceMax())
			value = getResourceMax();

		if (value < getResourceMin())
			return null;

		return (Resource) r.clone(value);
	}

	/**
	 * Returns the promise as given by the provisioner, or <tt>null</tt> if no provisioner was assigned to the operation or
	 * if <tt>null</tt> was returned by {@link LongOperationImpl#validateResourcePromise(LongResource)}.
	 *
	 * <p>Implementations should validate the promised resource using {@link LongOperationImpl#validateResourcePromise(LongResource)}.
	 *
	 * @return the promise as given by the provisioner, or <tt>null</tt>
	 */
	protected abstract Resource getProvisionerPromise();

	/**
	 * Will grant the allocated resource among each provisioner.
	 */
	protected abstract void grantAllocatedResource();

	/**
	 * Will revoke the allocated resource among each provisioner.
	 */
	protected abstract void revokeAllocatedResource();

	/**
	 * Registers listeners so that the operation is deactivated (and maybe reactivated) when:<ul>
	 * 	<li>The allocated resource is invalidated {@link NotificationCodes#OPERATION_RESOURCE_INVALIDATED};
	 * 	<li>The state of the host that is containing the source job VM changes {@link NotificationCodes#FAILURE_STATE_CHANGED}.
	 * </ul>
	 */
	@Override
	protected boolean registerListeners() {
		unregisterListeners();

		//the allocated resource is invalidated
		{
			NotificationListener vListener = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					Resource r = LongOperationImpl.this.getProvisionerPromise();
					if (r == null) {
						getLogger().log(Level.FINEST, LongOperationImpl.this, "Failed because the provisioner cannot re-allocate resources for this operation.");
						LongOperationImpl.this.doFail();
					} else if (r.getLong() != LongOperationImpl.this.getAllocatedResource().getLong()) {
						LongOperationImpl.this.deactivate0();
						LongOperationImpl.this.activate0();
					}
				}
			}; registeredListener(vListener);

			addListener(NotificationCodes.OPERATION_RESOURCE_INVALIDATED, vListener);
		}
		//*********************************************************************

		//failures listener
		{
			NotificationListener fListener = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					FailureProneEntity e = (FailureProneEntity) notifier;
					if (e.getFailureState() != FailureState.OK) {
						getLogger().log(Level.FINEST, LongOperationImpl.this, "Failed because a device (" + e + ") has failed.");
						LongOperationImpl.this.doFail();
					}
				}
			}; registeredListener(fListener);
		}
		//*********************************************************************

		//power listener
		{
			NotificationListener pListener = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					PoweredEntity e = (PoweredEntity) notifier;
					if (e.getPowerState() != PowerState.ON) {
						getLogger().log(Level.FINEST, LongOperationImpl.this, "Failed because a device (" + e + ") is powered-off.");
						LongOperationImpl.this.doFail();
					}
				}
			}; registeredListener(pListener);
		}
		//*********************************************************************


		//add a failure dependency to fail if the machine that contains the source job's VM, fails
		if (!addFailureDependency(getParent().getParent().getParent()))
			return false;

		//add a power dependency to fail if the machine that contains the source job's VM, is powered-off
		if (!addPowerDependency(getParent().getParent().getParent()))
			return false;

		return true;
	}

	/**
	 * Returns <tt>true</tt> if a listener was successfully added.
	 * <tt>false</tt> is returned if the given entity is already in failed state, and that no
	 * listener is added.
	 *
	 * <p>This method first checks that the given <tt>e</tt> is not failed (if so the operation fails), and also adds a listener so
	 * that when the given <tt>e</tt> fails, this operation will be deactivated.
	 *
	 * <p>You must first call {@link LongOperationImpl#registerListeners()} before calling this
	 * method.
	 *
	 * @param e
	 * @return <tt>true</tt> if a listener was successfully added
	 */
	protected boolean addFailureDependency(FailureProneEntity e) {
		if (e.getFailureState() != FailureState.OK) {
			getLogger().log(Level.FINEST, LongOperationImpl.this, "Failed because a device (" + e + ") has failed.");
			return false;
		}

		e.addListener(NotificationCodes.FAILURE_STATE_CHANGED, getRegisteredListener(1));
		return true;
	}

	/**
	 * Returns <tt>true</tt> if a listener was successfully added.
	 * <tt>false</tt> is returned if the given entity is not powered on, and that no
	 * listener is added.
	 *
	 * <p>This method first checks that the given <tt>e</tt> is powered-on (if not the operation fails), and also adds a listener so
	 * that when the given <tt>e</tt> is powered-off, this operation will be deactivated.
	 *
	 * <p>You must first call {@link LongOperationImpl#registerListeners()} before calling this
	 * method.
	 *
	 * @param e
	 * @return if <tt>false</tt> that means that the entity is powered-off and no listener is added
	 */
	protected boolean addPowerDependency(PoweredEntity e) {
		if (e.getPowerState() != PowerState.ON) {
			getLogger().log(Level.FINEST, LongOperationImpl.this, "Failed because a device (" + e + ") is powered-off.");
			return false;
		}

		e.addListener(NotificationCodes.POWER_STATE_CHANGED, getRegisteredListener(2));
		return true;
	}

	protected long getNextLengthForDelaying() {
		return Long.MAX_VALUE;
	}

	@Override
	public boolean isDelayed() {
		return ((this.delayedState & DELAYED) != 0);
	}

	@Override
	public boolean isDelayedBeforeCompletion() {
		int delayedState;

		if (isRunning())
			delayedState = this.delayedState;
		else {
			long old = this.maxActivationLength;
			delayedState = getDelayedStateAndSetMaxActivationLength();
			this.maxActivationLength = old;
		}

		return ((delayedState & DELAY_BEFORE_COMPLETION) != 0);
	}

	private int getDelayedStateAndSetMaxActivationLength() {
		int delayedState = 0;
		this.maxActivationLength = getNextLengthForDelaying();
		if (this.maxActivationLength >= this.getCompletedLength() && this.maxActivationLength <= this.getLength()) {
			if (this.maxActivationLength == this.getCompletedLength()) {
				delayedState = delayedState | DELAYED;
			}
			delayedState = delayedState | DELAY_BEFORE_COMPLETION;
		} else
			this.maxActivationLength = this.getLength(); //necessary to compute remaining length

		if (getSynchronizationTimeAdjust() < 0) {
			delayedState = delayedState | DELAYED;
			delayedState = delayedState | DELAY_BEFORE_COMPLETION;
		}

		if (isSynchronizedNeedsDelayingBeforeCompletion()) {
			if (this.getCompletedLength() == this.getLength())
				delayedState = delayedState | DELAYED;
			delayedState = delayedState | DELAY_BEFORE_COMPLETION;
		}

		return delayedState;
	}

	/**
	 * Activates the operation after the provisioner is ready and returns <tt>true</tt> on success.
	 *
	 * <p>This activation method does not throw notifications.
	 *
	 * @return <tt>true</tt> if the operation was successfully activate
	 * and <tt>false</tt> if the operation was not activated
	 */
	protected boolean activate0() {
		//register listeners to keep the operation consistent
		if (!registerListeners()) {
			doFail();
			return false;
		}

		//Fill the delayedState var
		this.delayedState = getDelayedStateAndSetMaxActivationLength();

		this.endOfOperationEvent = null;

		if (!isDelayed()) {
			long remainingLength = this.maxActivationLength - this.getCompletedLength();

			if (remainingLength > 0) {
				Resource r = LongOperationImpl.this.getProvisionerPromise();
				if (r == null) {
					getLogger().log(Level.FINEST, LongOperationImpl.this, "Failed because the provisioner cannot allocate resources for this operation.");
					doFail();
					return false;
				}

				setAllocatedResource(r);

				grantAllocatedResource();
			}

			//notify the operation start before scheduling endOfOperationEvent!
			this.lastActivated = Simulator.getSimulator().getTime();
			setRunnableState(RunnableState.RUNNING);

			//schedule an event for the end of the operation, or for the delaying
			long remainingDelay = getRemainingDelay(remainingLength);

			//if remainingDelay is negative, then run forever
			if (remainingDelay >= 0) {
				endOfOperationEvent = new EventImpl() {
					@Override
					public void process() {
						LongOperationImpl.this.doPause();
						if (!LongOperationImpl.this.isTerminated())
							LongOperationImpl.this.doStart();
					}
				};
				Simulator.getSimulator().schedule(remainingDelay, endOfOperationEvent);
			}
		} else {
			setAllocatedResource(null);
			endOfOperationEvent = null;

			this.lastActivated = Simulator.getSimulator().getTime();
			setRunnableState(RunnableState.RUNNING);
		}

		if (this.endOfOperationEvent == null) {
			//make sure there is a scheduled event, so that the simulation will not stop
			endOfOperationEvent = new EventImpl() {
				@Override
				public void process() {
					//dummy event
				}
			};
			Simulator.getSimulator().schedule(Simulator.getSimulator().getMaximumScheduleDelay(), endOfOperationEvent);
		}

		return true;
	}

	/**
	 * Deactivates the operation silently without throwing notifications
	 * and returns <tt>true</tt> on success.
	 *
	 * @return <tt>true</tt> if the operation was successfully deactivated
	 * and <tt>false</tt> if the operation was not deactivated
	 */
	protected boolean deactivate0() {
		long delay = Simulator.getSimulator().getTime() - this.getLastActivated();
		this.lastActivated = Long.MAX_VALUE;

		unregisterListeners();

		this.oldTotalRunningTime = this.oldTotalRunningTime + delay;

		if (endOfOperationEvent != null) {
			endOfOperationEvent.cancel();
			endOfOperationEvent = null;
		}

		if (!isDelayed()) {
			revokeAllocatedResource();

			long total = getCompletedLengthAfterDelay(delay);

			setCompletedLength(Math.min(this.maxActivationLength, getCompletedLength() + total));
		}

		if (getRunnableState() == RunnableState.RUNNING) //if the state was not changed by setCompletedLength()
			setRunnableState(RunnableState.PAUSED);

		setAllocatedResource(null);

		this.maxActivationLength = 0;

		this.delayedState = 0;

		return true;
	}

	/**
	 * For use by subclasses to prepare resources before starting the operation.
	 *
	 * <p>Exceptions may be thrown.
	 */
	protected abstract void prepareActivation();

	@Override
	public void doStart() {
		if (!canStart())
			throw new IllegalStateException(getCannotStartReason());

		prepareActivation();

		activate0();
	}

	@Override
	public void doPause() {
		if (!isRunning())
			throw new IllegalStateException("This operation (" + this + ") is not running.");

		deactivate0();
	}

	@Override
	public void doRestart() {
		if (!canRestart())
			throw new IllegalStateException(getCannotRestartReason());
		this.setRunnableState(RunnableState.PAUSED);
		this.setCompletedLength(0);
		if (!this.isTerminated())
			doStart();
	}

	@Override
	public void doTerminate() {
		if (this.isTerminated())
			return;

		if (this.isRunning())
			this.doPause();
		else
			this.setCompletedLength(this.getCompletedLength()); //make sure it is not completed

		if (!this.isTerminated())
			this.doCancel();
	}

	/**
	 * This method is used for synchronization.
	 *
	 * <p>It computes and returns minimum resource value to use in order to finish before the given <tt>delay</tt>.
	 *
	 * <p>This method assumes that the operation is not running.
	 *
	 * @return the minimum resource value to use in order to finish before the given <tt>delay</tt>
	 */
	protected abstract Resource computeSynchronizedResource(long delay);

	protected static long computeSynchronizedResource(long remainingLength, long unitoftime, long delay) {
		//return Math.round(Math.floor((double)remainingLength * unitoftime / (delay - 1)));
		return Math.round(Math.ceil((double)remainingLength * unitoftime / delay));
	}

	protected long getSynchronizedResource() {
		return this.synchronizedResource;
	}

	/**
	 * Returns the synchronization time adjust value which is positive, or -1 if
	 * there for an infinite adjust time (i.e. if the operation needs to be delayed).
	 *
	 * @return the synchronization time adjust value which is positive, or -1 if
	 * there for an infinite adjust time (i.e. if the operation needs to be delayed).
	 */
	private long getSynchronizationTimeAdjust() {
		return this.synchronizedTimeAdjust;
	}

	private void setSynchronizationTimeAdjust(long newValue) {
		this.synchronizedTimeAdjust = newValue;
	}

	private boolean isSynchronizedNeedsDelayingBeforeCompletion() {
		return this.synchronizedNeedsDelayingBeforeCompletion;
	}

	/**
	 * @see SynchronizableOperation#startSynchronization(OperationSynchronizer)
	 */
	protected void startSynchronization(OperationSynchronizer operationSynchronizer) {
		if (isRunning())
			throw new IllegalStateException("Not allowed when this operation is running");

		this.operationSynchronizer = operationSynchronizer;

		long delay = this.operationSynchronizer.getSynchronizationDelay();
		if (delay > 0l) {
			final Resource r = computeSynchronizedResource(delay);
			this.synchronizedResource = r.getLong();
			if (this.synchronizedResource == 0)
				this.synchronizedResource = Long.MAX_VALUE; //zero length remaining
			final boolean notifEnabled = !this.isNotificationsDisabled();
			if (notifEnabled)
				this.disableNotifications();
			final Resource oldAllocatedResource = this.getAllocatedResource();
			this.setAllocatedResource(r);
			this.synchronizedTimeAdjust = delay - this.getRemainingDelay();
			if (this.synchronizedTimeAdjust < 0l)
				this.synchronizedTimeAdjust = 0l;
			this.setAllocatedResource(oldAllocatedResource);
			if (notifEnabled)
				this.enableNotifications();
		} else if (delay == 0l) {
			this.synchronizedResource = Long.MAX_VALUE;
			this.synchronizedTimeAdjust = 0l;
		} else {
			this.synchronizedResource = Long.MAX_VALUE;
			this.synchronizedTimeAdjust = -1;
		}

		this.synchronizedNeedsDelayingBeforeCompletion = this.operationSynchronizer.isSynchronizeDelayBeforeCompletion();
	}

	/**
	 * @see SynchronizableOperation#stopSynchronization()
	 */
	protected void stopSynchronization() {
		if (isRunning())
			throw new IllegalStateException("Not allowed when this operation is running");

		this.synchronizedResource = Long.MAX_VALUE;
		this.synchronizedTimeAdjust = 0l;
		this.synchronizedNeedsDelayingBeforeCompletion = false;
		this.operationSynchronizer = null;
	}

	/**
	 * @see SynchronizableOperation#getOperationSynchronizer()
	 */
	protected OperationSynchronizer getOperationSynchronizer() {
		return this.operationSynchronizer;
	}

	protected void synchronizeWith(SynchronizableOperation<?> operation) {
		if (this.getOperationSynchronizer() != null) {
			if (operation.getOperationSynchronizer() != null) {
				List<SynchronizableOperation<?>> l = newArrayList(this.getOperationSynchronizer().getOperations().size() +
						operation.getOperationSynchronizer().getOperations().size());

				l.addAll(this.getOperationSynchronizer().getOperations());
				l.addAll(operation.getOperationSynchronizer().getOperations());

				OperationSynchronizer os = Factory.getFactory(this).newOperationSynchronizer(null);
				for (SynchronizableOperation<?> o: l) {
					o.getOperationSynchronizer().removeOperation(o);
					os.addOperation(o);
				}
			} else {
				this.getOperationSynchronizer().addOperation(operation);
			}
		} else {
			if (operation.getOperationSynchronizer() != null) {
				operation.getOperationSynchronizer().addOperation((SynchronizableOperation<?>) this);
			} else {
				OperationSynchronizer os = Factory.getFactory(this).newOperationSynchronizer(null);

				os.addOperation((SynchronizableOperation<?>) this);
				os.addOperation(operation);
			}
		}
	}

	protected void cancelSynchronization() {
		OperationSynchronizer os = this.getOperationSynchronizer();
		if (os == null)
			return;
		os.removeOperation((SynchronizableOperation<?>) this);

		if (os.getOperations().size() == 1)
			os.removeAllOperations();
	}
}
