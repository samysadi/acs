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

package com.samysadi.acs.utility;

import com.samysadi.acs.core.Simulator;
import com.samysadi.acs.core.entity.RunnableEntity;
import com.samysadi.acs.core.notifications.CoreNotificationCodes;
import com.samysadi.acs.hardware.Host;
import com.samysadi.acs.hardware.misc.MemoryUnit;
import com.samysadi.acs.hardware.misc.MemoryZone;
import com.samysadi.acs.hardware.network.NetworkDevice;
import com.samysadi.acs.hardware.network.NetworkInterface;
import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.network.routingprotocol.RoutingProtocol;
import com.samysadi.acs.hardware.pu.operation.ComputingOperation;
import com.samysadi.acs.hardware.storage.Storage;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.hardware.storage.operation.StorageOperation;
import com.samysadi.acs.service.CloudProvider;
import com.samysadi.acs.service.checkpointing.CheckpointingHandler;
import com.samysadi.acs.service.checkpointing.checkpoint.Checkpoint;
import com.samysadi.acs.service.jobplacement.JobPlacementPolicy;
import com.samysadi.acs.service.migration.MigrationHandler;
import com.samysadi.acs.service.staas.sfconsistency.SfConsistencyManager;
import com.samysadi.acs.service.staas.sfplacement.SfPlacementPolicy;
import com.samysadi.acs.service.staas.sfreplication.SfReplicationManager;
import com.samysadi.acs.service.vmplacement.VmPlacementPolicy;
import com.samysadi.acs.user.ThinClient;
import com.samysadi.acs.user.User;
import com.samysadi.acs.utility.workload.Workload;
import com.samysadi.acs.utility.workload.task.Task;
import com.samysadi.acs.virtualization.VirtualMachine;
import com.samysadi.acs.virtualization.job.Job;
import com.samysadi.acs.virtualization.job.operation.Operation;
import com.samysadi.acs.virtualization.job.operation.RemoteOperation;
import com.samysadi.acs.virtualization.job.operation.provisioner.Provisioner;

/**
 * This class contains all predefined notification codes.
 *
 * <p><tt>0x8XXXXXXX</tt> notification codes are reserved.
 *
 * @since 1.0
 */
@SuppressWarnings("unused")
public class NotificationCodes extends CoreNotificationCodes {

	public static String notificationCodeToString(int notification_code) {
		return notificationCodeToString(NotificationCodes.class, notification_code);
	}

	/* OPERATION
	 * ------------------------------------------------------------------------
	 */

	private static final int OPERATION_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when this operation gets new resource.<br/>
	 * <b>Notifier:</b> {@link Operation}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int OPERATION_RESOURCE_CHANGED		= OPERATION_MASK | 0x30;

	/**
	 * <b>Description:</b> Thrown when the allocated resource for this Operation is no more valid (to keep consistency with the {@link Provisioner} allocated resources). The {@link Provisioner} initiates this notification.<br/>
	 * <b>Notifier:</b> {@link Operation}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int OPERATION_RESOURCE_INVALIDATED	= OPERATION_MASK | 0x31;

	/**
	 * <b>Description:</b> Thrown when the the operation gets a new destination job.<br/>
	 * <b>Notifier:</b> {@link RemoteOperation}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int OPERATION_DEST_JOB_CHANGED		= OPERATION_MASK | 0x80;


	/* NETWORK DEVICE
	 * ------------------------------------------------------------------------
	 */

