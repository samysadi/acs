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

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.logging.Level;

import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.EntityImpl;
import com.samysadi.acs.core.event.DispensableEvent;
import com.samysadi.acs.core.event.Event;
import com.samysadi.acs.core.notifications.CoreNotificationCodes;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.probetypes.DataRateProbe;
import com.samysadi.acs.core.tracing.probetypes.DataSizeProbe;
import com.samysadi.acs.core.tracing.probetypes.EnergyProbe;
import com.samysadi.acs.core.tracing.probetypes.MiProbe;
import com.samysadi.acs.core.tracing.probetypes.MipsProbe;
import com.samysadi.acs.core.tracing.probetypes.PowerProbe;
import com.samysadi.acs.core.tracing.probetypes.PriceProbe;
import com.samysadi.acs.core.tracing.probetypes.TimeProbe;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.utility.collections.MultiListView;

/**
 * A simulator is an {@link Entity} that is the ancestor
 * of all other entities.
 *
 * <p>This class defines methods to schedule events and to start/stop the
 * processing of those events based on their scheduled process time, and the order
 * when they were added (if they were scheduled at the same time).
 *
 * <p>One simulator can be instantiated per execution thread.
 * If you instantiated a simulator on a thread then you want to to instantiate
 * a new simulator on the same thread, then you need to first discard the first simulator
 * using the {@link Simulator#free()} method.
 *
 * <p>Static methods are offered in order to format data for various simulation units.<br/>
 * Main simulation units are as follow:
 * <ul>
 * <li><strong>Time:</strong> {@link #MILLISECOND}s;
 * <li><strong>Data size:</strong> {@link #BYTE}s;
 * <li><strong>Data rate:</strong> {@link #BYTE}s per one second;
 * <li><strong>Computing task length:</strong> {@link #MI}s;
 * <li><strong>Computing speed:</strong> mips, {@link #MI}s per one second;
 * <li><strong>Power:</strong> {@link #WATT}s;
 * <li><strong>Energy:</strong> {@link #WATT} hours;
 * <li><strong>Currency code:</strong> {@link #CURRENCY_CODE};
 * <li><strong>Currency symbol:</strong> {@link #CURRENCY_SYMBOL};
 * <li><strong>Currency unit:</strong> {@link #CURRENCY_UNIT}.
 * </ul>
 * There is also other immutable values computed based on the previous values.
 *
 * <p>If you need different precision for previous values, use {@link SimulatorUnits} methods
 * <b>before</b> loading this class.
 *
 * @since 1.0
 */
public class Simulator extends EntityImpl {

	/**
	 * A simulation time value that is always in the past.
	 */
	public static final long PAST_TIME = -1l;

	/**
	 * A simulation time value that is equal to one millisecond.
	 */
	public static final long MILLISECOND		= SimulatorUnits.getMillisecond();

	/**
	 * A simulation time value that is equal to one second.
	 */
	public static final long SECOND		= 1000 * MILLISECOND;

	/**
	 * A simulation time value that is equal to one minute.
	 */
	public static final long MINUTE		= 60 * SECOND;

	/**
	 * A simulation time value that is equal to one hour.
	 */
	public static final long HOUR		= 60 * MINUTE;

	/**
	 * A simulation time value that is equal to one day.
	 */
	public static final long DAY		= 24 * HOUR;

	/**
	 * A simulation time value that is equal to one month.
	 */
	public static final long MONTH		= (long) (365.25f * DAY / 12);

	/**
	 * A memory value that is equal to one byte.
	 */
	public static final long BYTE				= SimulatorUnits.getByte();

	/**
	 * A memory value that is equal to one kibibyte or 2<sup>10</sup> bytes.
	 */
	public static final long KIBIBYTE	= 1024 * BYTE;

	/**
	 * A memory value that is equal to one mebibyte or 2<sup>20</sup> bytes.
	 */
	public static final long MEBIBYTE	= 1024 * KIBIBYTE;

	/**
	 * A memory value that is equal to one gibibyte or 2<sup>30</sup> bytes.
	 */
	public static final long GIBIBYTE	= 1024 * MEBIBYTE;

	/**
	 * A memory value that is equal to one tebibyte or 2<sup>40</sup> bytes.
	 */
	public static final long TEBIBYTE	= 1024 * GIBIBYTE;

	/**
	 * A computing task length value that is equal to one million of instructions.
	 */
	public static final long MI			= SimulatorUnits.getMi();

