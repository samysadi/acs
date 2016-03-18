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

package com.samysadi.acs.hardware.network.operation;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.routingprotocol.Route;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol.RouteInfo;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.LongOperationImpl;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.SynchronizableOperation;

/**
 * This implementation will automatically
 * find a new {@link Route} using the {@link RoutingProtocol} on the host of the parent job's VM each time it is activated.
 *
 * <p>Make sure that the {@link RoutingProtocol} on the host of the parent job's VM is not <tt>null</tt> or a
 * NullPointerException will be thrown whenever you try to start this operation.
 *
 * @since 1.0
 */
public class NetworkOperationDefault extends LongOperationImpl<NetworkResource> implements NetworkOperation, SynchronizableOperation<NetworkResource> {
	private Job destinationJob;
	private boolean retransmitOnError;
	/**
	 * Latency that was not yet counted
	 */
	private long remainingLatencyForRoute;
	private Route allocatedRoute;

	/**
	 * Empty constructor that creates a zero-length operation with a <tt>null</tt> destination job.
	 *
	 * <p>This constructor is provided only to satisfy the {@link Entity} contract.<br/>
	 * You should use {@link NetworkOperationDefault#NetworkOperationDefault(Job, long)} though.
	 */
	public NetworkOperationDefault() {
		this(null, 0);
	}

	public NetworkOperationDefault(Job destinationJob,
			long dataSize) {
		super(dataSize);
		this.retransmitOnError = true;
		this.setDestinationJob(destinationJob);
	}

	@Override
	public NetworkOperationDefault clone() {
		final NetworkOperationDefault clone = (NetworkOperationDefault) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.allocatedRoute = null;
		this.remainingLatencyForRoute = -1l;

		{
			//make sure the destination job is notified
			Job old = this.destinationJob;
			this.destinationJob = null;
			setDestinationJob(old);
		}
	}

	@Override
	public boolean isRetransmitOnError() {
		return retransmitOnError;
	}

	public void setRetransmitOnError(boolean v) {
		this.retransmitOnError = v;
	}

	@Override
	public Job getDestinationJob() {
		return destinationJob;
	}

	@Override
	public void setDestinationJob(Job destinationJob) {
		if (this.destinationJob == destinationJob)
			return;
		if (this.isRunning())
			throw new IllegalStateException("Not allowed when activated");

		Job old = this.destinationJob;
		this.destinationJob = destinationJob;
		notify(NotificationCodes.OPERATION_DEST_JOB_CHANGED, null);
		if (old != null)
			old.notify(NotificationCodes.JOB_DEST_OPERATION_REMOVED, this);
		if (getDestinationJob() != null)
			getDestinationJob().notify(NotificationCodes.JOB_DEST_OPERATION_ADDED, this);
	}

	@Override
	public boolean canRestart() {
		if (getDestinationJob() == null ||
				!getDestinationJob().isRunning())
			return false;
		return super.canRestart();
	}

	@Override
	protected String getCannotRestartReason() {
		if (getDestinationJob() == null)
			return "This operation (" + this + ") cannot be restarted because there is no given destination job.";
		if (!getDestinationJob().isRunning())
			return "This operation (" + this + ") cannot be restarted because the destination job is not running.";
		return super.getCannotRestartReason();
	}

	@Override
	public boolean canStart() {
		if (getDestinationJob() == null ||
				!getDestinationJob().isRunning())
			return false;
		return super.canStart();
	}

	@Override
	protected String getCannotStartReason() {
		if (getDestinationJob() == null)
			return "This operation (" + this + ") cannot be started because there is no given destination job.";
		if (!getDestinationJob().isRunning())
			return "This operation (" + this + ") cannot be started because the destination job is not running.";
		return super.getCannotStartReason();
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

		long total = 0;
		if (this.remainingLatencyForRoute < 0)
			this.remainingLatencyForRoute = this.getAllocatedResource().getLatency();
		total += this.remainingLatencyForRoute;

		total += Math.round(Math.ceil(((double) remainingLength * getAllocatedResource().getUnitOfTime() * (1.0d + (isRetransmitOnError() ? getAllocatedResource().getLossRate() : 0.0d))) / getAllocatedResource().getBw()));

		total += this.getSynchronizationTimeAdjust();

		if (isRunning()) {
			total -= Simulator.getSimulator().getTime() - this.getLastActivated();
			if (total <= 0l)
				return 0l;
		}

		return total;
	}

	@Override
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

		if (this.remainingLatencyForRoute > 0) {
			delay -= this.remainingLatencyForRoute;
			if (delay < 0) {
				this.remainingLatencyForRoute = -delay;
				return 0l;
			} else {
				this.remainingLatencyForRoute = 0l;
			}
		}

