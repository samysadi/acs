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

package com.samysadi.acs.utility.factory;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.factory.generation.flow.GenerationFlow;
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public abstract class UserFactory extends Factory {
	private CloudProvider cloudProvider;

	private GenerationMode thinClientGenerationMode;
	private GenerationFlow thinClientGenerationFlow;

	private GenerationMode vmGenerationMode;
	private GenerationFlow vmGenerationFlow;

	private GenerationMode workloadGenerationMode;
	private GenerationFlow workloadGenerationFlow;

	public UserFactory(Config config, CloudProvider cloudProvider) {
		super(config);
		this.cloudProvider = cloudProvider;

		this.thinClientGenerationMode = newGenerationMode(null, FactoryUtils.ThinClient_CONTEXT);
		this.thinClientGenerationFlow = newGenerationFlow(null, FactoryUtils.ThinClient_CONTEXT);

		this.vmGenerationMode = newGenerationMode(null, FactoryUtils.VirtualMachine_CONTEXT);
		this.vmGenerationFlow = newGenerationFlow(null, FactoryUtils.VirtualMachine_CONTEXT);

		this.workloadGenerationMode = newGenerationMode(null, FactoryUtils.Workload_CONTEXT);
		this.workloadGenerationFlow = newGenerationFlow(null, FactoryUtils.Workload_CONTEXT);
	}

	public CloudProvider getCloudProvider() {
		return this.cloudProvider;
	}

	protected GenerationMode getThinClientGenerationMode() {
		return thinClientGenerationMode;
	}

	protected GenerationFlow getThinClientGenerationFlow() {
		return thinClientGenerationFlow;
	}

	protected GenerationMode getVmGenerationMode() {
		return vmGenerationMode;
	}

	protected GenerationFlow getVmGenerationFlow() {
		return vmGenerationFlow;
	}

	protected GenerationMode getWorkloadGenerationMode() {
		return workloadGenerationMode;
	}

	protected GenerationFlow getWorkloadGenerationFlow() {
		return workloadGenerationFlow;
	}

	/**
	 * Generates a User and returns it.
	 * 
	 * @return generated {@link User}
	 */
	@Override
	public abstract User generate();
}
