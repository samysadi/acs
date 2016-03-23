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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Logger;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.entity.PoweredEntity.PowerState;
import com.samysadi.acs.core.notifications.NotificationListener;
import com.samysadi.acs.core.notifications.Notifier;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.Trace;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.pu.operation.provisioner.ComputingProvisioner;
import com.samysadi.acs.hardware.ram.Ram;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.CheckpointingHandler;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.user.ThinClientVirtualMachine;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.utility.factory.generation.mode.FrequencyGenerationMode;
import com.samysadi.acs.utility.random.Exponential;
import com.samysadi.acs.utility.random.NumberGenerator;
import com.samysadi.acs.utility.random.Uniform;
import com.samysadi.acs.utility.workload.Workload;
import com.samysadi.acs.utility.workload.task.Task;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;

/**
 * A class containing static utility methods.<br/>
 * Most of these methods behavior depends on a given (correctly contexted) configuration.
 *
 * @since 1.0
 */
public final class FactoryUtils {

	private FactoryUtils() {
		//nothing
	}

	/**
	 * Generates and returns a Double based on given arguments.
	 *
	 * <p>This method will check these next configurations sequentially in order to
	 * compute the returned double (examples given assuming {@code configName == "Example"}):
	 * <ul>
	 * 		<li><b>Example</b>: if set then its value is directly returned;
	 * 		<li><b>ExampleMin</b>, <b>ExampleMax</b>: if set then the returned value is
	 * 			computed following a uniform distribution (using the given inclusive min and max);
	 * 		<li><b>ExampleMean</b>: if set then the returned value is
	 * 			computed following a exponential distribution (using the given mean);
	 * 		<li><b>Example_Class</b>: if set and the class implements {@link NumberGenerator} then
	 * 			it is instantiated and used to generate the returned value;
	 * 		<li>If none of the previous conditions has returned, then defaultValue is returned.
	 * </ul>
	 *
	 * @param configName
	 * @param config the configuration object, may be <tt>null</tt>
	 * @param defaultValue
	 * @return generated value
	 */
	public static Double generateDouble(String configName, Config config, Double defaultValue) {
		if (config == null)
			return defaultValue;

		{
			Double v = config.getDouble(configName, null);
			if (v != null)
				return v;
		}

		{
			Double min = config.getDouble(configName + "Min", null);
			if (min != null) {
				Double max = config.getDouble(configName + "Max", null);
				if (max != null)
					return (new Uniform(min, max)).nextDouble();
			}
		}

		{
			Double mean = config.getDouble(configName + "Mean", null);
			if (mean != null)
				return (new Exponential(mean)).nextDouble();
		}

		{
			Class<?> clazz = config.getClassFromConfig(configName + "_Class", null, false);
			if (clazz != null) {
				try {
					return ((NumberGenerator) clazz.newInstance()).nextDouble();
				} catch (Exception e) {
					Logger.getGlobal().logInstantiationException(clazz, e);
				}
			}
		}

		return defaultValue;
	}

	/**
	 * Generates and returns a Long based on given arguments.
	 *
	 * <p>This method will check these next configurations sequentially in order to
	 * compute the returned long (examples given assuming {@code configName == "Example"}):
	 * <ul>
	 * 		<li><b>Example</b>: if set then its value is directly returned;
	 * 		<li><b>ExampleMin</b>, <b>ExampleMax</b>: if set then the returned value is
	 * 			computed following a uniform distribution (using the given inclusive min and max);
	 * 		<li><b>ExampleMean</b>: if set then the returned value is
	 * 			computed following a exponential distribution (using the given mean);
	 * 		<li><b>Example_Class</b>: if set and the class implements {@link NumberGenerator} then
	 * 			it is instantiated and used to generate the returned value;
	 * 		<li>If none of the previous conditions has returned, then defaultValue is returned.
	 * </ul>
	 *
	 * @param configName
	 * @param config the configuration object, may be <tt>null</tt>
	 * @param defaultValue
	 * @return generated value
	 */
	public static Long generateLong(String configName, Config config, Long defaultValue) {
		if (config == null)
			return defaultValue;

		{
			Long v = config.getLong(configName, null);
			if (v != null)
				return v;
		}

		{
			Long min = config.getLong(configName + "Min", null);
			if (min != null) {
				Long max = config.getLong(configName + "Max", null);
				if (max != null)
					return (new Uniform(min, max)).nextLong();
			}
		}

		{
			Long mean = config.getLong(configName + "Mean", null);
			if (mean != null)
				return (new Exponential(mean)).nextLong();
		}

		{
			Class<?> clazz = config.getClassFromConfig(configName + "_Class", null, false);
			if (clazz != null) {
				try {
					return ((NumberGenerator) clazz.newInstance()).nextLong();
				} catch (Exception e) {
					Logger.getGlobal().logInstantiationException(clazz, e);
				}
			}
		}

		return defaultValue;
	}

