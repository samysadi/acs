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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class is used to keep different configuration values
 * usually read from a configuration file.
 * 
 * <p>Configuration values are contexted.
 * You can get an instance of {@link Config} containing only 
 * configuration values in a given context using {@link Config#addContext(String)} method.
 * Moreover, you can get the parent instance of a {@link Config} using {@link Config#parentContext()}.<br/>
 * Note that contexted instances are transparently cached by this implementation. And modifications
 * to any instance will reflect on the parent/children instances.
 * 
 * <p>There are multiple get and set method to access and modify configuration values (String, Boolean, Short, 
 * Integer, Long, Float, Double).<br/>
 * These methods will only operate in the current context. Though, for get methods you can
 * set a recursive flag in order to look for a configuration value in parent contexts when
 * the configuration value is not present in current context.
 * 
 * <p><u><b>Configuration File Format:</b></u><br/>
 * The configuration is a text file that contains a {@code NAME=VALUE} pair on each line.<br/>
 * For each configuration file, we associate a load context that defines
 * the default context of all the configuration values in that file.
 * This default context can be modified using appropriate directives (see below). But
 * all modifications will ensure that the new context is equal to or is a sub context of the file's load context.
 * 
 * <p>Configuration values are defined using a {@code NAME=VALUE} line.<br/>
 * The NAME part of the line defines the configuration value name which may be split into one or more contexts.
 * The VALUE part contains the string representation of the configuration value.<br/>
 * <u>For example:</u><br/>
 * {@code CloudProvider0.User0.Budget=500}<br/>
 * Tells that the configuration named {@code "CloudProvider0.User0.Budget"}
 * has a value of 500.
 * The dot (.) can be used to determine configuration contexts.
 * You could use any of these next forms to access the previous configuration value:<ul>
 * <li>{@code config.getInt("CloudProvider0.User0.Budget")};
 * <li>{@code config.addContext("CloudProvider0").getInt("User0.Budget")};
 * <li>{@code config.addContext("CloudProvider0").addContext("User0").getInt("Budget")};
 * <li>{@code config.addContext("CloudProvider0.User0").getInt("Budget")}.
 * </ul>
 * Note that some configuration names are reserved and are described below.
 * 
 * <p>Lines starting with a {@code #} are comment lines, and are ignored.
 * 
 * <p>Lines starting with {@code context=}, {@code context=@} or {@code context=%} are special directives
 * that defines the current context. All next configuration values are put under the given context.<ul>
 * <li>{@code context=} computes the current context by appending
 * the given context to the load context of the configuration file (which is empty by default);
 * <li>{@code context=@} computes the current context by appending the given context to the previous 
 * context of the configuration file;
 * <li>{@code context=%} computes the current context by appending the given context to the
 * parent context of the previous context of the configuration file.
 * </ul>
 * When using these directives, a {@code $} can be used at the end of context name to
 * ask the configuration file loader to replace it with a number that is automatically incremented
 * starting from 0.
 * 
 * <p>Lines starting with <i>include=</i> are special directives that asks to include
 * another configuration file.<br/>
 * The load context of the configuration file is 
 * set to the current context.<br/>
 * You can use relative paths when including the configuration file. The path is resolving
 * using the current configuration file path as base path.
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public class Config {
	public final static String DEFAULT_CONFIG_FILENAME = checkConfigFilename(System.getProperty("acs.config", "./etc/config/main.config"));

	public static final String INCLUDE_KEY = "include";
	public static final String CONTEXT_KEY = "context";
	public static final String REMOVE_CONFIG_KEY = "remove_config";
	public static final char CONTEXT_SEPARATOR = '.';
	public static final char CONTEXT_VAR_SYMBOL = '$';
	private static final Pattern CONTEXT_VAR_SYMBOL_PATTERN = Pattern.compile("" + CONTEXT_VAR_SYMBOL, Pattern.LITERAL);
	public static final char CONTEXT_CURRENT_SYMBOL = '@';
	private static final Pattern CONTEXT_CURRENT_SYMBOL_PATTERN = Pattern.compile(Pattern.quote("" + CONTEXT_CURRENT_SYMBOL) + 
			"(" + Pattern.quote("" + CONTEXT_SEPARATOR) + ")*");
	public static final char CONTEXT_PARENT_SYMBOL = '%';
	private static final Pattern CONTEXT_PARENT_SYMBOL_PATTERN = Pattern.compile(Pattern.quote("" + CONTEXT_PARENT_SYMBOL) + 
			"(" + Pattern.quote("" + CONTEXT_SEPARATOR) + ")*");

	private final HashMap<String, Object> config;
	private final WeakHashMap<String, WeakReference<Config>> contextsCache;
	private final String context;

	private static String checkConfigFilename(String filename) {
		if (filename != null && !filename.isEmpty()) {
			try {
				File file = (new File(filename)).getCanonicalFile();
				if (file.isDirectory()) {
					file = new File(file, "main.config");
					if (!file.isFile())
						return filename;
				}
				filename = file.getPath();
			} catch (IOException e) {
			}
		}
		return filename;
	}

	private static String trimContext(String context) {
		int len = context.length();
		int st = 0;
		
		while ((st < len) && (context.charAt(st) == CONTEXT_SEPARATOR))
		    st++;
		while ((st < len) && (context.charAt(len - 1) == CONTEXT_SEPARATOR))
		    len--;
		return ((st > 0) || (len < context.length())) ? context.substring(st, len) : context;
	}

	protected final Logger getLogger() {
		return Logger.getGlobal();
	}

	protected Config(Config cfg) {
		this(cfg, "");
	}

	protected Config(Config config, String context) {
		super();
		this.config = config.config;
		this.contextsCache = config.contextsCache;
		this.context = trimContext(context) + CONTEXT_SEPARATOR;
	}

	private Config getConfigForContext(String context) {
		context = trimContext(context);
		if (!context.isEmpty())
			context += CONTEXT_SEPARATOR;

		if (context.equals(this.context))
			return this;

		WeakReference<Config> w = this.contextsCache.get(context);
		Config r = null;
		if (w == null || ((r=w.get()) == null)) {
			r = new Config(this, context);
			this.contextsCache.put(r.context, new WeakReference<Config>(r));
		}

		return r;
	}

	public Config addContext(String context) {
		return getConfigForContext(this.context + trimContext(context));
	}

	private static String getParentContext(String context) {
		if (context.isEmpty())
			return null;
		int p = context.lastIndexOf(CONTEXT_SEPARATOR, context.length() - 2);
		if (p < 0)
			return "";
		return context.substring(0, p+1);
	}

	public Config parentContext() {
		final String context = getParentContext(this.context);
		if (context == null)
			return null;
		return getConfigForContext(context);
	}

	public Config() {
		this((String)null);
	}

	public Config(String filename) {
		super();

		this.contextsCache = new WeakHashMap<String, WeakReference<Config>>();

		this.context = "";

		this.config = new HashMap<String, Object>();

		if (filename != null && !filename.isEmpty()) {
			File mainDir = null;
			try {
				File file = (new File(filename)).getCanonicalFile();
				filename = file.getPath();
				mainDir = file.getParentFile();
			} catch (IOException e) {
			}
	
			if (!includeConfigFile(mainDir, filename, this.context)) {
				getLogger().log(Level.SEVERE, "Cannot load the configuration file: " + filename);
			} else
				setString("ConfigDirectory", mainDir.getAbsolutePath());
		}
	}

	protected boolean includeConfigFile(File baseDir, String filename, final String loadContext, final HashMap<String, Integer> contextVars) {
		if (filename == null)
			return false;
		final File f;
		{
			File ff = new File(filename);
			if ((baseDir != null) && !ff.isAbsolute()) {
				ff = new File(baseDir, ff.getPath());
			}
			try {
				ff = ff.getCanonicalFile();
			} catch (IOException e) {
				return false;
			}
			f = ff;
		}
		
		final File newBaseDir = f.getParentFile();
		try {
			FileInputStream fis = new FileInputStream(f);
			try {
				@SuppressWarnings("serial")
				Properties p = new Properties() {
					String ctx = "";
					@Override
					public synchronized Object put(Object key, Object value) {
						if (key == null)
							return null;
						if (value == null)
							value = ""; //do not allow null values
						if (INCLUDE_KEY.equals(key)) {
							if (!Config.this.includeConfigFile(newBaseDir, value.toString(), loadContext + ctx, contextVars))
								Config.this.getLogger().log(Level.SEVERE, f.getPath() + ": Cannot include file: \"" + value.toString() + "\".");
							return null;
						} else if (CONTEXT_KEY.equals(key)) {
							String newCtx = trimContext(value.toString());
							if (!newCtx.isEmpty()) {
								int i;

								i = newCtx.lastIndexOf(CONTEXT_CURRENT_SYMBOL);
								if (i > 0) {
									Config.this.getLogger().log(Level.SEVERE, f.getPath() + ": " + CONTEXT_CURRENT_SYMBOL + " must the first and unique character in the context name.");
									return null;
								} else if (i == 0) {
									newCtx = trimContext(CONTEXT_CURRENT_SYMBOL_PATTERN.matcher(newCtx).replaceFirst(ctx));
								}

								i = newCtx.lastIndexOf(CONTEXT_PARENT_SYMBOL);
								if (i > 0) {
									Config.this.getLogger().log(Level.SEVERE, f.getPath() + ": " + CONTEXT_PARENT_SYMBOL + " must the first and unique character in the context name.");
									return null;
								} else if (i == 0) {
									String p = getParentContext(ctx);
									if (p == null)
										p = "";
									newCtx = trimContext(CONTEXT_PARENT_SYMBOL_PATTERN.matcher(newCtx).replaceFirst(p));
								}

								if (!newCtx.isEmpty()) {
									if (newCtx.indexOf(CONTEXT_VAR_SYMBOL) >= 0) {
										Integer v = contextVars.get(loadContext + newCtx);
										if (v == null)
											v = 0;
										else
											v = v + 1;
										contextVars.put(loadContext + newCtx, v);
										newCtx = CONTEXT_VAR_SYMBOL_PATTERN.matcher(newCtx).replaceFirst(v.toString());
										if (newCtx.indexOf(CONTEXT_VAR_SYMBOL) >= 0) {
											Config.this.getLogger().log(Level.SEVERE, f.getPath() + ": " + CONTEXT_VAR_SYMBOL + " can be used once in each context name.");
											return null;
										}
									}
	
									newCtx = newCtx + CONTEXT_SEPARATOR;
								}
							}
							ctx = newCtx;
							return null;
						} else if (REMOVE_CONFIG_KEY.equals(key)) {
							String v = value.toString();
							if (!v.isEmpty()) {
								if (v.charAt(0) == '^') {
									v = v.substring(1);
								} else {
									v = ".*" + v;
								}
							}
							if (v.isEmpty())
								return null;
							Pattern rmctx;
							try {
								rmctx = Pattern.compile("^" + Pattern.quote(loadContext + ctx + CONTEXT_SEPARATOR) + v);
							} catch (PatternSyntaxException e) {
								Config.this.getLogger().log(Level.SEVERE, f.getPath() + ": Cannot remove the given config key (bad RegExp) :\"" + value.toString() + "\"");
								return null;
							}
							Iterator<Entry<String, Object>> it = Config.this.config.entrySet().iterator();
							while (it.hasNext()) {
								Entry<String, Object> e = it.next();
								if (rmctx.matcher(e.getKey()).matches())
									it.remove();
							}
							return null;
						}


						//if (!(value instanceof String))
						//	value = value.toString();
						if (((String)value).isEmpty())
							return null;

						if (!(key instanceof String))
							key = key.toString();

						Config.this.config.put(loadContext + ctx + trimContext((String) key), value);

						return null;
					}
				};
				p.load(fis);
			} finally {
				fis.close();
			}
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	public boolean includeConfigFile(String filename) {
		return includeConfigFile((String) null, filename);
	}

	public boolean includeConfigFile(String filename, String loadContext) {
		return includeConfigFile(null, filename, loadContext);
	}

	public boolean includeConfigFile(File baseDir, String filename) {
		return includeConfigFile(baseDir, filename, this.context);
	}

	public boolean includeConfigFile(File baseDir, String filename, String loadContext) {
		return includeConfigFile(baseDir, filename, loadContext, new HashMap<String, Integer>());
	}

	public Iterator<String> getConfigNamesIterator() {
		return new ConfigNamesIterator(this);
	}

	private static class ConfigNamesIterator implements Iterator<String> {
		private String next;
		private Iterator<String> it;
		private String context;

		public ConfigNamesIterator(Config config) {
			it = config.config.keySet().iterator();
			context = config.context;
			findNext();
		}

		private void findNext() {
			while (it.hasNext()) {
				next = it.next();
				if (next.startsWith(context)) {
					next = next.substring(context.length());
					return;
				}
			}
			next = null;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public String next() {
			if (next == null)
				throw new NoSuchElementException();
			String n = next;
			findNext();
			return n;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public String getContext() {
		return trimContext(this.context);
	}

	public final boolean isEmpty() {
		return isEmpty(false);
	}

	public boolean isEmpty(boolean recursive) {
		if (recursive || this.context.isEmpty())
			return this.config.isEmpty();

		for (Entry<String, Object> e: this.config.entrySet())
			if (e.getKey().startsWith(this.context))
				return false;

		return true;
	}

	public final boolean hasConfig(String configName) {
		return hasConfig(configName, false);
	}

	public boolean hasConfig(String configName, boolean recursive) {
		return getConfig(configName, null, recursive) != null;
	}

	protected final Object getConfig(String configName, Object defaultValue) {
		return getConfig(configName, defaultValue, false);
	}

	/**
	 * Returns the configuration value that matches the given <tt>configName</tt> or 
	 * <tt>defaultValue</tt> if none is found.
	 * 
	 * <p>Set the <tt>recursive</tt> parameter to <tt>true</tt> if you want this method
	 * to look for the configuration value in parent contexts if there is no matching
	 * configuration in current context.
	 * 
	 * @param configName
	 * @param defaultValue
	 * @param recursive
	 * @return configuration value that matches the given <tt>configName</tt> or <tt>defaultValue</tt>
	 * if none is found
	 */
	protected Object getConfig(String configName, Object defaultValue, boolean recursive) {
		Object value;
		String context = this.context;
		do {
			value = this.config.get(context + configName);
			if (value != null)
				return value;
			if (!recursive)
				break;
			context = getParentContext(context);
			if (context == null)
				break;
		} while (true);
		return defaultValue;
	}

	protected void setConfig(String configName, Object value) {
		if (value == null)
			this.config.remove(this.context + configName);
		else
			this.config.put(this.context + configName, value);
	}

//	private static String stripDecimal(String value) {
//		int len = value.length();
//		int e = 0;
//		while (true) {
//			if (e == len)
//				return value;
//			if (value.charAt(e) == '.')
//				return value.substring(0, e);
//			e++;
//		}
//	}

	public final String getString(String configName, String defaultValue) {
		return getString(configName, defaultValue, false);
	}

	public String getString(String configName, String defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;
		return r.toString();
	}

	public void setString(String configName, String value) {
		setConfig(configName, value);
	}

	public final Boolean getBoolean(String configName, Boolean defaultValue) {
		return getBoolean(configName, defaultValue, false);
	}

	public Boolean getBoolean(String configName, Boolean defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Boolean)
			return (Boolean) r;

		if (r instanceof Number) {
			return ((Number) r).longValue() != 0;
		}

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Boolean v;
			if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on"))
				v = Boolean.TRUE;
			else if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off"))
				v = Boolean.FALSE;
			else {
				v = Long.parseLong(s) != 0;
			}
			setConfig(configName, v);
			return v;
		} catch (NumberFormatException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Boolean: " + this.context + configName);
			return defaultValue;
		}
	}

	public void setBoolean(String configName, Boolean value) {
		setConfig(configName, value);
	}

	public final Byte getByte(String configName, Byte defaultValue) {
		return getByte(configName, defaultValue, false);
	}

	public Byte getByte(String configName, Byte defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Byte)
			return (Byte) r;

		if (r instanceof Number) {
			byte v = ((Number) r).byteValue();
			if (r instanceof Short) {
				if (v != ((Number) r).shortValue())
					return defaultValue;
			} else if (r instanceof Integer) {
				if (v != ((Number) r).intValue())
					return defaultValue;
			} else if (r instanceof Long) {
				if (v != ((Number) r).longValue())
					return defaultValue;
			} else if (r instanceof Float) {
				if (v != ((Number) r).floatValue())
					return defaultValue;
			} else if (r instanceof Double) {
				if (v != ((Number) r).doubleValue())
					return defaultValue;
			}
			return Byte.valueOf(v);
		}

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Byte v = Byte.valueOf(s);
			setConfig(configName, v);
			return v;
		} catch (NumberFormatException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Byte: " + this.context + configName);
			return defaultValue;
		}
	}

	public void setByte(String configName, Byte value) {
		setConfig(configName, value);
	}

	public final Short getShort(String configName, Short defaultValue) {
		return getShort(configName, defaultValue, false);
	}

	public Short getShort(String configName, Short defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Short)
			return (Short) r;

		if (r instanceof Number) {
			short v = ((Number) r).shortValue();
			if (r instanceof Integer) {
				if (v != ((Number) r).intValue())
					return defaultValue;
			} else if (r instanceof Long) {
				if (v != ((Number) r).longValue())
					return defaultValue;
			} else if (r instanceof Float) {
				if (v != ((Number) r).floatValue())
					return defaultValue;
			} else if (r instanceof Double) {
				if (v != ((Number) r).doubleValue())
					return defaultValue;
			}
			return Short.valueOf(v);
		}

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Short v = Short.valueOf(s);
			setConfig(configName, v);
			return v;
		} catch (NumberFormatException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Short: " + this.context + configName);
			return defaultValue;
		}
	}

	public void setShort(String configName, Short value) {
		setConfig(configName, value);
	}

	public final Integer getInt(String configName, Integer defaultValue) {
		return getInt(configName, defaultValue, false);
	}

	public Integer getInt(String configName, Integer defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Integer)
			return (Integer) r;

		if (r instanceof Number) {
			int v = ((Number) r).intValue();
			if (r instanceof Long) {
				if (v != ((Number) r).longValue())
					return defaultValue;
			} else if (r instanceof Float) {
				if (v != ((Number) r).floatValue())
					return defaultValue;
			} else if (r instanceof Double) {
				if (v != ((Number) r).doubleValue())
					return defaultValue;
			}
			return Integer.valueOf(v);
		}

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Integer v = Integer.valueOf(s);
			setConfig(configName, v);
			return v;
		} catch (NumberFormatException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Integer: " + this.context + configName);
			return defaultValue;
		}
	}

	public void setInt(String configName, Integer value) {
		setConfig(configName, value);
	}

	public final Long getLong(String configName, Long defaultValue) {
		return getLong(configName, defaultValue, false);
	}

	public Long getLong(String configName, Long defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Long)
			return (Long) r;

		if (r instanceof Number) {
			long v = ((Number) r).longValue();
			if (r instanceof Float) {
				if (v != ((Number) r).floatValue())
					return defaultValue;
			} else if (r instanceof Double) {
				if (v != ((Number) r).doubleValue())
					return defaultValue;
			}
			return Long.valueOf(v);
		}

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Long v = Long.valueOf(s);
			setConfig(configName, v);
			return v;
		} catch (NumberFormatException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Long: " + this.context + configName);
			return defaultValue;
		}
	}

	public void setLong(String configName, Long value) {
		setConfig(configName, value);
	}

	public final Float getFloat(String configName, Float defaultValue) {
		return getFloat(configName, defaultValue, false);
	}

	public Float getFloat(String configName, Float defaultValue, boolean recursive) {
		final Double d = getDouble(configName, null, recursive);
		if (d == null)
			return defaultValue;
		return d.floatValue();
	}

	public void setFloat(String configName, Float value) {
		setConfig(configName, value);
	}

	public final Double getDouble(String configName, Double defaultValue) {
		return getDouble(configName, defaultValue, false);
	}

	public Double getDouble(String configName, Double defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Double)
			return (Double) r;

		if (r instanceof Number)
			return Double.valueOf(((Number) r).doubleValue());

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Double v = Double.valueOf(s);
			setConfig(configName, v);
			return v;
		} catch (NumberFormatException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Double: " + this.context + configName);
			return defaultValue;
		}
	}

	public void setDouble(String configName, Double value) {
		setConfig(configName, value);
	}

	public final Class<?> getClassFromConfig(String configName, Class<?> defaultValue) {
		return getClassFromConfig(configName, defaultValue, false);
	}

	public Class<?> getClassFromConfig(String configName, Class<?> defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Class)
			return (Class<?>) r;

		final String s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Class<?> clazz = Class.forName(s);
			setConfig(configName, clazz);
			return clazz;
		} catch (ClassNotFoundException e) {
			getLogger().log(Level.SEVERE, "Class " + s + " not found. This also may lead to performance issues.");
			return defaultValue;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Iterator<String> it = this.getConfigNamesIterator();
		while (it.hasNext()) {
			String n = it.next();
			sb.append('\n').append('\t').append(n).append(" = ").append(this.getString(n, "null"));
		}
		sb.append('\n').append('}');
		return sb.toString();
	}
}
