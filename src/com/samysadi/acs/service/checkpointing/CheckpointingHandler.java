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

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.checkpoint.Checkpoint;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * This interface defines methods to register a {@link RunnableEntity} for automatic checkpointing and
 * recovery.
 *
 * <p>Once an entity is registered, common implementations will regularly create a checkpoint
 * for the entity.
 * Then once the entity fails, the entity is automatically recovered.
 *
 * <p>It is left to implementations to define how many checkpoints are
 * created for each registered entity, and at which frequency they are created.<br/>
 * They should also define their own policies to select the right checkpoint to use during
 * the recovery process.
 *
 * <p>Finally, the creation (or the selection) of the {@link MemoryZone} to use
 * to save a checkpoint is also left to implementations.<br/>
 * In a like manner, during recovery, and after that a new {@link RunnableEntity} is created,
 * it is left to implementations to define its parent.
 *
 * @param <E> the type of the {@link RunnableEntity} for which the checkpoints are created
 * @param <C> the type of the {@link Checkpoint} which is created
 *
 * @since 1.2
 */
public interface CheckpointingHandler<E extends RunnableEntity, C extends Checkpoint<E,?>> extends Entity {

	@Override
	public CheckpointingHandler<E, C> clone();

	@Override
	public CloudProvider getParent();

	/**
	 * Registers the given <tt>entity</tt>, and enables automatic checkpointing and recovery for it.
	 *
	 * <p>The frequency of the checkpointing is defined by the implementation.
	 * In a like manner, the choice of the backup {@link MemoryZone} to use for saving
	 * the created checkpoints is implementation-specific.
	 * It is also left to implementations to the chose where
	 * a recovered entity is to be placed.
	 *
	 * <p>A {@link NotificationCodes#CHECKPOINTINGHANDLER_REGISTERED} notification is thrown after that
	 * the given <tt>entity</tt> is registered.<br/>
	 * Additionally, each time a checkpoint is created for a registered entity, then
	 * a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_SUCCESS} notification
	 * is thrown.
	 * And if an error happens, then a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_ERROR}
	 * notification is thrown.
	 *
	 * <p>Each time a checkpoint is automatically updated, a
	 * {@link NotificationCodes#CHECKPOINTINGHANDLER_AUTOUPDATE_SUCCESS} or a
	 * {@link NotificationCodes#CHECKPOINTINGHANDLER_AUTOUPDATE_ERROR} notification is thrown.
	 *
	 * <p>Each time a checkpoint is used for recovery, a
	 * {@link NotificationCodes#CHECKPOINTINGHANDLER_AUTORECOVER_SUCCESS} or a
	 * {@link NotificationCodes#CHECKPOINTINGHANDLER_AUTORECOVER_ERROR} notification is thrown.
	 *
	 * <p>If after a given set of tries, this handler fails to update (or to recover)
	 * a given entity, then the automatic update process and recovery process are aborted
	 * for that entity.
	 * Additionally, the entity is automatically unregistered (see {@link #unregister(RunnableEntity)}).
	 *
	 * <p>After that a checkpoint is successfully used for recovery, the <tt>entity</tt>
	 * that has failed is unregistered (see {@link #unregister(RunnableEntity)}) and
	 * the newly recovered entity is registered using this same method.
	 *
	 * @param entity the entity to register for automatic checkpointing/recovery
	 * @param checkpointConfig the configuration which is assigned to checkpoints when creating them (can be <tt>null</tt>, in which case the CheckpointingHandler's config is used)
	 * @throws IllegalArgumentException if the given <tt>entity</tt> is already registered
	 */
	public void register(E entity, Config checkpointConfig);

	/**
	 * Unregisters the given <tt>entity</tt>.<br/>
	 * No further automatic checkpointing or automatic recovery is performed.
	 *
	 * <p>All previously automatically created checkpoints by current {@link CheckpointingHandler} are deleted
	 * using the {@link CheckpointingHandler#deleteCheckpoint(Checkpoint)} method.<br/>
	 * Checkpoints created using {@link #takeCheckpoint(RunnableEntity, Config)}
	 * are not deleted.
	 *
	 * <p>A {@link NotificationCodes#CHECKPOINTINGHANDLER_UNREGISTERED} notification is thrown
	 * after that the given <tt>entity</tt> is unregistered.
	 *
	 * @param entity
	 * @throws IllegalArgumentException if the given <tt>entity</tt> is not registered
	 */
	public void unregister(E entity);