	/**
	 * Generates and returns a Integer based on given arguments.
	 *
	 * <p>This method will check these next configurations sequentially in order to
	 * compute the returned integer (examples given assuming {@code configName == "Example"}):
	 * <ul>
	 * 		<li><b>Example</b>: if set then its value is directly returned;
	 * 		<li><b>ExampleMin</b>, <b>ExampleMax</b>: if set then the returned value is
	 * 			computed following a uniform distribution (using the given inclusive min and max);
	 * 		<li><b>ExampleMean</b>: if set then the returned value is
	 * 			computed following a exponential distribution (using the given mean);
	 * 		<li><b>Example_Class</b>: if set and the class implements {@link NumberGenerator} then
	 * 			it is instantiated and used to generate the returned value;
	 * 		<li>If none of the previous conditions has returned, then defaultValue is returned.
	 * </ul>
	 *
	 * @param configName
	 * @param config the configuration object, may be <tt>null</tt>
	 * @param defaultValue
	 * @return generated value
	 */
	public static Integer generateInt(String configName, Config config, Integer defaultValue) {
		if (config == null)
			return defaultValue;

		{
			Integer v = config.getInt(configName, null);
			if (v != null)
				return v;
		}

		{
			Integer min = config.getInt(configName + "Min", null);
			if (min != null) {
				Integer max = config.getInt(configName + "Max", null);
				if (max != null)
					return (new Uniform(min, max)).nextInt();
			}
		}

		{
			Integer mean = config.getInt(configName + "Mean", null);
			if (mean != null)
				return (new Exponential(mean)).nextInt();
		}

		{
			Class<?> clazz = config.getClassFromConfig(configName + "_Class", null, false);
			if (clazz != null) {
				try {
					return ((NumberGenerator) clazz.newInstance()).nextInt();
				} catch (Exception e) {
					Logger.getGlobal().logInstantiationException(clazz, e);
				}
			}
		}

		return defaultValue;
	}

	/**
	 * This method is used in factories when generating objects to define how
	 * many objects will be generated.<br/>
	 * For more details, see {@link FactoryUtils#generateInt(String, Config, Integer)}.
	 */
	public static Integer generateCount(Config config, Integer defaultCount) {
		return generateInt("Count", config, defaultCount);
	}

