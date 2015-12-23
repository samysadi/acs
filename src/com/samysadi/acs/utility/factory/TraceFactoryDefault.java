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

package com.samysadi.acs.utility.factory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Logger;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.Trace;
import com.samysadi.acs.core.tracing.TraceItem;
import com.samysadi.acs.tracing.CustomProbe;
import com.samysadi.acs.utility.NotificationCodes;

/**
 * 
 * @since 1.0
 */
public class TraceFactoryDefault extends TraceFactory {

	public TraceFactoryDefault(Config config, Probed probed) {
		super(config, probed);
	}

	private static final String Trace_Ext = ".trace";
	private static final Object Simulator_Traces_Output_Key = new Object();

	/**
	 * Returns a list containing traces filenames.
	 * 
	 * @return a list containing traces filenames
	 */
	private static HashMap<Trace<?>, String> getSimulatorTracesOutput() {
		@SuppressWarnings("unchecked")
		HashMap<Trace<?>, String> h = (HashMap<Trace<?>, String>) Simulator.getSimulator().getProperty(Simulator_Traces_Output_Key);
		if (h == null) {
			h = new HashMap<Trace<?>, String>();
			Simulator.getSimulator().setProperty(Simulator_Traces_Output_Key, h);

			NotificationListener n = new TracingListener();

			Simulator.getSimulator().addListener(NotificationCodes.SIMULATOR_STOPPED, n);
		}
		
		return h;
	}

	private static final class TracingListener extends NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			for (Entry<Trace<?>, String> e: getSimulatorTracesOutput().entrySet())
				if (e.getValue() != null) {
					try {
						boolean console = e.getValue().equals("-");
						Writer writer;
						if (console) {
							writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.ISO_8859_1));
							writer.write("# Writing trace " + e.getKey().toString() + " to console\n");
						} else {
							File f = new File(e.getValue()).getCanonicalFile();
							f.mkdirs();

							int i = 0;
							while (true) {
								File ff =  new File(f, e.getKey().toString() + "." + (i++) + Trace_Ext);
								if (!ff.exists()) {
									f = ff;
									break;
								}
							}

							Logger.getGlobal().log(Level.FINE, "Writing trace file: " + f.getAbsolutePath());
							writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), StandardCharsets.ISO_8859_1));
						}
						try {
							if (!e.getKey().getValues().isEmpty()) {
								String unit = Simulator.probeValueUnit(e.getKey().getParent());
								if (!unit.isEmpty())
									unit = "_in_" + unit;
								writer.write("# data format is: time_in_seconds\tprobe_value" + unit + "\n");
							}
							for (TraceItem<?> item: e.getKey().getValues()) {
								writer.write(Simulator.formatTime(item.getTime(), false, false));
								writer.write('\t');
								writer.write(Simulator.probeValueToString(e.getKey().getParent(), item.getValue(), false, false));
								writer.write('\n');
							}
						} finally {
							if (console)
								writer.flush();
							else
								writer.close();
						}
					} catch (IOException e1) {
					}
				}
		}
	}

	protected void dumpToFile(Trace<?> trace, String filename) {
		getSimulatorTracesOutput().put(trace, filename);
	}

	@Override
	public Trace<?> generate() {
		if (TraceFactory.IS_TRACING_DISABLED)
			return null;

		String probe_key = getConfig().getString("Probe", null);
		Probe<?> probe = null;
		if (probe_key == null) {
			String custom_probe_key = getConfig().getString("CustomProbe", null);
			if (custom_probe_key == null)
				return null;
			probe_key = CustomProbe.CUSTOM_PROBE_PREFIX + custom_probe_key;
		}
		try {
			probe = getProbed().getProbe(probe_key);
		} catch (Exception e) {
			getLogger().log("The probe: " + probe_key + " cannot be created", e);
		}
		if (probe == null)
			return null;
		Trace<?> trace = newTrace(null, probe);

		Double delay = getConfig().getDouble("Delay", null);
		if (delay != null)
			trace.setDelay((long) (delay * Simulator.SECOND));

		Integer maxLength = getConfig().getInt("MaxLength", null);
		if (maxLength != null)
			trace.setMaxLength(maxLength);

		String filename;
		final String var_directory = getVarDirectory();

		if ("-".equals(var_directory)) {
			filename = var_directory;
		} else {
			filename = getConfig().getString("Output", null);
			if (filename != null) {
				File ff = new File(filename.toString());
	
				if (!ff.isAbsolute())
					ff = new File(var_directory, ff.getPath());
	
				try {
					ff = ff.getCanonicalFile();
					filename = ff.getAbsolutePath();
				} catch (IOException e) {
					getLogger().log(Level.WARNING, "Trace output directory cannot be resolved (" + trace.getParent().getKey() + ": "+ filename + ").");
					return null;
				}
			} else
				filename = var_directory + "/" + probe.getKey();
		}

		if (!filename.isEmpty())
			dumpToFile(trace, filename);
		trace.setEnabled(true);
		return trace;
	}

	private String getVarDirectory() {
		String var_directory = Simulator.getSimulator().getConfig().getString("VarDirectory", null);
		if ("-".equals(var_directory))
			return "-";
		if (var_directory != null) {
			File ff = new File(var_directory.toString());
			if (!ff.isDirectory() || !ff.isAbsolute()) {
				final String _baseDir = Simulator.getSimulator().getConfig().getString("ConfigDirectory", null);
				final File baseDir = _baseDir == null ? null : new File(_baseDir);
				if (baseDir == null || !baseDir.isDirectory()) {
					getLogger().log(Level.WARNING, "Var directory cannot be resolved.");
					var_directory = null;
					ff = null;
				} else {
					ff = new File(baseDir, ff.getPath());
				}
			}
			if (ff != null) {
				try {
					ff = ff.getCanonicalFile();
					var_directory = ff.getPath();
				} catch (IOException e) {
					getLogger().log(Level.WARNING, "Var directory cannot be resolved.");
					var_directory = null;
				}
			}
			Simulator.getSimulator().getConfig().setString("VarDirectory", var_directory);
		}
		return (var_directory == null) ? "var" : var_directory;
	}
}
