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

import java.util.logging.Level;

import com.samysadi.acs.core.Config;
import com.samysadi.acs.core.Logger;
import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.Entity;
import com.samysadi.acs.core.tracing.Probe;
import com.samysadi.acs.core.tracing.Probed;
import com.samysadi.acs.core.tracing.Trace;
import com.samysadi.acs.core.tracing.TraceDefault;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.HostDefault;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.NetworkInterfaceDefault;
import com.samysadi.acs.hardware.network.NetworkLink;
import com.samysadi.acs.hardware.network.NetworkLinkDefault;
import com.samysadi.acs.hardware.network.Switch;
import com.samysadi.acs.hardware.network.SwitchDefault;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.operation.NetworkOperationDefault;
import com.samysadi.acs.hardware.network.operation.provisioner.FairNetworkProvisioner;
import com.samysadi.acs.hardware.network.operation.provisioner.NetworkProvisioner;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocolDefault;
import com.samysadi.acs.hardware.pu.ProcessingUnit;
import com.samysadi.acs.hardware.pu.ProcessingUnitDefault;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.hardware.pu.operation.ComputingOperationDefault;
import com.samysadi.acs.hardware.pu.operation.provisioner.ComputingProvisioner;
import com.samysadi.acs.hardware.pu.operation.provisioner.FairComputingProvisioner;
import com.samysadi.acs.hardware.ram.Ram;
import com.samysadi.acs.hardware.ram.RamDefault;
import com.samysadi.acs.hardware.ram.RamZone;
import com.samysadi.acs.hardware.ram.RamZoneDefault;
import com.samysadi.acs.hardware.ram.VirtualRam;
import com.samysadi.acs.hardware.ram.VirtualRamFixedSize;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageDefault;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.StorageFileDefault;
import com.samysadi.acs.hardware.storage.VirtualStorage;
import com.samysadi.acs.hardware.storage.VirtualStorageFixedSize;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.hardware.storage.operation.StorageOperation.StorageOperationType;
import com.samysadi.acs.hardware.storage.operation.StorageOperationDefault;
import com.samysadi.acs.hardware.storage.operation.provisioner.FairStorageProvisioner;
import com.samysadi.acs.hardware.storage.operation.provisioner.StorageProvisioner;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.CloudProviderDefault;
import com.samysadi.acs.service.checkpointing.Checkpoint;
import com.samysadi.acs.service.checkpointing.CheckpointDefault;
import com.samysadi.acs.service.checkpointing.CheckpointingHandler;
import com.samysadi.acs.service.checkpointing.CheckpointingHandlerDefault;
import com.samysadi.acs.service.jobplacement.JobPlacementPolicy;
import com.samysadi.acs.service.jobplacement.JobPlacementPolicyRandomFit;
import com.samysadi.acs.service.migration.MigrationHandler;
import com.samysadi.acs.service.migration.MigrationHandlerDefault;
import com.samysadi.acs.service.power.PowerManager;
import com.samysadi.acs.service.power.PowerManagerDefault;
import com.samysadi.acs.service.staas.Staas;
import com.samysadi.acs.service.staas.StaasDefault;
import com.samysadi.acs.service.staas.sfconsistency.SfConsistencyManager;
import com.samysadi.acs.service.staas.sfconsistency.SfConsistencyManagerDefault;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicyRandomFit;
import com.samysadi.acs.service.staas.sfreplicaselection.SfReplicaSelectionPolicy;
import com.samysadi.acs.service.staas.sfreplicaselection.SfReplicaSelectionPolicyDefault;
import com.samysadi.acs.service.staas.sfreplication.SfReplicationManager;
import com.samysadi.acs.service.staas.sfreplication.SfReplicationManagerDefault;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicy;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicyRandomFit;
import com.samysadi.acs.tracing.AbstractProbe;
import com.samysadi.acs.tracing.CustomProbe;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.user.ThinClientDefault;
import com.samysadi.acs.user.ThinClientVirtualMachine;
import com.samysadi.acs.user.ThinClientVirtualMachineDefault;
import com.samysadi.acs.user.User;
import com.samysadi.acs.user.UserDefault;
import com.samysadi.acs.utility.factory.generation.flow.GenerationFlow;
import com.samysadi.acs.utility.factory.generation.flow.GenerationFlowDefault;
import com.samysadi.acs.utility.factory.generation.mode.FrequencyGenerationMode;
import com.samysadi.acs.utility.factory.generation.mode.GenerationMode;
import com.samysadi.acs.utility.workload.Workload;
import com.samysadi.acs.utility.workload.WorkloadDefault;
import com.samysadi.acs.virtualization.PuAllocator;
import com.samysadi.acs.virtualization.PuAllocatorDefault;
import com.samysadi.acs.virtualization.TemporaryVirtualMachine;
import com.samysadi.acs.virtualization.TemporaryVirtualMachineDefault;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.VirtualMachineDefault;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.JobDefault;

