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

package com.samysadi.acs_test.utility.structure;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.HostDefault;
import com.samysadi.acs.utility.collections.infrastructure.CloudImpl;
import com.samysadi.acs.utility.collections.infrastructure.Cluster;
import com.samysadi.acs.utility.collections.infrastructure.ClusterImpl;
import com.samysadi.acs.utility.collections.infrastructure.Datacenter;
import com.samysadi.acs.utility.collections.infrastructure.DatacenterImpl;
import com.samysadi.acs.utility.collections.infrastructure.Rack;
import com.samysadi.acs.utility.collections.infrastructure.RackImpl;


/**
 * 
 * @since 1.0
 */
@SuppressWarnings("unused")
public class InfrastructureTest {
	int HOST = 7;
	int RACK = 11;
	int CLUSTER = 13;
	int DATACENTER = 19;

	CloudImpl _cl;
	DatacenterImpl _d;

	@Before
	public void beforeTest() {
		_cl = new CloudImpl();
		{
			for (int d = 0; d<DATACENTER; d++) {
				DatacenterImpl _d = new DatacenterImpl();
				_d.setCloud(_cl);
				for (int c = 0; c<CLUSTER; c++) {
					ClusterImpl _c = new ClusterImpl();
					_c.setDatacenter(_d);
					for (int r = 0; r<RACK; r++) {
						RackImpl _r = new RackImpl();
						_r.setCluster(_c);
						for (int h = 0; h<HOST; h++) {
							Host _h = new HostDefault();
							_h.setProperty("d", d);
							_h.setProperty("c", c);
							_h.setProperty("r", r);
							_h.setProperty("h", h);
							_h.setName("c=" + c + "\tr=" + r + "\th=" + h);
							_r.add(_h);
						}
						_c.add(_r);
					}
					_d.add(_c);
				}
				_cl.add(_d);
			}

			Assert.assertEquals(DATACENTER, _cl.size());
			Assert.assertEquals(DATACENTER * CLUSTER, _cl.getClusters().size());
			Assert.assertEquals(DATACENTER * CLUSTER * RACK, _cl.getRacks().size());
			Assert.assertEquals(DATACENTER * CLUSTER * RACK * HOST, _cl.getHosts().size());
	
			_d = (DatacenterImpl) _cl.get(0);
			Assert.assertEquals(CLUSTER, _d.size());
			Assert.assertEquals(CLUSTER * RACK, _d.getRacks().size());
			Assert.assertEquals(CLUSTER * RACK * HOST, _d.getHosts().size());

			Assert.assertEquals(RACK, _d.get(0).size());
			Assert.assertEquals(RACK * HOST, _d.get(0).getHosts().size());
	
			Assert.assertEquals(HOST, _d.get(0).get(0).size());
		}
	}

	public static HashSet<Integer> build(int size) {
		ArrayList<Integer> r = new ArrayList<Integer>(size);
		while (size-->0)
			r.add(Integer.valueOf(size));
		return new HashSet<Integer>(r);
	}

