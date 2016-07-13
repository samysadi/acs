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

package com.samysadi.acs.service.checkpointing;

import java.util.List;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.entity.FailureProneEntity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.DispensableEventImpl;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.misc.MemoryUnit;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.checkpoint.Checkpoint;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.NotificationCodes.CheckpointingHandlerDeleteResult;

/**
 * This is an abstract implementation of the {@link CheckpointingHandler} which
 * keeps one checkpoint per registered entity and updates it automatically.<br/>
 * This implementation will also automatically recover a registered {@link RunnableEntity}
 * whenever it fails.<br/>
 * Additionally, if any parent of the {@link MemoryZone} (e.g. the parent {@link Host} or the
 * parent {@link MemoryUnit})) fails, then a new {@link MemoryZone} is selected, and a new
 * checkpoint is created using that {@link MemoryZone}.
 * The created checkpoint is immediately updated.
 *
 * <p>The update frequency for checkpoints is to be defined by sub-classes.
 * In a like manner, sub-classes must define how to select the {@link MemoryZone}
 * where to place checkpoints.
 * Sub-classes also decide where to place recovered entities.
 *
 * <p>If an error happens when trying to update a checkpoint, this implementation
 * will immediately retry to update the checkpoint.
 * If the error persists for a given number of tries (which is to be defined by subclasses),
 * then this implementation will give up the update process, and it unregisters
 * the parent RunnableEntity.
 * Applicable notifications are also thrown (see {@link CheckpointingHandler}).
 *
 * <p>Same logic applies for the recovery process. If this implementation
 * fails to recover the state of a runnable entity, it will immediately retry.
 * After retrying for a given number of tries (which is to be defined by subclasses),
 * then this implementation will give up the recovery process, and it unregisters
 * the parent {@link RunnableEntity}.
 * Applicable notifications are also thrown (see {@link CheckpointingHandler}).
 *
 * @since 1.2
 */
public abstract class CheckpointingHandlerAbstract<E extends RunnableEntity, C extends Checkpoint<E,?>> extends EntityImpl implements CheckpointingHandler<E,C> {
	private static final Object PROP_CHECKPOINT_NOT_AUTO_CHECKPOINT = new Object();
	private static final Object PROP_CHECKPOINT_TO_BE_DELETED = new Object();

	protected static class ProcessStatus {
		boolean canceled;

		public ProcessStatus(boolean canceled) {
			super();
			this.canceled = canceled;
		}

		public boolean isCanceled() {
			return canceled;
		}

		public void setCanceled(boolean canceled) {
			this.canceled = canceled;
		}
	}

	/**
	 * Data kept in each RunnableEntity when it is registered
	 */
	protected static class CheckpointingHandlerEntityData<E extends RunnableEntity, C extends Checkpoint<E,?>> {
		/**
		 * Counter for number of update/recover errors
		 */
		public int errorCount = 0;
		/**
		 * Last checkpoint that was updated successfully
		 */
		public C lastUpdatedCheckpoint = null;
		/**
		 * Listener for last checkpoint that was updated
		 */
		public NotificationListener lastUpdatedCheckpointValidityListener = null;
		/**
		 * Flag used to test if we are updating or recovering
		 */
		public boolean updating = true;

		public List<C> deleteLaterList = null;

		/**
		 * The next auto update event, or the next auto recover event.
		 * Or current checkpoint being used for updating or recovering.
		 * Or step info
		 */
		private Object processInfo = null;

		public Event getEvent() {
			if (processInfo instanceof Event)
				return (Event) processInfo;
			return null;
		}

		@SuppressWarnings("unchecked")
		public C getCurrentCheckpoint() {
			if (processInfo instanceof Checkpoint)
				return (C) processInfo;
			return null;
		}

		public ProcessStatus getProcessStatus() {
			if (processInfo instanceof ProcessStatus)
				return (ProcessStatus) processInfo;
			return null;
		}

