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

package com.samysadi.acs.service.jobplacement;

import java.util.Iterator;
import java.util.List;

import com.samysadi.acs.utility.collections.ShuffledIterator;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;

/**
 * A placement policy that chooses the vm that has enough resources for the job
 * among all given user's vms.<br/>
 * The vm is chosen according to the best fit method.
 * 
 * @since 1.0
 */
public class JobPlacementPolicyBestFit extends JobPlacementPolicyAbstract {

	public JobPlacementPolicyBestFit() {
		super();
	}

	protected boolean isVmScoreBetter(double newScore, double compareToScore) {
		return Double.compare(newScore, compareToScore) < 0;
	}

	@Override
	protected VirtualMachine _selectVm(Job job, List<VirtualMachine> vms) {
		final int VM_SEARCH_THRESHOLD = getConfigRec().getInt("JobPlacement.SearchThreshold", 20);

		int seenCount = 0;
		VirtualMachine bestVm = null;
		double bestScore = 0.0d;

		Iterator<VirtualMachine> it = new ShuffledIterator<VirtualMachine>(vms);
		while (it.hasNext()) {
			final VirtualMachine candidate = it.next();
			if (candidate.isRunning()) {
				final double s = computeJobScore(job, candidate);
				if (isVmScoreBetter(s, bestScore)) {
					bestVm = candidate;
					bestScore = s;
				}
			}

			seenCount++;
			if (bestVm != null && seenCount > VM_SEARCH_THRESHOLD)
				break;
		}

		return bestVm;
	}
}
