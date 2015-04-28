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

package com.samysadi.acs_test.core.entity;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.entity.RunnableEntity.RunnableState;
import com.samysadi.acs.core.event.EventImpl;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs_test.Utils;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
@SuppressWarnings("unused")
public abstract class RunnableEntityTest {
	protected Simulator simulator;
	protected CloudProvider cloudProvider;

	@After
	public void afterTest() {
		simulator.stop();
		simulator.free();
	}

	@Before
	public void prepareTest() {
		simulator = Utils.newSimulator();
		cloudProvider = simulator.getCloudProviders().get(0);
		Utils.generateTopology0(simulator);
	}

	private volatile AssertionError exc;

	public abstract RunnableEntity startRunnableEntity();

	public abstract RunnableEntity startChildForRunnableEntity(RunnableEntity parent);

	@Test
	public void test0() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final RunnableEntity r0 = startRunnableEntity();
				r0.doPause();
				
				final RunnableEntity c0 = startChildForRunnableEntity(r0);
			}
		});

		try {
			simulator.start();
			exc = new AssertionError("Paused RunnableEntity does not throw exception, when starting a child.", null);
		} catch (Exception e) {
			//ok
		}
		if (exc != null)
            throw exc;
	}

	@Test
	public void test1() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final RunnableEntity r0 = startRunnableEntity();
				
				final RunnableEntity c0 = startChildForRunnableEntity(r0);

				simulator.schedule(1, new EventImpl() {
					@Override
					public void process() {
						r0.doPause();
						try {
							Assert.assertEquals("Child RunnableEntity state (" + c0.getRunnableState() +
									") not valid after its parent RunnableEntity state was updated (" + r0.getRunnableState() + ").",
									r0.getRunnableState(), c0.getRunnableState());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
	}

	@Test
	public void test2() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final RunnableEntity r0 = startRunnableEntity();
				
				final RunnableEntity c0 = startChildForRunnableEntity(r0);

				simulator.schedule(1, new EventImpl() {
					@Override
					public void process() {
						r0.doCancel();
						try {
							Assert.assertEquals("Child RunnableEntity state (" + c0.getRunnableState() +
									") not valid after its parent RunnableEntity state was updated (" + r0.getRunnableState() + ").",
									r0.getRunnableState(), c0.getRunnableState());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
	}

	@Test
	public void test3() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final RunnableEntity r0 = startRunnableEntity();
				
				final RunnableEntity c0 = startChildForRunnableEntity(r0);

				simulator.schedule(1, new EventImpl() {
					@Override
					public void process() {
						r0.doFail();
						try {
							Assert.assertEquals("Child RunnableEntity state (" + c0.getRunnableState() +
									") not valid after its parent RunnableEntity state was updated (" + r0.getRunnableState() + ").",
									r0.getRunnableState(), c0.getRunnableState());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
	}

	@Test
	public void test4() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final RunnableEntity r0 = startRunnableEntity();
				
				final RunnableEntity c0 = startChildForRunnableEntity(r0);

				simulator.schedule(1, new EventImpl() {
					@Override
					public void process() {
						r0.doPause();
						r0.doRestart();
						try {
							Assert.assertEquals("Child RunnableEntity state (" + c0.getRunnableState() +
									") not valid after its parent RunnableEntity state was updated (" + r0.getRunnableState() + ").",
									RunnableState.CANCELED, c0.getRunnableState());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
	}

	@Test
	public void test5() {
		simulator.schedule(new EventImpl() {
			@Override
			public void process() {
				final RunnableEntity r0 = startRunnableEntity();
				
				final RunnableEntity c0 = startChildForRunnableEntity(r0);

				simulator.schedule(1, new EventImpl() {
					@Override
					public void process() {
						r0.doPause();
						r0.doStart();
						try {
							Assert.assertEquals("Child RunnableEntity state (" + c0.getRunnableState() +
									") not valid after its parent RunnableEntity state was updated (" + r0.getRunnableState() + ").",
									r0.getRunnableState(), c0.getRunnableState());
						} catch (AssertionError e) {
							exc = e;
						}
					}
				});
			}
		});

		simulator.start();
		if (exc != null)
            throw exc;
	}
}