	/**
	 * Value which is equal to one currency unit.
	 *
	 * <p>This value allows a precision of 9 digits.
	 */
	public static final long CURRENCY_UNIT = SimulatorUnits.getCurrencyUnit();

	/**
	 * 3 chars (ISO 4217) code of the currency
	 */
	public static final String CURRENCY_CODE = SimulatorUnits.getCurrencyCode();

	/**
	 * The symbol that correspond to the currency code above
	 */
	public static final String CURRENCY_SYMBOL = SimulatorUnits.getCurrencySymbol();

	/**
	 * A power value that is equal to one watt
	 */
	public static final long WATT					= SimulatorUnits.getWatt();

	/**
	 * Empirical value equal to the latency of a network link of which length is one kilometer
	 */
	public static final long LATENCY_PER_KILOMETER = (long) ((10.0f/1000.0f) * Simulator.SECOND / 1000.0f);

	private volatile Thread executionThread = null;
	private volatile boolean isNotStopped = false;
	private long systemTime = 0l;
	private long time = 0l;
	private long scheduledStop = 0l;
	private final TreeMap<Long, LinkedList<Event>> nextEvents = new TreeMap<Long, LinkedList<Event>>();
	// We could get rid of this next field, but it would imply not directly polling the
	// entry from nextEvents during the simulation.
	// We want to avoid this behavior to improve performances.
	private Entry<Long, LinkedList<Event>> currentEntry = null;
	private int nonDispensableEventsCount = 0;

	private int lastMemoryCleanupCount = memoryCleanupCount;

	private Logger logger;

	private Random random;
	protected static int RANDOM_SEEDS_COUNT = 1000;
	private long[] randomSeeds;
	private int remainingRandomSeeds;
	private final WeakHashMap<Object, Random> randomsCache = new WeakHashMap<Object, Random>();
	private final LinkedList<Random> randoms = new LinkedList<Random>();

	static {
		initMemoryListener();
	}

	public Simulator(Config config) {
		super();
		super.setConfig(config);
		init();
	}

	private static final WeakHashMap<Thread, Simulator> simulators = new WeakHashMap<Thread, Simulator>();

	public static Simulator getSimulator() {
		return simulators.get(Thread.currentThread());
	}

	private void init() {
		synchronized(simulators) {
			if (getSimulator() != null)
				throw new IllegalArgumentException("Only one instance of Simulator can be created per Thread. You have to call free() on the current thread's instance before creating another.");
			this.executionThread = Thread.currentThread();
			simulators.put(this.executionThread, this);
		}

		{
			final Config logCfg = getConfig().addContext("Log");
			logger = new Logger(logCfg.getLevel("Level", Logger.DEFAULT_LEVEL));

			if (logCfg.getBoolean("DisableConsole", false))
				getLogger().disableConsole();

			String file = logCfg.getString("Output", null);
			if (file != null && !file.isEmpty())
				getLogger().enableOutputToFile(file);
		}

		{
			long seed;
			if ("auto".compareToIgnoreCase(getConfig().getString("seed", "")) == 0)
				seed = System.currentTimeMillis();
			else
				seed = getConfig().getLong("seed", 0l);
			this.random = new Random(seed);
			this.remainingRandomSeeds = RANDOM_SEEDS_COUNT;
			this.randomSeeds = new long[this.remainingRandomSeeds];
			for (int i = 0; i< remainingRandomSeeds; i++)
				this.randomSeeds[i] = this.random.nextLong();
		}

		//
	}

	public void free() {
		if (!this.isStopped())
			throw new IllegalStateException("Simulator must be stopped first");

		if (this.getLogger() != null)
			this.getLogger().close();

		synchronized(simulators) {
			if (this == getSimulator())
				simulators.remove(Thread.currentThread());
		}

		this.executionThread = null;
	}

	@Override
	public Simulator clone() {
		throw new UnsupportedOperationException("Whole simulator cloning was not implemented.");
	}

	private List<CloudProvider> cloudProviders;