/**
 * This Factory contains methods to create instances of different classes, accordingly to a given
 * configuration.
 * 
 * @since 1.0
 */
public class Factory  {
	private Config config;

	protected Factory(Config config) {
		super();
		setConfig(config);
	}

	public Config getConfig() {
		return this.config;
	}

	private void setConfig(Config config) {
		this.config = config;
	}

	protected final Logger getLogger() {
		return Logger.getGlobal();
	}

	protected Object generate() {
		getLogger().log("Nothing to generate");
		return null;
	}

	private static <T extends Entity> T setParentFor(T o, Entity parent) {
		o.setParent(parent);
		return o;
	}

	/* ********************************************************************* */
	
	/**
	 * Returns a factory instance created using the given configuration.
	 * 
	 * @return a factory instance created using the given configuration
	 */
	public static Factory getFactory(Config config) {
		return new Factory(config);
	}
	
	/**
	 * Returns a factory instance created using the given entity's configuration
	 * 
	 * @return a factory instance created using the given entity's configuration
	 * @see Entity#getConfigRec()
	 */
	public static Factory getFactory(Entity entity) {
		return new Factory(entity.getConfigRec());
	}

	/* ********************************************************************* */

	/**
	 * Adds a link to the Internet for the given device.
	 * 
	 * <p>See this {@link Factory#linkDevices(NetworkDevice, NetworkDevice, Class, Class, Class, Class, long, long, long, double) method} for
	 * more details.
	 * 
	 * @param networkDevice the device to add a link to
	 * @param networkInterfaceClass
	 * @param networkLinkClass
	 * @param networkProvisionerClass
	 * @param linkUpBw maximum bandwidth when the given <tt>networkDevice</tt> sends data to the Internet
	 * @param linkDownBw maximum bandwidth when the given <tt>networkDevice</tt> receives data from the Internet
	 * @param linkLatency latency of the link between the given <tt>networkDevice</tt> and the Internet
	 * @param linkLossRate loss rate of the link between the given <tt>networkDevice</tt> and the Internet
	 */
	public void connectToInternet(
			NetworkDevice networkDevice,
			Class<?> networkInterfaceClass,
			Class<?> networkLinkClass, Class<?> networkProvisionerClass,
			long linkUpBw, long linkDownBw, long linkLatency, double linkLossRate) {
		this.linkDevices(
				networkDevice, getOrCreateInternetSwitch(),
				networkInterfaceClass == null ? Factory.getFactory(networkDevice).getNetworkInterfaceClass() : networkInterfaceClass,
				this.getNetworkInterfaceClass(),
				networkLinkClass, networkProvisionerClass,
				linkUpBw, linkDownBw, linkLatency, linkLossRate
			);
	}

	/**
	 * Removes all links between the given <tt>networkDevice</tt> and the Internet.
	 * 
	 * <p>After calling this method, the given <tt>networkDevice</tt> will have no direct access to 
	 * the Internet through its own network interfaces.
	 * 
	 * @param networkDevice
	 */
	public static void disconnectFromInternet(NetworkDevice networkDevice) {
		Switch internetSwitch = getInternetSwitch();
		if (internetSwitch == null)
			return;
		for (NetworkInterface ni: networkDevice.getInterfaces())
			if (internetSwitch == ni.getRemoteNetworkInterface().getParent()) {
				ni.getUpLink().setParent(null);
				ni.getDownLink().setParent(null);
				ni.setParent(null);
				ni.getRemoteNetworkInterface().setParent(null);
			}
	}

