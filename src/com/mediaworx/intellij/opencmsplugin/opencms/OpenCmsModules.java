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

import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;

import java.io.File;
import java.util.*;

// RTASK: decouple intellij module and OpenCms module. Save OpenCms modules by their base path (DONE), replace "findModuleForFile" with custom implementation
/**
 * Module repository for the currently open project. All OpenCms modules are registered here.
 */
public class OpenCmsModules {

	private static final Logger LOG = Logger.getInstance(OpenCmsModules.class);

	OpenCmsPlugin plugin;
	List<OpenCmsModuleExportPoint> allExportPoints;

	private Map<String, OpenCmsModule> openCmsModuleMap = new LinkedHashMap<String, OpenCmsModule>();

	/**
	 * Creates a new OpenCmsModules repository
	 * @param plugin  the current plugin instance
	 */
	public OpenCmsModules(OpenCmsPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * registers the module with the given base path
	 * @param moduleBasePath the module's base path
	 * @param moduleConfig   the configuration data from the module's configuration dialog
	 */
	public void registerModule(String moduleBasePath, OpenCmsModuleConfigurationData moduleConfig) {
		LOG.info("registering module: " + moduleBasePath);
		moduleBasePath = PluginTools.ensureUnixPath(moduleBasePath);
		allExportPoints = null;
		if (!moduleConfig.isOpenCmsModuleEnabled()) {
			return;
		}
		OpenCmsModule ocmsModule;
		if (openCmsModuleMap.containsKey(moduleBasePath)) {
			ocmsModule = openCmsModuleMap.get(moduleBasePath);
			ocmsModule.refresh(moduleConfig);
		}
		else {
			ocmsModule = new OpenCmsModule(plugin, moduleBasePath);
			ocmsModule.init(moduleConfig);
			openCmsModuleMap.put(moduleBasePath, ocmsModule);
		}
	}

	/**
	 * removes the OpenCms module linked to the IntelliJ module with the given base path from the repository
	 * @param moduleBasePath the IntelliJ module's basePath
	 */
	public void unregisterModule(String moduleBasePath) {
		LOG.info("unregistering module: " + moduleBasePath);
		allExportPoints = null;
		openCmsModuleMap.remove(moduleBasePath);
	}

	/**
	 * @return a Collection of all modules in the repository
	 */
	public Collection<OpenCmsModule> getAllModules() {
		return openCmsModuleMap.values();
	}

	public OpenCmsModule getModuleForFile(File file) {
		if (file == null) {
			return null;
		}
		for (String basePath : openCmsModuleMap.keySet()) {
			String filePath = PluginTools.ensureUnixPath(file.getPath());
			if (filePath.startsWith(basePath)) {
				return openCmsModuleMap.get(basePath);
			}
		}
		return null;
	}

	/**
	 * @param path any path inside the module (local root path)
	 * @return the module containing the path
	 */
	public OpenCmsModule getModuleForPath(String path) {
		if (path == null) {
			return null;
		}
		for (String basePath : openCmsModuleMap.keySet()) {
			if (path.startsWith(basePath)) {
				return openCmsModuleMap.get(basePath);
			}
		}
		return null;
	}

	/**
	 * @param moduleBasePath the modules base path
	 * @return the module linked to the base path
	 */
	public OpenCmsModule getModuleForBasePath(String moduleBasePath) {
		if (moduleBasePath == null) {
			return null;
		}
		return openCmsModuleMap.get(moduleBasePath);
	}

	/**
	 * @param file  the file to check
	 * @return <code>true</code> if the file is contained in an OpenCms module, <code>false</code> otherwise
	 */
	public boolean isFileOpenCmsModuleResource(final File file) {
		OpenCmsModule ocmsModule = getModuleForFile(file);
		if (ocmsModule == null) {
			LOG.info("No module configured for the file " + PluginTools.ensureUnixPath(file.getPath()));
			return false;
		}
		return ocmsModule.isFileModuleResource(file);
	}

	/**
	 * @param resourcePath path pf the resource to check
	 * @return the export point for the given resource path, <code>null</code> if no export point is configured for
	 *         the resource
	 */
	public OpenCmsModuleExportPoint getExportPointForVfsResource(String resourcePath) {
		for (OpenCmsModuleExportPoint exportPoint : getAllExportPoints()) {
			if (resourcePath.startsWith(exportPoint.getVfsSource())) {
				return exportPoint;
			}
		}
		return null;
	}

	/**
	 * @return List of all export points defined by all the modules in the repository
	 */
	public List<OpenCmsModuleExportPoint> getAllExportPoints() {
		if (allExportPoints == null) {
			allExportPoints = new ArrayList<OpenCmsModuleExportPoint>();
			for (OpenCmsModule ocmsModule : getAllModules()) {
				List<OpenCmsModuleExportPoint> exportPoints = ocmsModule.getExportPoints();
				if (exportPoints != null) {
					for (OpenCmsModuleExportPoint exportPoint : exportPoints) {
						allExportPoints.add(exportPoint);
					}
				}
			}
		}
		return allExportPoints;
	}

	/**
	 * refreshes all modules (e.g. after configuration changes)
	 */
	public void refreshAllModules() {
		for (OpenCmsModule ocmsModule : getAllModules()) {
			ocmsModule.refresh();
		}
		allExportPoints = null;
	}
}