	@Override
	protected void initializeEntity() {
		super.initializeEntity();

		this.cloudProviders = new ArrayList<CloudProvider>();
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof CloudProvider) {
			if (!this.cloudProviders.add((CloudProvider) entity))
				return;
		} else {
			super.addEntity(entity);
			return;
		}
		notify(CoreNotificationCodes.ENTITY_ADDED, entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof CloudProvider) {
			if (!this.cloudProviders.remove(entity))
				return;
		} else {
			super.removeEntity(entity);
			return;
		}
		notify(CoreNotificationCodes.ENTITY_REMOVED, entity);
	}

	@Override
	public List<Entity> getEntities() {
		List<Entity> s = super.getEntities();

		List<Entity> l = new ArrayList<Entity>(0);

		List<List<? extends Entity>> r = new ArrayList<List<? extends Entity>>();
		r.add(s);
		r.add(l);
		r.add(this.cloudProviders);
		return new MultiListView<Entity>(r);
	}

	public List<CloudProvider> getCloudProviders() {
		return Collections.unmodifiableList(this.cloudProviders);
	}

	@Override
	public void setParent(Entity entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasParentRec() {
		return true;
	}

	@Override
	public void setConfig(Config config) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the current simulation time.
	 *
	 * <p>Divide by {@link Simulator#SECOND} to convert to seconds.
	 *
	 * @return the current simulation time
	 */
	public long getTime() {
		return this.time;
	}

	/**
	 * Returns accumulated system time in milliseconds when this simulator was running.
	 *
	 * @return accumulated system time in milliseconds when this simulator was running
	 */
	public long getSystemTime() {
		return this.systemTime;
	}

	/**
	 * Cancels the given <tt>event</tt>, and removes it from this simulator's event queue.
	 *
	 * <p>After calling this method the event is not scheduled anymore, and can be rescheduled again at another simulation time safely.
	 *
	 * <p>Prefer to use {@link Event#cancel()} which is safe even if the given event is not scheduled.
	 *
	 * @param event
	 * @throws IllegalArgumentException if the given event is not scheduled
	 */
	public void cancel(Event event) {
		final Long t = event.getScheduledAt();
		if (t == null)
			throw new IllegalArgumentException("This event is not scheduled.");
		final boolean isNotCurrent = this.currentEntry == null || !this.currentEntry.getKey().equals(t);
		final LinkedList<Event> l;
		if (isNotCurrent)
			l = this.nextEvents.get(t);
		else
			l = this.currentEntry.getValue();
		if (l != null && l.remove(event)) {
			if (!(event instanceof DispensableEvent))
				this.nonDispensableEventsCount--;
			if (isNotCurrent && l.isEmpty())
				this.nextEvents.remove(t);
		}
		event.scheduledAt(null);
	}

	/**
	 * Schedules the given <tt>event</tt> to be processed at the current simulator time ({@link Simulator#getTime()}).
	 *
	 * <p>This method is the same as {@link Simulator#schedule(long, Event) schedule(0, Event)}.
	 *
	 * @param event the event to be scheduled
	 */
	public void schedule(Event event) {
		schedule(0l, event);
	}

	/**
	 * Schedules the given <tt>event</tt> to be processed after that the given <tt>delay</tt> has passed.<br/>
	 * In other words, schedules the given <tt>event</tt> to be processed at {@code Simulator.getTime() + delay}.
	 *
	 * @param delay
	 * @param event the event to be scheduled
	 * @throws IllegalArgumentException if the given <tt>event</tt> is already scheduled, or if the given time is in the past
	 */
	public void schedule(long delay, Event event) {
		if (event.isScheduled())
			throw new IllegalArgumentException("The given event is already scheduled");
		delay = this.time + delay;
		if (delay < this.time)
			throw new IllegalArgumentException("The given time is in the past");
		final Long t = Long.valueOf(delay);
		LinkedList<Event> l = null;
		if (this.currentEntry != null && this.currentEntry.getKey().equals(t)) {
			l = this.currentEntry.getValue();
		} else
			l = nextEvents.get(t);
		if (l == null) {
			l = new LinkedList<Event>();
			nextEvents.put(t, l);
		}
		l.add(event);
		event.scheduledAt(t);
		if (!(event instanceof DispensableEvent))
			this.nonDispensableEventsCount++;
	}

	protected void run() {
		synchronized(this) {
			if (this.isNotStopped)
				throw new IllegalStateException("Simulator already started");
			this.isNotStopped = true;
		}

		if (Thread.currentThread() != this.executionThread) {
			if (this.executionThread == null)
				throw new IllegalArgumentException("This Simulator cannot be run if it was freed");
			else
				throw new IllegalArgumentException("This Simulator cannot be run using another Thread than the Thread that was used for its instantiation");
		}

		getLogger().log(Level.FINE, "Simulation started.");

		final Level progress_level = getConfig().getLevel("Log.Progress.Level", Level.FINER);
		final boolean progress_level_loggable = getLogger().isLoggable(progress_level);
		final long progress_delay = Math.round(getConfig().getDouble("Log.Progress.Delay", 10d) *
				1000000000);
		final int progress_accuracy = getConfig().getInt("Log.Progress.Accuracy", 9999);

		final long tick = System.nanoTime();
		long tick2 = tick; //used for estimating simulator speed

		int next_report = 0;
		int added_report = 0;
		notifyNow(CoreNotificationCodes.SIMULATOR_STARTED, null);
		while (this.isNotStopped && !Thread.currentThread().isInterrupted()
				&& this.time < this.scheduledStop) {

			if (this.nonDispensableEventsCount == 0)
				break; //nothing to process

			// let's find the next valid (not discarded) event
			this.currentEntry = this.nextEvents.pollFirstEntry();

			// note: this.currentEntry should never be null
			// because nonDispensableEventsCount >, 0 that means that this.nextEvents is not empty

			if (this.currentEntry.getValue().isEmpty())
				continue;

			//update time
			this.time = this.currentEntry.getKey();
			// notify that the simulation time progressed
			notifyNow(CoreNotificationCodes.SIMULATOR_TICK, null);

			// let's process next event
			Event next = null;
			while (null != (next = this.currentEntry.getValue().pollFirst())) {
				if (progress_level_loggable && (next_report-- == 0)) {
					next_report = progress_accuracy;
					long tt = System.nanoTime();
					if (tt - tick2 > progress_delay) {
						long h = added_report == 0 ? 0 : Math.round((1000000000d / (tt - tick2)) * (added_report+1));
						tick2 = tt;
						Logger.getGlobal().log(Level.FINER, "Simulation progress: remains " + this.nonDispensableEventsCount + " events and " + this.nextEvents.size() + " ticks (" + h + "e/s)");
						Logger.getGlobal().log(Level.FINER, "Memory used: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + "MiB");
						added_report = 0;
					}
					added_report += next_report;
				}

				next.process();
				notifyNow(CoreNotificationCodes.SIMULATOR_EVENT_PROCESSED, next);
				next.scheduledAt(null); //next is not scheduled anymore

				if (!(next instanceof DispensableEvent))
					this.nonDispensableEventsCount--;

				if (this.nonDispensableEventsCount == 0)
					break; //don't put inside last condition block, this field may have been modified (ie:decremented) through next.process() if Event.cancel() is called

				if (lastMemoryCleanupCount != memoryCleanupCount) {
					getLogger().log(Level.WARNING, "Memory is low, performing cleanup...");
					memoryCleanupCount = lastMemoryCleanupCount;
					performMemoryCleanup(this);
					System.gc(); //gc must be called, so that cleaned memory is collected. Otherwise, performMemoryCleanup may not be called again
				}
			}

			// notify that all events in current simulation time are processed
			notifyNow(CoreNotificationCodes.SIMULATOR_TICK_PASSED, null);
			if (this.nonDispensableEventsCount != 0 && !this.currentEntry.getValue().isEmpty())
				throw new IllegalStateException("Scheduling events at current time is not allowed under listeners of the " + CoreNotificationCodes.notificationCodeToString(CoreNotificationCodes.SIMULATOR_TICK_PASSED) + " notification code.");
		}
		this.currentEntry = null;
		notifyNow(CoreNotificationCodes.SIMULATOR_STOPPED, null);

		this.systemTime += (System.nanoTime() - tick) / 1000000;

		getLogger().log(Level.FINE, "Simulation stopped. Total execution time: " + Simulator.formatTime(getSystemTime() * Simulator.MILLISECOND) + ".");

		if (!this.hasMoreEvents())
			getLogger().log(Level.FINE, "SIMULATION ENDED.");

		this.isNotStopped = false;
	}

	public boolean hasMoreEvents() {
		return (this.nonDispensableEventsCount != 0);
	}

	public boolean isStopped() {
		return !this.isNotStopped;
	}

	public final void start() {
		this.start(0l);
	}

	public void start(long maxRunTime) {
		if (maxRunTime <= 0)
			this.scheduledStop = Long.MAX_VALUE;
		else
			this.scheduledStop = this.time + maxRunTime;
		this.run();
	}

	public void stop() {
		if (!this.isStopped() && Thread.currentThread() != this.executionThread) {
			//wait for the job to return
			this.executionThread.interrupt();
			try {
				this.executionThread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		this.isNotStopped = false;
	}

	public static long MEMORY_CLEANUP_THRESHOLD = 50 * (1 << 20); //50 mebibytes

	private static int memoryCleanupCount = 0;
	private static void initMemoryListener() {
		if (MEMORY_CLEANUP_THRESHOLD <= 0)
			return;

		final java.lang.management.MemoryPoolMXBean pool;
		{
			java.lang.management.MemoryPoolMXBean thePool = null;
			for (java.lang.management.MemoryPoolMXBean p : java.lang.management.ManagementFactory.getMemoryPoolMXBeans()) {
				if (p.getType() == java.lang.management.MemoryType.HEAP && p.isUsageThresholdSupported()) {
					thePool = p;
					break;
				}
			}

			if (thePool == null)
				return;

			pool = thePool;
		}

		{
			long maxMemory = pool.getUsage().getMax();
			long warningThreshold = maxMemory - MEMORY_CLEANUP_THRESHOLD;
			if (warningThreshold <= 0)
				return;
			pool.setUsageThreshold(warningThreshold);
		}

		java.lang.management.MemoryMXBean mbean = java.lang.management.ManagementFactory.getMemoryMXBean();
		javax.management.NotificationEmitter emitter = (javax.management.NotificationEmitter) mbean;
		emitter.addNotificationListener(new javax.management.NotificationListener() {
			@Override
			public void handleNotification(javax.management.Notification n, Object hb) {
				if (n.getType().equals(
						java.lang.management.MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
//					long maxMemory = pool.getUsage().getMax();
//					long usedMemory = pool.getUsage().getUsed();
					Simulator.memoryCleanupCount++;
				}
			}
		}, null, null);
	}

	private static void performMemoryCleanup(Entity entity) {
		entity.cleanupListeners();
		for (Probe<?> p: entity.getProbes())
			p.cleanupListeners();
		for (Entity e: entity.getEntities())
			performMemoryCleanup(e);
	}

	/**
	 * Returns currently set random generator of the simulator.
	 *
	 * @return currently set random generator of the simulator
	 * @see Simulator#setRandomGenerator(Random)
	 * @see Simulator#setRandomGenerator(Object)
	 */
	public Random getRandomGenerator() {
		if (this.randoms.isEmpty())
			return this.random;
		return this.randoms.getLast();
	}

	/**
	 * Returns a random generator that matches the given key.
	 *
	 * <p>This method first tries to find an existing instance matching the given <tt>key</tt>.
	 * If none is found one instantiated and returned and further calls to this function with the
	 * same key will return the same instance.
	 *
	 * @param key
	 * @return a random generator that matches the given key
	 */
	public Random getRandomGenerator(Object key) {
		Random random = this.randomsCache.get(key);
		if (random == null) {
			long seed;
			if (this.remainingRandomSeeds == 0) {
				getLogger().log(Level.WARNING, "There is no remaining pre-generated random seeds.");
				seed = getRandomGenerator().nextLong();
			} else {
				this.remainingRandomSeeds--;
				seed = this.randomSeeds[this.remainingRandomSeeds];
				if (this.remainingRandomSeeds == 0)
					this.randomSeeds = null;
			}
			random = new Random(seed);
			this.randomsCache.put(key, random);
		}
		return random;
	}

	/**
	 * Updates the random generator of the simulator to the given value.
	 *
	 * <p>If given random is <tt>null</tt> then default simulator's random generator is set.
	 *
	 * <p>You can restore previous context using {@link Simulator#restoreRandomGenerator()}.
	 *
	 * @param random
	 */
	public void setRandomGenerator(Random random) {
		if (random == null)
			random = this.random;
		this.randoms.push(random);
	}

	/**
	 * Updates the random generator of the simulator using the given key.
	 *
	 * <p>If given key is <tt>null</tt> then default simulator's random generator is set.
	 *
	 * <p>You can restore previous context using {@link Simulator#restoreRandomGenerator()}.
	 *
	 * @param key
	 */
	public void setRandomGenerator(Object key) {
		if (key == null)
			setRandomGenerator(null);
		else
			setRandomGenerator(getRandomGenerator(key));
	}

	/**
	 * Sets the simulator's random generator to the previous one and
	 * returns the current random generator (as it was before changing it).
	 *
	 * @return old random generator
	 */
	public Random restoreRandomGenerator() {
		return this.randoms.removeLast();
	}

	/**
	 * See {@link Simulator#restoreRandomGenerator(int)} for more information about this method.
	 */
	public int getRandomGeneratorId() {
		return this.randoms.size();
	}

	/**
	 * Restores the random context as defined by the given id.
	 *
	 * <p>Random generators are usually set at the beginning of a given code block,
	 * and when the block ends, old random generator is restored.<br/>
	 * Though, if an exception happens before the block ends, the generator is never restored.
	 * This is not a problem if exceptions are intended to end the simulation, which
	 * is the most common expected behavior. Thus, we can avoid to explicitly put the code
	 * between a try .. finally block which will unnecessarily impact simulation performances.<br/>
	 * However, if you plan to catch the exception, you may need to restore the random generator
	 * before continuing. To do so, call the {@link Simulator#getRandomGeneratorId()} method before
	 * the risky code and call this method when catching an exception.
	 */
	public void restoreRandomGenerator(int id) {
		id = this.randoms.size() - id;
		if (id < 0)
			throw new IllegalArgumentException("Given generator id cannot be restored.");

		while (id-- > 0)
			this.randoms.removeLast();
	}

	@Override
	public Logger getLogger() {
		return this.logger;
	}

	private static NumberFormat default_formatter = getDefaultFormatter();

	private static NumberFormat getDefaultFormatter() {
		if (default_formatter == null) {
			default_formatter = NumberFormat.getNumberInstance();
			default_formatter.setGroupingUsed(false);
			default_formatter.setMaximumFractionDigits(1);
			default_formatter.setMinimumFractionDigits(1);
		}
		return (NumberFormat) default_formatter.clone();
	}

	/**
	 * Converts the given time to a human readable string format and returns it.
	 *
	 * @param time the time to convert
	 * @param useAlternativeUnits if <tt>false</tt> then the time is formatted using seconds only, and will
	 * consider alternative time units (minutes, hours, days)
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string. If <tt>useAlternativeUnits</tt> is <tt>true</tt>
	 * then this parameter is ignored and units are always appended.
	 * @return formatted time
	 */
	public static String formatTime(long time, boolean useAlternativeUnits, boolean appendUnits) {
		final StringBuilder sb = new StringBuilder();
		final DecimalFormat f = new DecimalFormat();
		f.setMaximumFractionDigits(3);
		f.setGroupingUsed(false);

		if (useAlternativeUnits) {
			final long days = time / DAY; time-= days * DAY;
			final long hours = time / HOUR; time-= hours * HOUR;
			final long minutes = time / MINUTE; time-= minutes * MINUTE;

			if (days!=0)
				sb.append(days).append("d:").append(hours).append("h:").append(minutes).append("m:");
			else if (hours!=0)
				sb.append(hours).append("h:").append(minutes).append("m:");
			else if (minutes!=0)
				sb.append(minutes).append("m:");
		}

		sb.append(f.format((double) time / SECOND));
		if (appendUnits || useAlternativeUnits)
			sb.append("s");
		return sb.toString();
	}

	/**
	 * See {@link Simulator#formatTime(long, boolean, boolean) formatTime(time, true, true)}.
	 */
	public static String formatTime(long time) {
		return formatTime(time, true, true);
	}

	/**
	 * Converts the given size to a human readable string format and returns it.
	 *
	 * @param size the size to convert
	 * @param useAlternativeUnits if <tt>false</tt> then the size is formatted using bytes only, and will
	 * not consider alternative units (kilo/kibi, Mega/Mebi, Giga/Gibi etc...)
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string. If <tt>useAlternativeUnits</tt> is <tt>true</tt>
	 * then this parameter is ignored and units are always appended.
	 * @param si if <tt>true</tt> then use International System 1000-based units (ie: kilo, Mega, Giga etc...).<br/>
	 * If <tt>false</tt> then use usual 1024-based units (ie: kibi, Mebi, Gibi etc...).
	 * This parameter is only relevant if the <tt>useAlternativeUnits</tt> parameter is <tt>true</tt>.
	 * @return formatted size
	 */
	public static String formatSize(long size, boolean useAlternativeUnits, boolean appendUnits, boolean si) {
		size = size / BYTE;
		final long unit = si ? 1000 : 1024;
		if (!useAlternativeUnits || size < unit) {
			if (appendUnits)
				return size + " B";
			else
				return size + "";
		}
		int exp = (int) (Math.log(size) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (si ? "" : "i") + "B";
		return getDefaultFormatter().format(size / Math.pow(unit, exp)) + " " + pre;
	}

	/**
	 * See {@link Simulator#formatSize(long, boolean, boolean, boolean) formatSize(size, useAlternativeUnits, appendUnits, false)}.
	 */
	public static String formatSize(long size, boolean useAlternativeUnits, boolean appendUnits) {
		return formatSize(size, useAlternativeUnits, appendUnits, false);
	}

	/**
	 * See {@link Simulator#formatSize(long, boolean, boolean) formatSize(size, true, true)}.
	 */
	public static String formatSize(long size) {
		return formatSize(size, true, true);
	}

	/**
	 * See {@link Simulator#formatSize(long, boolean, boolean, boolean)}.
	 */
	public static String formatDataRate(long sizePerSec, boolean useAlternativeUnits, boolean appendUnits, boolean si) {
		StringBuilder sb = new StringBuilder(formatSize(sizePerSec, useAlternativeUnits, appendUnits, si));
		if (appendUnits || useAlternativeUnits)
			sb.append("/s");
		return sb.toString();
	}

	/**
	 * See {@link Simulator#formatDataRate(long, boolean, boolean, boolean) formatDataRate(sizePerSec, useAlternativeUnits, appendUnits, false)}.
	 */
	public static String formatDataRate(long sizePerSec, boolean useAlternativeUnits, boolean appendUnits) {
		return formatDataRate(sizePerSec, useAlternativeUnits, appendUnits, false);
	}

	/**
	 * See {@link Simulator#formatDataRate(long, boolean, boolean) formatDataRate(sizePerSec, true, true)}.
	 */
	public static String formatDataRate(long sizePerSec) {
		return formatDataRate(sizePerSec, true, true);
	}

	/**
	 * Converts the given price to human readable string format and returns it.
	 *
	 * @param price the price to convert
	 * @param precision how many decimal digits will be displayed
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string
	 * @return formatted price
	 */
	public static String formatPrice(long price, int precision, boolean appendUnits) {
		final NumberFormat f = (NumberFormat) getDefaultFormatter().clone();
		f.setMaximumFractionDigits(precision);
		f.setMinimumFractionDigits(precision);
		f.setRoundingMode(RoundingMode.HALF_UP);

		StringBuilder sb = new StringBuilder(f.format((double) price / CURRENCY_UNIT));
		if (appendUnits)
			sb.append(' ').append(CURRENCY_CODE);
		return sb.toString();
	}

	/**
	 * See {@link Simulator#formatPrice(long, int, boolean) formatPrice(price, 2, appendUnits)}.
	 */
	public static String formatPrice(long price, boolean appendUnits) {
		return formatPrice(price, 2, appendUnits);
	}

	/**
	 * See {@link Simulator#formatPrice(long, boolean) formatPrice(price, true)}.
	 */
	public static String formatPrice(long price) {
		return formatPrice(price, true);
	}

	/**
	 * Converts the given mi (millions of instructions) to human readable string format and returns it.
	 *
	 * @param mi the mi to convert
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string
	 * @return formatted mi
	 */
	public static String formatMi(long mi, boolean appendUnits) {
		StringBuilder sb = new StringBuilder(String.valueOf(Math.round((double) mi / MI)));
		if (appendUnits)
			sb.append(" mi");
		return sb.toString();
	}

	/**
	 * See {@link Simulator#formatMi(long, boolean) formatMi(mi, true)}.
	 */
	public static String formatMi(long mi) {
		return formatMi(mi, true);
	}

	/**
	 * Converts the given mips (millions of instructions per second) to human readable string format and returns it.
	 *
	 * @param mips the mips to convert
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string
	 * @return formatted mips
	 */
	public static String formatMips(long mips, boolean appendUnits) {
		StringBuilder sb = new StringBuilder(String.valueOf(formatMi(mips, appendUnits)));
		if (appendUnits)
			sb.append("ps");
		return sb.toString();
	}

	/**
	 * See {@link Simulator#formatMips(long, boolean) formatMips(mips, true)}.
	 */
	public static String formatMips(long mips) {
		return formatMips(mips, true);
	}

	/**
	 * Converts the given power to human readable string format and returns it.
	 *
	 * @param power the power to convert
	 * @param useAlternativeUnits if <tt>false</tt> then the power is formatted using watts only, and will
	 * not consider alternative units (kilo, Mega, Giga etc...)
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string. If <tt>useAlternativeUnits</tt> is <tt>true</tt>
	 * then this parameter is ignored and units are always appended
	 * @return formatted power
	 */
	public static String formatPower(long power, boolean useAlternativeUnits, boolean appendUnits) {
		final String CODE = "W";
		final long unit = 1000;
		double p = (double) power / WATT;

		NumberFormat format = getDefaultFormatter();

		StringBuilder r = new StringBuilder();
		if (useAlternativeUnits && p >= unit) {
			int exp = (int) (Math.log(p) / Math.log(unit));
			r.append(format.format(p / Math.pow(unit, exp)));
			r.append(' ');
			r.append("kMGTPE".charAt(exp - 1));
		} else {
			if (p < 1d)
				format.setMaximumFractionDigits(3);
			r.append(format.format(p));
			if (useAlternativeUnits || appendUnits)
				r.append(' ');
		}
		if (useAlternativeUnits || appendUnits)
			r.append(CODE);

		return r.toString();
	}

	/**
	 * See {@link Simulator#formatPower(long, boolean, boolean) formatPower(power, true, true)}.
	 */
	public static String formatPower(long power) {
		return formatPower(power, true, true);
	}

	/**
	 * Converts the given power to human readable string format and returns it.
	 *
	 * @param energy the energy to convert
	 * @param useAlternativeUnits if <tt>false</tt> then the energy is formatted using watt-hour unit only, and will
	 * not consider alternative units (kilo, Mega, Giga etc...)
	 * @param appendUnits if <tt>false</tt> then units are not appended to the returned string. If <tt>useAlternativeUnits</tt> is <tt>true</tt>.
	 * then this parameter is ignored and units are always appended
	 * @return formatted power
	 */
	public static String formatEnergy(long energy, boolean useAlternativeUnits, boolean appendUnits) {
		final String CODE = "Wh";
		double e = (double) energy / WATT;
		final long unit = 1000;

		NumberFormat format = getDefaultFormatter();

		StringBuilder r = new StringBuilder();
		if (useAlternativeUnits && e >= unit) {
			int exp = (int) (Math.log(e) / Math.log(unit));
			r.append(format.format(e / Math.pow(unit, exp)));
			r.append(' ');
			r.append("kMGTPE".charAt(exp - 1));
		} else {
			if (e < 1d)
				format.setMaximumFractionDigits(3);
			r.append(format.format(e));
			if (useAlternativeUnits || appendUnits)
				r.append(' ');
		}
		if (useAlternativeUnits || appendUnits)
			r.append(CODE);

		return r.toString();

	}

	/**
	 * See {@link Simulator#formatEnergy(long, boolean, boolean) formatEnergy(energy, true, true)}.
	 */
	public static String formatEnergy(long energy) {
		return formatEnergy(energy, true, true);
	}

	public static String probeValueToString(Probe<?> p, Object value, boolean useAlternativeUnits, boolean appendUnits) {
		if (value == null)
			return "null";
		if (p instanceof DataRateProbe)
			return Simulator.formatDataRate((Long)value, useAlternativeUnits, appendUnits);
		else if (p instanceof DataSizeProbe)
			return Simulator.formatSize((Long) value, useAlternativeUnits, appendUnits);
		else if (p instanceof EnergyProbe)
			return Simulator.formatEnergy((Long) value, useAlternativeUnits, appendUnits);
		else if (p instanceof MiProbe)
			return Simulator.formatMi((Long) value, appendUnits);
		else if (p instanceof MipsProbe)
			return Simulator.formatMips((Long) value, appendUnits);
		else if (p instanceof PowerProbe)
			return Simulator.formatPower((Long) value, useAlternativeUnits, appendUnits);
		else if (p instanceof PriceProbe)
			return Simulator.formatPrice((Long) value, appendUnits);
		else if (p instanceof TimeProbe)
			return Simulator.formatTime((Long) value, useAlternativeUnits, appendUnits);
		else
			return value.toString();
	}

	public static String probeValueUnit(Probe<?> p) {
		String s = probeValueToString(p, 0l, false, true);
		int i = 0;
		while (i < (s.length() - 1) && s.charAt(i) != ' ')
			i++;
		return s.substring(i+1);
	}
}