	/**
	 * Returns <tt>true</tt> if the given <tt>networkDevice</tt> has at least one direct access to the Internet
	 * through one of its own interfaces.
	 * 
	 * @param networkDevice
	 * @return <tt>true</tt> if the given <tt>networkDevice</tt> is connected to the Internet
	 */
	public static boolean isConnectedToInternet(NetworkDevice networkDevice) {
		Switch internetSwitch = getInternetSwitch();
		if (internetSwitch == null)
			return false;
		for (NetworkInterface ni: networkDevice.getInterfaces())
			if (internetSwitch == ni.getRemoteNetworkInterface().getParent())
				return true;
		return false;
	}


	private static final Object INTERNET_SWITCH = new Object();

	private Switch getOrCreateInternetSwitch() {
		Switch s = getInternetSwitch();
		if (s != null)
			return s;
		
		s = newSwitch(null, Simulator.getSimulator());
		RoutingProtocol rp = new InternetRoutingProtocol();
		rp.setParent(s);

		Simulator.getSimulator().setProperty(INTERNET_SWITCH, s);
		return s;
	}

	private static final class InternetRoutingProtocol extends
			RoutingProtocolDefault {
		@Override
		protected double getMetricCost() {
			return 1024.0d;
		}

		@Override
		protected RouteConstraints getNextConstraintsForInterface(
				NetworkDevice destinationDevice,
				NetworkInterface nextInterface, RouteConstraints constraints) {
			RouteConstraints r = super.getNextConstraintsForInterface(destinationDevice, nextInterface,
					constraints);
			if (r == null)
				return null;

			Entity np = nextInterface.getRemoteNetworkInterface().getParent().getParent();
			Entity dp = destinationDevice.getParent();
			if (np == dp)
				return r;
			if (dp instanceof CloudProvider)
				return null;

			return r;
		}
	}

	private static Switch getInternetSwitch() {
		return (Switch) Simulator.getSimulator().getProperty(INTERNET_SWITCH);
	}

	/* ********************************************************************* */

	/**
	 * Creates a symmetrical connection (independent upload and download operations)
	 * between device0 and device1 and returns an array containing the two created {@link NetworkInterface}s.
	 * The first is the interface of <tt>device0</tt>. And the second is the interface of <tt>device1</tt>.
	 * 
	 * @param device0
	 * @param device1
	 * @param networkInterface0Class needs not to be <tt>null</tt> or an exception is thrown
	 * @param networkInterface1Class needs not to be <tt>null</tt> or an exception is thrown
	 * @param networkLinkClass if <tt>null</tt> will use current factory's defaults
	 * @param networkProvisionerClass if <tt>null</tt> will use current factory's defaults
	 * @param linkUpBw Maximum BW when sending data from device0 to device1
	 * @param linkDownBw Maximum BW when sending data from device1 to device0
	 * @param linkLatency
	 * @param linkLossRate
	 * @return an array containing the two created {@link NetworkInterface}s
	 * @throws IllegalArgumentException if <tt>device0</tt> or <tt>device1</tt> has no defined parent
	 */
	public NetworkInterface[] linkDevices(
			NetworkDevice device0, NetworkDevice device1,
			Class<?> networkInterface0Class, Class<?> networkInterface1Class,
			Class<?> networkLinkClass, Class<?> networkProvisionerClass,
			long linkUpBw, long linkDownBw, long linkLatency, double linkLossRate) {
		if (device0.getParent() == null || device1.getParent() == null)
			throw new IllegalArgumentException("Both devices must have a defined parent in order to link them");

		final NetworkInterface[] ni = new NetworkInterface[2];

		if (networkInterface0Class == null || networkInterface1Class == null)
			throw new NullPointerException("You must supply network interface classes");

		//create interfaces
		ni[0] = newNetworkInterface(networkInterface0Class, device0);
		ni[1] = newNetworkInterface(networkInterface1Class, device1);

		//create links
		final NetworkLink upLink = newNetworkLink(networkLinkClass, device0.getParent(), ni[0], ni[1]);
		final NetworkLink downLink = newNetworkLink(networkLinkClass, device1.getParent(), ni[1], ni[0]);

		//attach links to interfaces
		ni[0].setUpLink(upLink); // should update ni[1] down link
		ni[1].setUpLink(downLink); // should update ni[0] down link

		//Add provisioners
		Factory f = new Factory(config);
		f.newNetworkProvisioner(networkProvisionerClass, ni[0].getUpLink(), linkUpBw, linkLatency, linkLossRate);
		f.newNetworkProvisioner(networkProvisionerClass, ni[0].getDownLink(), linkDownBw, linkLatency, linkLossRate);

		return ni;
	}

