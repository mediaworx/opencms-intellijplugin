/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.sync;

import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A List of SyncEntities
 */
public class SyncList extends ArrayList<SyncEntity> {

	private boolean syncModuleMetaData = false;
	private Collection<OpenCmsModule> ocmsModules;
	private boolean pullMetaDataOnly = false;

	/**
	 * @return <code>true</code> if the mode "pull meta data only" is enabled, <code>false</code> otherwise
	 */
	public boolean isPullMetaDataOnly() {
		return pullMetaDataOnly;
	}

	/**
	 * @param pullMetaDataOnly <code>true</code> to enable the mode "pull meta data only", <code>false</code> (default)
	 *                         to diable it
	 */
	public void setPullMetaDataOnly(boolean pullMetaDataOnly) {
		this.pullMetaDataOnly = pullMetaDataOnly;
	}

	/**
	 * @return <code>true</code> if module meta data is to be pulled, <code>false</code> otherwise
	 */
	public boolean isSyncModuleMetaData() {
		return syncModuleMetaData;
	}

	/**
	 * @param syncModuleMetaData <code>true</code> if module meta data should be pulled, <code>false</code> if
	 *                           meta data should not be pulled
	 */
	public void setSyncModuleMetaData(boolean syncModuleMetaData) {
		this.syncModuleMetaData = syncModuleMetaData;
	}

	/**
	 * @return Collection of OpenCms modules that are affected by the sync (modules that contain entities in this
	 *         SyncList)
	 */
	public Collection<OpenCmsModule> getOcmsModules() {
		return ocmsModules;
	}

	/**
	 * Adds a module to this SyncList
	 * @param ocmsModule the module to add
	 */
	public void addOcmsModule(OpenCmsModule ocmsModule) {
		if (ocmsModules == null) {
			ocmsModules = new ArrayList<OpenCmsModule>();
		}
		if (!ocmsModules.contains(ocmsModule)) {
			ocmsModules.add(ocmsModule);
		}
	}
}
