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

/**
 * Configuration data container for the module level configuration of the OpenCms plugin for IntelliJ.
 */
public class OpenCmsModuleConfigurationData {

	private boolean openCmsModuleEnabled = false;
	private boolean useProjectDefaultModuleNameEnabled = true;
    private String moduleName;
	private boolean useProjectDefaultVfsRootEnabled = true;
    private String localVfsRoot;
    private String exportImportSiteRoot;
	private boolean useProjectDefaultSyncModeEnabled = true;
	private SyncMode syncMode;
	private boolean setSpecificModuleVersionEnabled = false;
	private String moduleVersion;

	/**
	 * Flag denoting if this IntelliJ module represents an OpenCms module
	 * @return <code>true</code> if this IntelliJ module represents an OpenCms module, <code>false</code> otherwise
	 */
	public boolean isOpenCmsModuleEnabled() {
		return openCmsModuleEnabled;
	}

	/**
	 * Sets the flag denoting if this IntelliJ module represents an OpenCms module
	 * @param openCmsModuleEnabled <code>true</code> if this IntelliJ module represents an OpenCms module,
	 *                             <code>false</code> otherwise
	 */
	public void setOpenCmsModuleEnabled(boolean openCmsModuleEnabled) {
		this.openCmsModuleEnabled = openCmsModuleEnabled;
	}

	/**
	 * Flag denoting if the module uses the project's default OpenCms module name.
	 * @return  <code>true</code> if the module uses the project's default OpenCms module name, <code>false</code> if a
	 *          differing OpenCms module name is configured for the module.
	 * @see OpenCmsPluginConfigurationData#getModuleNamingScheme()
	 */
	public boolean isUseProjectDefaultModuleNameEnabled() {
		return useProjectDefaultModuleNameEnabled;
	}

	/**
	 * Sets the flag denoting if the module uses the project's default OpenCms module name.
	 * @param useProjectDefaultModuleNameEnabled <code>true</code> if the module should use the project's default OpenCms module name,
	 *                                        <code>false</code> if a differing OpenCms module name is configured for the module.
	 * @see OpenCmsPluginConfigurationData#setModuleNamingScheme(String)
	 */
	public void setUseProjectDefaultModuleNameEnabled(boolean useProjectDefaultModuleNameEnabled) {
		this.useProjectDefaultModuleNameEnabled = useProjectDefaultModuleNameEnabled;
	}

	/**
	 * Returns the configured OpenCms module package name (e.g. "com.mycompany.myproject.mymodule").
	 * @return  the configured OpenCms module package name
	 */
	public String getModuleName() {
        return moduleName;
    }

	/**
	 * Sets the OpenCms module package name.
	 * @param moduleName the OpenCms module package name (e.g. "com.mycompany.myproject.mymodule").
	 */
    public void setModuleName(String moduleName) {
	    this.moduleName = moduleName;
    }

	/**
	 * Flag denoting if the module uses the project's default VFS root.
	 * @return  <code>true</code> if the module uses the project's default VFS root, <code>false</code> if a
	 *          differing VFS root is configured for the module.
	 * @see OpenCmsPluginConfigurationData#getDefaultLocalVfsRoot()
	 */
	public boolean isUseProjectDefaultVfsRootEnabled() {
		return useProjectDefaultVfsRootEnabled;
	}

	/**
	 * Sets the flag denoting if the module uses the project's default VFS root.
	 * @param useProjectDefaultVfsRootEnabled <code>true</code> if the module should use the project's default VFS root,
	 *                                        <code>false</code> if a differing VFS root is configured for the module.
	 * @see OpenCmsPluginConfigurationData#setDefaultLocalVfsRoot(String)
	 */
	public void setUseProjectDefaultVfsRootEnabled(boolean useProjectDefaultVfsRootEnabled) {
		this.useProjectDefaultVfsRootEnabled = useProjectDefaultVfsRootEnabled;
	}

	/**
	 * Returns the local VFS root path configured for the module. Only used if {@link #isUseProjectDefaultVfsRootEnabled}
	 * returns <code>true</code>.
	 * @return the local VFS root path configured for the module
	 * @see OpenCmsPluginConfigurationData#getDefaultLocalVfsRoot()
	 */
	public String getLocalVfsRoot() {
        return localVfsRoot;
    }

