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

package com.samysadi.acs.core;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import com.samysadi.acs.core.entity.Entity;

/**
 * 
 * @since 1.0
 */
public class Logger {
	private java.util.logging.Logger logger = null;

	public Logger() {
		super();

		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");
		this.logger = java.util.logging.Logger.getAnonymousLogger();
		this.logger.setFilter(null);
		this.logger.setLevel(Simulator.LOG_LEVEL);

		for (Handler handler: this.logger.getHandlers()) {
			this.logger.removeHandler(handler);
		}

		this.logger.setUseParentHandlers(false);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Simulator.LOG_LEVEL);
		this.logger.addHandler(handler);
	}

	public boolean isLoggable(Level level) {
		return this.logger.isLoggable(level);
	}

	public void log(Level logLevel, Entity entity, String message, Throwable thrown) {
		String s = "";
		if (Simulator.getSimulator() != null)
			s = Simulator.formatTime(Simulator.getSimulator().getTime()) + " > ";
		else
			s = "";
		if (entity != null)
			s+= entity + " > ";
		s+= message;
		if (thrown != null)
			this.logger.log(logLevel, s, thrown);
		else
			this.logger.log(logLevel, s);
	}

	public void log(Level logLevel, Entity entity, String message) {
		log(logLevel, entity, message, null);
	}

	public void log(Entity entity, String message, Throwable thrown) {
		log(Level.FINE, entity, message, thrown);
	}

	public void log(Entity entity, String message) {
		log(entity, message, null);
	}

	public void log(String message, Throwable thrown) {
		log((Entity) null, message, thrown);
	}

	public void log(String message) {
		log(message, null);
	}

	public void log(Level logLevel, String message, Throwable thrown) {
		log(logLevel, null, message, thrown);
	}

	public void log(Level logLevel, String message) {
		log(logLevel, message, null);
	}

	public void logInstantiationException(Class<?> clazz, Exception e) {
		Throwable t = e;
		if (e instanceof InvocationTargetException &&
				e.getCause() != null) {
			t = e.getCause();
			if (t instanceof RuntimeException)
				throw (RuntimeException) e.getCause();
			else if (t instanceof Exception)
				throw new RuntimeException(e.getCause());
		}
		log(Level.SEVERE, "Cannot instantiate the class " + clazz.getName(), t);
	}

	private final static Logger global = new Logger();

	/**
	 * Returns current simulator's logger or a global logger.
	 * 
	 * <p>A global logger is returned if no simulator is accessible in the current thread.
	 * 
	 * @return current simulator's logger or a global logger
	 */
	public static Logger getGlobal() {
		if (Simulator.getSimulator() == null)
			return global;
		return Simulator.getSimulator().getLogger();
	}
}
