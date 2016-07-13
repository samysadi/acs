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

package com.samysadi.acs.core.entity;

import java.util.List;
import java.util.Map;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Logger;
import com.samysadi.acs.core.notifications.CoreNotificationCodes;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;

/**
 * This interface describes a simulation entity.
 *
 * <p>A simulation entity has one parent and can have multiple children entities.<br/>
 * In order to add or to remove children for this entity call the {@link Entity#setParent(Entity)} method
 * on each child.<br/>
 * Don't forget to use {@link Entity#setParent(Entity) setParent(null)} on not used entities so that
 * they can be garbage-collected.
 *
 * <p>All entity implementation classes should provide at least one
 * "standard" constructor: a void (no arguments) constructor which creates an
 * empty entity with no parent and no children.
 *
 * @since 1.0
 */
public interface Entity extends Notifier, Probed, Cloneable {

	/**
	 * Creates a clone of this entity and sets its parent to <tt>null</tt>.
	 *
	 * <p>This is a deep clone method which also clones almost all children entities and sets their parents
	 * to the newly created clone.
	 * The only children which are not cloned are those implementing the {@link UncloneableEntity} interface.
	 *
	 * <p>Implementations must ensure that the new clone will be independent from the current entity.
	 */
	@Override
	public Entity clone();

	/**
	 * Returns a unique Id describing this entity.
	 *
	 * @return a unique Id describing this entity
	 */
	public long getId();

	/**
	 * Returns the name of this entity as set using {@link Entity#setName(String)}.
	 *
	 * @return the name of this entity
	 */
	public String getName();

	/**
	 * Updates the name of this entity.
	 *
	 * @param name
	 */
	public void setName(String name);

	/**
	 * Returns the parent of this entity, or <tt>null</tt> if no parent is set
	 *
	 * @return the parent of this entity, or <tt>null</tt>
	 */
	public Entity getParent();

	/**
	 * Updates the parent of this entity.<br/>
	 * This method makes sure that the old and the new parent keep their entities list updated, by automatically calling
	 * {@link Entity#addEntity(Entity)} and {@link Entity#removeEntity(Entity)}.
	 *
	 * <p>A {@link CoreNotificationCodes#ENTITY_PARENT_CHANGED} notification is thrown.<br/>
	 * Additionally, each descendant of this entity (ie: entities who have this entity as their direct or not direct parent) are
	 * notified using {@link CoreNotificationCodes#ENTITY_ANCESTOR_CHANGED}.
	 *
	 * @param parent
	 * @throws IllegalStateException if the parent was locked using {@link Entity#lockParent()}.
	 */
	public void setParent(Entity parent);

	/**
	 * Returns <tt>true</tt> if this entity has a parent (different from <tt>null</tt>)
	 * and calling {@link Entity#hasParentRec()} on the parent of this entity also returns <tt>true</tt>.
	 *
	 * @return <tt>true</tt> if this entity and each of its parents has a non null parent
	 */
	public boolean hasParentRec();

	/**
	 * Increments the parent lock, and will prevent parent change until
	 * {@link Entity#unlockParent()} is called.
	 *
	 * <p>This method is useful if you need to prevent erroneous parent changes during specific moments of the simulation.
	 *
	 * <p>You need to call {@link Entity#unlockParent()} at least the same times as you called {@link Entity#lockParent()}
	 * to be able to change the parent again.
	 */
	public void lockParent();

	/**
	 * Calls {@link Entity#lockParent()} on this entity, and
	 * then calls {@link Entity#lockParentRec()} on the parent of this entity if it is set.
	 */
	public void lockParentRec();

	/**
	 * Decrements the parent lock.
	 *
	 * <p>You need to call {@link Entity#unlockParent()} at least the same times as you called {@link Entity#lockParent()}
	 * to be able to change the parent.
	 */
	public void unlockParent();

	/**
	 * Calls {@link Entity#unlockParent()} on this entity, and
	 * then calls {@link Entity#unlockParentRec()} on the parent of this entity if it is set.
	 */
	public void unlockParentRec();

	/**
	 * Returns this entity's configuration as set using {@link Entity#setConfig(Config)}, or <tt>null</tt>.
	 *
	 * <p>This method may return <tt>null</tt> if no configuration was set for
	 * this Entity.
	 *
	 * <p><b>Note:</b> a configuration may be shared between more than one entity,
	 * use properties ({@link Entity#setProperty(Object, Object)}),
	 * if you want to set data for this sole instance.
	 *
	 * @return this entity's configuration or <tt>null</tt> if none is set
	 * @see Entity#getConfigRec()
	 */
	public Config getConfig();

	/**
	 * Returns a not null configuration for this entity.
	 *
	 * <p>This method first tries to return this entity's configuration if it is set (ie: not <tt>null</tt>).
	 *
	 * <p>If this entity has a <tt>null</tt> configuration, then this method returns
	 * the parent's configuration by calling {@link Entity#getConfigRec()} on the parent
	 * of this entity.
	 *
	 * <p>If this entity has a <tt>null</tt> configuration and a <tt>null</tt> parent,
	 * then the simulator's configuration is returned.
	 *
	 * <p>Unlike {@link Entity#getConfig()}, this method should not return <tt>null</tt>.
	 *
	 * @return a not null configuration for this entity
	 *
	 */
	public Config getConfigRec();

	/**
	 * Updates this entity's configuration.
	 *
	 * @param config new configuration
	 */
	public void setConfig(Config config);

	/**
	 * Returns a {@link Logger} instance for this Entity.
	 *
	 * <p>Default is to return the logger of the parent of this entity.
	 *
	 * <p>If this entity has a <tt>null</tt> parent, then {@link Logger#getGlobal()} is returned.
	 *
	 * @return a {@link Logger} instance for this Entity
	 */
	public Logger getLogger();

	/**
	 * Adds a new children entity.<br/>
	 * This does not update the parent of the given entity.
	 *
	 * <p><b>Note:</b> You should not need to call this method,
	 * instead use {@link EntityImpl#setParent(Entity)}.
	 *
	 * @param entity
	 */
	public void addEntity(Entity entity);

	/**
	 * Returns a list containing all children entities.
	 * The order of these entities is not defined, and can be different from the insertion order.
	 *
	 * @return a list containing all children entities
	 */
	public List<Entity> getEntities();

	/**
	 * Search for the given children entity and removes it.<br/>
	 * This does not update the parent property of the entity.
	 *
	 * <p><b>Note:</b> You should not need to call this method,
	 * instead use {@link EntityImpl#setParent(Entity)} with null parameter.
	 *
	 * @param entity
	 */
	public void removeEntity(Entity entity);

	/**
	 * Returns the property for the given <tt>key</tt> or <tt>defaultValue</tt> if not found.
	 *
	 * @param key
	 * @param defaultValue
	 * @return the property or <tt>defaultValue</tt> if not found
	 */
	public Object getProperty(Object key, Object defaultValue);

	/**
	 * Returns the property for the given <tt>key</tt> or <tt>null</tt> if not found.
	 *
	 * @param key
	 * @return the property or <tt>null</tt> if not found
	 */
	public Object getProperty(Object key);

	/**
	 * Returns a map containing all properties of this entity.
	 *
	 * @return all properties of this entity
	 */
	public Map<Object, Object> getProperties();

	/**
	 * Sets or updates the property specified by the given <tt>key</tt>.
	 *
	 * @param key
	 * @param value
	 */
	public void setProperty(Object key, Object value);

	/**
	 * Unsets the property specified by the given <tt>key</tt>.
	 *
	 * @param key
	 */
	public void unsetProperty(Object key);
}