	private static final DecimalFormat percentFormat = new DecimalFormat("0.00");
	public static void logAdvancement(String s, int total, double percent) {
		if (!Logger.getGlobal().isLoggable(Level.FINER))
			return;
		Logger.getGlobal().log(Level.FINER, "Total Number of " + s + " Generated: " + total + " ( " + percentFormat.format(percent) + "% )");
		Logger.getGlobal().log(Level.FINER, "Memory used: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20) + "MiB");
	}

	/*
	 * Other entity generation methods
	 * ************************************************************************
	 */

	public static Simulator generateSimulator() {
		return generateSimulator(Config.DEFAULT_CONFIG_FILENAME);
	}

	public static Simulator generateSimulator(String configFilename) {
		Config config = new Config(configFilename);
		return Factory.getFactory(config).newSimulatorFactory(null, config).generate();
	}

	public static Simulator generateSimulator(Config config) {
		return Factory.getFactory(config).newSimulatorFactory(null, config).generate();
	}

	public static final String CloudProvider_CONTEXT		= "CloudProvider";

	public static CloudProvider generateCloudProvider(Config config) {
		return Factory.getFactory(config).newCloudProviderFactory(null, config)
				.generate();
	}

	public static final String Failures_CONTEXT				= "Failures";

	public static void generateFailures(Config config) {
		Factory.getFactory(config).newFailureFactory(null, config)
				.generate();
	}

	public static final String NetworkInterface_CONTEXT		= "NetworkInterface";
	public static final String NetworkLink_CONTEXT			= "NetworkLink";


	/* ********************************************************************* */

	/**
	 * This calls {@link FactoryUtils#linkDevices(Config, NetworkDevice, NetworkDevice, long, long, long, double)}
	 * such that <b>linkUpBw</b> and <b>linkDownBw</b> are gotten from the device0 configuration contexted with
	 * {@link FactoryUtils#NetworkInterface_CONTEXT} (default is {@code 100MiB/s}).
	 */
	public static NetworkInterface[] linkDevices(
			Config config,
			NetworkDevice device0, NetworkDevice device1,
			long linkLatency, double linkLossRate) {
		Config c = device0.getConfig();
		long linkUpBw;
		long linkDownBw;
		if (c == null) {
			linkUpBw = 100l;
			linkDownBw = 100l;
		} else {
			c = c.addContext(NetworkInterface_CONTEXT);
			linkUpBw = c.getLong("MaxUploadBw", 100l) * Simulator.MEBIBYTE;
			linkDownBw = c.getLong("MaxDownloadBw", 100l) * Simulator.MEBIBYTE;
		}

		return linkDevices(config,
				device0, device1,
				linkUpBw, linkDownBw,
				linkLatency, linkLossRate
			);
	}

	/**
	 * This method calls {@link Factory#linkDevices(NetworkDevice, NetworkDevice, Class, Class, Class, Class, long, long, long, double)}
	 * with the following parameters:<ul>
	 * <li>networkInterface0Class: is gotten from device0 configuration contexted with {@link FactoryUtils#NetworkInterface_CONTEXT};
	 * <li>networkInterface1Class: is gotten from device1 configuration contexted with {@link FactoryUtils#NetworkInterface_CONTEXT};
	 * <li>networkLinkClass: is gotten from given configuration contexted with {@link FactoryUtils#NetworkLink_CONTEXT};
	 * <li>networkProvisionerClass: is gotten from given configuration contexted with {@link FactoryUtils#NetworkLink_CONTEXT}.
	 * </ul>
	 * Additionally returned network interfaces' configuration are set.
	 */
	public static NetworkInterface[] linkDevices(
			Config config,
			NetworkDevice device0, NetworkDevice device1,
			long linkUpBw, long linkDownBw,
			long linkLatency, double linkLossRate) {
		Config lConfig = config.addContext(NetworkLink_CONTEXT);
		Config c0 = device0.getConfigRec().addContext(NetworkInterface_CONTEXT);
		Config c1 = device1.getConfigRec().addContext(NetworkInterface_CONTEXT);
		final NetworkInterface[] ni = Factory.getFactory(config).linkDevices(
				device0, device1,
				Factory.getFactory(c0).getNetworkInterfaceClass(),
				Factory.getFactory(c1).getNetworkInterfaceClass(),
				Factory.getFactory(lConfig).getNetworkLinkClass(),
				Factory.getFactory(lConfig).getNetworkProvisionerClass(),
				linkUpBw, linkDownBw,
				linkLatency, linkLossRate
			);

		ni[0].setConfig(c0);
		ni[1].setConfig(c1);

		return ni;
	}

	/**
	 * This calls {@link FactoryUtils#connectToInternet(Config, NetworkDevice, long, long, long, double)}
	 * such that <b>linkUpBw</b> and <b>linkDownBw</b> are directly gotten from the given <tt>config</tt>
	 * or from the networkDevice contexted configuration (contexted using {@link FactoryUtils#NetworkInterface_CONTEXT})
	 * (default is {@code 100MiB/s}).
	 */
	public static void connectToInternet(
			Config config,
			NetworkDevice networkDevice,
			long linkLatency, double linkLossRate) {
		long linkUpBw = config.getLong("MaxUploadBw", 100l);
		long linkDownBw = config.getLong("MaxDownloadBw", 100l);

		//allow config value to be in the networkDevice's config
		Config c = networkDevice.getConfig();
		if (c != null) {
			c = c.addContext(NetworkInterface_CONTEXT);
			linkUpBw = c.getLong("MaxUploadBw", linkUpBw);
			linkDownBw = c.getLong("MaxDownloadBw", linkDownBw);
		}

		linkUpBw *= Simulator.MEBIBYTE;
		linkDownBw *= Simulator.MEBIBYTE;

		connectToInternet(config,
				networkDevice,
				linkUpBw, linkDownBw,
				linkLatency, linkLossRate
			);
	}

	/**
	 * This methods instantiates a factory using the given <tt>config</tt> and then
	 * calls {@link Factory#connectToInternet(NetworkDevice, Class, Class, Class, long, long, long, double)}
	 * <ul>
	 * <li>networkInterfaceClass: is gotten from networkDevice configuration contexted with {@link FactoryUtils#NetworkInterface_CONTEXT};
	 * <li>networkLinkClass: is gotten from given configuration contexted with {@link FactoryUtils#NetworkLink_CONTEXT};
	 * <li>networkProvisionerClass: is gotten from given configuration contexted with {@link FactoryUtils#NetworkLink_CONTEXT}.
	 * </ul>
	 */
	public static void connectToInternet(
			Config config,
			NetworkDevice networkDevice,
			long linkUpBw, long linkDownBw,
			long linkLatency, double linkLossRate) {
		Config lConfig = config.addContext(NetworkLink_CONTEXT);
		Config c = networkDevice.getConfigRec().addContext(NetworkInterface_CONTEXT);
		Factory.getFactory(c).connectToInternet(
				networkDevice,
				Factory.getFactory(c).getNetworkInterfaceClass(),
				Factory.getFactory(lConfig).getNetworkLinkClass(),
				Factory.getFactory(lConfig).getNetworkProvisionerClass(),
				linkUpBw, linkDownBw,
				linkLatency, linkLossRate
			);
	}

	public static final String Host_CONTEXT					= "Host";

	/**
	 * Generates and returns a new Host based on the given configuration.
	 *
	 * <p>Default state of the host is {@link PowerState#OFF} unless otherwise specified.
	 *
	 * @param config contexted configuration
	 * @param parent
	 * @return the generated host
	 */
	@SuppressWarnings("unused")
	public static Host generateHost(Config config, CloudProvider parent) {
		Host host = Factory.getFactory(config).newHost(null, parent);
		host.setConfig(config);
		host.setName(config.getString("Name", null));

		host.setPowerState(config.getBoolean("PowerState", false) ? PowerState.ON : PowerState.OFF);

		Config cfg;

		cfg = config.addContext("Ram");
		Ram r = Factory.getFactory(cfg).newRam(null, host, cfg.getLong("Capacity", 1024l) * Simulator.MEBIBYTE);
		r.setConfig(cfg);

		cfg = config.addContext("Pu");
		int pu_count = FactoryUtils.generateCount(cfg, 4);
		while (pu_count > 0) {
			ProcessingUnit pu = Factory.getFactory(cfg).newProcessingUnit(null, host);
			pu.setConfig(cfg);
			pu.setName(cfg.getString("Name", null));

			ComputingProvisioner cp = Factory.getFactory(cfg).newComputingProvisioner(null, pu, cfg.getLong("Mips", 1000l) * Simulator.MI);

			pu_count--;
		}

		cfg = config.addContext("Storage");
		int storage_count = FactoryUtils.generateCount(cfg, 1);
		while (storage_count > 0) {
			Storage storage = Factory.getFactory(cfg).newStorage(null, host, cfg.getLong("Capacity", 500000l) * Simulator.MEBIBYTE);
			storage.setConfig(cfg);
			storage.setName(cfg.getString("Name", null));

			StorageProvisioner sp = Factory.getFactory(cfg).newStorageProvisioner(null, storage, cfg.getLong("TransferRate", 300l) * Simulator.MEBIBYTE);

			storage_count--;
		}

		RoutingProtocol rp = Factory.getFactory(config).newRoutingProtocol(null, host);

		FactoryUtils.generateTraces(config, host);

		return host;
	}

	public static final String Switch_CONTEXT				= "Switch";

	/**
	 * Generates and returns a new {@link Switch}.
	 *
	 * @param config contexted configuration
	 * @param parent
	 * @return generated {@link Switch}
	 */
	public static Switch generateSwitch(Config config, Entity parent) {
		Switch s = Factory.getFactory(config).newSwitch(null, parent);
		s.setConfig(config);
		s.setName(config.getString("Name", null));

		Factory.getFactory(config).newRoutingProtocol(null, s);

		return s;
	}

	public static final String ThinClient_CONTEXT			= "ThinClient";

	/**
	 * Generate and returns a new thin client for the given user, and connects it to the Internet.
	 *
	 * <p>After that the thin client is generated,
	 * the {@link NotificationCodes#FACTORY_THINCLIENT_GENERATED} notification
	 * is thrown.
	 *
	 * @param config contexted configuration
	 * @param parent
	 * @return generated {@link ThinClient}
	 */
	public static ThinClient generateThinClient(Config config, User parent) {
		ThinClient h = Factory.getFactory(config).newThinClient(null, parent);
		h.setConfig(config);
		h.setName(config.getString("Name", null));

		Factory.getFactory(config).newRoutingProtocol(null, h);

		generateThinClientVirtualMachine(config, h);

		final Uniform lossRateGenerator = new Uniform(
				config.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMin", 0.0d),
				config.addContext(FactoryUtils.NetworkLink_CONTEXT).getDouble("LossRateMax", 0.0d)
			);
		final Uniform distanceGenerator = new Uniform(
				config.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMin", 0l),
				config.addContext(FactoryUtils.NetworkLink_CONTEXT).getLong("LengthMax", 0l)
			);

		FactoryUtils.connectToInternet(config,
				h,
				distanceGenerator.nextLong() * Simulator.LATENCY_PER_KILOMETER / 1000, //distance in meters
				lossRateGenerator.nextDouble()
			);

		FactoryUtils.generateTraces(config, h);

		if (parent != null)
			parent.notify(NotificationCodes.FACTORY_THINCLIENT_GENERATED, h);

		return h;
	}


	public static ThinClientVirtualMachine generateThinClientVirtualMachine(Config config, ThinClient parent) {
		ThinClientVirtualMachine r = Factory.getFactory(config).newThinClientVirtualMachine(null, parent);

		return r;
	}

	public static final String VirtualMachine_CONTEXT		= "VirtualMachine";

	/**
	 * Generates and returns a virtual machine (but does not place it).
	 *
	 * @param config contexted configuation
	 * @param user
	 * @return generated {@link VirtualMachine}
	 */
	public static final VirtualMachine generateVirtualMachine(Config config, User user) {
		return generateVirtualMachine(config, null, user);
	}

	/**
	 * Generates and returns a virtual machine after placing it using the cloud provider's placement policy.
	 *
	 * @param config contexted configuation
	 * @param cloudProvider
	 * @param user
	 * @return generated {@link VirtualMachine}
	 */
	public static final VirtualMachine generateVirtualMachine(Config config, CloudProvider cloudProvider, User user) {
		return Factory.getFactory(config).newVirtualMachineFactory(null, config, cloudProvider, user)
				.generate();
	}

	public static final String Topology_CONTEXT				= "Topology";

	public static void generateTopology(Config config, CloudProvider cloudProvider) {
		Factory.getFactory(config).newTopologyFactory(null, config, cloudProvider)
				.generate();
	}

	public static final String User_CONTEXT					= "User";

	public static User generateUser(Config config, CloudProvider parent) {
		return Factory.getFactory(config).newUserFactory(null, config, parent)
				.generate();
	}

	public static final String Trace_CONTEXT				= "Trace";

	/**
	 * Generates and returns a new {@link Trace}.
	 *
	 * @param config contexted configuation
	 * @param parent
	 * @return generated {@link Trace}
	 */
	public static Trace<?> generateTrace(Config config, Probed parent) {
		return Factory.getFactory(config).newTraceFactory(null, config, parent)
			.generate();
	}

	/**
	 * @param config configuation which contains all traces configurations
	 * @param parent
	 */
	public static void generateTraces(Config config, Probed parent) {
		FrequencyGenerationMode traceGenerationMode = new FrequencyGenerationMode(config, FactoryUtils.Trace_CONTEXT);
		for (int i=0; i<traceGenerationMode.getConfigurations().size(); i++)
			generateTrace(traceGenerationMode.next(), parent);
	}

	public static final String Job_CONTEXT				= "Job";

	/**
	 * Generates and returns a new {@link Job}.
	 *
	 * @param config contexted configuation
	 * @return generated {@link Job}
	 */
	public static Job generateJob(Config config) {
		Job job = Factory.getFactory(config).newJob(null, null);
		job.setConfig(config);
		job.setName(config.getString("Name", null));

		generateTraces(config, job);

		return job;
	}

	public static final String Workload_CONTEXT				= "Workload";

	/**
	 * <p>After that the workload is generated,
	 * a {@link NotificationCodes#FACTORY_WORKLOAD_GENERATED} notification
	 * is thrown.
	 *
	 * @param config
	 * @param owner
	 * @param initiating if <tt>true</tt>, then the simulator is initiating and workload is only
	 * started after that the simulator is initiated
	 * @return generated workload
	 */
	public static Workload generateWorkload(Config config, User owner, boolean initiating) {
		Workload workload = Factory.getFactory(config).newWorkload(null);
		workload.setConfig(config);
		generateTraces(config, Simulator.getSimulator());

		//place and start workload
		if (initiating)
			InitiateWorkloadNotificationListener.getInstance().addWorkload(workload);
		else
			workload.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new GenerateWorkloadStaticListener0());

		if (workload.getConfig().getBoolean("ThinClientWorkload", false))
			placeWorkloadOnThinClient(workload, owner);
		else
			placeWorkloadOnCloud(workload, owner);

		owner.notify(NotificationCodes.FACTORY_WORKLOAD_GENERATED, workload);

		return workload;
	}

