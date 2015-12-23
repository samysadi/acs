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
public class DatacenterImpl extends MyArrayList<ClusterImpl> implements Datacenter {
	private static final long serialVersionUID = 1L;
	private CloudImpl parent;

	public DatacenterImpl() {
		super();
	}

	public DatacenterImpl(Collection<? extends ClusterImpl> c) {
		super(c);
	}

	public DatacenterImpl(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public List<ClusterImpl> getClusters() {
		return new UnmodifiableDatacenter(this);
	}

	@Override
	public List<RackImpl> getRacks() {
		return new MyMultiListView<RackImpl>() {
			@Override
			protected List<? extends List<RackImpl>> lists() {
				return DatacenterImpl.this;
			}

			@Override
			protected int getModCount() {
				//mod count needs to include only datacenter modifs (modCount) + cluster modifs (ie: add/remove rack) --> childModCount level = 0
				return DatacenterImpl.this.modCount + DatacenterImpl.this.getChildModCount(0);
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
				return DatacenterImpl.this.modCount + DatacenterImpl.this.getChildModCount();
			}
		};
	}

	protected static class UnmodifiableDatacenter extends MyUnmodifiableList<ClusterImpl> implements Datacenter {
		public UnmodifiableDatacenter(DatacenterImpl datacenter) {
			super(datacenter);
		}

		@Override
		public List<ClusterImpl> getClusters() {
			return this;
		}

		@Override
		public List<RackImpl> getRacks() {
			return ((DatacenterImpl)list).getRacks();
		}

		@Override
		public List<Host> getHosts() {
			return ((DatacenterImpl)list).getHosts();
		}

		@Override
		public CloudImpl getCloud() {
			return ((DatacenterImpl)list).getCloud();
		}
	}

	@Override
	public MyArrayList<?> getParent() {
		return parent;
	}

	@Override
	public CloudImpl getCloud() {
		return this.parent;
	}

	public void setCloud(CloudImpl cloud) {
		this.parent = cloud;
	}
}