	/**
	 * Returns <tt>true</tt> if the given <tt>entity</tt> has been registered
	 * using this {@link CheckpointingHandler}.
	 *
	 * @param entity
	 * @return <tt>true</tt> if the given <tt>entity</tt> has been registered
	 */
	public boolean isRegistered(E entity);

	/**
	 * Creates a checkpoint for the given entity and then updates it.
	 *
	 * <p>After that the checkpoint is created, a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_SUCCESS} notification
	 * is thrown.
	 * And if an error happens, then a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_CREATION_ERROR} notification
	 * is thrown.
	 *
	 * <p>The {@link Checkpoint#update()} method is called as soon as possible on the created checkpoint.
	 * However, you should listen to appropriate notifications on the created checkpoint to
	 * know when the checkpoint is updated (and if the update is successful).
	 *
	 * @param entity the entity for which a checkpoint is created
	 * @param checkpointConfig the configuration which is assigned to checkpoints when
	 * creating them (can be <tt>null</tt>, in which case the {@link CheckpointingHandler}'s config is used)
	 */
	public void takeCheckpoint(E entity, Config checkpointConfig);

	/**
	 * Uses the current {@link CheckpointingHandler} policies to recover the <tt>entity</tt>
	 * using one of its checkpoints.
	 *
	 * <p>After that a checkpoint is found for recovery, a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_SUCCESS} notification
	 * is thrown.
	 * And if an error happens, then a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_ERROR} notification
	 * is thrown.
	 *
	 * <p>Usually, the checkpoint that is used for recovery is the most recent checkpoint.
	 * However, nothing guarantees this behavior and it is left to implementations to chose
	 * the right checkpoint.
	 *
	 * <p>The {@link Checkpoint#recover(Entity, RunnableEntity)} method is called as soon as possible on the chosen checkpoint.
	 * However, you should listen to appropriate notifications on the chosen checkpoint to
	 * know when the recovery process is finished (and if the recovery is successful).
	 *
	 * <p>It is left to implementations to chose where the recovered entity is to be placed.
	 *
	 * @param entity
	 */
	public void recover(E entity);

	/**
	 * Uses the current {@link CheckpointingHandler} policies to recover an entity
	 * using the given <tt>checkpoint</tt>.
	 *
	 * <p>If the given checkpoint can be used for recovery, then a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_SUCCESS} notification
	 * is thrown.
	 * And if it cannot be used, then a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_SELECTION_ERROR} notification
	 * is thrown.
	 *
	 * <p>The {@link Checkpoint#recover(Entity, RunnableEntity)} method is called as soon as possible on the
	 * given <tt>checkpoint</tt>.
	 * However, you should listen to appropriate notifications on the given <tt>checkpoint</tt> to
	 * know when the recovery process is finished (and if the recovery is successful).
	 *
	 * <p>It is left to implementations to chose where the recovered entity is to be placed.
	 *
	 * @param checkpoint
	 */
	public void recover(C checkpoint);

	/**
	 * Schedules a delete event for the given checkpoint.
	 *
	 * <p>This method, on top of calling {@link Checkpoint#delete()} on the checkpoint, will
	 * free any resources allocated by the current checkpointing handler.
	 * Thus, the {@link MemoryZone} used by the checkpoint is unplaced if it was
	 * placed by the current {@link CheckpointingHandler}.
	 * Finally, the <tt>null</tt> parent is also set for the deleted checkpoint.
	 *
	 * <p>If the checkpoint is busy (e.g. updating) then it cannot be deleted immediately,
	 * and an event is scheduled to delete the checkpoint as soon as the checkpoint is not busy.
	 *
	 * <p>After that the checkpoint is deleted, a {@link NotificationCodes#CHECKPOINTINGHANDLER_CHECKPOINT_DELETED} notification
	 * is thrown.
	 *
	 * @param c
	 * @throws IllegalArgumentException if the given checkpoint was not created using this {@link CheckpointingHandler}
	 */
	public void deleteCheckpoint(C c);
}
