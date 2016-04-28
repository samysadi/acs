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
package com.samysadi.acs.service.checkpointing.checkpoint;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.UncloneableEntity;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.service.checkpointing.CheckpointingHandler;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * A checkpoint is a snapshot of the running state of a {@link RunnableEntity} at a given
 * moment of the simulation.<br/>
 * After that a checkpoint is created and updated, it can then be used at any other moment of the simulation
 * to create a copy of the {@link RunnableEntity} which was used to update the checkpoint.
 * The copy will have the same state as the original {@link RunnableEntity} at the moment when
 * the checkpoint was last updated (see {@link Checkpoint#getCheckpointTime()}).
 *
 * <p>The parent of the checkpoint is the {@link RunnableEntity} for which it was created.
 * The parent is used as the source for updating the checkpoint when the {@link Checkpoint#update()}
 * method is called.
 * You can safely change the parent of the checkpoint as long as the checkpoint is not busy ({@link Checkpoint#isCheckpointBusy()}).
 * The new parent will be used for next updates.
 *
 * <p>A {@link MemoryZone} must be set for each checkpoint.
 * See {@link Checkpoint#setMemoryZone(MemoryZone)} and {@link Checkpoint#getMemoryZone()} for more information.
 *
 * <p>Three main methods are offered by this interface. The {@link Checkpoint#update()},
 * {@link Checkpoint#recover(Entity, RunnableEntity)}, and {@link Checkpoint#copy(MemoryZone)}.<br/>
 * Have a look at their respective documentation for more information.
 *
 * @param <E> the type of the {@link RunnableEntity} for which the checkpoint was created
 * @param <P> the type of the {@link Entity} which is used as a parent for newly created {@link RunnableEntity} during the recovery process
 *
 * @since 1.2
 */
public interface Checkpoint<E extends RunnableEntity, P extends Entity> extends UncloneableEntity {

	/**
	 * The checkpoint is being updated
	 */
	public static final byte CHECKPOINT_STATE_UPDATING		= 0x02;
	/**
	 * The checkpoint is being used for recovery
	 */
	public static final byte CHECKPOINT_STATE_RECOVERING	= 0x04;
	/**
	 * The checkpoint is being copied to another {@link MemoryZone}
	 */
	public static final byte CHECKPOINT_STATE_COPYING		= 0x08;

	@Override
	public E getParent();

	/**
	 * Returns the {@link MemoryZone} which is used by the checkpoint
	 * (i.e. the {@link MemoryZone} where the checkpoint is saved).
	 *
	 * @return the {@link MemoryZone} where the checkpoint is saved
	 */
	public MemoryZone getMemoryZone();

	/**
	 * Updates the {@link MemoryZone} of the checkpoint.
	 *
	 * <p>The {@link Checkpoint#delete()} method is first called before
	 * the new <tt>zone</tt> is set.
	 * This means that after changing the {@link MemoryZone}, any saved state in the checkpoint will be lost.<br/>
	 * Use {@link Checkpoint#copy(MemoryZone)} to copy the checkpoint to another {@link MemoryZone} without losing
	 * the saved state.
	 *
	 * @throws IllegalStateException if {@link Checkpoint#canDelete()} returns <tt>false</tt>.
	 */
	public void setMemoryZone(MemoryZone zone);

	/**
	 * Returns the simulation time corresponding to the moment
	 * when the state of the parent runnable entity was last checkpointed.
	 *
	 * @return the simulation time when this checkpoint was
	 * updated or a negative value if the checkpoint has never been updated
	 */
	public long getCheckpointTime();

	/**
	 * Tests if the given <tt>state</tt> is set and returns <tt>true</tt> if it is and <tt>false</tt>
	 * if it is not.
	 *
	 * <p>See
	 * {@link Checkpoint#CHECKPOINT_STATE_UPDATING},
	 * {@link Checkpoint#CHECKPOINT_STATE_RECOVERING} and
	 * {@link Checkpoint#CHECKPOINT_STATE_COPYING} for possible values for the <tt>state</tt>
	 * parameter.
	 *
	 * <p>A {@link NotificationCodes#CHECKPOINT_STATE_CHANGED} notification is thrown when
	 * the state of the checkpoint changes.
	 *
	 * @param state the state to be tested
	 * @return <tt>true</tt> if the given <tt>state</tt> is set, and <tt>false</tt> otherwise.
	 */
	public boolean isCheckpointStateSet(byte state);

	/**
	 * Returns <tt>true</tt> if the checkpoint is being used.
	 *
	 * <p>If you need to check single state, then use {@link Checkpoint#isCheckpointStateSet(byte)} instead.
	 *
	 * @return <tt>true</tt> if the checkpoint is being used
	 */
	public boolean isCheckpointBusy();

	/**
	 * Returns <tt>true</tt> if the checkpoint can be updated.
	 *
	 * <p>This method returns <tt>false</tt> in the following situations:
	 * <ul>
	 * <li>The checkpoint has no defined parent {@link RunnableEntity};
	 * <li>The parent {@link RunnableEntity} has terminated (e.g. failed);
	 * <li>The checkpoint has no set {@link MemoryZone} or it is not usable;
	 * <li>There is an active process using the checkpoint: either an update, a recovery or
	 * a copy (see {@link Checkpoint#isCheckpointBusy()});
	 * <li>Other implementation-specific reasons that may prevent the update.
	 * </ul>
	 *
	 * @return <tt>true</tt> if the checkpoint can be updated
	 */
	public boolean canUpdate();

	/**
	 * This method updates the checkpoint.
	 * After the update the checkpoint will reflect the state of its parent {@link RunnableEntity}
	 * at the current simulation time.
	 *
	 * <p>The parent {@link RunnableEntity} may be paused at any moment of the update, and it is resumed
	 * once the update is finished.
	 *
	 * <p>During the update, the state {@link Checkpoint#CHECKPOINT_STATE_UPDATING} is set.
	 * Once the update is finished, either a {@link NotificationCodes#CHECKPOINT_UPDATE_SUCCESS} notification or
	 * a {@link NotificationCodes#CHECKPOINT_UPDATE_ERROR} notification is thrown depending
	 * on if the checkpoint was updated and acknowledged successfully or not.
	 *
	 * @throws IllegalStateException if {@link Checkpoint#canUpdate()} returns <tt>false</tt>.
	 */
	public void update();

	/**
	 * Cancels an ongoing update.
	 *
	 * <p>You need to listen to appropriate notifications to know when the update is completed (canceled or succeeded)
	 *
	 * @throws IllegalStateException if the checkpoint is not being updated (see {@link Checkpoint#isCheckpointStateSet(byte)})
	 */
	public void cancelUpdate();

	/**
	 * Returns <tt>true</tt> if the checkpoint can be used for recovery with the given parameters.
	 *
	 * <p>This method returns <tt>false</tt> in the following situations:
	 * <ul>
	 * <li>The checkpoint has no defined parent {@link RunnableEntity};
	 * <li>The checkpoint has no set {@link MemoryZone} or it is not usable;
	 * <li>The checkpoint was never updated and there is no saved state to use for recovery;
	 * <li>There is an active update process which is modifying the checkpoint (see {@link Checkpoint#isCheckpointStateSet(byte)});
	 * <li>The given <tt>parent</tt> entity cannot be used (it has no parents, it / or one of its parents has failed, it / or one of its parents is powered off);
	 * <li>Other implementation-specific reasons that may prevent the recovery.
	 * </ul>
	 *
	 * @param parent the entity which will be used as a parent for the newly created {@link RunnableEntity} during the recovery process (might be <tt>null</tt>, in which case no tests relative to this param is done)
	 * @param toReplace the {@link RunnableEntity} to replace if the recovery process is successful (might be <tt>null</tt>, in which case no tests relative to this param is done)
	 * @return <tt>true</tt> if the checkpoint can be used for recovery using the given parameters
	 */
	public boolean canRecover(P parent, E toReplace);

	/**
	 * Uses the checkpoint data to create a new {@link RunnableEntity} that has
	 * the same state as the parent {@link RunnableEntity} when the checkpoint was
	 * last updated.
	 *
	 * <p>The newly created {@link RunnableEntity} is defined as a child of the given <tt>parent</tt> entity.
	 * And all necessary resources are allocated for the newly created {@link RunnableEntity} on the <tt>parent</tt> entity.
	 *
	 * <p>If the <tt>toReplace<tt> entity is given and is not <tt>null</tt>, then
	 * we first remove that entity before trying to allocate new resources
	 * for the newly created {@link RunnableEntity}.
	 * Note, however, that if the recovery process fails, then the <tt>toReplace<tt> entity is not removed.
	 *
	 * <p>During the recovery, the state {@link Checkpoint#CHECKPOINT_STATE_RECOVERING} is set.
	 * Once the recovery is finished, either a {@link NotificationCodes#CHECKPOINT_RECOVER_SUCCESS} notification or
	 * a {@link NotificationCodes#CHECKPOINT_RECOVER_ERROR} notification is thrown depending
	 * on if the recovery process was successful or not.
	 *
	 * @param parent the entity which is used as a parent for the newly created {@link RunnableEntity} during the recovery process
	 * @param toReplace the {@link RunnableEntity} to replace if the recovery process is successful (might be <tt>null</tt>)
	 * @throws IllegalStateException if {@link Checkpoint#canRecover(Entity, RunnableEntity)} returns <tt>false</tt>
	 */
	public void recover(P parent, E toReplace);

	/**
	 * Cancels an ongoing recovery.
	 *
	 * <p>You need to listen to appropriate notifications to know when the recover is completed (canceled or succeeded)
	 *
	 * @throws IllegalStateException if the checkpoint is not being used for recovery (see {@link Checkpoint#isCheckpointStateSet(byte)})
	 */
	public void cancelRecover();

	/**
	 * Returns <tt>true</tt> if the checkpoint can be copied to the given <tt>zone</tt>.
	 *
	 * <p>This method returns <tt>false</tt> in the following situations:
	 * <ul>
	 * <li>The checkpoint has no defined parent {@link RunnableEntity};
	 * <li>The checkpoint has not a set {@link MemoryZone} or it is not usable;
	 * <li>The checkpoint was never updated and there is no saved state to copy;
	 * <li>There is an active update process which is modifying the checkpoint (see {@link Checkpoint#isCheckpointStateSet(byte)});
	 * <li>The given <tt>zone</tt> is the same as the Checkpoint's {@link MemoryZone} (see {@link Checkpoint#getMemoryZone()});
	 * <li>The given <tt>zone</tt> is not usable;
	 * <li>Other implementation-specific reasons that may prevent the copy.
	 * </ul>
	 *
	 * @param zone the {@link MemoryZone} where to copy the checkpoint
	 * @return <tt>true</tt> if the checkpoint can be copied to the given <tt>zone</tt>
	 */
	public boolean canCopy(MemoryZone zone);

	/**
	 * Same as {@link Checkpoint#canCopy(MemoryZone)}, but does not make any tests
	 * relative to the {@link MemoryZone} where the checkpoint will be copied.
	 *
	 * @return <tt>true</tt> if the copy is possible using this checkpoint given a valid {@link MemoryZone}.
	 */
	public boolean canCopy();

	/**
	 * Create a copy of the checkpoint using another {@link MemoryZone}.
	 *
	 * <p>This method returns immediately, you need to listen to appropriate
	 * notification codes to know when the checkpoint have been copied.
	 *
	 * <p>During the copy, the state {@link Checkpoint#CHECKPOINT_STATE_COPYING} is set.
	 * Once the copy is finished, either a {@link NotificationCodes#CHECKPOINT_COPY_SUCCESS} notification or
	 * a {@link NotificationCodes#CHECKPOINT_COPY_ERROR} notification is thrown depending
	 * on if the copy process was successful or not.
	 *
	 * @param zone the {@link MemoryZone} where to copy the checkpoint
	 * @throws IllegalStateException if {@link Checkpoint#canCopy(MemoryZone)} returns <tt>false</tt>
	 */
	public void copy(MemoryZone zone);

	/**
	 * Cancels an ongoing copy.
	 *
	 * <p>You need to listen to appropriate notifications to know when the copy is completed (canceled or succeeded)
	 *
	 * @throws IllegalStateException if the checkpoint is not being copied (see {@link Checkpoint#isCheckpointStateSet(byte)})
	 */
	public void cancelCopy();

	/**
	 * Returns <tt>true</tt> if the checkpoint can be deleted.
	 *
	 * <p>This method returns <tt>false</tt> in the following situations:
	 * <ul>
	 * <li>There is an active process which is using the checkpoint (see {@link Checkpoint#isCheckpointBusy()});
	 * <li>Other implementation-specific reasons that may prevent the deletion.
	 * </ul>
	 *
	 * @return <tt>true</tt> if the checkpoint can be deleted
	 */
	public boolean canDelete();

	/**
	 * Deletes the checkpoint.
	 *
	 * <p>This method will reduce the size of the MemoryZone used by the checkpoint to zero.<br/>
	 * The parent and the MemoryZone of the checkpoint are not modified.
	 *
	 * <p>After deleting the checkpoint, it will no longer be usable by the {@link Checkpoint#update()},
	 * {@link Checkpoint#recover(Entity, RunnableEntity)} or {@link Checkpoint#copy(MemoryZone)} methods.
	 *
	 * @throws IllegalStateException if {@link Checkpoint#canDelete()} returns <tt>false</tt>
	 */
	public void delete();

	/**
	 * Returns the {@link CheckpointingHandler} that has created
	 * this checkpoint or <tt>null</tt> if this checkpoint was not created
	 * by a {@link CheckpointingHandler}.
	 *
	 * @return the {@link CheckpointingHandler} that has created
	 * this checkpoint or <tt>null</tt>
	 * @see Checkpoint#setCheckpointingHandler(CheckpointingHandler)
	 */
	public CheckpointingHandler<E, ? extends Checkpoint<E, P>> getCheckpointingHandler();

	/**
	 * Updates the {@link CheckpointingHandler} associated with this checkpoint.
	 *
	 * @param ch
	 */
	public void setCheckpointingHandler(CheckpointingHandler<E, ? extends Checkpoint<E, P>> ch);
}
