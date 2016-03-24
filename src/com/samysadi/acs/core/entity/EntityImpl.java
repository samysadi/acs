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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Logger;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.notifications.CoreNotificationCodes;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.ProbedImpl;
import com.samysadi.acs.utility.factory.Factory;

/**
 *
 * @since 1.0
 */
public class EntityImpl extends ProbedImpl implements Entity {
	private static long idCounter = 0;
	private static int MAX_PARENT_LOCKS = Integer.MAX_VALUE;

	private int parentLock;
	private Entity parent;
	private List<Entity> entities;
	private long id;
	private String name;
	private Config config;
	private Map<Object, Object> properties;

	public EntityImpl() {
		super();

		this.name = null;
		this.config = null;
		this.properties = null;

		this.initializeEntity();
	}

	@Override
	public EntityImpl clone() {
		final EntityImpl clone = (EntityImpl) super.clone();

		if (this.properties != null)
			clone.properties = new HashMap<Object, Object>(this.properties);

		clone.initializeEntity();
		for (Entity e: this.getEntities()) {
			if (e instanceof UncloneableEntity)
				continue;
			Entity cloneEntity = e.clone();
			cloneEntity.setParent(clone);
		}

		this.notify(CoreNotificationCodes.ENTITY_CLONED, clone);

		return clone;
	}

	/**
	 * This method is called on Entity creation and before it is set a parent. And when
	 * the entity is cloned on the newly created clone before its children are cloned.
	 */
	protected void initializeEntity() {
		this.id = ++EntityImpl.idCounter;
		this.parent = null;
		this.parentLock = 0;
		this.entities = null;
	}

	@Override
	public long getId() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(getId()).hashCode();
	}

	@Override
	public String toString() {
		if (this.name == null)
			return this.getName();
		return this.getName() + "#" + getId();
	}

	@Override
	public String getName() {
		if (this.name == null)
			return "E#" + getId();
		return this.name;
	}

	@Override
	public void setName(String name) {
		if (name == null || name.isEmpty())
			this.name = null;
		else
			this.name = name;
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity.getParent() != null)
			throw new IllegalArgumentException("The supplied entity has already a parent");
		if (this.entities == null)
			this.entities = newArrayList();
		if (!this.entities.add(entity))
			return;
		notify(CoreNotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity.getParent() != this)
			throw new IllegalArgumentException("The supplied entity has another parent");
		if (this.entities == null)
			return;
		if (!this.entities.remove(entity))
			return;
		if (this.entities.isEmpty())
			this.entities = null;
		notify(CoreNotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public Entity getParent() {
		return this.parent;
	}

	/**
	 * This method is called before this entity gets a new parent.
	 *
	 * <p>Use {@link Entity#getParent()} to get the old parent.
	 *
	 * @param newParent
	 */
	protected void beforeSetParent(Entity newParent) {
		//
	}

	/**
	 * This method is called after this entity gets a new parent (and before it is notified).<br/>
	 * You can also listen to the appropriate notification.
	 *
	 * <p>Use {@link Entity#getParent()} to get the new parent.
	 *
	 * @param oldParent
	 */
	protected void afterSetParent(Entity oldParent) {
		//
	}

	@Override
	public void setParent(Entity parent) {
		if (this.parent == parent)
			return;

		if (this.parentLock > 0)
			throw new IllegalStateException("Parent is locked and cannot be changed now.");

		beforeSetParent(parent);
		Entity old = this.parent;
		if (this.parent != null) {
			this.parent.removeEntity(this);
			this.parent = null;
		}
		if (parent != null)
			parent.addEntity(this);
		this.parent = parent;
		afterSetParent(old);
		notify(CoreNotificationCodes.ENTITY_PARENT_CHANGED, null);

		//notify children
		ancestorChanged(this);
	}

	/**
	 * This method is called when an ancestor of this entity changes.
	 *
	 * <p>Default behavior is to recursively notify, and call {@link EntityImpl#ancestorChanged(EntityImpl)} on children.
	 *
	 * <p>Override this method, or listen to notification ({@link CoreNotificationCodes#ENTITY_ANCESTOR_CHANGED}) if
	 * you want to take actions when this entity gets a new ancestor.
	 *
	 * @param ancestor
	 */
	protected void ancestorChanged(EntityImpl ancestor) {
		for (Entity child: this.getEntities()) {
			if (child instanceof EntityImpl)
				((EntityImpl) child).ancestorChanged(ancestor);
			child.notify(CoreNotificationCodes.ENTITY_ANCESTOR_CHANGED, ancestor);
		}
	}

	@Override
	public boolean hasParentRec() {
		if (this.getParent() == null)
			return false;
		return this.getParent().hasParentRec();
	}

	@Override
	public void lockParent() {
		if (this.parentLock >= MAX_PARENT_LOCKS)
			throw new IllegalStateException("Too many locks > " + String.valueOf(MAX_PARENT_LOCKS));
		this.parentLock++;
	}

	@Override
	public void unlockParent() {
		if (this.parentLock <= 0)
			return;
		this.parentLock--;
	}

	@Override
	public void lockParentRec() {
		this.lockParent();
		if (getParent() != null)
			getParent().lockParentRec();
	}

	@Override
	public void unlockParentRec() {
		this.unlockParent();
		if (getParent() != null)
			getParent().unlockParentRec();
	}

	@Override
	public Config getConfig() {
		return this.config;
	}

	@Override
	public Config getConfigRec() {
		if (this.config != null)
			return this.config;
		if (this.getParent() != null)
			return this.getParent().getConfigRec();
		return Simulator.getSimulator().getConfig();
	}

	@Override
	public void setConfig(Config config) {
		this.config = config;
	}

	@Override
	protected Probe<?> newProbe(String probeKey) {
		return Factory.getFactory(this).newProbe(probeKey);
	}

	@Override
	public Logger getLogger() {
		if (this.getParent() != null)
			return this.getParent().getLogger();
		return Logger.getGlobal();
	}

	@Override
	public List<Entity> getEntities() {
		if (this.entities == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(this.entities);
	}

	@Override
	public Object getProperty(Object key) {
		if (this.properties == null)
			return null;
		return this.properties.get(key);
	}

	@Override
	public Object getProperty(Object key, Object defaultValue) {
		if (this.properties == null)
			return defaultValue;
		if (!this.properties.containsKey(key))
			return defaultValue;
		return this.properties.get(key);
	}

	@Override
	public Map<Object, Object> getProperties() {
		if (this.properties == null)
			return Collections.emptyMap();
		else
			return Collections.unmodifiableMap(this.properties);
	}

	@Override
	public void setProperty(Object key, Object value) {
		if (this.properties == null)
			this.properties = new HashMap<Object, Object>();
		this.properties.put(key, value);
	}

	@Override
	public void unsetProperty(Object key) {
		if (this.properties == null)
			return;
		this.properties.remove(key);
		if (this.properties.isEmpty())
			this.properties = null;
	}

	public static long getNextId() {
		return idCounter+1;
	}

	protected final <C> ArrayList<C> newArrayList() {
		//let's privilege memory consumption
		return new ArrayList<C>(1);
	}

	protected final <C> ArrayList<C> newArrayList(int initialCapacity) {
		return new ArrayList<C>(initialCapacity);
	}
}
