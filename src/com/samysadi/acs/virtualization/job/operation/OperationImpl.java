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

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntityImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.Job;


/**
 *
 * @since 1.0
 */
public abstract class OperationImpl<Resource> extends RunnableEntityImpl implements Operation<Resource> {
	private Resource allocatedResource;
	private List<NotificationListener> registeredListeners;

	public OperationImpl() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public OperationImpl<Resource> clone() {
		final OperationImpl<Resource> clone = (OperationImpl<Resource>) super.clone(); //super class clone will also ensure that this entity is not running
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.allocatedResource = null;
		this.registeredListeners = null;
	}

	@Override
	public Job getParent() {
		return (Job) super.getParent();
	}

	@Override
	public void setParent(Entity parent) {
		if (parent != null && !(parent instanceof Job))
			throw new IllegalArgumentException("The given entity cannot be a parent of this entity");
		super.setParent(parent);
	}

	@Override
	protected void afterSetParent(Entity oldParent) {
		super.afterSetParent(oldParent);
		if (oldParent != null)
			oldParent.notify(NotificationCodes.JOB_SRC_OPERATION_REMOVED, this);
		if (getParent() != null)
			getParent().notify(NotificationCodes.JOB_SRC_OPERATION_ADDED, this);
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof RunnableEntity) {
			throw new UnsupportedOperationException("Adding this RunnableEntity is not supported by this implementation.");
		} else {
			super.addEntity(entity);
			return;
		}
		//notify(Notifications.ENTITY_ADDED, entity);
	}

	@Override
	public void doCancel() {
		if (this.isRunning())
			this.doPause();
		if (this.isTerminated())
			return;
		setRunnableState(RunnableState.CANCELED);
	}

	@Override
	public void doFail() {
		if (this.isRunning())
			this.doPause();
		if (this.isTerminated())
			return;
		setRunnableState(RunnableState.FAILED);
	}

	/**
	 * Pauses the current operation. These basic steps are taken:<ul>
	 * 	<li>Frees the allocated resource on each Provisioner that is related to this operation;
	 * 	<li>Updates this operation state and discards any scheduled event during operation activation;
	 * 	<li>Updates total completed length, and sets the operation state to {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#COMPLETED} if needed.
	 * </ul>
	 * The new Operation state is set to {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#PAUSED} or to {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#COMPLETED}.
	 *
	 * <p>
	 * @throws IllegalStateException if the operation is not in {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#RUNNING} state.
	 */
	@Override
	public abstract void doPause();

	@Override
	public abstract void doRestart();

	/**
	 * Activate the current operation. These basic steps are taken:<ul>
	 * 	<li>Allocates the wanted resource on each Provisioner that is related to this operation;
	 * 	<li>Updates this operation state and schedules an event for the end of operation
	 * to automatically deactivate it when it is completed.
	 * </ul>
	 * The new operation state is set to {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#RUNNING} if successful or to {@link com.samysadi.acs.core.entity.RunnableEntity.RunnableState#FAILED} if the operation cannot be activated (example: not enough resources).
	 *
	 * @throws IllegalStateException if the operation is terminated (see {@link Operation#isTerminated()}) or if it is already running.
	 */
	@Override
	public abstract void doStart();

	/**
	 * Will pause the operation and cancel it if it is not completed.
	 */
	@Override
	public void doTerminate() {
		if (this.isTerminated())
			return;
		this.doCancel();
	}

	/**
	 * Returns the listener at the given index as returned by {@link OperationImpl#registeredListener(NotificationListener)}.
	 *
	 * @param index
	 * @return the listener at the given index
	 */
	protected NotificationListener getRegisteredListener(int index) {
		return this.registeredListeners.get(index);
	}

	/**
	 * Adds and registers listeners for this operation in order to keep it consistent with other entities in the simulator
	 * and returns <tt>true</tt> on success.
	 *
	 * <p>For example, to deactivate the operation in case of a failure of a device that is related to it.
	 *
	 * <p>This method is called during activation of this operation, and {@link OperationImpl#unregisterListeners()} should
	 * be called during deactivation.
	 *
	 * @return <tt>true</tt> if listeners were registered successfully and operation activation should continue
	 * and <tt>false</tt> if an error occurred and you want to stop operation activation
	 */
	protected abstract boolean registerListeners();

	/**
	 * Unregisters and discards all the listeners that were registered using {@link OperationImpl#registeredListener(NotificationListener)}.
	 */
	protected void unregisterListeners() {
		if (this.registeredListeners == null)
			return;
		for (NotificationListener n: this.registeredListeners)
			n.discard();
		this.registeredListeners = null;
	}

	/**
	 * Adds the given listener to an internal list and returns its index.
	 *
	 * <p>Use this method to keep track of newly added listeners in order to automatically unregister them if needed using {@link OperationImpl#unregisterListeners()}.
	 *
	 * @param listener the listener that has been registered.
	 * @return index of the registered listener
	 */
	protected int registeredListener(NotificationListener listener) {
		if (this.registeredListeners == null)
			this.registeredListeners = newArrayList();
		this.registeredListeners.add(listener);
		return this.registeredListeners.size()-1;
	}

	@Override
	public Resource getAllocatedResource() {
		return allocatedResource;
	}

	/**
	 * Updates the allocated resource for this operation.<br/>
	 * A notification {@link NotificationCodes#OPERATION_RESOURCE_CHANGED} is thrown.
	 *
	 * @param resource
	 */
	protected void setAllocatedResource(Resource resource) {
		if (resource == null) {
			if (this.allocatedResource == null)
				return;
		} else if (resource.equals(this.allocatedResource))
			return;

		this.allocatedResource = resource;
		notify(NotificationCodes.OPERATION_RESOURCE_CHANGED, null);
	}
}
