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

package com.samysadi.acs.virtualization.job.operation.provisioner;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.virtualization.job.operation.Operation;

/**
 *
 * @param <OperationType>
 * @param <Resource>
 *
 * @since 1.0
 */
public interface Provisioner<OperationType extends Operation<Resource>, Resource> extends Entity {

	@Override
	public Provisioner<OperationType, Resource> clone();

	/**
	 * Computes and returns the maximum amount of resource that can be granted for the given
	 * <tt>operation</tt> by this provisioner at current point of the simulation.
	 *
	 * <p>The operation may already have granted resource using this provisioner and those are
	 * included in the returned resource as if {@link Provisioner#revokeAllocatedResource(Operation)}
	 * was called before.
	 *
	 * @param operation the operation you want to get resource promise for
	 * @return resource promise for the given <tt>operation</tt>
	 */
	public Resource getResourcePromise(OperationType operation);

	/**
	 * Grants the allocated resource of the given <tt>operation</tt> (see {@link Operation#getAllocatedResource()}).
	 *
	 * <p>Implementations must make sure that the operation's resource has not already been granted using this provisioner.
	 * If so then an IllegalArgumentException is thrown.
	 *
	 * @param operation the operation of which you want to grant resource
	 * @throws NullPointerException if the given <tt>operation</tt> is <tt>null</tt> or its allocated resource is <tt>null</tt>
	 * @throws IllegalArgumentException if the allocated resource for the operation is greater than the resource returned by {@link Provisioner#getResourcePromise(Operation)}
	 * @throws IllegalArgumentException if the operation has already granted resource using this provisioner (you need to revoke it first)
	 */
	public void grantAllocatedResource(OperationType operation);

	/**
	 * Revokes the allocated resource of the given <tt>operation</tt> (see {@link Operation#getAllocatedResource()}).
	 *
	 * <p>Implementations must make sure that the operation's resource has been granted using this provisioner (and not yet revoked).
	 * If not then an IllegalArgumentException is thrown.
	 *
	 * @param operation the operation of which you want to revoke resource
	 * @throws NullPointerException if the given <tt>operation</tt> is <tt>null</tt> or its allocated resource is <tt>null</tt>
	 * @throws IllegalArgumentException if no resource were granted for the operation using this provisioner (or if they were already revoked)
	 */
	public void revokeAllocatedResource(OperationType operation);
}