	private static final int ND_MASK = nextMask();

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link NetworkDevice}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int ND_LOCATION_CHANGED				= ND_MASK | 0x81;


	/* NETWORK INTERFACE
	 * ------------------------------------------------------------------------
	 */

	private static final int NI_MASK = nextMask();

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link NetworkInterface}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int NI_LINKING_UPDATED				= NI_MASK | 0x81;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link NetworkInterface}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int NI_IPADDRESS_CHANGED			= NI_MASK | 0x88;


	/* NETWORK OPERATION
	 * ------------------------------------------------------------------------
	 */

	private static final int NETOP_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when a new route has been assigned for the current NetworkOperation.<br/>
	 * <b>Notifier:</b> {@link NetworkOperation}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int NETOP_ROUTE_CHANGED			= NETOP_MASK | 0x81;


	/* ROUTING PROTOCOL
	 * ------------------------------------------------------------------------
	 */

	private static final int RP_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when the routes that were generated using this RoutingProtocol have to be reviewed because they may be no more valid.<br/>
	 * <b>Notifier:</b> {@link RoutingProtocol}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int RP_ROUTING_UPDATED				= RP_MASK | 0x00;

	/* HOST
	 * ------------------------------------------------------------------------
	 */
	private static final int HOST_MASK = nextMask();

	/* MemoryUnit
	 * ------------------------------------------------------------------------
	 */
	private static final int MU_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when this memory unit's free capacity changes<br/>
	 * <b>Notifier:</b> {@link MemoryUnit}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int MU_FREE_CAPACITY_CHANGED		= MU_MASK | 0x80;

	/**
	 * <b>Description:</b> Thrown when this memory unit's capacity changes<br/>
	 * <b>Notifier:</b> {@link MemoryUnit}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int MU_CAPACITY_CHANGED				= MU_MASK | 0x81;

	/* MemoryZone
	 * ------------------------------------------------------------------------
	 */
	private static final int MZ_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when this memory zone's size changes<br/>
	 * <b>Notifier:</b> {@link MemoryZone}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int MZ_SIZE_CHANGED					= MZ_MASK | 0x81;

	/**
	 * <b>Description:</b> Thrown when this memory zone's MetaData changes<br/>
	 * <b>Notifier:</b> {@link MemoryZone}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int MZ_METADATA_CHANGED				= MZ_MASK | 0x84;

	/**
	 * <b>Description:</b> Thrown when this memory zone is modified<br/>
	 * <b>Notifier:</b> {@link MemoryZone}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int MZ_MODIFIED						= MZ_MASK | 0x88;

	/* Ram
	 * ------------------------------------------------------------------------
	 */
	private static final int RAM_MASK = nextMask();

	/* RamZone
	 * ------------------------------------------------------------------------
	 */
	private static final int RZ_MASK = nextMask();

	/* STORAGE
	 * ------------------------------------------------------------------------
	 */
	private static final int STORAGE_MASK = nextMask();

	/* STORAGE FILE
	 * ------------------------------------------------------------------------
	 */
	private static final int SF_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when this storage file's share mode changes.<br/>
	 * <b>Notifier:</b> {@link StorageFile}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int SF_SHAREMODE_CHANGED			= SF_MASK | 0x82;

	/**
	 * <b>Description:</b> Thrown when this storage file's user changes.<br/>
	 * <b>Notifier:</b> {@link StorageFile}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int SF_USER_CHANGED					= SF_MASK | 0x90;

	/* STORAGE OPERATION
	 * ------------------------------------------------------------------------
	 */

	private static final int SO_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when a new storage file has been assigned for the current {@link StorageOperation}.<br/>
	 * <b>Notifier:</b> {@link StorageOperation}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int SO_SF_CHANGED					= SO_MASK | 0x81;

	/* PROCESSING UNIT
	 * ------------------------------------------------------------------------
	 */
	private static final int PU_MASK = nextMask();

	/* COMPUTING OPERATION
	 * ------------------------------------------------------------------------
	 */

	private static final int CO_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when a new processing unit has been assigned for the current ComputingOperation.<br/>
	 * <b>Notifier:</b> {@link ComputingOperation}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CO_PU_CHANGED					= CO_MASK | 0x81;

	/* USER
	 * ------------------------------------------------------------------------
	 */
	private static final int USER_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when the current user is defined as the owner of a VM.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> the attached {@link VirtualMachine}
	 */
	public static final int USER_VM_ATTACHED				= USER_MASK | 0x30;

	/**
	 * <b>Description:</b> Thrown when the current user is no more defined as the owner of a VM.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> the detached {@link VirtualMachine}
	 */
	public static final int USER_VM_DETACHED				= USER_MASK | 0x31;

	/**
	 * <b>Description:</b> Thrown when the current user is defined as the owner of a file.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> the attached {@link StorageFile}
	 */
	public static final int USER_STORAGEFILE_ATTACHED		= USER_MASK | 0x32;

	/**
	 * <b>Description:</b> Thrown when the current user is no more defined as the owner of a file.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> the detached {@link StorageFile}
	 */
	public static final int USER_STORAGEFILE_DETACHED		= USER_MASK | 0x33;


	/* VIRTUAL MACHINE
	 * ------------------------------------------------------------------------
	 */
	private static final int VM_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when this VM's flag has changed.<br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int VM_FLAG_CHANGED 				= VM_MASK | 0x80;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int VM_RAM_CHANGED 			= VM_MASK | 0x88;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int VM_STORAGE_CHANGED 		= VM_MASK | 0x89;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int VM_USABLE_PROCESSING_UNITS_CHANGED = VM_MASK | 0x8A;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int VM_USABLE_NETWORK_INTERFACES_CHANGED = VM_MASK | 0x8B;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int VM_USER_CHANGED 				= VM_MASK | 0x90;

	/**
	 * <b>Description:</b> Thrown after that this VM was migrated, and gets a new parent.<br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> {@link MigrationResult}
	 */
	public static final int VM_MIGRATED						= VM_MASK | 0xB0;

	/**
	 * <b>Description:</b> Thrown if this VM cannot be migrated.<br/>
	 * <b>Notifier:</b> {@link VirtualMachine}<br/>
	 * <b>Object:</b> {@link MigrationRequest}
	 */
	public static final int VM_CANNOT_BE_MIGRATED			= VM_MASK | 0xB1;

	/* JOB
	 * ------------------------------------------------------------------------
	 */
	private static final int JOB_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown when the current job is defined as the source job of a operation (you can also listen to {@link NotificationCodes#ENTITY_ADDED} on the current job).<br/>
	 * <b>Notifier:</b> {@link Job}<br/>
	 * <b>Object:</b> {@link Operation}
	 */
	public static final int JOB_SRC_OPERATION_ADDED			= JOB_MASK | 0x30;

	/**
	 * <b>Description:</b> Thrown when the current job is no more defined as the source job of a operation (you can also listen to {@link NotificationCodes#ENTITY_REMOVED} on the current job).<br/>
	 * <b>Notifier:</b> {@link Job}<br/>
	 * <b>Object:</b> {@link Operation}
	 */
	public static final int JOB_SRC_OPERATION_REMOVED		= JOB_MASK | 0x31;

	/**
	 * <b>Description:</b> Thrown when the current job is defined as the destination job of a operation.<br/>
	 * <b>Notifier:</b> {@link Job}<br/>
	 * <b>Object:</b> {@link Operation}
	 */
	public static final int JOB_DEST_OPERATION_ADDED		= JOB_MASK | 0x38;

	/**
	 * <b>Description:</b> Thrown when the current job is no more defined as the destination job of a operation.<br/>
	 * <b>Notifier:</b> {@link Job}<br/>
	 * <b>Object:</b> {@link Operation}
	 */
	public static final int JOB_DEST_OPERATION_REMOVED		= JOB_MASK | 0x39;

	/**
	 * <b>Description:</b> Thrown when the current job's priority is modified.<br/>
	 * <b>Notifier:</b> {@link Job}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int JOB_PRIORITY_CHANGED			= JOB_MASK | 0x80;

	/* CLOUD_PROVIDER
	 * ------------------------------------------------------------------------
	 */
	private static final int CP_MASK = nextMask();

	/* VM_PLACEMENT_POLICY
	 * ------------------------------------------------------------------------
	 */
	private static final int VMPLACEMENT_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after successful call to {@link VmPlacementPolicy#selectHost(VirtualMachine)},
	 * and that a potential host was found to place the VM on it.<br/>
	 * <b>Notifier:</b> {@link VmPlacementPolicy}<br/>
	 * <b>Object:</b> the {@link VirtualMachine}
	 */
	public static final int VMPLACEMENT_VMSELECTION_SUCCESS				= VMPLACEMENT_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that no host could be found for a VM to place it on, after
	 * calling {@link VmPlacementPolicy#selectHost(VirtualMachine)}.<br/>
	 * <b>Notifier:</b> {@link VmPlacementPolicy}<br/>
	 * <b>Object:</b> the {@link VirtualMachine} that cannot be placed
	 */
	public static final int VMPLACEMENT_VMSELECTION_FAILED				= VMPLACEMENT_MASK | 0x01;

	/* JOB_PLACEMENT_POLICY
	 * ------------------------------------------------------------------------
	 */
	private static final int JOBPLACEMENT_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after successful call to {@link JobPlacementPolicy#selectVm(Job, java.util.List)},
	 * and that a potential VM was found to place the Job on it.<br/>
	 * <b>Notifier:</b> {@link JobPlacementPolicy}<br/>
	 * <b>Object:</b> the {@link Job}
	 */
	public static final int JOBPLACEMENT_VMSELECTION_SUCCESS			= JOBPLACEMENT_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that no VM could be found for a Job to place it on, after
	 * calling {@link JobPlacementPolicy#selectVm(Job, java.util.List)}<br/>
	 * <b>Notifier:</b> {@link JobPlacementPolicy}<br/>
	 * <b>Object:</b> the {@link Job}
	 */
	public static final int JOBPLACEMENT_VMSELECTION_FAILED				= JOBPLACEMENT_MASK | 0x01;

	/*
	 * staas.REPLICATION_MANAGER
	 * ------------------------------------------------------------------------
	 */
	private static final int SFRM_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after that a new {@link StorageFile} is registered for replication.<br/>
	 * <b>Notifier:</b> {@link SfReplicationManager}<br/>
	 * <b>Object:</b> the registered {@link StorageFile}
	 */
	public static final int SFRM_REGISTERED					= SFRM_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that a {@link StorageFile} is unregistered from replication.<br/>
	 * <b>Notifier:</b> {@link SfReplicationManager}<br/>
	 * <b>Object:</b> the unregistered {@link StorageFile}
	 */
	public static final int SFRM_UNREGISTERED				= SFRM_MASK | 0x01;

	/*
	 * staas.CONSISTENCY_MANAGER
	 * ------------------------------------------------------------------------
	 */
	private static final int SFCM_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after that a new {@link StorageFile} is registered for consistency management.<br/>
	 * <b>Notifier:</b> {@link SfConsistencyManager}<br/>
	 * <b>Object:</b> the registered {@link StorageFile}
	 */
	public static final int SFCM_REGISTERED					= SFCM_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that a {@link StorageFile} is unregistered from consistency management.<br/>
	 * <b>Notifier:</b> {@link SfConsistencyManager}<br/>
	 * <b>Object:</b> the unregistered {@link StorageFile}
	 */
	public static final int SFCM_UNREGISTERED				= SFCM_MASK | 0x01;

	/*
	 * staas.PLACEMENT_POLICY
	 * ------------------------------------------------------------------------
	 */
	private static final int SFP_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after successful call to {@link SfPlacementPolicy#selectStorage(StorageFile)},
	 * and that a potential {@link Storage} was found to place the storage file on it.<br/>
	 * <b>Notifier:</b> {@link SfPlacementPolicy}<br/>
	 * <b>Object:</b> the {@link StorageFile}
	 */
	public static final int SFP_STORAGESELECTION_SUCCESS	= SFP_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that no Storage could be found for a {@link StorageFile} to place it on, after
	 * calling {@link SfPlacementPolicy#selectStorage(StorageFile)}<br/>
	 * <b>Notifier:</b> {@link SfPlacementPolicy}<br/>
	 * <b>Object:</b> the {@link StorageFile} that cannot be placed
	 */
	public static final int SFP_STORAGESELECTION_FAILED		= SFP_MASK | 0x01;

	/* CHEKPOINT_HANDLER
	 * ------------------------------------------------------------------------
	 */
	private static final int CH_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after that a checkpoint was successfully updated.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_UPDATE_SUCCESS		= CH_MASK | 0x10;

	/**
	 * <b>Description:</b> Thrown after that an error happens during a checkpoint update.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_UPDATE_ERROR			= CH_MASK | 0x11;

	/**
	 * <b>Description:</b> Thrown after that a checkpoint was successfully used for recovery.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> the newly created {@link RunnableEntity}
	 */
	public static final int CHECKPOINT_RECOVER_SUCCESS		= CH_MASK | 0x20;

	/**
	 * <b>Description:</b> Thrown after that an error happens when using a checkpoint for recovery.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_RECOVER_ERROR		= CH_MASK | 0x21;

	/**
	 * <b>Description:</b> Thrown after that a copy of the checkpoint was successfully created on a new memory zone.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> the newly created {@link Checkpoint}
	 */
	public static final int CHECKPOINT_COPY_SUCCESS			= CH_MASK | 0x30;

	/**
	 * <b>Description:</b> Thrown after that an error happens when trying to copy the checkpoint to a new memory zone.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_COPY_ERROR			= CH_MASK | 0x31;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_MEMORYZONE_CHANGED	= CH_MASK | 0x80;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_STATE_CHANGED 		= CH_MASK | 0x81;

	/**
	 * <b>Description:</b><br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_CHECKPOINTINGHANDLER_CHANGED = CH_MASK | 0x88;

	/**
	 * <b>Description:</b> Thrown after that the checkpoint has been deleted.<br/>
	 * <b>Notifier:</b> {@link Checkpoint}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int CHECKPOINT_DELETED				= CH_MASK | 0xA0;

	/* CHEKPOINTING_HANDLER
	 * ------------------------------------------------------------------------
	 */
	private static final int CHH_MASK = nextMask();

	public static class CheckpointingHandlerDeleteResult<E extends RunnableEntity, C extends Checkpoint<E,?>> {
		private E entity;
		private C checkpoint;

		public CheckpointingHandlerDeleteResult(E entity, C checkpoint) {
			super();
			this.entity = entity;
			this.checkpoint = checkpoint;
		}

		/**
		 * Returns the entity for which the checkpoint was deleted.
		 *
		 * @return the entity for which the checkpoint was deleted.
		 */
		public E getEntity() {
			return entity;
		}

		/**
		 * Returns the {@link Checkpoint} that was deleted.
		 *
		 * <p><b>Note</b> that using {@link Checkpoint#getParent()} on the checkpoint will return <tt>null</tt>
		 * after the deletion.
		 * Use {@link CheckpointingHandlerDeleteResult#getEntity()} instead.
		 *
		 * @return the {@link Checkpoint} that was deleted.
		 */
		public C getCheckpoint() {
			return checkpoint;
		}
	}

	/**
	 * <b>Description:</b> Thrown after that an entity was registered for automatic checkpointing.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link RunnableEntity} that was registered.
	 */
	public static final int CHECKPOINTINGHANDLER_REGISTERED		= CHH_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that an entity was unregistered from automatic checkpointing.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link RunnableEntity} that was unregistered.
	 */
	public static final int CHECKPOINTINGHANDLER_UNREGISTERED	= CHH_MASK | 0x01;

	/**
	 * <b>Description:</b> Thrown after that a entity's checkpoint is automatically updated.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link Checkpoint}.
	 */
	public static final int CHECKPOINTINGHANDLER_AUTOUPDATE_SUCCESS	= CHH_MASK | 0x20;

	/**
	 * <b>Description:</b> Thrown after that an error happens when trying to automatically update an entity's checkpoint.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link Checkpoint}.
	 */
	public static final int CHECKPOINTINGHANDLER_AUTOUPDATE_ERROR	= CHH_MASK | 0x21;

	/**
	 * <b>Description:</b> Thrown after that a entity's checkpoint is automatically used for recovery.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link Checkpoint}.
	 */
	public static final int CHECKPOINTINGHANDLER_AUTORECOVER_SUCCESS	= CHH_MASK | 0x30;

	/**
	 * <b>Description:</b> Thrown after that an error happens when trying to automatically recover using an entity's checkpoint.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link Checkpoint}.
	 */
	public static final int CHECKPOINTINGHANDLER_AUTORECOVER_ERROR	= CHH_MASK | 0x31;

	/**
	 * <b>Description:</b> Thrown after that a new checkpoint was create by the {@link CheckpointingHandler}.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the created {@link Checkpoint}.
	 */
	public static final int CHECKPOINTINGHANDLER_CHECKPOINT_CREATED	= CHH_MASK | 0x40;

	/**
	 * <b>Description:</b> Thrown after that an error is thrown while the {@link CheckpointingHandler} is trying to create a checkpoint.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link RunnableEntity} for which the handler tried to create a checkpoint.
	 */
	public static final int CHECKPOINTINGHANDLER_CHECKPOINT_ERROR	= CHH_MASK | 0x41;

	/**
	 * <b>Description:</b> Thrown after that a checkpoint was deleted using the {@link CheckpointingHandler} delete methods.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link CheckpointingHandlerDeleteResult} object holding the information about the deleted checkpoint.
	 */
	public static final int CHECKPOINTINGHANDLER_CHECKPOINT_DELETED	= CHH_MASK | 0x48;

	/**
	 * <b>Description:</b> Thrown after that the {@link CheckpointingHandler} found a checkpoint to use for recovery.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the found {@link Checkpoint}
	 */
	public static final int CHECKPOINTINGHANDLER_RECOVER_SUCCESS		= CHH_MASK | 0x50;

	/**
	 * <b>Description:</b> Thrown if the {@link CheckpointingHandler} cannot find a checkpoint to use for recovery.<br/>
	 * <b>Notifier:</b> {@link CheckpointingHandler}<br/>
	 * <b>Object:</b> the {@link RunnableEntity} for which the handler tried to find a checkpoint
	 */
	public static final int CHECKPOINTINGHANDLER_RECOVER_ERROR		= CHH_MASK | 0x51;



	/* MIGRATION_HANDLER
	 * ------------------------------------------------------------------------
	 */
	private static final int MH_MASK = nextMask();

	public static class MigrationRequest {
		private VirtualMachine vm;
		private Host destinationHost;

		public MigrationRequest(VirtualMachine vm, Host destinationHost) {
			super();
			this.vm = vm;
			this.destinationHost = destinationHost;
		}

		public VirtualMachine getVm() {
			return vm;
		}

		public Host getDestinationHost() {
			return destinationHost;
		}
	}

	public static class MigrationResult {
		private MigrationRequest request;

		public MigrationResult(MigrationRequest request) {
			super();
			this.request = request;
		}

		public MigrationRequest getRequest() {
			return request;
		}
	}

	/**
	 * <b>Description:</b> Thrown after that the VM was successfully migrated.<br/>
	 * <b>Notifier:</b> {@link MigrationHandler}<br/>
	 * <b>Object:</b> {@link MigrationResult}.
	 */
	public static final int MIGRATION_SUCCESS				= MH_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that an error happens when migrating a VM.<br/>
	 * <b>Notifier:</b> {@link MigrationHandler}<br/>
	 * <b>Object:</b> {@link MigrationRequest}.
	 */
	public static final int MIGRATION_ERROR					= MH_MASK | 0x01;

	/* WORKLOAD
	 * ------------------------------------------------------------------------
	 */
	private static final int WORKLOAD_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown by a {@link Task} when it is completed successfully.<br/>
	 * <b>Notifier:</b> {@link Workload}<br/>
	 * <b>Object:</b> {@link Task}.
	 */
	public static final int WORKLOAD_TASK_COMPLETED			= WORKLOAD_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown by a {@link Task} when and error happens.<br/>
	 * <b>Notifier:</b> {@link Workload}<br/>
	 * <b>Object:</b> {@link Task}.
	 */
	public static final int WORKLOAD_TASK_FAILED			= WORKLOAD_MASK | 0x01;

	/* FACTORY
	 * ------------------------------------------------------------------------
	 */
	private static final int FACTORY_MASK = nextMask();

	/**
	 * <b>Description:</b> Thrown after that the whole simulator have been generated including all its children.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_SIMULATOR_GENERATED				= FACTORY_MASK | 0x00;

	/**
	 * <b>Description:</b> Thrown after that all cloud providers have been generated including all their children.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_ALL_CLOUDPROVIDERS_GENERATED	= FACTORY_MASK | 0x10;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated the cloud provider and all its children.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> {@link CloudProvider}
	 */
	public static final int FACTORY_CLOUDPROVIDER_GENERATED			= FACTORY_MASK | 0x11;

	/**
	 * <b>Description:</b> Thrown after that failures have been generated.<br/>
	 * <b>Notifier:</b> {@link Simulator}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_FAILURES_GENERATED				= FACTORY_MASK | 0x12;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated the topology.<br/>
	 * <b>Notifier:</b> {@link CloudProvider}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_TOPOLOGY_GENERATED				= FACTORY_MASK | 0x20;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated all users and all their children.<br/>
	 * <b>Notifier:</b> {@link CloudProvider}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_ALL_USERS_GENERATED				= FACTORY_MASK | 0x22;

	/**
	 * <b>Description:</b> Thrown after that the factory have been generated the user and all its children.<br/>
	 * <b>Notifier:</b> {@link CloudProvider}<br/>
	 * <b>Object:</b> {@link User}
	 */
	public static final int FACTORY_USER_GENERATED					= FACTORY_MASK | 0x23;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated all thin clients in the
	 * current stage (the factory may generate more thin clients afterwards).<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_ALL_THINCLIENTS_GENERATED		= FACTORY_MASK | 0x30;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated the thin client and all its children.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> {@link ThinClient}
	 */
	public static final int FACTORY_THINCLIENT_GENERATED			= FACTORY_MASK | 0x31;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated all virtual machines in the
	 * current stage (the factory may generate more virtual machines afterwards).<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_ALL_VIRTUALMACHINES_GENERATED	= FACTORY_MASK | 0x32;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated the virtual machine and all its children.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> {@link VirtualMachine}
	 */
	public static final int FACTORY_VIRTUALMACHINE_GENERATED		= FACTORY_MASK | 0x33;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated all workloads in the
	 * current stage (the factory may generate more workloads afterwards).<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> <tt>null</tt>
	 */
	public static final int FACTORY_ALL_WORKLOADS_GENERATED			= FACTORY_MASK | 0x34;

	/**
	 * <b>Description:</b> Thrown after that the factory have generated the workload and all its children.<br/>
	 * <b>Notifier:</b> {@link User}<br/>
	 * <b>Object:</b> {@link Workload}
	 */
	public static final int FACTORY_WORKLOAD_GENERATED				= FACTORY_MASK | 0x35;

}
