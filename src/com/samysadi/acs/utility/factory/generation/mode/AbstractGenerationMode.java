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

package com.samysadi.acs.utility.factory.generation.mode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.samysadi.acs.core.Config;

/**
 * Abstract {@link GenerationMode} where possible configurations
 * are determined as follows.
 * 
 * <p>It is assumed that all configuration candidates are put under the same configuration.<br/>
 * Each candidate's context contains two String parts. A fixed prefix which is the same among
 * all other candidates. And, an integer suffix that distinguish it among other candidates.<br/>
 * For example, multiple host configurations maybe named: <i>Host0</i>, <i>Host1</i>, <i>Host2</i> ...
 * 
 * <p>When instantiating this class, all configuration candidates are determined and put under
 * a list which is accessible by subclasses using the protected <tt>configurations</tt> field.
 * 
 * @since 1.0
 */
public abstract class AbstractGenerationMode implements GenerationMode {
	private Config config;

	protected List<Config> configurations;

	public AbstractGenerationMode(Config config, String context) {
		super();
		this.config = config;

		this.configurations = new ArrayList<Config>();

		int i = 0;
		while (getConfig().hasContext(context, i)) {
			Config cfg = getConfig().addContext(context, i);
			this.configurations.add(cfg);
			i++;
		}

		if (i == 0)
			this.configurations.add(getConfig().addContext(context, i)); //make sure we have at least one config
	}

	public Config getConfig() {
		return this.config;
	}

	/**
	 * Returns an unmodifiable list containing all possible configurations.
	 * 
	 * @return an unmodifiable list containing all possible configurations
	 */
	public List<Config> getConfigurations() {
		return Collections.unmodifiableList(this.configurations);
	}

	@Override
	public AbstractGenerationMode clone() {
		AbstractGenerationMode clone;
		try {
			clone = (AbstractGenerationMode) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		if (clone.configurations != null)
			clone.configurations = new ArrayList<Config>(clone.configurations);
		return clone;
	}
}