	private static void checkStartWorkload(Workload workload) {
		if (workload.isRunning())
			return;
		if (!workload.canStart()) {
			if (workload.getParent().isTerminated())
				workload.doFail();
		} else
			workload.doStart();
	}

	private static class InitiateWorkloadNotificationListener extends NotificationListener {
		private ArrayList<Workload> workloads = new ArrayList<Workload>();

		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			this.discard();

			for (Workload workload: workloads) {
				if (workload.getParent() == null) {
					workload.addListener(NotificationCodes.ENTITY_PARENT_CHANGED, new GenerateWorkloadStaticListener0());
				} else
					checkStartWorkload(workload);
			}
			workloads = null;

			if (this == instance) {
				Simulator.getSimulator().removeListener(NotificationCodes.FACTORY_SIMULATOR_GENERATED, instance);
				instance = null;
			}
		}

		public void addWorkload(Workload workload) {
			if (workloads != null)
				workloads.add(workload);
		}

		private static InitiateWorkloadNotificationListener instance = null;
		private static InitiateWorkloadNotificationListener getInstance() {
			if (instance == null) {
				instance = new InitiateWorkloadNotificationListener();
				Simulator.getSimulator().addListener(NotificationCodes.FACTORY_SIMULATOR_GENERATED, instance);
			}
			return instance;
		}
	}

	private static final class GenerateWorkloadStaticListener0 extends
			NotificationListener {
		@Override
		protected void notificationPerformed(Notifier notifier,
				int notification_code, Object data) {
			this.discard();

			Workload workload = (Workload) notifier;
			workload.removeListener(NotificationCodes.ENTITY_PARENT_CHANGED, this);

			checkStartWorkload(workload);
		}
	}

	private static void placeWorkloadOnThinClient(Workload workload, User owner) {
		ThinClient h = null;
		List<ThinClient> l = owner.getThinClients();
		if (l.size() > 0)
			h = l.get((new Uniform(l.size() - 1)).nextInt());
		if (h == null)
			h = FactoryUtils.generateThinClient(owner.getConfig().addContext(FactoryUtils.ThinClient_CONTEXT), owner);

		workload.setParent(h.getVirtualMachine());
	}

	private static void placeWorkloadOnCloud(Workload workload, User owner) {
		VirtualMachine vm = owner.getParent().getJobPlacementPolicy().selectVm(workload, owner.getVirtualMachines());

		if (vm == null) //not found
			workload.doFail();
		else
			workload.setParent(vm);
	}

	public static final String Workload_TASK_CONTEXT		= "Task";

	private static Class<?> workloadTaskForName(String name) {
		if (name == null)
			return null;

		name = Task.class.getPackage().getName() + "." + name + "Task";

		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			Logger.getGlobal().log(Level.SEVERE, "Class " + name + " not found. This also may lead to performance issues.");
			return null;
		}
	}

	private static Class<?> getWorkloadTaskClass(Config config) {
		return config.getClassFromConfig("Task_Class", null);
	}

	private static Task newWorkloadTask(Class<?> clazz,
			Workload workload, Config config) {
		if (clazz == null) {
			clazz = getWorkloadTaskClass(config);

			if (clazz == null) {
				clazz = workloadTaskForName(config.getString("Task", null));

				if (clazz == null)
					return null;
			}
		}

		try {
			return (Task) clazz
					.getConstructor(Workload.class, Config.class)
					.newInstance(workload, config);
		} catch (Exception e) {
			Logger.getGlobal().logInstantiationException(clazz, e);
			return null;
		}
	}

	public static Task generateWorkloadTask(Workload workload, Config config) {
		Task t = newWorkloadTask(null, workload, config);
		if (t == null)
			Logger.getGlobal().log(Level.SEVERE, "Config task cannot be loaded. Context: " + config.getContext());
		return t;
	}

	public static final String CheckpointingHandler_CONTEXT		= "CheckpointingHandler";

	public static CheckpointingHandler generateCheckpointingHandler(CloudProvider cloudProvider, Config config) {
		CheckpointingHandler ch = Factory.getFactory(config).newCheckpointingHandler(null, cloudProvider);
		ch.setConfig(config);
		return ch;
	}

	public static final String Checkpoint_CONTEXT					= "Checkpointing";
}
