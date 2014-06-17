/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
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

public class SyncList extends ArrayList<SyncEntity> {

	private boolean syncModuleMetaData = false;
	private Collection<OpenCmsModule> ocmsModules;
	private boolean pullMetaDataOnly = false;

	public boolean isPullMetaDataOnly() {
		return pullMetaDataOnly;
	}

	public void setPullMetaDataOnly(boolean pullMetaDataOnly) {
		this.pullMetaDataOnly = pullMetaDataOnly;
	}

	public boolean isSyncModuleMetaData() {
		return syncModuleMetaData;
	}

	public void setSyncModuleMetaData(boolean syncModuleMetaData) {
		this.syncModuleMetaData = syncModuleMetaData;
	}

	public Collection<OpenCmsModule> getOcmsModules() {
		return ocmsModules;
	}

	public void addOcmsModule(OpenCmsModule ocmsModule) {
		if (ocmsModules == null) {
			ocmsModules = new ArrayList<OpenCmsModule>();
		}
		if (!ocmsModules.contains(ocmsModule)) {
			ocmsModules.add(ocmsModule);
		}
	}
}
