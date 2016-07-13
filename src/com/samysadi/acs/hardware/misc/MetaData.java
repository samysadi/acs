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

package com.samysadi.acs.hardware.misc;

import com.samysadi.acs.utility.collections.Bitmap;

/**
 * This class contains meta data of a memory zone.<br/>
 * Especially, it contains a <i>data identifier</i> to uniquely identify the
 * data contained inside of that memory zone. This <i>data identifier</i>
 * is the equivalent to the path used for uniquely identifying a file inside a file-system.
 *
 * <p>You can test if a MetaData is describing the same data using the method {@link MetaData#isSameData(MetaData)}.
 *
 * <p>This class only allows you to set basic meta-data. You are free to create subclasses to add extra fields.
 *
 * @since 1.0
 */
public class MetaData implements Cloneable {
	private static long dataIdCounter = 0l;
	private long dataId;
	private long versionId;

	public MetaData() {
		newDataId();
		this.versionId = 0;
	}

	/**
	 * This method creates and returns a copy of the current meta-data.
	 *
	 * <p>The newly created meta-data instance will contain the same <i>data identifier</i> as
	 * the current instance. In other words, the two instances will be describing the same data.
	 *
	 * <p>The {@link Bitmap} is also cloned.
	 *
	 * @return the newly created MetaData
	 */
	@Override
	public MetaData clone() {
		final MetaData clone;
		try {
			clone = (MetaData) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

		return clone;
	}

	/**
	 * Returns an long that uniquely identifies the data related to this MetaData.
	 * This is the equivalent to the path used for uniquely identifying a file inside a file-system.
	 *
	 * @return a long that uniquely identifies the data related to this MetaData
	 */
	protected long getDataId() {
		return this.dataId;
	}

	/**
	 * Sets a new <tt>data identifier</tt> for this meta data.
	 *
	 * <p>Concretely, this means that this meta-data describes a totally different data.
	 */
	protected void newDataId() {
		dataIdCounter++;
		this.dataId = dataIdCounter;
	}

	/**
	 * Return a long that contains the version number of the data related to this MetaData.
	 * This long should be modified accordingly to reflect changes on the data that is related to this MetaData.
	 *
	 * @return a long that contains the version number of the data related to this MetaData
	 */
	public long getVersionId() {
		return versionId;
	}

	/**
	 * Updates the version number of this data.
	 *
	 * @param versionId
	 */
	public void setVersionId(long versionId) {
		this.versionId = versionId;
	}


	/**
	 * Returns <tt>true</tt> if this MetaData describes the same Data as the given <tt>metaData</tt>.
	 *
	 * <p>The returned value is:<br/>
	 * <pre>{@code this.getDataId() == metaData.getDataId();}</pre>
	 *
	 * @param metaData
	 * @return <tt>true</tt> if this MetaData describes the same Data as the given <tt>metaData</tt>
	 */
	public boolean isSameData(MetaData metaData) {
		return this.getDataId() == metaData.getDataId();
	}
}
