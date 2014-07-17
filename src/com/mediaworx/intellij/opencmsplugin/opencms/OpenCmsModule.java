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

package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;
import com.mediaworx.intellij.opencmsplugin.tools.ModuleTools;
import com.mediaworx.intellij.opencmsplugin.tools.VfsFileAnalyzer;

import java.util.List;

public class OpenCmsModule implements OpenCmsConfiguration.ConfigurationChangeListener {

	private static final Logger LOG = Logger.getInstance(OpenCmsModule.class);

	private OpenCmsPlugin plugin;
	private Module intelliJModule;
	private OpenCmsConfiguration openCmsConfig;
	private OpenCmsPluginConfigurationData pluginConfig;
	private OpenCmsModuleConfigurationData moduleConfig;
	private String moduleName;

	private List<OpenCmsModuleExportPoint> exportPoints;
	private List<String> moduleResources;
	private String intelliJModuleRoot;
	private String localVfsRoot;

	public OpenCmsModule(OpenCmsPlugin plugin, Module intelliJModule) {
		this.plugin = plugin;
		this.intelliJModule = intelliJModule;

		pluginConfig = plugin.getPluginConfiguration();
		openCmsConfig = plugin.getOpenCmsConfiguration();
		openCmsConfig.registerConfigurationChangeListener(this);
	}

	public void init(OpenCmsModuleConfigurationData moduleConfig) {
		this.moduleConfig = moduleConfig;
		moduleName = moduleConfig.getModuleName();

		exportPoints = openCmsConfig.getExportPointsForModule(moduleName);
		moduleResources = openCmsConfig.getModuleResourcesForModule(moduleName);

		String relativeVfsRoot;

		if (moduleConfig.isUseProjectDefaultVfsRootEnabled()) {
			relativeVfsRoot = pluginConfig.getDefaultLocalVfsRoot();
		}
		else {
			relativeVfsRoot = moduleConfig.getLocalVfsRoot();
		}

		String intelliJModuleRoot = ModuleTools.getModuleContentRoot(intelliJModule);
		if (intelliJModuleRoot == null || intelliJModuleRoot.length() == 0) {
			this.intelliJModuleRoot = null;
			localVfsRoot = null;
		}
		else {
			this.intelliJModuleRoot = intelliJModuleRoot;
			StringBuilder vfsRootBuilder = new StringBuilder();
			vfsRootBuilder.append(this.intelliJModuleRoot).append("/").append(relativeVfsRoot);
			localVfsRoot = vfsRootBuilder.toString();
		}
	}

	public void refresh(OpenCmsModuleConfigurationData moduleConfig) {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	public void refresh() {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	public String getModuleName() {
		return moduleConfig.getModuleName();
	}

	public String getIntelliJModuleRoot() {
		return intelliJModuleRoot;
	}

	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public String getManifestRoot() {
		return intelliJModuleRoot + "/" + plugin.getPluginConfiguration().getManifestRoot();
	}

	public SyncMode getSyncMode() {
		if (moduleConfig.isUseProjectDefaultSyncModeEnabled()) {
			return pluginConfig.getDefaultSyncMode();
		}
		else {
			return moduleConfig.getSyncMode();
		}
	}

	public boolean isSetSpecificModuleVersionEnabled() {
		return moduleConfig.isSetSpecificModuleVersionEnabled();
	}

	public String getModuleVersion() {
		return moduleConfig.getModuleVersion();
	}

	public List<OpenCmsModuleExportPoint> getExportPoints() {
		return exportPoints;
	}

	public List<String> getModuleResources() {
		return moduleResources;
	}

	public boolean isIdeaVFileModuleResource(VirtualFile ideaVFile) {
		return isPathModuleResource(ideaVFile.getPath());
	}

	public boolean isPathModuleResource(String resourcePath) {
		for (String moduleResourcePath : getModuleResources()) {
			String localModuleResourcePath = getLocalVfsRoot() + moduleResourcePath;
			if ((resourcePath + "/").endsWith(localModuleResourcePath)) {
				resourcePath = resourcePath + "/";
			}
			LOG.info("localModuleResourcePath: " + localModuleResourcePath);
			LOG.info("resourcePath:     " + resourcePath);
			if (resourcePath.startsWith(localModuleResourcePath)) {
				return true;
			}
		}
		return false;
	}

	public boolean isIdeaVFileModuleRoot(VirtualFile ideaVFile) {
		if (!ideaVFile.isDirectory()) {
			return false;
		}
		String filePath = ideaVFile.getPath();
		return filePath.equals(intelliJModuleRoot);
	}

	public boolean isIdeaVFileInVFSPath(VirtualFile ideaVFile) {
		if (VfsFileAnalyzer.fileOrPathIsIgnored(plugin.getPluginConfiguration(), ideaVFile)) {
			return false;
		}
		String filePath = ideaVFile.getPath();
		return filePath.startsWith(localVfsRoot);
	}


	public String getVfsPathForIdeaVFile(final VirtualFile ideaVFile) {
		String filepath = ideaVFile.getPath();
		String relativeName = filepath.substring(localVfsRoot.length());
		if (relativeName.length() == 0) {
			relativeName = "/";
		}
		return relativeName;
	}

	@Override
	public void handleOpenCmsConfigurationChange(OpenCmsConfiguration.ConfigurationChangeType changeType) {
		if (changeType == OpenCmsConfiguration.ConfigurationChangeType.MODULECONFIGURATION) {
			exportPoints = openCmsConfig.getExportPointsForModule(moduleName);
			moduleResources = openCmsConfig.getModuleResourcesForModule(moduleName);
		}
	}
}
