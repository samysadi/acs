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
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;

/**
 * Defines a factory for generating hosts and the network 
 * interconnection between them.
 * 
 * @since 1.0
 */
public abstract class TopologyFactory extends Factory {
	private CloudProvider cloudProvider;
	private GenerationMode hostGenerationMode;

	public static final String INTERNETLAYER_CONTEXT = "InternetLayer";

	public TopologyFactory(Config config, CloudProvider cloudProvider) {
		super(config);
		this.cloudProvider = cloudProvider;

		this.hostGenerationMode = newGenerationMode(null, FactoryUtils.Host_CONTEXT);
	}

	public CloudProvider getCloudProvider() {
		return this.cloudProvider;
	}

	protected GenerationMode getHostGenerationMode() {
		return hostGenerationMode;
	}

	/**
	 * Generates a topology (hosts, switches and links) and returns <tt>null</tt>.
	 * 
	 * @return <tt>null</tt>
	 */
	@Override
	public abstract Object generate();
}