		public void setProcessInformation(Event event, C currentCheckpoint, ProcessStatus processStatus) {
			if (event != null) {
				processInfo = event;
				//either event or currentCheckpoint should be null
				if (currentCheckpoint != null || processStatus != null)
					throw new IllegalArgumentException("Either event or currentCheckpoint or stepInfo must be null");
			} else if (currentCheckpoint != null) {
				processInfo = currentCheckpoint;
				//either event or currentCheckpoint should be null
				if (processStatus != null)
					throw new IllegalArgumentException("Either event or currentCheckpoint or stepInfo must be null");
			} else {
				processInfo = processStatus;
			}
		}

		/**
		 *
		 * @return <tt>true</tt> if there is an active update or recovery process (i.e. either event or currentCheckpoint is not null)
		 */
		public boolean isProcessActive() {
			return processInfo != null;
		}
	}

	/**
	 * Main listener used for detecting failures, and checkpoints' state
	 */
	private NotificationListener mainListener;

	public CheckpointingHandlerAbstract() {
		super();
	}

	@Override
	public CheckpointingHandlerAbstract<E,C> clone() {
		@SuppressWarnings("unchecked")
		final CheckpointingHandlerAbstract<E,C> clone = (CheckpointingHandlerAbstract<E,C>) super.clone();
		return clone;
	}

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.mainListener = null;
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

	@SuppressWarnings("unchecked")
	protected CheckpointingHandlerEntityData<E,C> getCheckpointingHandlerEntityData(E entity) {
		//we need to use an inaccessible object as key to avoid any conflict
		//	mainListener is perfect for this, and we avoid to keep an extra field
		final Object PROP_KEY = getMainListener();

		return (CheckpointingHandlerEntityData<E,C>) entity.getProperty(PROP_KEY);
	}

	protected void setCheckpointingHandlerEntityData(E entity, CheckpointingHandlerEntityData<E,C> d) {
		//See comments above (in getCheckpointingHandlerEntityData )
		final Object PROP_KEY = getMainListener();

		if (d == null)
			entity.unsetProperty(PROP_KEY);
		else
			entity.setProperty(PROP_KEY, d);
	}

	@Override
	public final boolean isRegistered(E entity) {
		return getCheckpointingHandlerEntityData(entity) != null;
	}

	@Override
	public void register(E entity, Config checkpointConfig) {
		if (isRegistered(entity))
			throw new IllegalArgumentException("The given entity is already registered using this CheckpointingHandler.");

		//add failure listener
		entity.addListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getMainListener());

		//mark entity as being registered
		setCheckpointingHandlerEntityData(entity, new CheckpointingHandlerEntityData<E, C>());

		//schedule auto update event
		scheduleAutoUpdate(entity, checkpointConfig, 0l);