	public Class<?> getCheckpointClass() {
		return getConfig().getClassFromConfig("Checkpoint_Class", CheckpointDefault.class, true);
	}

	public Checkpoint newCheckpoint(Class<?> clazz, VirtualMachine parent) {
		if (clazz == null)
			clazz = getCheckpointClass();

		try {
			return setParentFor((Checkpoint) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getCheckpointingHandlerClass() {
		return getConfig().getClassFromConfig("CheckpointingHandler_Class", CheckpointingHandlerDefault.class, true);
	}

	public CheckpointingHandler newCheckpointingHandler(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getCheckpointingHandlerClass();

		try {
			return setParentFor((CheckpointingHandler) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getCloudProviderClass() {
		return getConfig().getClassFromConfig("CloudProvider_Class", CloudProviderDefault.class, true);
	}

	public CloudProvider newCloudProvider(Class<?> clazz, Simulator parent) {
		if (clazz == null)
			clazz = getCloudProviderClass();

		try {
			return setParentFor((CloudProvider) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getCloudProviderFactoryClass() {
		return getConfig().getClassFromConfig("CloudProviderFactory_Class", CloudProviderFactoryDefault.class, true);
	}

	public CloudProviderFactory newCloudProviderFactory(Class<?> clazz,
			Config config) {
		if (clazz == null)
			clazz = getCloudProviderFactoryClass();

		try {
			return (CloudProviderFactory) clazz
					.getConstructor(Config.class)
					.newInstance(config);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getComputingOperationClass() {
		return getConfig().getClassFromConfig("ComputingOperation_Class", ComputingOperationDefault.class, true);
	}

	public ComputingOperation newComputingOperation(Class<?> clazz, Job parent,
			long lengthInMi) {
		if (clazz == null)
			clazz = getComputingOperationClass();

		try {
			return setParentFor((ComputingOperation) clazz
					.getConstructor(long.class)
					.newInstance(lengthInMi), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getComputingProvisionerClass() {
		return getConfig().getClassFromConfig("ComputingProvisioner_Class", FairComputingProvisioner.class, true);
	}

	public ComputingProvisioner newComputingProvisioner(Class<?> clazz, ProcessingUnit parent,
			long mipsCapacity) {
		if (clazz == null)
			clazz = getComputingProvisionerClass();

		try {
			return setParentFor((ComputingProvisioner) clazz
					.getConstructor(long.class)
					.newInstance(mipsCapacity), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getFailureFactoryClass() {
		return getConfig().getClassFromConfig("FailureFactory_Class", FailuresFactoryDefault.class, true);
	}

	public FailuresFactory newFailureFactory(Class<?> clazz,
			Config config) {
		if (clazz == null)
			clazz = getFailureFactoryClass();

		try {
			return (FailuresFactory) clazz
					.getConstructor(Config.class)
					.newInstance(config);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getGenerationFlowClass() {
		return getConfig().getClassFromConfig("GenerationFlow_Class", GenerationFlowDefault.class);
	}

	public GenerationFlow newGenerationFlow(Class<?> clazz,
			Config config) {
		if (clazz == null)
			clazz = getGenerationFlowClass();

		try {
			return (GenerationFlow) clazz
					.getConstructor(Config.class)
					.newInstance(config);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public GenerationFlow newGenerationFlow(Class<?> clazz,
			String context) {
		return newGenerationFlow(clazz, getConfig().addContext(context));
	}

	public Class<?> getGenerationModeClass() {
		return getConfig().getClassFromConfig("GenerationMode_Class", FrequencyGenerationMode.class);
	}

	public GenerationMode newGenerationMode(Class<?> clazz,
			Config config, String context) {
		if (clazz == null)
			clazz = getGenerationModeClass();

		try {
			return (GenerationMode) clazz
					.getConstructor(Config.class, String.class)
					.newInstance(config, context);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public GenerationMode newGenerationMode(Class<?> clazz,
			String context) {
		return newGenerationMode(clazz, getConfig(), context);
	}

	public Class<?> getHostClass() {
		return getConfig().getClassFromConfig("Host_Class", HostDefault.class, true);
	}

	public Host newHost(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getHostClass();

		try {
			return setParentFor((Host) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getJobClass() {
		return getConfig().getClassFromConfig("Job_Class", JobDefault.class, true);
	}

	public Job newJob(Class<?> clazz,
			VirtualMachine parent) {
		if (clazz == null)
			clazz = getJobClass();

		try {
			return setParentFor((Job) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getJobPlacementPolicyClass() {
		return getConfig().getClassFromConfig("JobPlacementPolicy_Class", JobPlacementPolicyRandomFit.class, true);
	}

	public JobPlacementPolicy newJobPlacementPolicy(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getJobPlacementPolicyClass();

		try {
			return setParentFor((JobPlacementPolicy) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getMigrationHandlerClass() {
		return getConfig().getClassFromConfig("MigrationHandler_Class", MigrationHandlerDefault.class, true);
	}

	public MigrationHandler newMigrationHandler(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getMigrationHandlerClass();

		try {
			return setParentFor((MigrationHandler) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getNetworkInterfaceClass() {
		return getConfig().getClassFromConfig("NetworkInterface_Class", NetworkInterfaceDefault.class, true);
	}

	public NetworkInterface newNetworkInterface(Class<?> clazz, NetworkDevice parent) {
		if (clazz == null)
			clazz = getNetworkInterfaceClass();

		try {
			return setParentFor((NetworkInterface) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getNetworkLinkClass() {
		return getConfig().getClassFromConfig("NetworkLink_Class", NetworkLinkDefault.class, true);
	}

	public NetworkLink newNetworkLink(Class<?> clazz, Entity parent,
			NetworkInterface ni0, NetworkInterface ni1) {
		if (clazz == null)
			clazz = getNetworkLinkClass();

		try {
			return setParentFor((NetworkLink) clazz
					.getConstructor(NetworkInterface.class, NetworkInterface.class)
					.newInstance(ni0, ni1), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getNetworkOperationClass() {
		return getConfig().getClassFromConfig("NetworkOperation_Class", NetworkOperationDefault.class, true);
	}

	public NetworkOperation newNetworkOperation(Class<?> clazz, Job parent,
			Job destinationJob, long dataSize) {
		if (clazz == null)
			clazz = getNetworkOperationClass();

		try {
			return setParentFor((NetworkOperation) clazz
					.getConstructor(Job.class, long.class)
					.newInstance(destinationJob, dataSize), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getNetworkProvisionerClass() {
		return getConfig().getClassFromConfig("NetworkProvisioner_Class", FairNetworkProvisioner.class, true);
	}

	public NetworkProvisioner newNetworkProvisioner(Class<?> clazz, Entity parent,
			long bwCapacity, long latency, double lossRate) {
		if (clazz == null)
			clazz = getNetworkProvisionerClass();

		try {
			return setParentFor((NetworkProvisioner) clazz
					.getConstructor(long.class, long.class, double.class)
					.newInstance(bwCapacity, latency, lossRate), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getPowerManagerClass() {
		return getConfig().getClassFromConfig("PowerManager_Class", PowerManagerDefault.class, true);
	}

	public PowerManager newPowerManager(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getPowerManagerClass();

		try {
			return setParentFor((PowerManager) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	private String getProbeKeySubPackage(String probeKey) {
		int len = probeKey.length();
		int e = 1;
		
		while (e < len) {
			char c = probeKey.charAt(e);
			if (c >= 'A' && c <= 'Z')
				break;
			e++;
		}
		return Character.toLowerCase(probeKey.charAt(0)) + probeKey.substring(1, e) + ".";
	}

	public Class<?> getProbeClass(String probeKey) {
		Class<?> clazz = getConfig().getClassFromConfig(probeKey + "Probe_Class", null, true);
		if (clazz == null) {
			String className;
			if (probeKey.startsWith(CustomProbe.CUSTOM_PROBE_PREFIX))
				className = CustomProbe.class.getName();
			else
				className = AbstractProbe.class.getPackage().getName()
						+ "." + getProbeKeySubPackage(probeKey) + probeKey + "Probe";
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				getLogger().log(Level.SEVERE, "Class " + className + " not found. This also may lead to performance issues.");
			}
		}
		return clazz;
	}

	public Probe<?> newProbe(Class<?> clazz) {
		if (clazz == null) {
			getLogger().log(Level.SEVERE, "Cannot create probe: null class supplied.");
			return null;
		}

		try {
			return (Probe<?>) clazz
					.getConstructor()
					.newInstance();
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Probe<?> newProbe(String probeKey) {
		Probe<?> p = newProbe(getProbeClass(probeKey));
		if (p instanceof CustomProbe)
			((CustomProbe) p).setKey(probeKey);
		return p;
	}

	public Class<?> getProcessingUnitClass() {
		return getConfig().getClassFromConfig("ProcessingUnit_Class", ProcessingUnitDefault.class, true);
	}

	public ProcessingUnit newProcessingUnit(Class<?> clazz, Host parent) {
		if (clazz == null)
			clazz = getProcessingUnitClass();

		try {
			return setParentFor((ProcessingUnit) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getPuAllocatorClass() {
		return getConfig().getClassFromConfig("PuAllocator_Class", PuAllocatorDefault.class, true);
	}

	public PuAllocator newPuAllocator(Class<?> clazz, VirtualMachine parent) {
		if (clazz == null)
			clazz = getPuAllocatorClass();

		try {
			return setParentFor((PuAllocator) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getRamClass() {
		return getConfig().getClassFromConfig("Ram_Class", RamDefault.class, true);
	}

	public Ram newRam(Class<?> clazz, Host parent,
			long capacity) {
		if (clazz == null)
			clazz = getRamClass();

		try {
			return setParentFor((Ram) clazz
					.getConstructor(long.class)
					.newInstance(capacity), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getRamZoneClass() {
		return getConfig().getClassFromConfig("RamZone_Class", RamZoneDefault.class, true);
	}

	public RamZone newRamZone(Class<?> clazz, Ram parent,
			long size) {
		if (clazz == null)
			clazz = getRamZoneClass();

		try {
			return setParentFor((RamZone) clazz
					.getConstructor(long.class)
					.newInstance(size), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getRoutingProtocolClass() {
		return getConfig().getClassFromConfig("RoutingProtocol_Class", RoutingProtocolDefault.class, true);
	}

	public RoutingProtocol newRoutingProtocol(Class<?> clazz, NetworkDevice parent) {
		if (clazz == null)
			clazz = getRoutingProtocolClass();

		try {
			return setParentFor((RoutingProtocol) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSfConsistencyManagerClass() {
		return getConfig().getClassFromConfig("SfConsistencyManager_Class", SfConsistencyManagerDefault.class, true);
	}

	public SfConsistencyManager newSfConsistencyManager(Class<?> clazz, Staas parent) {
		if (clazz == null)
			clazz = getSfConsistencyManagerClass();

		try {
			return setParentFor((SfConsistencyManager) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSfPlacementPolicyClass() {
		return getConfig().getClassFromConfig("SfPlacementPolicy_Class", SfPlacementPolicyRandomFit.class, true);
	}

	public SfPlacementPolicy newSfPlacementPolicy(Class<?> clazz, Staas parent) {
		if (clazz == null)
			clazz = getSfPlacementPolicyClass();

		try {
			return setParentFor((SfPlacementPolicy) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSfReplicaSelectionPolicyClass() {
		return getConfig().getClassFromConfig("SfReplicaSelectionPolicy_Class", SfReplicaSelectionPolicyDefault.class, true);
	}

	public SfReplicaSelectionPolicy newSfReplicaSelectionPolicy(Class<?> clazz, Staas parent) {
		if (clazz == null)
			clazz = getSfReplicaSelectionPolicyClass();

		try {
			return setParentFor((SfReplicaSelectionPolicy) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSfReplicationManagerClass() {
		return getConfig().getClassFromConfig("SfReplicationManager_Class", SfReplicationManagerDefault.class, true);
	}

	public SfReplicationManager newSfReplicationManager(Class<?> clazz, Staas parent) {
		if (clazz == null)
			clazz = getSfReplicationManagerClass();

		try {
			return setParentFor((SfReplicationManager) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSimulatorClass() {
		return getConfig().getClassFromConfig("Simulator_Class", Simulator.class, true);
	}

	public Simulator newSimulator(Class<?> clazz,
			Config config) {
		if (clazz == null)
			clazz = getSimulatorClass();

		try {
			return (Simulator) clazz
					.getConstructor(Config.class)
					.newInstance(config);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSimulatorFactoryClass() {
		return getConfig().getClassFromConfig("SimulatorFactory_Class", SimulatorFactoryDefault.class, true);
	}

	public SimulatorFactory newSimulatorFactory(Class<?> clazz,
			Config config) {
		if (clazz == null)
			clazz = getSimulatorFactoryClass();

		try {
			return (SimulatorFactory) clazz
					.getConstructor(Config.class)
					.newInstance(config);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getStaasClass() {
		return getConfig().getClassFromConfig("Staas_Class", StaasDefault.class, true);
	}

	public Staas newStaas(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getStaasClass();

		try {
			return setParentFor((Staas) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getStorageClass() {
		return getConfig().getClassFromConfig("Storage_Class", StorageDefault.class, true);
	}

	public Storage newStorage(Class<?> clazz, Host parent,
			long storageCapacity) {
		if (clazz == null)
			clazz = getStorageClass();

		try {
			return setParentFor((Storage) clazz
					.getConstructor(long.class)
					.newInstance(storageCapacity), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getStorageFileClass() {
		return getConfig().getClassFromConfig("StorageFile_Class", StorageFileDefault.class, true);
	}

	public StorageFile newStorageFile(Class<?> clazz, Storage parent,
			long size) {
		if (clazz == null)
			clazz = getStorageFileClass();

		try {
			return setParentFor((StorageFile) clazz
					.getConstructor(long.class)
					.newInstance(size), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getStorageOperationClass() {
		return getConfig().getClassFromConfig("StorageOperation_Class", StorageOperationDefault.class, true);
	}

	public StorageOperation newStorageOperation(Class<?> clazz, Job parent,
			StorageFile file, StorageOperationType type, long filePos, long size) {
		if (clazz == null)
			clazz = getStorageOperationClass();

		try {
			return setParentFor((StorageOperation) clazz
					.getConstructor(StorageFile.class, StorageOperationType.class, long.class, long.class)
					.newInstance(file, type, filePos, size), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getStorageProvisionerClass() {
		return getConfig().getClassFromConfig("StorageProvisioner_Class", FairStorageProvisioner.class, true);
	}

	public StorageProvisioner newStorageProvisioner(Class<?> clazz, Storage parent,
			long transferRateCapacity) {
		if (clazz == null)
			clazz = getStorageProvisionerClass();

		try {
			return setParentFor((StorageProvisioner) clazz
					.getConstructor(long.class)
					.newInstance(transferRateCapacity), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getSwitchClass() {
		return getConfig().getClassFromConfig("Switch_Class", SwitchDefault.class, true);
	}

	public Switch newSwitch(Class<?> clazz, Entity parent) {
		if (clazz == null)
			clazz = getSwitchClass();

		try {
			return setParentFor((Switch) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getTemporaryVirtualMachineClass() {
		return getConfig().getClassFromConfig("TemporaryVirtualMachine_Class", TemporaryVirtualMachineDefault.class, true);
	}

	public TemporaryVirtualMachine newTemporaryVirtualMachine(Class<?> clazz) {
		if (clazz == null)
			clazz = getTemporaryVirtualMachineClass();

		try {
			return (TemporaryVirtualMachine) clazz
					.getConstructor()
					.newInstance();
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getThinClientClass() {
		return getConfig().getClassFromConfig("ThinClient_Class", ThinClientDefault.class, true);
	}

	public ThinClient newThinClient(Class<?> clazz, User parent) {
		if (clazz == null)
			clazz = getThinClientClass();

		try {
			return setParentFor((ThinClient) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getThinClientVirtualMachineClass() {
		return getConfig().getClassFromConfig("ThinClientVirtualMachine_Class", ThinClientVirtualMachineDefault.class, true);
	}

	public ThinClientVirtualMachine newThinClientVirtualMachine(Class<?> clazz, ThinClient parent) {
		if (clazz == null)
			clazz = getThinClientVirtualMachineClass();

		try {
			return setParentFor((ThinClientVirtualMachine) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getTopologyFactoryClass() {
		return getConfig().getClassFromConfig("TopologyFactory_Class", TopologyFactoryFlat.class, true);
	}

	public TopologyFactory newTopologyFactory(Class<?> clazz,
			Config config, CloudProvider cloudProvider) {
		if (clazz == null)
			clazz = getTopologyFactoryClass();

		try {
			return (TopologyFactory) clazz
					.getConstructor(Config.class, CloudProvider.class)
					.newInstance(config, cloudProvider);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getTraceClass() {
		return getConfig().getClassFromConfig("Trace_Class", TraceDefault.class, true);
	}

	public Trace<?> newTrace(Class<?> clazz,
			Probe<?> probe) {
		if (clazz == null)
			clazz = getTraceClass();

		try {
			return (Trace<?>) clazz
					.getConstructor(Probe.class)
					.newInstance(probe);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getTraceFactoryClass() {
		return getConfig().getClassFromConfig("TraceFactory_Class", TraceFactoryDefault.class, true);
	}

	public TraceFactory newTraceFactory(Class<?> clazz,
			Config config, Probed probed) {
		if (clazz == null)
			clazz = getTraceFactoryClass();

		try {
			return (TraceFactory) clazz
					.getConstructor(Config.class, Probed.class)
					.newInstance(config, probed);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getUserClass() {
		return getConfig().getClassFromConfig("User_Class", UserDefault.class, true);
	}

	public User newUser(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getUserClass();

		try {
			return setParentFor((User) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getUserFactoryClass() {
		return getConfig().getClassFromConfig("UserFactory_Class", UserFactoryDefault.class, true);
	}

	public UserFactory newUserFactory(Class<?> clazz,
			Config config, CloudProvider cloudProvider) {
		if (clazz == null)
			clazz = getUserFactoryClass();

		try {
			return (UserFactory) clazz
					.getConstructor(Config.class, CloudProvider.class)
					.newInstance(config, cloudProvider);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getVirtualMachineClass() {
		return getConfig().getClassFromConfig("VirtualMachine_Class", VirtualMachineDefault.class, true);
	}

	public VirtualMachine newVirtualMachine(Class<?> clazz) {
		if (clazz == null)
			clazz = getVirtualMachineClass();

		try {
			return (VirtualMachine) clazz
					.getConstructor()
					.newInstance();
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getVirtualRamClass() {
		return getConfig().getClassFromConfig("VirtualRam_Class", VirtualRamFixedSize.class, true);
	}

	public VirtualRam newVirtualRam(Class<?> clazz, Ram parent,
			long capacity) {
		if (clazz == null)
			clazz = getVirtualRamClass();

		try {
			return setParentFor((VirtualRam) clazz
					.getConstructor(long.class)
					.newInstance(capacity), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getVirtualStorageClass() {
		return getConfig().getClassFromConfig("VirtualStorage_Class", VirtualStorageFixedSize.class, true);
	}

	public VirtualStorage newVirtualStorage(Class<?> clazz, Storage parent,
			long capacity) {
		if (clazz == null)
			clazz = getVirtualStorageClass();

		try {
			return setParentFor((VirtualStorage) clazz
					.getConstructor(long.class)
					.newInstance(capacity), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getVirtualMachineFactoryClass() {
		return getConfig().getClassFromConfig("VirtualMachineFactory_Class", VirtualMachineFactoryDefault.class, true);
	}

	public VirtualMachineFactory newVirtualMachineFactory(Class<?> clazz,
			Config config, CloudProvider cloudProvider, User user) {
		if (clazz == null)
			clazz = getVirtualMachineFactoryClass();

		try {
			return (VirtualMachineFactory) clazz
					.getConstructor(Config.class, CloudProvider.class, User.class)
					.newInstance(config, cloudProvider, user);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getVmPlacementPolicyClass() {
		return getConfig().getClassFromConfig("VmPlacementPolicy_Class", VmPlacementPolicyRandomFit.class, true);
	}

	public VmPlacementPolicy newVmPlacementPolicy(Class<?> clazz, CloudProvider parent) {
		if (clazz == null)
			clazz = getVmPlacementPolicyClass();

		try {
			return setParentFor((VmPlacementPolicy) clazz
					.getConstructor()
					.newInstance(), parent);
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}

	public Class<?> getWorkloadClass() {
		return getConfig().getClassFromConfig("Workload_Class", WorkloadDefault.class, true);
	}

	public Workload newWorkload(Class<?> clazz) {
		if (clazz == null)
			clazz = getWorkloadClass();

		try {
			return (Workload) clazz
					.getConstructor()
					.newInstance();
		} catch (Exception e) {
			getLogger().logInstantiationException(clazz, e);
			return null;
		}
	}
}