		return Math.round(Math.floor((double)this.getAllocatedResource().getBw() * delay / (Simulator.SECOND * (1.0d + (isRetransmitOnError() ? this.getAllocatedResource().getLossRate() : 0.0d)))));
	}

	@Override
	protected NetworkResource computeSynchronizedResource(long delay) {
		return new NetworkResource(Math.round(Math.ceil(((double) (this.getLength() - this.getCompletedLength()) * Simulator.SECOND) / delay)),
				0,
				0);
	}

	@Override
	protected boolean deactivate0() {
		long delay = Simulator.getSimulator().getTime() - this.getLastActivated();
		if (!super.deactivate0())
			return false;

		final long delta = this.remainingLatencyForRoute - delay;
		if (delta < 0)
			this.remainingLatencyForRoute = 0l;
		else
			this.remainingLatencyForRoute = delta;

		return true;
	}

	@Override
	protected boolean registerListeners() {
		if (!super.registerListeners())
			return false;

		//the route is no longer valid, try to find another or fail
		{
			NotificationListener resendListener = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					NetworkOperationDefault.this.deactivate0();
					NetworkOperationDefault.this.allocateRoute();
					if (NetworkOperationDefault.this.getAllocatedRoute() == null) {
						getLogger().log(NetworkOperationDefault.this, "Failed because a route cannot be found (again) for this operation.");
						NetworkOperationDefault.this.doFail();
						return;
					}
					NetworkOperationDefault.this.activate0();
				}
			}; registeredListener(resendListener);

			getAllocatedRoute().registerListenerForRoutingUpdates(resendListener);

			//the destination device fails (not covered by above method)
			if (!addFailureDependency(getDestinationJob().getParent().getParent()))
				return false;

			//the destination device is powered-off (not covered by above method)
			if (!addPowerDependency(getDestinationJob().getParent().getParent()))
				return false;

			//the parent VM of the destination Job changes
			getDestinationJob().addListener(NotificationCodes.ENTITY_PARENT_CHANGED, resendListener);
			//the parent Host of the destination Job changes
			getDestinationJob().getParent().addListener(NotificationCodes.ENTITY_PARENT_CHANGED, resendListener);
		}
		//*********************************************************************

		return true;
	}

	protected void allocateRoute() {
		RouteInfo routeInfo = null;

		if (getDestinationJob() != null) {
			final NetworkDevice source = getParent().getParent().getParent();
			final NetworkDevice dest = getDestinationJob().getParent().getParent();

			//let's try to find a route for this data
			routeInfo = source.getRoutingProtocol().findRoute(dest, null);
		}

		if (routeInfo == null)
			setAllocatedRoute(null);
		else
			setAllocatedRoute(routeInfo.getRoute());
	}

	@Override
	protected void prepareActivation() {
		allocateRoute();
	}

	@Override
	public Route getAllocatedRoute() {
		return allocatedRoute;
	}

	protected void setAllocatedRoute(Route route) {
		if (this.isRunning())
			throw new IllegalStateException("Cannot change the route when the operation is activated");
		if (route == allocatedRoute)
			return;

		allocatedRoute = route;
		this.remainingLatencyForRoute = -1l;
		notify(NotificationCodes.NETOP_ROUTE_CHANGED, null);
	}

	@Override
	protected NetworkResource getProvisionerPromise() {
		if (getAllocatedRoute() == null)
			return null;
		return validateResourcePromise(getAllocatedRoute().getResourcePromise(this));
	}

	@Override
	protected void grantAllocatedResource() {
		if (getAllocatedRoute() == null)
			return;

		getAllocatedRoute().grantAllocatedResource(this);
	}

	@Override
	protected void revokeAllocatedResource() {
		if (getAllocatedRoute() == null)
			return;

		getAllocatedRoute().revokeAllocatedResource(this);
	}

	@Override
	public void startSynchronization(long delay, Operation<?> operation) {
		super.startSynchronization(delay, operation);
	}

	@Override
	public void stopSynchronization() {
		super.stopSynchronization();
	}

	@Override
	public boolean isSynchronized(Operation<?> operation) {
		return super.isSynchronized(operation);
	}

	@Override
	public void notify(int notification_code, Object data) {
		if (notification_code == NotificationCodes.RUNNABLE_STATE_CHANGED &&
				getParent() != null &&
				getParent().getParent() != null &&
				getParent().getParent().getFlag(VirtualMachine.FLAG_BUFFER_NETWORK_OUTPUT))
			getParent().getParent().bufferNotification(this, notification_code, data);
		else
			super.notify(notification_code, data);
	}
}
