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
package com.samysadi.acs.utility.workload.task;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.workload.Workload;

/**
 * A task defines a sequence of operations to be executed
 * by a {@link Workload}.
 * 
 * <p>The task notifies the {@link Workload} when it ends using
 * {@link NotificationCodes#WORKLOAD_TASK_COMPLETED} or {@link NotificationCodes#WORKLOAD_TASK_FAILED}.
 * 
 * <p>All implementation classes should provide a two arguments constructor
 * that defines respectively the {@link Workload}, and the {@link Config} of the task.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface Task extends Cloneable {
	/**
	 * Creates and returns a clone of current task.
	 * 
	 * <p>The clone should not rely on any of the current task's workload resources.
	 * 
	 * <p>The clone should be defined so that executing it will continue at the
	 * same point the current task was when the clone was created.<br/>
	 * However, if this is not possible, the clone should then restart the whole 
	 * task from the beginning.
	 * 
	 * @param workload the workload of the newly created clone
	 * @return the current task's clone
	 * @throws IllegalStateException if the task is discarded
	 */
	public Task clone(Workload workload);

	/**
	 * Returns the task's workload.
	 * This value may not be available after discarding the task.
	 * 
	 * @return the task's workload
	 */
	public Workload getWorkload();

	/**
	 * Returns the task's {@link Config}.
	 * 
	 * @return the task's {@link Config}
	 */
	public Config getConfig();

	/**
	 * Runs / continues the task execution.
	 * 
	 * @throws IllegalStateException if the task is discarded
	 */
	public void execute();

	/**
	 * Interrupts the task execution. Calling {@link Task#execute()} should continue the task without restarting it.
	 */
	public void interrupt();
}