	/**
	 * Sets the local VFS root path for the module. Only used if {@link #setUseProjectDefaultVfsRootEnabled(boolean)}
	 * is set to <code>true</code>.
	 * @param localVfsRoot the local VFS root path to be used for the module
	 * @see OpenCmsPluginConfigurationData#setDefaultLocalVfsRoot(String)
	 */
    public void setLocalVfsRoot(String localVfsRoot) {
        this.localVfsRoot = localVfsRoot;
    }

	/**
	 * Returns the VFS site root to be used when importing the module. Each module can configure its own import root.
	 * That might be useful if you want to have a site's content in a module (then the module root would be something
	 * like /sites/mysite).
	 */
	public String getExportImportSiteRoot() {
		return exportImportSiteRoot;
	}

	/**
	 * Sets VFS site root to be used when importing the module. Each module can configure its own import root. That
	 * might be useful if you want to have a site's content in a module (then the module root would be something like
	 * /sites/mysite).
	 */
	public void setExportImportSiteRoot(String exportImportSiteRoot) {
		this.exportImportSiteRoot = exportImportSiteRoot;
	}

	/**
	 * Flag denoting if the module uses the project's default Sync Mode.
	 * @return  <code>true</code> if the module uses the project's default Sync Mode, <code>false</code> if a
	 *          differing Sync Mode is configured for the module.
	 * @see OpenCmsPluginConfigurationData#getDefaultSyncMode()
	 */
	public boolean isUseProjectDefaultSyncModeEnabled() {
		return useProjectDefaultSyncModeEnabled;
	}

	/**
	 * Sets the flag denoting if the module uses the project's default Sync Mode.
	 * @param useProjectDefaultSyncModeEnabled <code>true</code> if the module should use the project's default Sync Mode,
	 *                                        <code>false</code> if a differing Sync Mode is configured for the module.
	 * @see OpenCmsPluginConfigurationData#setDefaultSyncMode(SyncMode)
	 */
	public void setUseProjectDefaultSyncModeEnabled(boolean useProjectDefaultSyncModeEnabled) {
		this.useProjectDefaultSyncModeEnabled = useProjectDefaultSyncModeEnabled;
	}

	/**
	 * Returns the module's Sync Mode (PUSH, SYNC or PULL). Only used if {@link #isUseProjectDefaultSyncModeEnabled}
	 * returns <code>true</code>.
	 * @return  the module's Sync Mode
	 */
	public SyncMode getSyncMode() {
		return syncMode != null ? syncMode : SyncMode.PUSH;
	}

	/**
	 * Sets the module's Sync Mode. Only used if {@link #setUseProjectDefaultSyncModeEnabled(boolean)} is set to
	 * <code>true</code>.
	 * @param syncMode Sync Mode to be used by the module (PUSH, SYNC or PULL)
	 * @see OpenCmsPluginConfigurationData#setDefaultSyncMode(SyncMode)
	 */
	public void setSyncMode(SyncMode syncMode) {
		this.syncMode = syncMode;
	}

	/**
	 * Flag denoting if a specific module version is to be set in the module's manifest file
	 * (<code>manifest.xml</code>).
	 * @return <code>true</code> if a specific module version is to be set in the manifest file, <code>false</code>
	 *         otherwise
	 */
	public boolean isSetSpecificModuleVersionEnabled() {
		return setSpecificModuleVersionEnabled;
	}

	/**
	 * Sets the flag denoting if a specific module version is to be set in the module's manifest file
	 * (<code>manifest.xml</code>).
	 * @param setSpecificModuleVersionEnabled <code>true</code> if a specific module version is to be set in the
	 *                                        manifest file, <code>false</code> otherwise
	 */
	public void setSetSpecificModuleVersionEnabled(boolean setSpecificModuleVersionEnabled) {
		this.setSpecificModuleVersionEnabled = setSpecificModuleVersionEnabled;
	}

	/**
	 * Returns the module version to be used in the module's manifest file (<code>manifest.xml</code>).
	 * @return the module version to be used in the module's manifest file
	 */
	public String getModuleVersion() {
		return moduleVersion;
	}

	/**
	 * Sets the module version to be used in the module's manifest file (<code>manifest.xml</code>).
	 * @param moduleVersion the module version to be used in the module's manifest file (e.g. "2.1")
	 */
	public void setModuleVersion(String moduleVersion) {
		if (!moduleVersion.equals(this.moduleVersion)) {
			this.moduleVersion = moduleVersion;

		}
	}
}
