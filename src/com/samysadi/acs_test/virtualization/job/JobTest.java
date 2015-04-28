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

package com.samysadi.acs_test.virtualization.job;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.utility.factory.Factory;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs_test.Utils;
import com.samysadi.acs_test.core.entity.RunnableEntityTest;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class JobTest extends RunnableEntityTest {

	private Job newJob(int i) {
		Job j = Factory.getFactory(simulator).newJob(null, null);
		j.setParent(Utils.getVmFor(cloudProvider.getHosts().get(i)));
		j.doStart();
		return j;
	}

	@Override
	public RunnableEntity startRunnableEntity() {
		return newJob(0);
	}

	@Override
	public RunnableEntity startChildForRunnableEntity(RunnableEntity parent) {
		ComputingOperation o = Factory.getFactory(Simulator.getSimulator())
				.newComputingOperation(null, ((Job) parent), 1000l);
		o.doStart();
		return o;
	}
}
