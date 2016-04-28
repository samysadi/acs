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

package com.samysadi.acs.hardware.storage.operation;

import com.samysadi.acs.hardware.network.operation.NetworkOperation;
import com.samysadi.acs.hardware.storage.StorageFile;
import com.samysadi.acs.utility.NotificationCodes;
import com.samysadi.acs.virtualization.job.operation.LongOperation;

/**
 * This interface defines methods to simulate a storage operation on a given {@link StorageFile}.
 *
 * <p>When starting a storage operation, it seeks transfer rate resource from the parent storage of its {@link StorageFile} and
 * keeps running until its entire length is processed (read, written or appended), until a failure happens or until it is explicitly stopped (using appropriate method).
 *
 * <p>A storage operation can be created for a local file that is located in the same
 * host with the operation, or for a remote file that is located on another host (ex: SAN storages).<br/>
 * If the file is remote, then a {@link NetworkOperation} is also instantiated to handle
 * the data transmission between the remote host and the local host. This operation is
 * transparently synchronized with the network operation to run at the same speed and to be paused
 * or to fail accordingly.
 *
 * <p>You must ensure that the parent Job is started in order to start this operation, otherwise an IllegalStateException is thrown.<br/>
 * If you start this operation while it has a <tt>null</tt> parent then a NullPointerException is thrown.
 *
 * <p>If your try to start this operation, while its {@link StorageFile} has a <tt>null</tt> storage parent or if that storage has <tt>null</tt> provisioner then a NullPointerException
 * is thrown when star.
 *
 * <p>Implementations have to listen and automatically stop the operation when:<ul>
 * 		<li> {@link NotificationCodes#OPERATION_RESOURCE_INVALIDATED}
 * 			The allocated TR for the operation changed;
 * 		<li> {@link NotificationCodes#FAILURE_STATE_CHANGED}
 * 			The Storage fails. Or the device that contains the storage fails.
 * </ul>
 * All implementation classes should provide a constructor with four (4) arguments.
 * The first specifies the {@link StorageFile}. The second specifies the {@link StorageOperationType}.
 * The third of <tt>long</tt> type specifies file position at which the operation will start.
 * And the fourth specifies the operation length.
 *
 * @since 1.0
 */
public interface StorageOperation extends LongOperation<StorageResource> {
	public enum StorageOperationType {
		NONE,
		READ,
		WRITE,
		APPEND,
		USER1, USER2, USER3
	}

	@Override
	public StorageOperation clone();

	public StorageFile getStorageFile();

	/**
	 * Updates the {@link StorageFile} of this operation. The operation continues (and does not restart) on the new file.
	 *
	 * <p>A {@link NotificationCodes#SO_SF_CHANGED} is also thrown.
	 *
	 * @param storageFile
	 * @throws NullPointerException if the given file is <tt>null</tt>
	 * @throws IllegalArgumentException if the operation cannot continue on the given file for any reason
	 * @throws IllegalStateException if the operation is running
	 */
	public void setStorageFile(StorageFile storageFile);

	public StorageOperationType getType();

	public long getFilePos();
}