	@Test
	public void test0() {

		{
			HashSet<Integer> vd = build(CLUSTER * RACK * HOST);
			for (Cluster _c : _d.getClusters()) {
				for (Rack _r: _c.getRacks()) {
					for (Host _h: _r.getHosts()) {
						vd.remove(Integer.valueOf(
								((Integer)_h.getProperty("c")) * RACK * HOST +
								((Integer)_h.getProperty("r")) * HOST +
								((Integer)_h.getProperty("h"))
							));
					}
				}
			}

			Assert.assertEquals(0, vd.size());
		}

		{
			HashSet<Integer> vd = build(CLUSTER * RACK * HOST);
			for (Rack _r: _d.getRacks()) {
				for (Host _h: _r.getHosts()) {
					vd.remove(Integer.valueOf(
							((Integer)_h.getProperty("c")) * RACK * HOST +
							((Integer)_h.getProperty("r")) * HOST +
							((Integer)_h.getProperty("h"))
						));
				}
			}

			Assert.assertEquals(0, vd.size());
		}

		{
			HashSet<Integer> vd = build(CLUSTER * RACK * HOST);
			for (Host _h: _d.getHosts()) {
				vd.remove(Integer.valueOf(
						((Integer)_h.getProperty("c")) * RACK * HOST +
						((Integer)_h.getProperty("r")) * HOST +
						((Integer)_h.getProperty("h"))
					));
			}

			Assert.assertEquals(0, vd.size());
		}

		{
			HashSet<Integer> vd = build(RACK * HOST);
			for (Rack _r: _d.get(0).getRacks()) {
				for (Host _h: _r.getHosts()) {
					vd.remove(Integer.valueOf(
							((Integer)_h.getProperty("r")) * HOST +
							((Integer)_h.getProperty("h"))
						));
				}
			}

			Assert.assertEquals(0, vd.size());
		}

		{
			HashSet<Integer> vd = build(RACK * HOST);
			for (Host _h: _d.get(0).getHosts()) {
				vd.remove(Integer.valueOf(
						((Integer)_h.getProperty("r")) * HOST +
						((Integer)_h.getProperty("h"))
					));
			}

			Assert.assertEquals(0, vd.size());
		}

		{
			HashSet<Integer> vd = build(HOST);
			for (Host _h: _d.get(0).get(0).getHosts()) {
				vd.remove(
						((Integer)_h.getProperty("h"))
					);
			}

			Assert.assertEquals(0, vd.size());
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test1() {
		{
			for (Host _h : _d.getHosts()) {
				_d.get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test2() {
		{
			for (Host _h : _d.getHosts()) {
				_d.get(0).add(new RackImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test3() {
		{
			for (Host _h : _d.getHosts()) {
				_d.add(new ClusterImpl());
			}
		}
	}

	@Test
	public void test4() {
		{
			for (Rack _r : _d.getRacks()) {
				_d.get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test5() {
		{
			for (Rack _r : _d.getRacks()) {
				_d.get(0).add(new RackImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test6() {
		{
			for (Rack _r : _d.getRacks()) {
				_d.add(new ClusterImpl());
			}
		}
	}

	@Test
	public void test7() {
		{
			for (Cluster _c : _d.getClusters()) {
				_d.get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test
	public void test8() {
		{
			for (Cluster _c : _d.getClusters()) {
				_d.get(0).add(new RackImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test9() {
		{
			for (Cluster _c : _d.getClusters()) {
				_d.add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test10() {
		{
			for (Host _h : _d.get(0).getHosts()) {
				_d.get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test11() {
		{
			for (Host _h : _d.get(0).getHosts()) {
				_d.get(0).add(new RackImpl());
			}
		}
	}

	@Test
	public void test12() {
		{
			for (Host _h : _d.get(0).getHosts()) {
				_d.add(new ClusterImpl());
			}
		}
	}

	@Test
	public void test13() {
		{
			for (Rack _r : _d.get(0).getRacks()) {
				_d.get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test14() {
		{
			for (Rack _r : _d.get(0).getRacks()) {
				_d.get(0).add(new RackImpl());
			}
		}
	}

	@Test
	public void test15() {
		{
			for (Rack _r : _d.get(0).getRacks()) {
				_d.add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test16() {
		{
			for (Host _h : _d.get(0).get(0).getHosts()) {
				_d.get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test
	public void test17() {
		{
			for (Host _h : _d.get(0).get(0).getHosts()) {
				_d.get(0).add(new RackImpl());
			}
		}
	}

	@Test
	public void test18() {
		{
			for (Host _h : _d.get(0).get(0).getHosts()) {
				_d.add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test19() {
		{
			for (Host _h : _cl.getHosts()) {
				_cl.get(0).get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test20() {
		{
			for (Host _h : _cl.getHosts()) {
				_cl.get(0).get(0).add(new RackImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test21() {
		{
			for (Host _h : _cl.getHosts()) {
				_cl.get(0).add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test22() {
		{
			for (Host _h : _cl.getHosts()) {
				_cl.add(new DatacenterImpl());
			}
		}
	}

	@Test
	public void test23() {
		{
			for (Rack _r : _cl.getRacks()) {
				_cl.get(0).get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test24() {
		{
			for (Rack _r : _cl.getRacks()) {
				_cl.get(0).get(0).add(new RackImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test25() {
		{
			for (Rack _r : _cl.getRacks()) {
				_cl.get(0).add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test26() {
		{
			for (Rack _r : _cl.getRacks()) {
				_cl.add(new DatacenterImpl());
			}
		}
	}

	@Test
	public void test27() {
		{
			for (Cluster _c : _cl.getClusters()) {
				_cl.get(0).get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test
	public void test28() {
		{
			for (Cluster _c : _cl.getClusters()) {
				_cl.get(0).get(0).add(new RackImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test29() {
		{
			for (Cluster _c : _cl.getClusters()) {
				_cl.get(0).add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test30() {
		{
			for (Cluster _c : _cl.getClusters()) {
				_cl.add(new DatacenterImpl());
			}
		}
	}

	@Test
	public void test31() {
		{
			for (Datacenter _d : _cl.getDatacenters()) {
				_cl.get(0).get(0).get(0).add(new HostDefault());
			}
		}
	}

	@Test
	public void test32() {
		{
			for (Datacenter _d : _cl.getDatacenters()) {
				_cl.get(0).get(0).add(new RackImpl());
			}
		}
	}

	@Test
	public void test33() {
		{
			for (Datacenter _d : _cl.getDatacenters()) {
				_cl.get(0).add(new ClusterImpl());
			}
		}
	}

	@Test(expected=ConcurrentModificationException.class)
	public void test34() {
		{
			for (Datacenter _d : _cl.getDatacenters()) {
				_cl.add(new DatacenterImpl());
			}
		}
	}
}
