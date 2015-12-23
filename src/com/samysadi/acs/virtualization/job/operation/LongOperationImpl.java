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

/**
 * This class defines a Operation that uses a {@link LongResource}.
 * 
 * @since 1.0
 */
public abstract class LongOperationImpl<Resource extends LongResource> extends OperationImpl<Resource> {
	private long resourceMax;
	private long resourceMin;
	private long lastActivated;
	private long length;

	private long oldTotalRunningTime;
	private long completedLength;

	private Event endOfOperationEvent;

	private long synchronizedResource;
	private long synchronizedTimeAdjust;
	private Operation<?> synchronizedWith;

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
		this.endOfOperationEvent = null;

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
		this.synchronizedWith = null;
	}

	/**
	 * Returns the length of this operation.
	 * 
	 * @return the length of this operation
	 */
	public long getLength() {
		return this.length;
	}

	private void setLength(long length) {
		this.length = length;
	}

	/**
	 * Returns the maximum resource value that this operation can use (inclusive).
	 * 
	 * <p>On activation, this operation will ask the provisioner to grant at most a value equals to this value, even if this operation receives 
	 * a promise greater than this value.
	 * 
	 * <p><b>Default</b> is {@code Long.MAX_VALUE}.
	 * 
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates etc..), the value returned here
	 * is assumed to be the maximum length that can be operated in one {@link Simulator#SECOND}.
	 * 
	 * @return the maximum resource value that this operation can use (inclusive)
	 */
	public long getResourceMax() {
		return resourceMax;
	}

	/**
	 * Updates the maximum resource that can be granted for this operation.
	 * 
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates etc..), the value set here
	 * is assumed to be the maximum length that can be operated in one {@link Simulator#SECOND}.
	 * 
	 * @param resourceMax
	 * @throws IllegalStateException if this operation is activated
	 */
	public void setResourceMax(long resourceMax) {
		if (this.resourceMax == resourceMax)
			return;
		if (this.isRunning())
			throw new IllegalStateException("The operation is activated.");
		this.resourceMax = resourceMax;
	}

	/**
	 * Returns the minimum resource value that this operation needs for activation (inclusive).
	 * 
	 * <p>On activation, if this operation receives a smaller promise than this value from the provisioner, then the activation fails.
	 * 
	 * <p><b>Default</b> is {@code 1l}.
	 * 
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates etc..), the value returned here
	 * is assumed to be the minimum length that can be operated in one {@link Simulator#SECOND}.
	 * 
	 * @return the minimum resource value that this operation needs for activation (inclusive)
	 */
	public long getResourceMin() {
		return resourceMin;
	}

	/**
	 * Updates the minimum resource that has to be granted for this operation on activation.
	 * 
	 * <p>For resources that needs a unit of time (to represent bandwidth, transfer rates etc..), the value set here
	 * is assumed to be the minimum length that can be operated in one {@link Simulator#SECOND}.
	 * 
	 * @param resourceMin
	 * @throws IllegalStateException if this operation is activated
	 */
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

	/**
	 * Returns the completed length of this operation.
	 * If this operation is active then this does not include 
	 * the completed length since last activation.
	 * 
	 * @return the completed length of this operation
	 */
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
	protected void setCompletedLength(long completedLength) {
		if (this.completedLength != completedLength) {
			if (completedLength<0)
				throw new IllegalArgumentException("Negative length");
			if (completedLength > this.getLength())
				completedLength = this.getLength();
			this.completedLength = completedLength;
		}
		if (completedLength == this.getLength())
			setRunnableState(RunnableState.COMPLETED);
	}

	@Override
	public long getRemainingDelay() {
		if (this.getAllocatedResource() == null)
			throw new IllegalStateException("Not allowed when there is no resource allocated");
		if (this.getAllocatedResource().getLong() == 0)
			return Long.MAX_VALUE;

		long remainingLength = this.getLength() - this.getCompletedLength();
		if (remainingLength <= 0l)
			return 0l;

		long total = 0l;

		total += Math.round(Math.ceil((double) remainingLength * this.getAllocatedResource().getUnitOfTime() / this.getAllocatedResource().getLong()));

		total += this.getSynchronizationTimeAdjust();

		if (isRunning()) {
			total -= Simulator.getSimulator().getTime() - this.getLastActivated();
			if (total <= 0l)
				return 0l;
		}

		return total;
	}

	/**
	 * Returns the length completed after computing for the given <tt>delay</tt> using the
	 * current allocated resource. This should <b>not</b> include old Completed Length if any.
	 * 
	 * @param delay
	 * @return the length completed after computing for the given <tt>delay</tt>
	 */
	protected long getCompletedLengthAfterDelay(long delay) {
		if (this.getSynchronizationTimeAdjust() > 0) {
			delay -= this.getSynchronizationTimeAdjust();
			if (delay < 0) {
				this.setSynchronizationTimeAdjust(-delay);
				return 0l;
			} else {
				this.setSynchronizationTimeAdjust(0l);
			}
		}

		return Math.round(Math.floor((double) delay * getAllocatedResource().getLong() / this.getAllocatedResource().getUnitOfTime()));
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

		if (r.getLong() > getSynchronizedResource())
			value = getSynchronizedResource();

		if (r.getLong() > getResourceMax())
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
						getLogger().log(LongOperationImpl.this, "Failed because the provisioner cannot re-allocate resources for this operation.");
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
						getLogger().log(LongOperationImpl.this, "Failed because a device (" + e + ") has failed.");
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
						getLogger().log(LongOperationImpl.this, "Failed because a device (" + e + ") is powered-off.");
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
			getLogger().log(LongOperationImpl.this, "Failed because a device (" + e + ") has failed.");
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
			getLogger().log(LongOperationImpl.this, "Failed because a device (" + e + ") is powered-off.");
			return false;
		}
			
		e.addListener(NotificationCodes.POWER_STATE_CHANGED, getRegisteredListener(2));
		return true;
	}

	/**
	 * Activates the operation after the provisioner is ready and returns <tt>true</tt> on success.
	 * 
	 * <p>This activation method does not throw notification unlike {@link Operation#activate()} does.
	 * 
	 * @return <tt>true</tt> if the operation was successfully activate
	 * and <tt>false</tt> if the operation was not activated
	 */
	protected boolean activate0() {
		Resource r = LongOperationImpl.this.getProvisionerPromise();
		if (r == null) {
			getLogger().log(LongOperationImpl.this, "Failed because the provisioner cannot allocate resources for this operation.");
			doFail();
			return false;
		}

		//register listeners to keep the operation consistent
		if (!registerListeners()) {
			doFail();
			return false;
		}

		setAllocatedResource(r);

		grantAllocatedResource();

		//schedule an event for the end of the operation
		endOfOperationEvent = new EventImpl() {
			@Override
			public void process() {
				LongOperationImpl.this.doPause();
			}
		};
		Simulator.getSimulator().schedule(getRemainingDelay(), endOfOperationEvent);

		this.lastActivated = Simulator.getSimulator().getTime();
		setRunnableState(RunnableState.RUNNING);
			
		return true;
	}

	/**
	 * Deactivates the operation silently without throwing notifications unlike {@link Operation#deactivate()} does
	 * and returns <tt>true</tt> on success.
	 * 
	 * @return <tt>true</tt> if the operation was successfully deactivated
	 * and <tt>false</tt> if the operation was not deactivated
	 */
	protected boolean deactivate0() {
		long delay = Simulator.getSimulator().getTime() - this.getLastActivated();
		this.lastActivated = Long.MAX_VALUE;

		unregisterListeners();
		if (endOfOperationEvent != null) {
			endOfOperationEvent.cancel();
			endOfOperationEvent = null;
		}

		revokeAllocatedResource();

		this.oldTotalRunningTime = this.oldTotalRunningTime + delay;

		long total = getCompletedLengthAfterDelay(delay);

		setCompletedLength(getCompletedLength() + total);

		if (getRunnableState() == RunnableState.RUNNING) //if the state was not changed by setCompletedLength()
			setRunnableState(RunnableState.PAUSED);

		setAllocatedResource(null);

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

	/**
	 * This method is used for synchronization.
	 * 
	 * <p>This method assumes that the operation is not running.
	 * 
	 * @return the resource value to use in order to finish after the given <tt>delay</tt>
	 */
	protected abstract Resource computeSynchronizedResource(long delay);

	protected long getSynchronizedResource() {
		return this.synchronizedResource;
	}

	protected long getSynchronizationTimeAdjust() {
		return this.synchronizedTimeAdjust;
	}

	protected void setSynchronizationTimeAdjust(long newValue) {
		this.synchronizedTimeAdjust = newValue;
	}

	protected Operation<?> getSynchronizedWith() {
		return this.synchronizedWith;
	}

	protected void startSynchronization(long delay, Operation<?> operation) {
		if (delay <= 0)
			throw new IllegalArgumentException("Given delay is negative");

		if (isRunning())
			throw new IllegalStateException("Not allowed when this operation is running");

		this.synchronizedWith = operation;

		final Resource r = computeSynchronizedResource(delay);
		this.synchronizedResource = r.getLong();
		final boolean notifEnabled = !this.isNotificationsDisabled();
		if (notifEnabled)
			this.disableNotifications();
		final Resource oldAllocatedResource = this.getAllocatedResource();
		this.setAllocatedResource(r);
		this.synchronizedTimeAdjust = 0l;
		this.synchronizedTimeAdjust = delay - this.getRemainingDelay();
		if (this.synchronizedTimeAdjust < 0l)
			this.synchronizedTimeAdjust = 0l;
		this.setAllocatedResource(oldAllocatedResource);
		if (notifEnabled)
			this.enableNotifications();
	}

	protected void stopSynchronization() {
		if (isRunning())
			throw new IllegalStateException("Not allowed when this operation is running");

		this.synchronizedResource = Long.MAX_VALUE;
		this.synchronizedTimeAdjust = 0l;
		this.synchronizedWith = null;
	}

	protected boolean isSynchronized(Operation<?> operation) {
		return (this.synchronizedResource != Long.MAX_VALUE) && this.synchronizedWith == operation;
	}
}
