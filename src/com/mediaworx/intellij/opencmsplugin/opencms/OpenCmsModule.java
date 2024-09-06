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

package com.mediaworx.intellij.opencmsplugin.opencms;

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import com.mediaworx.intellij.opencmsplugin.tools.VfsFileAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents an OpenCms module with all its configured properties like resource paths and export points.
 */
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

	/**
	 * Creates a new OpenCms module
	 * @param plugin            the current plugin instance
	 * @param moduleBasePath    absolute base path of the module on the local file system
	 */
	public OpenCmsModule(OpenCmsPlugin plugin, String moduleBasePath) {
		this.plugin = plugin;
		this.moduleBasePath = moduleBasePath;

		pluginConfig = plugin.getPluginConfiguration();
		openCmsConfig = plugin.getOpenCmsConfiguration();
		openCmsConfig.registerConfigurationChangeListener(this);
	}

	/**
	 * Initializes the module with the given configuration data (from the module's configuration dialog)
	 * @param moduleConfig the module's configuration data
	 */
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

	/**
	 * refreshes the module after module configuration changes
	 * @param moduleConfig  the updated configuration data
	 */
	public void refresh(OpenCmsModuleConfigurationData moduleConfig) {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	/**
	 * refreshes the module with unchanged configuration data
	 */
	public void refresh() {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	/**
	 * @return the module's package name name, e.g. com.mycompany.opencms.contenttypes
	 */
	public String getModuleName() {
		return moduleName;
	}

	/**
	 * @return  absolute base path of the module on the local file system
	 */
	public String getModuleBasePath() {
		return moduleBasePath;
	}

	/**
	 * @return absolute local path to the module's vfs folder
	 */
	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public String getExportImportSiteRoot() {
		String exportImportSiteRoot = moduleConfig.getExportImportSiteRoot();
		if (StringUtils.isBlank(exportImportSiteRoot)) {
			exportImportSiteRoot = "/";
		}
		return exportImportSiteRoot;
	}

	/**
	 * @return absolute local path to the folder containing the module's meta data
	 */
	public String getManifestRoot() {
		return moduleBasePath + "/" + plugin.getPluginConfiguration().getManifestRoot();
	}

	/**
	 * @return the module's sync mode as configured in either the global plugin configuration or the module's
	 *         configuration
	 */
	public SyncMode getSyncMode() {
		if (moduleConfig.isUseProjectDefaultSyncModeEnabled()) {
			return pluginConfig.getDefaultSyncMode();
		}
		else {
			return moduleConfig.getSyncMode();
		}
	}

	/**
	 * @return <code>true</code> if the module configuration defines a specific module version to use,
	 *         <code>false</code> otherwise
	 */
	public boolean isSetSpecificModuleVersionEnabled() {
		return moduleConfig.isSetSpecificModuleVersionEnabled();
	}

	/**
	 * @return the module version configured for this module (if any)
	 */
	public String getModuleVersion() {
		return moduleConfig.getModuleVersion();
	}

	/**
	 * @return a list of export points configured for this module
	 */
	public List<OpenCmsModuleExportPoint> getExportPoints() {
		return exportPoints;
	}

	/**
	 * @return list of the module resource paths configured for this module (relative to VFS root)
	 */
	public List<String> getModuleResources() {
		return moduleResources;
	}

	/**
	 * Checks if the given file is placed inside a module resource path
	 * @param file  the file to check
	 * @return  <code>true</code> if the file is contained in one of the module's resource paths,
	 *          <code>false</code> otherwise
	 */
	public boolean isFileModuleResource(File file) {
		return isPathModuleResource(file.getPath());
	}

	/**
	 * Checks if the given path is inside a module resource path
	 *
	 * @param resourcePath the path to check
	 * @return <code>true</code> if the path is contained in one of the module's resource paths,
	 * <code>false</code> otherwise
	 */
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

	/**
	 * checks if the given file represents the module's root folder
	 * @param file  the file to check
	 * @return <code>true</code> if the file represents the module's root folder, <code>false</code> otherwise
	 */
	public boolean isFileModuleRoot(File file) {
		if (!file.isDirectory()) {
			return false;
		}
		String filePath = PluginTools.ensureUnixPath(file.getPath());
		return filePath.equals(moduleBasePath);
	}

	/**
	 * checks if the given path is the module's root folder
	 *
	 * @param path the path to check
	 * @return <code>true</code> if the path is the module's root folder, <code>false</code> otherwise
	 */
	public boolean isPathModuleRoot(String path) {
		if (StringUtils.isBlank(path)) {
			return false;
		}
		// RTASK: check for trailing slashes on either path. How is moduleBasePath stored? How should it be stored?
		return path.equals(moduleBasePath);
	}

	/**
	 * checks if the given file is placed under the local VFS root
	 * @param file  the file to check
	 * @return <code>true</code> if the file is contained in the local VFS root, <code>false</code> otherwise
	 */
	public boolean isFileInVFSPath(File file) {
		if (VfsFileAnalyzer.fileOrPathIsIgnored(plugin.getPluginConfiguration(), file)) {
			return false;
		}
		String filePath = PluginTools.ensureUnixPath(file.getPath());
		return filePath.startsWith(localVfsRoot);
	}

	/**
	 * checks if the given path is under the local VFS root
	 *
	 * @param path the path to check
	 * @return <code>true</code> if the path is contained in the local VFS root, <code>false</code> otherwise
	 */
	public boolean isPathInVFSPath(String path) {
		String filename = StringUtils.substringAfterLast(path, "/");
		if (VfsFileAnalyzer.fileOrPathIsIgnored(plugin.getPluginConfiguration(), path, filename)) {
			return false;
		}
		return path.startsWith(localVfsRoot);
	}


	/**
	 * @param file the file for which the VFS is to be retrieved
	 * @return the VFS relative path for the given file
	 */
	public String getVfsPathForFile(final File file) {
		String filepath = PluginTools.ensureUnixPath(file.getPath());
		return getVfsPathForRealPath(filepath);
	}

	/**
	 * @param path a local root path
	 * @return the VFS relative path for the given local path
	 */
	public String getVfsPathForRealPath(final String path) {
		String relativeName;
		try {
			relativeName = path.substring(localVfsRoot.length());
		}
		catch (StringIndexOutOfBoundsException e) {
			LOG.warn("There was a problem loading the VFS path for the path " + path + " - localVfsRoot: " + localVfsRoot);
			relativeName = "/";
		}
		if (relativeName.length() == 0) {
			relativeName = "/";
		}
		return PluginTools.addVfsSiteRootToLocalPath(this, relativeName);
	}

	/**
	 * @return the path of the newest module zip in the target folder, null if no module zip exists
	 */
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


	/**
	 * handles changes to the module configuration, refreshes export points and module resources
	 * @param changeType    the type of the changed OpenCms configuration (right now only MODULECONFIGURATION)
	 */
	@Override
	public void handleOpenCmsConfigurationChange(OpenCmsConfiguration.ConfigurationChangeType changeType) {
		if (changeType == OpenCmsConfiguration.ConfigurationChangeType.MODULECONFIGURATION) {
			exportPoints = openCmsConfig.getExportPointsForModule(moduleName);
			moduleResources = openCmsConfig.getModuleResourcesForModule(moduleName);
		}
	}

}
