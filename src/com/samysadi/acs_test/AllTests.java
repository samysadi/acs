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

package com.samysadi.acs_test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

import com.samysadi.acs.core.Logger;
import com.samysadi.acs.core.Simulator;

/**
 * Run with <tt>junit</tt>.
 *
 * @since 1.0
 */
@RunWith(AllTests.Runner.class)
public final class AllTests {
	private static final String TEST_PACKAGE = AllTests.class.getName().substring(0, AllTests.class.getName().lastIndexOf('.'));

	private AllTests() {}

	public static class Runner extends Suite {
		public Runner(final Class<?> clazz) throws InitializationError {
			super(clazz, getClasses(getClassesDirectory()));
		}

		@Override
		public void run(final RunNotifier notifier) {
			Logger.DEFAULT_LEVEL = Level.WARNING;

			notifier.addListener(new RunListener() {
				boolean failed;
				@Override
				public void testStarted(final Description description) {
					System.out.println("Running " + description.getDisplayName());
					StringBuilder s = new StringBuilder();
					for (int i = 0; i < description.getDisplayName().length() + 9; i++)
						s.append('¯');
					System.out.println(s.toString());
					failed = false;
				}

				@Override
				public void testFailure(final Failure failure) {
					System.out.println(failure.getDescription().getDisplayName() + " FAILED");
					failed = true;
				}

				@Override
				public void testFinished(final Description description) {
					if (Simulator.getSimulator() != null) {
						if (!Simulator.getSimulator().isStopped())
							Simulator.getSimulator().stop();
						Simulator.getSimulator().free();
					}

					if (!failed)
						System.out.println(description.getDisplayName() + " OK");

					System.out.println();
				}

				@Override
				public void testRunFinished(Result result) throws Exception {
					System.out.print("ALL TESTS FINISHED");
					if (result.getFailureCount() > 0)
						System.out.println(" WITH " + result.getFailureCount() + " FAILURE" + (result.getFailureCount() == 1 ? "" : "S"));
					else
						System.out.println(" SUCCESSFULLY");
				}
			});

			super.run(notifier);
		}

		private static Class<?> getClassFromFile(final File file, final File topDir) {
			String name = file.getPath()
					.substring(topDir.getPath().length() + 1)
					.replace('/', '.').replace('\\', '.');
			name = name.substring(0, name.length() - 6);

			if (!name.startsWith(TEST_PACKAGE))
				return null;

			if (name.contains("$"))
				return null;

			final Class<?> c;
			try {
				c = Class.forName(name);
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e);
			}
			if (!Modifier.isPublic(c.getModifiers()))
				return null;
			if (Modifier.isAbstract(c.getModifiers()))
				return null;

			//see if there is a test method
			boolean found = false;
			MAINLOOP:for (Method m :c.getMethods()) {
				if (!Modifier.isPublic(m.getModifiers()))
					continue;
				if (Modifier.isAbstract(m.getModifiers()))
					continue;
				for (Annotation a: m.getAnnotations()) {
					if (a instanceof org.junit.Test) {
						found = true;
						break MAINLOOP;
					}
				}
			}

			if (!found)
				return null;

			return c;
		}

		private static Class<?>[] getClasses(final File dir) {
			final List<Class<?>> classes = getClasses(dir, dir);
			final Class<?>[] r = classes.toArray(new Class<?>[classes.size()]);

			Arrays.sort(r, new Comparator<Class<?>>() {
				@Override
				public int compare(final Class<?> c1, final Class<?> c2) {
					return c1.getName().compareTo(c2.getName());
				}
			});

			return r;
		}

		private static List<Class<?>> getClasses(final File dir, final File topDir) {
			final List<Class<?>> l = new ArrayList<Class<?>>();
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					l.addAll(getClasses(file, topDir));
				} else if (file.getName().toLowerCase().endsWith(".class")) {
					Class<?> c = getClassFromFile(file, topDir);
					if (c != null)
						l.add(c);
				}
			}
			return l;
		}
	}

	private static File getClassesDirectory() {
		try {
			String path = AllTests.class.getProtectionDomain().getCodeSource()
					.getLocation().getFile();
			return new File(URLDecoder.decode(path, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