		//throw registered notification
		CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_REGISTERED, entity);
	}

	/**
	 * This method is called before using a cloned entity for the
	 * recovery process, in order to do any necessary cleanup.
	 *
	 * @param entity
	 */
	protected void cleanupEntityForRecovery(E entity) {
		//remove failure listener
		entity.removeListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getMainListener());

		//mark entity as being unregistered, as the checkpointingHandlerEntityData is also cloned
		setCheckpointingHandlerEntityData(entity, null);
	}

	@Override
	public void unregister(E entity) {
		if (!isRegistered(entity))
			throw new IllegalArgumentException("The given entity is not registered using this CheckpointingHandler.");

		//remove failure listener
		entity.removeListener(NotificationCodes.RUNNABLE_STATE_CHANGED, getMainListener());

		//get data
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);

		//cancel update/recover process
		if (entityData.isProcessActive())
			cancelActiveProcess(entity);

		//remove validity listeners on last updated checkpoint
		removeCheckpointValidityListeners(entity);

		//mark entity as being unregistered
		setCheckpointingHandlerEntityData(entity, null);

		//delete automatically created checkpoints
		for (Entity _e: getCheckpoints(entity)) {
			if (!(_e instanceof Checkpoint<?, ?>))
				continue;
			Checkpoint<?,?> _c = (Checkpoint<?,?>) _e;
			if (_c.getCheckpointingHandler() != this)
				continue;
			@SuppressWarnings("unchecked")
			C c = (C) _c;
			if (!isAutomaticCheckpoint(c))
				continue;
			deleteCheckpoint(c);
		}

		//throw unregister notification
		CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_UNREGISTERED, entity);
	}

	/*
	 * MainListener
	 * ########################################################################
	 */

	private NotificationListener getMainListener() {
		if (this.mainListener == null) {
			this.mainListener = new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					if (notification_code == NotificationCodes.CHECKPOINT_UPDATE_SUCCESS) {
						@SuppressWarnings("unchecked")
						C c = (C) notifier;

						c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, this);
						c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, this);

						CheckpointingHandlerAbstract.this.afterAutoUpdate(c);
					} else if (notification_code == NotificationCodes.CHECKPOINT_UPDATE_ERROR) {
						@SuppressWarnings("unchecked")
						C c = (C) notifier;

						c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, this);
						c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, this);

						CheckpointingHandlerAbstract.this.afterAutoUpdateError(c.getParent(), c.getConfig());
					} else if (notification_code == NotificationCodes.CHECKPOINT_RECOVER_SUCCESS) {
						@SuppressWarnings("unchecked")
						C c = (C) notifier;

						@SuppressWarnings("unchecked")
						E newEntity = (E) data;

						c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, this);
						c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, this);

						afterAutoRecover(c, newEntity);
					} else if (notification_code == NotificationCodes.CHECKPOINT_RECOVER_ERROR) {
						@SuppressWarnings("unchecked")
						C c = (C) notifier;
						E entity = c.getParent();

						c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, this);
						c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, this);

						afterAutoRecoverError(entity);
					} else if (notification_code == NotificationCodes.CHECKPOINT_STATE_CHANGED) {
						@SuppressWarnings("unchecked")
						C c = (C) notifier;
						E entity = c.getParent();

						CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
						if (entityData == null) {
							//the entity is not registered !?
							c.removeListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, this);
							return;
						}

						if (entityData.updating) {
							if (c.isCheckpointBusy())
								return;

							c.removeListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, this);

							if (!_performAutoUpdate(c)) {
								Config checkpointConfig = c.getConfig();
								CheckpointingHandlerAbstract.this.afterAutoUpdateError(entity, checkpointConfig);
							}
						} else {
							if (c.isCheckpointStateSet(Checkpoint.CHECKPOINT_STATE_UPDATING))
								return;

							c.removeListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, this);

							if (!_performAutoRecover(c)) {
								CheckpointingHandlerAbstract.this.afterAutoRecoverError(entity);
							}
						}
					} else if (notification_code == NotificationCodes.RUNNABLE_STATE_CHANGED) {
						@SuppressWarnings("unchecked")
						E entity = (E) notifier;

						if (entity.getRunnableState() == RunnableState.FAILED) {
							scheduleAutoRecover(entity);
						}
					}
				}
			};
		}
		return this.mainListener;
	}

	/*
	 * Update methods
	 * ########################################################################
	 */

	@Override
	public void takeCheckpoint(final E entity, Config checkpointConfig) {
		generateCheckpoint(entity, checkpointConfig, new _CHMethodReturn<C>() {
			@Override
			public void run(C c) {
				E entity = c.getParent();

				c.setProperty(PROP_CHECKPOINT_NOT_AUTO_CHECKPOINT, Boolean.TRUE);

				if (!c.canUpdate()) {
					deleteCheckpoint(c);
					CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_ERROR, entity);
					return;
				}

				CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_SUCCESS, c);
				c.update();
			}
		}, new _CHMethodReturnSimple() {
			@Override
			public void run() {
				CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_ERROR, entity);
			}
		});
	}

	/**
	 * Updates the checkpoint, and registers notification listeners.
	 *
	 * <p>If the update is not possible then this method returns <tt>false</tt>.
	 *
	 * @param c
	 * @return <tt>true</tt> if everything's fine, <tt>false</tt> if the update is not possible
	 */
	private boolean _performAutoUpdate(C c) {
		if (!c.canUpdate()) {
			//already updating ?
			if (c.isCheckpointStateSet(Checkpoint.CHECKPOINT_STATE_UPDATING)) {
				c.addListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, getMainListener());
				c.addListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, getMainListener());
				return true;
			}
			//if checkpoint is busy then wait
			if (c.isCheckpointBusy()) {
				//wait for the current operation to finish (to fail or to succeed, doesn't matter), and then retry
				c.addListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, getMainListener());
				return true;
			}
			//otherwise fail
			return false;
		}
		c.addListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, getMainListener());
		c.addListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, getMainListener());
		c.update();
		return true;
	}

	/**
	 * Schedules an automatic checkpoint update for the given entity.
	 *
	 * <p>This method should create a checkpoint (if necessary) and run
	 * a {@link Checkpoint#update()} at a given moment in the simulation.
	 *
	 * @param entity
	 */
	protected void scheduleAutoUpdate(final E entity, final Config checkpointConfig, long delay) {
		if (entity.isTerminated())
			return;

		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		//There is another running auto update / recovery ?
		if (entityData.isProcessActive())
			return;

		entityData.updating = true;

		//create update event
		Event event = new DispensableEventImpl() {
			@Override
			public void process() {
				CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
				if (entityData == null) {
					//the entity is not registered !?
					return;
				}

				C c = selectCheckpointForUpdate(entity);

				if (c != null) {
					entityData.setProcessInformation(null, c, null);
					if (!_performAutoUpdate(c)) {
						Config checkpointConfig = c.getConfig();
						CheckpointingHandlerAbstract.this.afterAutoUpdateError(entity, checkpointConfig);
					}
				} else {
					final ProcessStatus canceled = new ProcessStatus(false);
					entityData.setProcessInformation(null, null, canceled);
					//try to create the checkpoint
					generateCheckpoint(entity, checkpointConfig, new _CHMethodReturn<C>() {
						@Override
						public void run(C c) {
							if (canceled.isCanceled())
								return;

							E entity = c.getParent();
							CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_SUCCESS, c);

							CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
							entityData.setProcessInformation(null, c, null);

							if (!_performAutoUpdate(c)) {
								deleteCheckpoint(c);
								Config checkpointConfig = c.getConfig();
								CheckpointingHandlerAbstract.this.afterAutoUpdateError(entity, checkpointConfig);
							}
						}
					}, new _CHMethodReturnSimple() {
						@Override
						public void run() {
							if (canceled.isCanceled())
								return;

							CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_ERROR, entity);
							CheckpointingHandlerAbstract.this.afterAutoUpdateError(entity, checkpointConfig);
						}
					});
				}
			}
		};
		entityData.setProcessInformation(event, null, null);

		Simulator.getSimulator().schedule(delay, event);
	}

	/**
	 * This method is called after a successful checkpoint update.
	 *
	 * <p>Before returning, this method schedules another update and
	 * throws a {@link NotificationCodes#CHECKPOINTINGHANDLER_AUTOUPDATE_SUCCESS}
	 * notification.
	 *
	 * @param entity
	 */
	protected void afterAutoUpdate(C c) {
		E entity = c.getParent();
		if (entity == null) {
			//checkpoint deleted!?
			return;
		}
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		//auto update finished
		entityData.setProcessInformation(null, null, null);

		//check delete checkpoints
		checkDeleteLater(entityData);

		entityData.errorCount = 0;

		//remove validity listeners on the old checkpoint
		removeCheckpointValidityListeners(entity);

		//define new last updated checkpoint
		entityData.lastUpdatedCheckpoint = c;

		//add validity listeners
		addCheckpointValidityListeners(entity);

		//request next update
		long delay;
		if (!entityData.lastUpdatedCheckpoint.isCheckpointBusy() && !entityData.lastUpdatedCheckpoint.canRecover(null, null))
			delay = 0l;
		else
			delay = getDelayBeforeNextAutoUpdate(entity);
		scheduleAutoUpdate(entity, c.getConfig(), delay);

		//notify
		CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_AUTOUPDATE_SUCCESS, c);
	}

	/**
	 * This method is called after an error happens during checkpoint update.
	 *
	 * @param entity the entity for which the auto update failed
	 * @param checkpointConfig the checkpoint config to be used when retrying to update
	 */
	protected void afterAutoUpdateError(E entity, Config checkpointConfig) {
		if (entity == null) {
			//checkpoint deleted!?
			return;
		}
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		//auto update finished
		entityData.setProcessInformation(null, null, null);

		//check delete checkpoints
		checkDeleteLater(entityData);

		//notify
		CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_AUTOUPDATE_ERROR, entity);

		//check error count and unregister the entity if necessary
		entityData.errorCount++;
		if (entityData.errorCount >= CheckpointingHandlerAbstract.this.getUpdateErrorsThreshold(entity)) {
			unregister(entity);
		} else {
			scheduleAutoUpdate(entity, checkpointConfig, 0l);
		}
	}

	/*
	 * Recover methods
	 * ########################################################################
	 */

	@Override
	public void recover(final E entity) {
		C c = selectCheckpointForRecovery(entity);
		if (c != null) {
			recover(c);
		} else {
			CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_ERROR, entity);
		}
	}

	@Override
	public void recover(final C c) {
		if (!recoverUsingCheckpoint(c)) {
			CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_ERROR, c.getParent());
		} else {
			CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_SUCCESS, c);
		}
	}

	/**
	 * Uses the checkpoint for recovery
	 *
	 * <p>If the recovery is not possible then try to reschedule the recovery and
	 * return immediately.
	 *
	 * @param c
	 * @return <tt>true</tt> if everything's fine, <tt>false</tt> if the recovery is not possible
	 */
	private boolean _performAutoRecover(C c) {
		if (!recoverUsingCheckpoint(c)) {
			//if checkpoint is busy then wait
			if (c.isCheckpointBusy()) {
				//wait for the current operation to finish (to fail or to succeed, doesn't matter), and then retry
				c.addListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, getMainListener());
				return true;
			}
			//otherwise fail
			return false;
		}

		//success, let's add listeners to c, the recover method is already called
		c.addListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, getMainListener());
		c.addListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, getMainListener());

		return true;
	}

	/**
	 * Schedules an automatic recovery for the given entity.
	 *
	 * @param entity
	 */
	protected void scheduleAutoRecover(final E entity) {
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		//There is another running auto update / recovery ?
		if (entityData.isProcessActive()) {
			//if it's a recovery then return
			if (!entityData.updating)
				return;
			//otherwise cancel ongoing auto update
			cancelActiveProcess(entity);
		}

		entityData.updating = false;

		//create update event
		Event event = new EventImpl() {
			@Override
			public void process() {
				CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
				if (entityData == null) {
					//the entity is not registered !?
					return;
				}

				C c = selectCheckpointForRecovery(entity);

				if (c != null) {
					CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_SUCCESS, c);
					entityData.setProcessInformation(null, c, null);
					if (!_performAutoRecover(c)) {
						CheckpointingHandlerAbstract.this.afterAutoRecoverError(entity);
					}
				} else {
					CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_ERROR, entity);
					CheckpointingHandlerAbstract.this.afterAutoRecoverError(entity);
				}
			}
		};
		entityData.setProcessInformation(event, null, null);

		//schedule the event
		Simulator.getSimulator().schedule(event);
	}

	/**
	 * This method is called after a successful checkpoint recovery.
	 *
	 * <p>Before returning, this method schedules another update and
	 * throws a {@link NotificationCodes#CHECKPOINTINGHANDLER_AUTOUPDATE_SUCCESS}
	 * notification.
	 *
	 * @param entity
	 */
	protected void afterAutoRecover(C c, E newEntity) {
		E entity = c.getParent();
		if (entity == null) {
			//checkpoint deleted!?
			return;
		}
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		//auto recover finished
		entityData.setProcessInformation(null, null, null);

		//check delete checkpoints
		checkDeleteLater(entityData);

		entityData.errorCount = 0;

		//unregister failed entity from auto update
		unregister(entity);

		//properties of the newEntity have been cloned from the original entity, so let's cleanup
		cleanupEntityForRecovery(newEntity);

		//start the new entity
		if (newEntity.canStart())
			newEntity.doStart();

		//register new entity
		register(newEntity, c.getConfig());

		CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_AUTORECOVER_SUCCESS,
				new NotificationCodes.CheckpointingHandlerRecoverResult<E, C>(entity, c, newEntity));
	}

	/**
	 * This method is called after an error happens during the recovery process.
	 *
	 * @param entity the entity for which the auto recovery failed
	 */
	protected void afterAutoRecoverError(E entity) {
		if (entity == null) {
			//checkpoint deleted!?
			return;
		}
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		//auto recover finished
		entityData.setProcessInformation(null, null, null);

		//check delete checkpoints
		checkDeleteLater(entityData);

		//notify
		CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_AUTORECOVER_ERROR, entity);

		//check error count and unregister the entity if necessary
		entityData.errorCount++;
		if (entityData.errorCount >= CheckpointingHandlerAbstract.this.getRecoverErrorsThreshold(entity)) {
			unregister(entity);
		} else {
			scheduleAutoRecover(entity);
		}
	}



	/*
	 * Delete method
	 * ########################################################################
	 */

	private void checkDeleteLater(CheckpointingHandlerEntityData<E, C> entityData) {
		List<C> l = entityData.deleteLaterList;
		if (l == null)
			return;
		entityData.deleteLaterList = null;

		for (C c: l)
			_deleteCheckpoint(c);
	}

	private void _deleteCheckpoint(final C c) {
		E entity = c.getParent();
		if (entity == null)
			return; // already deleted?

		NotificationListener l = new NotificationListener() {
			@Override
			protected void notificationPerformed(Notifier notifier,
					int notification_code, Object data) {
				this.discard();

				c.removeListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, this);

				_deleteCheckpoint(c);
			}
		};

		if (c.isCheckpointBusy()) {
			c.addListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, l);
		} else {
			CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
			if (entityData != null && entityData.isProcessActive()) {
				if (entityData.deleteLaterList == null)
					entityData.deleteLaterList = newArrayList();
				entityData.deleteLaterList.add(c);
			} else {
				CheckpointingHandlerDeleteResult<E, C> nData = new NotificationCodes.CheckpointingHandlerDeleteResult<E, C>(c.getParent(), c);
				CheckpointingHandlerAbstract.this.freeCheckpoint(c);
				CheckpointingHandlerAbstract.this.notify(NotificationCodes.CHECKPOINTINGHANDLER_CHECKPOINT_DELETED, nData);
			}
		}
	}

	@Override
	public void deleteCheckpoint(final C c) {
		if (c.getCheckpointingHandler() != this)
			throw new IllegalArgumentException("The checkpoint was not created using this CheckpointingHandler");

		if (isCheckpointMarkedToBeDeleted(c))
			return;

		c.setProperty(PROP_CHECKPOINT_TO_BE_DELETED, Boolean.TRUE);

		Event e = new DispensableEventImpl() {
			@Override
			public void process() {
				c.unsetProperty(PROP_CHECKPOINT_TO_BE_DELETED);
				_deleteCheckpoint(c);
			}
		};
		Simulator.getSimulator().schedule(e);
	}

	/*
	 * Other methods
	 * ########################################################################
	 */

	/**
	 * Cancels the ongoing auto update / auto recover process.
	 *
	 * <p>If there is no ongoing process, then nothing is done.
	 *
	 * @param entity
	 */
	protected void cancelActiveProcess(E entity) {
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//the entity is not registered !?
			return;
		}

		if (!entityData.isProcessActive())
			return;

		Event event = entityData.getEvent();
		if (event != null) {
			event.cancel();
		} else {
			C c = entityData.getCurrentCheckpoint();
			if (c != null) {
				if (entityData.updating) {
					c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_SUCCESS, getMainListener());
					c.removeListener(NotificationCodes.CHECKPOINT_UPDATE_ERROR, getMainListener());
				} else {
					c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_SUCCESS, getMainListener());
					c.removeListener(NotificationCodes.CHECKPOINT_RECOVER_ERROR, getMainListener());
				}
				c.removeListener(NotificationCodes.CHECKPOINT_STATE_CHANGED, getMainListener());
			} else {
				ProcessStatus b = entityData.getProcessStatus();
				if (b != null) {
					b.setCanceled(true);
				}
			}
		}

		entityData.setProcessInformation(null, null, null);
	}

	private NotificationListener getCheckpointValidityListener(final C c) {
		return new NotificationListener() {
				@Override
				protected void notificationPerformed(Notifier notifier,
						int notification_code, Object data) {
					this.discard();

					E entity = c.getParent();
					if (entity == null)
						return;
					removeCheckpointValidityListeners(entity);

					CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
					if (entityData == null) {
						//not registered !?
						return;
					}
					if (entityData.lastUpdatedCheckpoint != c)
						return;

					if (!c.canRecover(null, null)) {
						//c is not valid anymore
						deleteCheckpoint(c);

						scheduleAutoUpdate(entity, c.getConfig(), 0l);
					} else {
						//re-add listener which might be added to other entities too
						addCheckpointValidityListeners(entity);
					}
				}
			};
	}

	private void removeCheckpointValidityListeners(E entity) {
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//not registered !?
			return;
		}

		if (entityData.lastUpdatedCheckpointValidityListener == null)
			return;
		entityData.lastUpdatedCheckpointValidityListener.discard();
		NotificationListener l = entityData.lastUpdatedCheckpointValidityListener;
		entityData.lastUpdatedCheckpointValidityListener = null;

		if (entityData.lastUpdatedCheckpoint == null)
			return;

		MemoryZone z = entityData.lastUpdatedCheckpoint.getMemoryZone();
		if (z == null)
			return;
		z.removeListener(NotificationCodes.ENTITY_PARENT_CHANGED, l);
		z.removeListener(NotificationCodes.ENTITY_ANCESTOR_CHANGED, l);

		Entity p = z;
		while (((p = p.getParent()) != null)) {
			if (p instanceof FailureProneEntity)
				p.removeListener(NotificationCodes.FAILURE_STATE_CHANGED, l);
		}
	}

	private void addCheckpointValidityListeners(E entity) {
		CheckpointingHandlerEntityData<E, C> entityData = getCheckpointingHandlerEntityData(entity);
		if (entityData == null) {
			//not registered !?
			return;
		}

		removeCheckpointValidityListeners(entity);

		if (entityData.lastUpdatedCheckpoint == null)
			return;

		C c = entityData.lastUpdatedCheckpoint;

		MemoryZone z = c.getMemoryZone();
		if (z == null)
			return;

		NotificationListener l = getCheckpointValidityListener(c);
		entityData.lastUpdatedCheckpointValidityListener = l;

		z.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, l);
		z.addListener(NotificationCodes.ENTITY_ANCESTOR_CHANGED, l);

		Entity p = z;
		while (((p = p.getParent()) != null)) {
			if (p instanceof FailureProneEntity)
				p.addListener(NotificationCodes.FAILURE_STATE_CHANGED, l);
		}
	}

	/**
	 * Returns <tt>true</tt> if the checkpoint was automatically created
	 * by current {@link CheckpointingHandler},
	 * <tt>false</tt> otherwise.
	 *
	 * @param checkpoint
	 * @return <tt>true</tt> if the checkpoint was automatically created
	 * by current {@link CheckpointingHandler},
	 * <tt>false</tt> otherwise.
	 */
	protected boolean isAutomaticCheckpoint(C checkpoint) {
		if (checkpoint.getCheckpointingHandler() != this)
			return false;
		Object v = checkpoint.getProperty(PROP_CHECKPOINT_NOT_AUTO_CHECKPOINT);
		return (v == null);
	}

	/**
	 * Returns <tt>true</tt> if a delete event is scheduled for the checkpoint,
	 * <tt>false</tt> otherwise.
	 *
	 * @param checkpoint
	 * @return <tt>true</tt> if a delete event is scheduled for the checkpoint,
	 * <tt>false</tt> otherwise.
	 */
	protected boolean isCheckpointMarkedToBeDeleted(C checkpoint) {
		Object v = checkpoint.getProperty(PROP_CHECKPOINT_TO_BE_DELETED);
		return (v != null);
	}

	/**
	 * This method returns a list containing all (but not exclusively) checkpoints of
	 * the given <tt>entity</tt>.
	 *
	 * <p>By default this method returns a list containing
	 * allY children entities of the given <tt>entity</tt>.
	 * You should override this method to only return checkpoints.
	 *
	 * @param entity
	 * @return a list containing all (but not exclusively) checkpoints of
	 * the given <tt>entity</tt>
	 */
	protected List<? extends Entity> getCheckpoints(E entity) {
		return entity.getEntities();
	}

	/*
	 * Abstract methods
	 * ########################################################################
	 */

	protected static interface _CHMethodReturnSimple {
		public void run();
	}

	protected static interface _CHMethodReturn<C extends Checkpoint<?,?>> {
		public void run(C c);
	}

	/**
	 * Creates a Checkpoint, sets its parent and configuration, and
	 * allocates any necessary resources for it (including its {@link MemoryZone}).
	 *
	 * <p>Current {@link CheckpointingHandler} is set for the checkpoint (see {@link Checkpoint#setCheckpointingHandler(CheckpointingHandler)}).
	 *
	 * @param entity
	 */
	protected abstract void generateCheckpoint(E entity, Config checkpointConfig, _CHMethodReturn<C> success, _CHMethodReturnSimple error);

	/**
	 * This methods frees any resources which were previously allocated for the given checkpoint
	 * by the {@link #generateCheckpoint(RunnableEntity, Config, _CHMethodReturn, _CHMethodReturnSimple)}
	 * method.
	 *
	 * <p>The checkpoint is then deleted, and a <tt>null</tt> parent is set for the checkpoint.
	 *
	 * @param c
	 */
	protected abstract void freeCheckpoint(C c);

	/**
	 * Selects an existing checkpoint to be used for an automatic update.
	 *
	 * <p>Implementation must ensure that the returned checkpoint is
	 * an automatic checkpoint ({@link #isAutomaticCheckpoint(Checkpoint)}) and that
	 * it was not scheduled for deletion ({@link #isCheckpointMarkedToBeDeleted(Checkpoint)}).
	 *
	 * @param entity the parent entity of the checkpoint to be selected
	 *
	 * @return the selected checkpoint or <tt>null</tt> if no checkpoint was found
	 */
	protected abstract C selectCheckpointForUpdate(E entity);

	/**
	 * Selects an existing checkpoint to be used for recovery.
	 *
	 * <p>Implementation must ensure that the returned checkpoint is
	 * an automatic checkpoint ({@link #isAutomaticCheckpoint(Checkpoint)}) and that
	 * it was not scheduled for deletion ({@link #isCheckpointMarkedToBeDeleted(Checkpoint)}).
	 *
	 * @param entity the parent entity of the checkpoint to be selected
	 *
	 * @return the selected checkpoint or <tt>null</tt> if no checkpoint was found
	 */
	protected abstract C selectCheckpointForRecovery(E entity);

	/**
	 * Recovers using the given checkpoint.
	 *
	 * <p>Implementations decide where to place the new entity after recovery, and whether it
	 * should replace another entity.
	 *
	 * @param c
	 * @result <tt>true</tt> if the recover method was called successfully
	 */
	protected abstract boolean recoverUsingCheckpoint(C c);

	/**
	 * Returns the delay before next automatic update for the given entity
	 *
	 * @param entity the entity for which to compute the update delay
	 * @return the delay before next automatic update for the given entity
	 */
	protected abstract long getDelayBeforeNextAutoUpdate(E entity);

	/**
	 * Returns the number of errors before aborting the auto update process.
	 *
	 * @param entity
	 * @return the number of errors before aborting the auto update process
	 */
	protected abstract int getUpdateErrorsThreshold(E entity);

	/**
	 * Returns the number of errors before aborting the auto recover process.
	 *
	 * @param entity
	 * @return the number of errors before aborting the auto recover process
	 */
	protected abstract int getRecoverErrorsThreshold(E entity);
}
