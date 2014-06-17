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

package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;

public class OpenCmsModuleConfigurationData {

	private boolean openCmsModuleEnabled = false;
    private String moduleName;
	private boolean useProjectDefaultVfsRootEnabled = true;
    private String localVfsRoot;
	private boolean useProjectDefaultSyncModeEnabled = true;
	private SyncMode syncMode;
	private boolean setSpecificModuleVersionEnabled = false;
	private String moduleVersion;

	public boolean isOpenCmsModuleEnabled() {
		return openCmsModuleEnabled;
	}

	public void setOpenCmsModuleEnabled(boolean openCmsModule) {
		this.openCmsModuleEnabled = openCmsModule;
	}

	public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

	public boolean isUseProjectDefaultVfsRootEnabled() {
		return useProjectDefaultVfsRootEnabled;
	}

	public void setUseProjectDefaultVfsRootEnabled(boolean useProjectDefaultVfsRootEnabled) {
		this.useProjectDefaultVfsRootEnabled = useProjectDefaultVfsRootEnabled;
	}

	public String getLocalVfsRoot() {
        return localVfsRoot;
    }

    public void setLocalVfsRoot(String localVfsRoot) {
        this.localVfsRoot = localVfsRoot;
    }

	public boolean isUseProjectDefaultSyncModeEnabled() {
		return useProjectDefaultSyncModeEnabled;
	}

	public void setUseProjectDefaultSyncModeEnabled(boolean useProjectDefaultSyncModeEnabled) {
		this.useProjectDefaultSyncModeEnabled = useProjectDefaultSyncModeEnabled;
	}

	public SyncMode getSyncMode() {
		return syncMode != null ? syncMode : SyncMode.PUSH;
	}

	public void setSyncMode(SyncMode syncMode) {
		this.syncMode = syncMode;
	}

	public boolean isSetSpecificModuleVersionEnabled() {
		return setSpecificModuleVersionEnabled;
	}

	public void setSetSpecificModuleVersionEnabled(boolean setSpecificModuleVersionEnabled) {
		this.setSpecificModuleVersionEnabled = setSpecificModuleVersionEnabled;
	}

	public String getModuleVersion() {
		return moduleVersion;
	}

	public void setModuleVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}
}
