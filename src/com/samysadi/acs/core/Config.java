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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is used to keep configuration values that can be fetch from a configuration file.
 * 
 * <p>Configuration values are contexted.
 * You can get an instance of {@link Config} containing only 
 * configuration values in a given context using {@link Config#addContext(String)} method.
 * Moreover, you can get the parent instance of a {@link Config} using {@link Config#parentContext()}.<br/>
 * Note that contexted instances are transparently cached by this implementation. And modifications
 * to any instance will reflect on the parent/children instances.
 * 
 * <p>There are multiple get and set methods to access and modify configuration values (String, Boolean, Short, 
 * Integer, Long, Float, Double).<br/>
 * These methods will only operate in the current context. Though, for get methods you can
 * set a recursive flag in order to look for a configuration value in parent contexts when
 * the configuration value is not present in current context.
 * 
 * <p><u><b>Configuration File Format:</b></u><br/>
 * The configuration can be read from a xml file where tags define configuration names (i.e. keys), and tag contents define the configuration values.
 * Each configuration can have an id attribute which is used to refer back to it (in order to remove for instance).<br/>
 * When defining children elements, their context will be defined using the context and the configuration name of their parent element.
 * So, for instance, if an element {@code A} in the context {@code C} has a child {@code B}, then the context of {@code B} will be {@code C.A}.<br/> 
 * 
 * <p>We define some special tags that cannot be used as configuration names. These are listed in the following:
 * <ul>
 * <li><b>&lt;include&gt;</b> includes another configuration file in the current context. If the filename is not absolute,
 * the file is searched in the same directory as the current configuration file. Also, relative paths are determined based on the directory
 * of the current configuration file.
 * <li><b>&lt;AddFoo&gt;</b> adds a configuration named Foo in the current context. This configuration does not replace existing configurations named Foo.
 * Instead, we keep a counter internally so that when you call multiple AddFoo, we actually create Foo#0, Foo#1, ... etc.
 * If you want to create the configuration Foo, then use directly the tag &lt;Foo&gt;.
 * <li><b>&lt;EditFoo&gt;</b> sets the context to the one defined by
 * the id attribute and the Foo tag. This method is useful to edit configurations inside contexts created
 * using the special &lt;AddFoo&gt; tag.
 * <li><b>&lt;RemoveFoo&gt;</b> removes the configuration named Foo (or Foo#n where n is a number) in the current context. You must give its id using the id attribute.
 * if you use the wildcard (*) value for the id attribute, then all configurations in the current context whose name starts with Foo are removed.
 * <li><b>&lt;Remove&gt;</b> same as &lt;RemoveFoo&gt; but applies for all configurations in the current context independently from their name.
 * </ul>
 * 
 * @since 1.0
 */
public class Config {
	public final static String DEFAULT_CONFIG_FILENAME = checkConfigFilename(System.getProperty("acs.config", "./etc/config/main.xml"));

	public static final char CONTEXT_SEPARATOR = '.';
	public static final char CONTEXT_ARRAY_SEPARATOR = '#';

	private final HashMap<String, Object> config;
	private final WeakHashMap<String, WeakReference<Config>> contextsCache;
	/**
	 * Current context of the Config. Either empty (""), or ending with CONTEXT_SEPARATOR
	 */
	private final String context;

	private static String checkConfigFilename(String filename) {
		if (filename != null && !filename.isEmpty()) {
			try {
				File file = (new File(filename)).getCanonicalFile();
				if (file.isDirectory()) {
					file = new File(file, "main.xml");
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
		String c = trimContext(context);
		this.context = c.isEmpty() ? "" : c + CONTEXT_SEPARATOR;
	}

	/**
	 * 
	 * @param stdContext Must contain a trailing {@link Config#CONTEXT_SEPARATOR} (if not empty).
	 * @return a config with the given context
	 */
	private Config getConfigForContext(String stdContext) {
		if (stdContext.equals(this.context))
			return this;

		WeakReference<Config> w = this.contextsCache.get(stdContext);
		Config r = null;
		if (w == null || ((r=w.get()) == null)) {
			r = new Config(this, stdContext);
			this.contextsCache.put(r.context, new WeakReference<Config>(r));
		}

		return r;
	}

	public Config addContext(String context) {
		context = trimContext(context);
		if (!context.isEmpty())
			context += CONTEXT_SEPARATOR;
		return getConfigForContext(this.context + context);
	}

	public Config addContext(String context, int index) {
		return addContext(context + CONTEXT_ARRAY_SEPARATOR + String.valueOf(index));
	}

	public boolean hasContext(String context) {
		return getConfig(context, null) != null;
	}

	public boolean hasContext(String context, int index) {
		return getConfig(context + CONTEXT_ARRAY_SEPARATOR + String.valueOf(index), null) != null;
	}

	/**
	 * 
	 * @param stdContext Must contain a trailing {@link Config#CONTEXT_SEPARATOR} (if not empty).
	 * @return parent context with a trailing {@link Config#CONTEXT_SEPARATOR} (if not empty).
	 */
	private static String getParentContext(String stdContext) {
		if (stdContext.isEmpty())
			return null;
		int p = stdContext.lastIndexOf(CONTEXT_SEPARATOR, stdContext.length() - 2);
		if (p < 0)
			return "";
		return stdContext.substring(0, p+1);
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
				//
			} else
				setString("ConfigDirectory", mainDir.getAbsolutePath());
		}
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

	public final Level getLevel(String configName, Level defaultValue) {
		return getLevel(configName, defaultValue, false);
	}

	public Level getLevel(String configName, Level defaultValue, boolean recursive) {
		Object r = getConfig(configName, defaultValue, recursive);
		if (r == null)
			return null;

		if (r instanceof Level)
			return (Level) r;

		final String s;
		if (r instanceof Number)
			s = String.valueOf(((Number) r).intValue());
		else
			s = r.toString();
		if (s.isEmpty())
			return defaultValue;

		try {
			Level v = Level.parse(s);
			setConfig(configName, v);
			return v;
		} catch (IllegalArgumentException e) {
			getLogger().log(Level.WARNING, "Config value is not a correct Level: " + this.context + configName);
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

	/*
	 * Configuration parsing
	 * *********************************************************************
	 */

	protected static class ConfigIncludeTrace extends LinkedList<String> {
		private static final long serialVersionUID = 1L;

		public ConfigIncludeTrace() {
			super();
		}

		@Override
		public String toString() {
			return (size() == 0 ? "" : getLast() + ": ");
		}
		
	}

	protected boolean includeConfigFile(File baseDir, String filename, ConfigIncludeTrace includeTrace, String loadContext) {
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
				Config.this.getLogger().log(Level.SEVERE, includeTrace.toString() +
						"Configuration file not found: \"" + filename + "\".");
				return false;
			}
			f = ff;
		}

		final File newBaseDir = f.getParentFile();
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			try {
				//check file extension
				String ext = f.getName();
				int i = ext.lastIndexOf('.');
				ext = i == -1 ? "" : ext.substring(i+1).toLowerCase();

				includeTrace.add(f.getPath());

				boolean loaded = false;
				if ("xml".equals(ext)) {
					loaded = includeXMLConfigFile(br, newBaseDir, includeTrace, loadContext);
				} else if ("config".equals(ext)) {
					includeTrace.removeLast();
					Config.this.getLogger().log(Level.SEVERE, includeTrace.toString() +
							"Old .config files are not supported anymore. Please use XML format instead for file: \"" + f.getPath() + "\".");
					return false;
				} else {
					includeTrace.removeLast();
					Config.this.getLogger().log(Level.SEVERE, includeTrace.toString() +
							"Configuration file extension unknown: \"" + f.getPath() + "\".");
					return false;
				}

				if (!loaded) {
					includeTrace.removeLast();
					Config.this.getLogger().log(Level.SEVERE, includeTrace.toString() +
							"Error when loading configuration file: \"" + f.getPath() + "\".");
					return false;
				}
				
			} finally {
				br.close();
			}
		} catch (FileNotFoundException e) {
			Config.this.getLogger().log(Level.SEVERE, includeTrace.toString() +
					"Configuration file not found: \"" + f.getPath() + "\".");
			return false;
		} catch (IOException e) {
			Config.this.getLogger().log(Level.SEVERE, includeTrace.toString() +
					"Configuration file cannot be opened: \"" + f.getPath() + "\".");
			return false;
		}

		return true;
	}

	public final boolean includeConfigFile(String filename) {
		return includeConfigFile((String) null, filename);
	}

	public final boolean includeConfigFile(String filename, String loadContext) {
		return includeConfigFile(null, filename, loadContext);
	}

	public final boolean includeConfigFile(File baseDir, String filename) {
		return includeConfigFile(baseDir, filename, this.context);
	}

	public final boolean includeConfigFile(File baseDir, String filename, String loadContext) {
		return includeConfigFile(baseDir, filename, new ConfigIncludeTrace(), loadContext);
	}

	/*
	 * XMLConfig
	 * *********************************************************************
	 */

	public static final String ROOT_TAG = "config";
	public static final String INCLUDE_TAG = "include";
	public static final String ADD_TAG = "add";
	public static final String EDIT_TAG = "edit";
	public static final String REMOVE_TAG = "remove";
	public static final String ID_ATTRIBUTE = "id";
	public static final String WILDCARD_ID_ATTRIBUTE_VALUE = "*";
	

	public static final String DEFAULT_SAX_FACTORY = "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"; 

	private static HashMap<String, SAXParserFactory> parserFactories = new HashMap<String, SAXParserFactory>();
	protected static SAXParserFactory getSAXParserFactory(String driver) {
		SAXParserFactory rtr = parserFactories.get(driver);
		if (rtr == null) {
			rtr = SAXParserFactory.newInstance(driver, null);
			try {
				rtr.setNamespaceAware(true);
				rtr.setValidating(false);
				rtr.setXIncludeAware(false);
				rtr.setSchema(null);
			} catch(UnsupportedOperationException e) { }

			String falseParams[] = new String[] {
					"external-general-entities",
					"resolve-dtd-uris",
					"external-parameter-entities",
					"validation",
					"nonvalidating/load-external-dtd"
			};
			for (String s: falseParams) {
				try {
					rtr.setFeature("http://xml.org/sax/features/" + s, false);
				} catch (Exception e) {}
			}
	
			parserFactories.put(driver, rtr);
		}
		return rtr;
	}

	protected SAXParserFactory getSAXParserFactory() {
		return getSAXParserFactory(DEFAULT_SAX_FACTORY);
	}

	private enum IncludeXMLConfigFileAction {
		NONE, DEFAULT, ADD, EDIT, REMOVE, INCLUDE
	}

	/**
	 * Removes configurations belonging to the given context and all configurations belonging to 
	 * sub-contexts of the given context.
	 */
	private void _removeContext(String key) {
		Iterator<Entry<String, Object>> it = Config.this.config.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> e = it.next();
			if (e.getKey().equals(key) || e.getKey().startsWith(key + CONTEXT_SEPARATOR))
				it.remove();
		}
	}

	private void _renameContext(String oldKey, String newKey) {
		HashMap<String, Object> m = new HashMap<String, Object>();

		Iterator<Entry<String, Object>> it = Config.this.config.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> e = it.next();
			if (e.getKey().equals(oldKey) || e.getKey().startsWith(oldKey + CONTEXT_SEPARATOR)) {
				String k = (oldKey.length() >= e.getKey().length()) ?
						newKey :
						newKey + e.getKey().substring(oldKey.length());
				m.put(k, e.getValue());
				it.remove();
			}
		}

		Config.this.config.putAll(m);
	}

	/**
	 * Looks for all configuration belonging to the given context
	 * and removes them.
	 * This method will also remove all contexts whose parent is the given context.
	 * 
	 * @param stdContext a context name with a trailing {@link Config#CONTEXT_SEPARATOR} if not empty.
	 */
	private void removeContext(String stdContext) {
			if (stdContext.isEmpty())
				return;
			_removeContext(stdContext);

			//make sure other contexts names keep consistent
			StringBuilder si = new StringBuilder();
			int i = stdContext.length();
			while (i > 0) {
				i--;
				char c = stdContext.charAt(i);
				if (Character.isDigit(c))
					si.append(c);
				else
					break;
			}

			if (si.length() > 0) {
				String key0 = stdContext.substring(0, i + 1);

				si.reverse();
				i = Integer.valueOf(si.toString());

				String newKey = stdContext;
				int k = i + 1;
				while (true) {
					String oldKey = key0 + String.valueOf(k);
					if (!Config.this.config.containsKey(oldKey))
						break;
					_renameContext(oldKey, newKey);
					k++;
					newKey = oldKey;
				}
			}
	}

	protected boolean includeXMLConfigFile(BufferedReader br, final File baseDir,
			final ConfigIncludeTrace includeTrace, final String loadContext) throws IOException {
		XMLReader rtr;
		{
			SAXParserFactory factory = getSAXParserFactory();
			String factoryDesc = factory == null ? "null" : factory.getClass().getName();

			try {
				rtr = getSAXParserFactory().newSAXParser().getXMLReader();
			} catch (SAXException e) {
				Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Error when initiating XML reader (using factory:" + factoryDesc + ").");
				return false;
			} catch (ParserConfigurationException e) {
				Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Error when initiating XML reader (using factory:" + factoryDesc + ").");
				return false;
			}
		}

		rtr.setContentHandler(new DefaultHandler() {
			StringBuilder cont = new StringBuilder();
			Pattern trimmer = Pattern.compile("^\\s+|\\s+$");

			String ctx = "";
			String lastId = null;

			boolean in_root_tag = false;

			boolean childrenElementsForbidden = false;
			IncludeXMLConfigFileAction ixcfa = IncludeXMLConfigFileAction.NONE;

			private void remove(String tag, String id) {
				HashSet<String> toRemove = new HashSet<String>();
				if (tag.isEmpty() && id.equals(WILDCARD_ID_ATTRIBUTE_VALUE)) {
					toRemove.add(trimContext(loadContext + ctx));
				} else {
					String _ctx = trimContext(loadContext + ctx + tag);

					Iterator<Entry<String, Object>> it = Config.this.config.entrySet().iterator();
					MAINLOOP:while (it.hasNext()) {
						Entry<String, Object> e = it.next();
						if (!_ctx.isEmpty()) {
							if (e.getKey().startsWith(_ctx)) {
								//make sure _ctx is not something like _ctx + "abc", in which case it's another context
								int k = _ctx.length();
								while (k < e.getKey().length()) {
									char c = e.getKey().charAt(k);
									if (c == CONTEXT_SEPARATOR)
										break;
									if (!(Character.isDigit(c) || (c == CONTEXT_ARRAY_SEPARATOR)))
										continue MAINLOOP;
									k++;
								}
							} else
								continue; //config is in another context, don't remove
						}

						//at this point config is a candidate to be removed, make sure id matches too
						if (WILDCARD_ID_ATTRIBUTE_VALUE.equals(id)) {
							int p = e.getKey().indexOf(CONTEXT_SEPARATOR, _ctx.length());
							if (p == -1)
								toRemove.add(e.getKey());
							else
								toRemove.add(e.getKey().substring(0, p));
						} else {
							String id0key = e.getKey() + CONTEXT_SEPARATOR + ID_ATTRIBUTE;
							Object id0 = Config.this.config.get(id0key);
							if (id0 != null && id0.toString().equals(id)) {
								toRemove.add(e.getKey());
							}
						}
					}
				}

				for (String key: toRemove)
					Config.this.removeContext(key);
			}

			private String findEditContext(String tag, String id) {
				String _ctx = loadContext + ctx;

				int i = 0;
				while (true) {
					String tag0 = tag + CONTEXT_ARRAY_SEPARATOR + String.valueOf(i); 
					String ctx0 = _ctx + tag0;
					String ctx0id = ctx0 + CONTEXT_SEPARATOR + ID_ATTRIBUTE;
					if (Config.this.config.containsKey(ctx0)) {
						if (Config.this.config.containsKey(ctx0id)) {
							Object _id0 = Config.this.config.get(ctx0id);
							if (_id0 != null && _id0.toString().equals(id)) {
								return tag0 + CONTEXT_SEPARATOR;
							}
						}
					} else
						break;
					i++;
				}

				return null;
			}

			private Boolean isTagPrefixed(String rawTag, String pref) {
				return pref.compareToIgnoreCase(rawTag.substring(0, Math.min(pref.length(), rawTag.length()))) == 0;
			}

			private String extractTag(String rawTag, String pref) {
				return pref.length() >= rawTag.length() ? "" : rawTag.substring(pref.length());
			}

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes atts) throws SAXException {
				String tag = localName;
				if (childrenElementsForbidden) {
					Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Children elements not allowed For the element \"" + tag + "\".");
					throw new SAXException("Malformed document");
				}

				ixcfa = IncludeXMLConfigFileAction.NONE;
				if (!in_root_tag) {
					if (tag.equalsIgnoreCase(ROOT_TAG)) {
						in_root_tag = true;
						return;
					} else {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Root tag of the document must be \"" + ROOT_TAG + "\".");
						throw new SAXException("Malformed document");
					}
				}

				if (tag.indexOf(CONTEXT_SEPARATOR) >= 0) {
					Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Element name (\"" + tag + "\") contains illegal character.");
					throw new SAXException("Malformed document");
				} else if (tag.equalsIgnoreCase(ROOT_TAG) || tag.equalsIgnoreCase(ID_ATTRIBUTE)) {
					Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Element name \"" + tag + "\" not allowed.");
					throw new SAXException("Malformed document");
				} else if (Character.isDigit(tag.charAt(tag.length()-1))) {
					Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Element names ending with digits are reserved (\"" + tag + "\" not allowed).");
					throw new SAXException("Malformed document");
				}

				String id = atts.getValue(ID_ATTRIBUTE); 
				lastId = id;

				if (INCLUDE_TAG.compareToIgnoreCase(tag) == 0) {
					ixcfa = IncludeXMLConfigFileAction.INCLUDE;
					childrenElementsForbidden = true;
				} else if (isTagPrefixed(tag, REMOVE_TAG)) {
					tag = extractTag(tag, REMOVE_TAG);
					ixcfa = IncludeXMLConfigFileAction.REMOVE;
					childrenElementsForbidden = true;

					if (id == null || id.isEmpty()) {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "You need to specify the "+ ID_ATTRIBUTE +" of the element to remove. Or use \"" + WILDCARD_ID_ATTRIBUTE_VALUE + "\" to remove all elements in this context.");
						throw new SAXException("Malformed document");
					}
				} else if (isTagPrefixed(tag, ADD_TAG)) {
					tag = extractTag(tag, ADD_TAG);
					if (tag.isEmpty()) {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "The \"" + ADD_TAG + "\" element is a special tag which needs a suffix.");
						throw new SAXException("Malformed document");
					}
					ixcfa = IncludeXMLConfigFileAction.ADD;

					ctx = ctx + trimContext((String) tag);
					int i = 0;
					while (true) {
						String ctx0 = ctx + CONTEXT_ARRAY_SEPARATOR + String.valueOf(i);
						if (!Config.this.config.containsKey(loadContext + ctx0)) {
							ctx = ctx0 + CONTEXT_SEPARATOR;
							break;
						}
						i++;	
					}
					if (id == null)
						id = lastId = String.valueOf(i);
				} else if (isTagPrefixed(tag, EDIT_TAG)) {
					tag = extractTag(tag, EDIT_TAG);
					if (tag.isEmpty()) {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "The \"" + EDIT_TAG + "\" element is a special tag which needs a suffix.");
						throw new SAXException("Malformed document");
					}
					ixcfa = IncludeXMLConfigFileAction.EDIT;

					if (id == null || id.isEmpty()) {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "You need to specify the "+ ID_ATTRIBUTE +" when using the " + EDIT_TAG + " tag.");
						throw new SAXException("Malformed document");
					} else if (WILDCARD_ID_ATTRIBUTE_VALUE.equals(id)) {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "The wildcard value is not allowed for the "+ ID_ATTRIBUTE +" when using the " + EDIT_TAG + " tag.");
						throw new SAXException("Malformed document");
					}

					String editCtx = findEditContext(tag, id);
					if (editCtx == null) {
						Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Cannot find the specified context (<" + (tag.isEmpty() ? "?" :  tag) + " id=\"" + id + "\">).");
						throw new SAXException("Malformed document");
					}

					ctx = ctx + editCtx;
				} else {
					ixcfa = IncludeXMLConfigFileAction.DEFAULT;
					ctx = ctx + trimContext((String) tag) + CONTEXT_SEPARATOR;
				}

				if (ixcfa == IncludeXMLConfigFileAction.ADD || ixcfa == IncludeXMLConfigFileAction.DEFAULT) {
					if (id != null) {
						if (id.equals(WILDCARD_ID_ATTRIBUTE_VALUE)) {
							Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Wildcard id attribute not allowed for this element (\"" + tag + "\").");
							throw new SAXException("Malformed document");
						}

						Config.this.config.put(trimContext(loadContext + ctx + ID_ATTRIBUTE), id);
					}

					Config.this.config.put(trimContext(loadContext + ctx), "");
				}
			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				childrenElementsForbidden = false;

				if (!in_root_tag)
					return;

				String tag = localName;
				if (tag.equalsIgnoreCase(ROOT_TAG)) {
					in_root_tag = false;
					return;
				}

				String value = trimmer.matcher(cont).replaceAll("");
				cont = new StringBuilder();

				switch (ixcfa) {
				case INCLUDE:
					if (!Config.this.includeConfigFile(baseDir, value, includeTrace, loadContext + ctx)) {
						throw new SAXException("Included document not found");
					}
					break;
				case REMOVE:
					remove(extractTag(tag, REMOVE_TAG), lastId);

					break;
				case ADD:
				case EDIT:
				case DEFAULT:
					Config.this.config.put(trimContext(loadContext + ctx), value);
				case NONE:
					ctx = getParentContext(ctx);
					break;
				}
				ixcfa = IncludeXMLConfigFileAction.NONE;
			}

			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {
				cont.append(ch, start, length);
			}

			@Override
			public void ignorableWhitespace(char[] ch, int start, int length)
					throws SAXException {
				cont.append(ch, start, length);
			}
			
		});

		final SAXParseException[] pexc = new SAXParseException[] {null};

		rtr.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				//nothing
			}
			
			@Override
			public void error(SAXParseException exception) throws SAXException {
				//nothing
			}

			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				pexc[0] = exception;
			}
		});

		try {
			rtr.parse(new InputSource(br));
		} catch (SAXException e) {
			Config.this.getLogger().log(Level.WARNING, includeTrace.toString() + "Error when parsing XML file" +
					(pexc[0] == null ? "" : "(" + pexc[0].getMessage() + ")") + ".");
			return false;
		}

		return true;
	}
}
