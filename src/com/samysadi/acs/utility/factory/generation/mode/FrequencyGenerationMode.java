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

import java.util.Collections;
import java.util.Comparator;

import com.samysadi.acs.core.Config;


/**
 * A {@link AbstractGenerationMode} subclass that selects next candidate
 * configuration accordingly to a given frequency.
 *
 * <p>Each configuration frequency is given through its <tt>Frequency</tt> configuration value. If it is
 * not set then <tt>1</tt> is assumed.
 *
 * <p>The frequency of each configuration indicates the number of times it is chosen. The higher it is
 * the more frequent that configuration is chosen.
 *
 * <p>For example, if there is two configurations the first with a frequency set to 1 and the second
 * with a frequency set to 3, then the second configuration is chosen 3 times more than the first configuration.
 *
 * <p>More formally, if there is <math><mi>n</mi></math> configuration candidates so that, for each
 * <math><mi>i</mi><mo>&isin;</mo><mo>{</mo><mn>0</mn><mo>..</mo><mn>n-1</mn><mo>}</mo></math>,
 * the frequency of each candidates is
 * <math><msub><mi>f</mi><mi>i</mi></msub></math>
 * and the number of times the <math><mi>i</mi><sup>th</sup></math> configuration is selected is
 * <math><msub><mi>t</mi><mi>i</mi></msub></math>
 * then:<br/>
 * <math><mfrac>
 * 		<msub><mi>f</mi><mi>i</mi></msub>
 * 		<mrow>
 * 			<munderover><mo>&sum;</mo><mrow><mi>k</mi><mo>=</mo><mn>0</mn></mrow><mrow><mi>n</mi><mo>-</mo><mn>1</mn></mrow></munderover>
 * 			<msub><mi>f</mi><mi>k</mi></msub>
 * 		</mrow>
 * 	</mfrac>
 * 	<mo>&asymp;</mo>
 * 	<mfrac>
 * 		<msub><mi>t</mi><mi>i</mi></msub>
 * 		<mrow>
 * 			<munderover><mo>&sum;</mo><mrow><mi>k</mi><mo>=</mo><mn>0</mn></mrow><mrow><mi>n</mi><mo>-</mo><mn>1</mn></mrow></munderover>
 * 			<msub><mi>t</mi><mi>k</mi></msub>
 * 		</mrow>
 * 	</mfrac>
 * </math>
 *
 * @since 1.0
 */
public class FrequencyGenerationMode extends AbstractGenerationMode {
	private int[] freq;
	private int[] freqOrig;
	private int nextFreq;

	public FrequencyGenerationMode(Config config, String context) {
		super(config, context);

		Collections.sort(this.configurations, new FrequencyComparator());

		this.freqOrig = new int[this.configurations.size()];
		int i = 0;
		for (Config c: this.configurations)
			this.freqOrig[i++] = c.getInt("Frequency", 1);

		this.freq = this.freqOrig.clone();
		this.nextFreq = 0;
	}

	private static final class FrequencyComparator implements
			Comparator<Config> {
		@Override
		public int compare(Config o1, Config o2) {
			//descending order
			return o2.getInt("Frequency", 1) - o1.getInt("Frequency", 1);
		}
	}

	@Override
	public FrequencyGenerationMode clone() {
		FrequencyGenerationMode clone = (FrequencyGenerationMode) super.clone();
		clone.freq = clone.freq.clone();
		//no need to clone freqOrig
		return clone;
	}

	@Override
	public Config next() {
		if (this.nextFreq == this.freq.length || this.freq[this.nextFreq] == 0) {
			if (this.freq[0] == 0)
				this.freq = this.freqOrig.clone();
			this.nextFreq = 0;
		}
		Config r = this.configurations.get(this.nextFreq);
		this.freq[this.nextFreq]--;
		this.nextFreq++;

		return r;
	}
}
