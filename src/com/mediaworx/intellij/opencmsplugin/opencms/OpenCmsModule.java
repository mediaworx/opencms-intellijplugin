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

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import com.mediaworx.intellij.opencmsplugin.tools.VfsFileAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class OpenCmsModule implements OpenCmsConfiguration.ConfigurationChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(OpenCmsModule.class);

	private OpenCmsPlugin plugin;
	private String moduleBasePath;
	private OpenCmsConfiguration openCmsConfig;
	private OpenCmsPluginConfigurationData pluginConfig;
	private OpenCmsModuleConfigurationData moduleConfig;
	private String moduleName;

	private List<OpenCmsModuleExportPoint> exportPoints;
	private List<String> moduleResources;
	private String localVfsRoot;

	public OpenCmsModule(OpenCmsPlugin plugin, String moduleBasePath) {
		this.plugin = plugin;
		this.moduleBasePath = moduleBasePath;

		pluginConfig = plugin.getPluginConfiguration();
		openCmsConfig = plugin.getOpenCmsConfiguration();
		openCmsConfig.registerConfigurationChangeListener(this);
	}

	public void init(OpenCmsModuleConfigurationData moduleConfig) {
		this.moduleConfig = moduleConfig;

		if (moduleConfig.isUseProjectDefaultModuleNameEnabled()) {
			String moduleNamingScheme = pluginConfig.getModuleNamingScheme();
			String moduleFolderName = StringUtils.substringAfterLast(this.moduleBasePath, "/");
			if (moduleNamingScheme != null && moduleNamingScheme.length() > 0) {
				moduleName = moduleNamingScheme.replaceAll(Pattern.quote("${modulename}"), moduleFolderName);
			}
			else {
				moduleName = moduleFolderName;
			}
		}
		else {
			moduleName = moduleConfig.getModuleName();
		}

		exportPoints = openCmsConfig.getExportPointsForModule(moduleName);
		moduleResources = openCmsConfig.getModuleResourcesForModule(moduleName);

		String relativeVfsRoot;

		if (moduleConfig.isUseProjectDefaultVfsRootEnabled()) {
			relativeVfsRoot = pluginConfig.getDefaultLocalVfsRoot();
		}
		else {
			relativeVfsRoot = moduleConfig.getLocalVfsRoot();
		}
		localVfsRoot = this.moduleBasePath + "/" + relativeVfsRoot;
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
		return moduleName;
	}

	public String getModuleBasePath() {
		return moduleBasePath;
	}

	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public String getManifestRoot() {
		return moduleBasePath + "/" + plugin.getPluginConfiguration().getManifestRoot();
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

	public boolean isFileModuleResource(File file) {
		return isPathModuleResource(file.getPath());
	}

	public boolean isPathModuleResource(String resourcePath) {
		resourcePath = PluginTools.ensureUnixPath(resourcePath);
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

	public boolean isFileModuleRoot(File file) {
		if (!file.isDirectory()) {
			return false;
		}
		String filePath = PluginTools.ensureUnixPath(file.getPath());
		return filePath.equals(moduleBasePath);
	}

	public boolean isPathModuleRoot(String path) {
		if (StringUtils.isBlank(path)) {
			return false;
		}
		// RTASK: check for trailing slashes on either path. How is moduleBasePath stored? How should it be stored?
		return path.equals(moduleBasePath);
	}

	public boolean isFileInVFSPath(File file) {
		if (VfsFileAnalyzer.fileOrPathIsIgnored(plugin.getPluginConfiguration(), file)) {
			return false;
		}
		String filePath = PluginTools.ensureUnixPath(file.getPath());
		return filePath.startsWith(localVfsRoot);
	}

	public boolean isPathInVFSPath(String path) {
		String filename = StringUtils.substringAfterLast(path, "/");
		if (VfsFileAnalyzer.fileOrPathIsIgnored(plugin.getPluginConfiguration(), path, filename)) {
			return false;
		}
		return path.startsWith(localVfsRoot);
	}


	public String getVfsPathForFile(final File file) {
		String filepath = PluginTools.ensureUnixPath(file.getPath());
		String relativeName = filepath.substring(localVfsRoot.length());
		if (relativeName.length() == 0) {
			relativeName = "/";
		}
		return relativeName;
	}

	public String getVfsPathForRealPath(final String path) {
		String relativeName = path.substring(localVfsRoot.length());
		if (relativeName.length() == 0) {
			relativeName = "/";
		}
		return relativeName;
	}

	public String findNewestModuleZipPath() {
		String zipParentPath = getModuleBasePath() + "/" + plugin.getPluginConfiguration().getModuleZipTargetFolderPath();
		Collection<File> moduleZips = FileUtils.listFiles(new File(zipParentPath), new String[]{"zip"}, false);
		File newestModuleZip = null;
		for (File moduleZip : moduleZips) {
			if (newestModuleZip == null || FileUtils.isFileNewer(moduleZip, newestModuleZip)) {
				newestModuleZip = moduleZip;
			}
		}
		if (newestModuleZip != null) {
			return newestModuleZip.getPath();
		}
		else {
			return null;
		}
	}


	@Override
	public void handleOpenCmsConfigurationChange(OpenCmsConfiguration.ConfigurationChangeType changeType) {
		if (changeType == OpenCmsConfiguration.ConfigurationChangeType.MODULECONFIGURATION) {
			exportPoints = openCmsConfig.getExportPointsForModule(moduleName);
			moduleResources = openCmsConfig.getModuleResourcesForModule(moduleName);
		}
	}

}
