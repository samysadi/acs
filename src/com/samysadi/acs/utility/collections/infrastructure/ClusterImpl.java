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
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class ClusterImpl extends MyArrayList<RackImpl> implements Cluster {
	private static final long serialVersionUID = 1L;
	private DatacenterImpl parent;

	public ClusterImpl() {
		super();
	}

	public ClusterImpl(Collection<? extends RackImpl> c) {
		super(c);
	}

	public ClusterImpl(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public List<RackImpl> getRacks() {
		return new UnmodifiableCluster(this);
	}

	@Override
	public List<Host> getHosts() {
		return new MyMultiListView<Host>() {
			@Override
			protected List<? extends List<Host>> lists() {
				return ClusterImpl.this;
			}

			@Override
			protected int getModCount() {
				return ClusterImpl.this.modCount + ClusterImpl.this.getChildModCount();
			}
		};
	}

	private static class UnmodifiableCluster extends MyUnmodifiableList<RackImpl> implements Cluster {
		public UnmodifiableCluster(ClusterImpl cluster) {
			super(cluster);
		}

		@Override
		public List<RackImpl> getRacks() {
			return this;
		}

		@Override
		public List<Host> getHosts() {
			return ((ClusterImpl)list).getHosts();
		}

		@Override
		public DatacenterImpl getDatacenter() {
			return ((ClusterImpl) list).getDatacenter();
		}
	}

	@Override
	public DatacenterImpl getDatacenter() {
		return this.parent;
	}

	public void setDatacenter(DatacenterImpl datacenter) {
		this.parent = datacenter;
	}

	@Override
	public MyArrayList<?> getParent() {
		return this.parent;
	}
}
