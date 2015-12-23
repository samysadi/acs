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

package com.samysadi.acs.utility.collections.infrastructure;

import java.util.Collection;
import java.util.List;

import com.samysadi.acs.hardware.Host;


/**
 * 
 * @since 1.0
 */
public class CloudImpl extends MyArrayList<DatacenterImpl> implements Cloud {
	private static final long serialVersionUID = 1L;

	public CloudImpl() {
		super();
	}

	public CloudImpl(Collection<? extends DatacenterImpl> c) {
		super(c);
	}

	public CloudImpl(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public List<DatacenterImpl> getDatacenters() {
		return new UnmodifiableCloud(this);
	}

	@Override
	public List<ClusterImpl> getClusters() {
		return new MyMultiListView<ClusterImpl>() {
			@Override
			protected List<? extends List<ClusterImpl>> lists() {
				return CloudImpl.this;
			}

			@Override
			protected int getModCount() {
				//mod count needs to include only cloud modifs (modCount) + data-center modifs (ie: add/remove rack) --> childModCount level = 0
				return CloudImpl.this.modCount + CloudImpl.this.getChildModCount(0);
			}
		};
	}

	@Override
	public List<RackImpl> getRacks() {
		final List<ClusterImpl> clusters = this.getClusters();
		return new MyMultiListView<RackImpl>() {
			@Override
			protected List<? extends List<RackImpl>> lists() {
				return clusters;
			}

			@Override
			protected int getModCount() {
				//mod count needs to include only cloud modifs (modCount) + data-center & cluster modifs (ie: add/remove rack) --> childModCount level = 1
				return CloudImpl.this.modCount + CloudImpl.this.getChildModCount(1);
			}
		};
	}

	@Override
	public List<Host> getHosts() {
		final List<RackImpl> racks = this.getRacks();
		return new MyMultiListView<Host>() {
			@Override
			protected List<? extends List<Host>> lists() {
				return racks;
			}

			@Override
			protected int getModCount() {
				return CloudImpl.this.modCount + CloudImpl.this.getChildModCount();
			}
		};
	}

	protected static class UnmodifiableCloud extends MyUnmodifiableList<DatacenterImpl> implements Cloud {
		public UnmodifiableCloud(CloudImpl cloud) {
			super(cloud);
		}

		@Override
		public List<DatacenterImpl> getDatacenters() {
			return this;
		}

		@Override
		public List<ClusterImpl> getClusters() {
			return ((CloudImpl)list).getClusters();
		}

		@Override
		public List<RackImpl> getRacks() {
			return ((CloudImpl)list).getRacks();
		}

		@Override
		public List<Host> getHosts() {
			return ((CloudImpl)list).getHosts();
		}
	}

	@Override
	public MyArrayList<?> getParent() {
		return null;
	}
}
