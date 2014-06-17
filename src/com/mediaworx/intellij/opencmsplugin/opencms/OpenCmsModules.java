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
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMainMenu;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModuleConfigurationData;

import java.util.*;

public class OpenCmsModules {

	private static final Logger LOG = Logger.getInstance(OpenCmsModules.class);

	OpenCmsPlugin plugin;
	List<OpenCmsModuleExportPoint> allExportPoints;

	private Map<Module, OpenCmsModule> OCMSMODULE_BY_IDEAMODULE = new LinkedHashMap<Module, OpenCmsModule>();

	public OpenCmsModules(OpenCmsPlugin plugin) {
		this.plugin = plugin;
	}

	public void registerModule(Module ideaModule, OpenCmsModuleConfigurationData moduleConfig) {
		LOG.info("registering module: " + moduleConfig.getModuleName());
		allExportPoints = null;
		if (!moduleConfig.isOpenCmsModuleEnabled()) {
			return;
		}
		OpenCmsModule ocmsModule;
		if (OCMSMODULE_BY_IDEAMODULE.containsKey(ideaModule)) {
			ocmsModule = OCMSMODULE_BY_IDEAMODULE.get(ideaModule);
			ocmsModule.refresh(moduleConfig);
		}
		else {
			ocmsModule = new OpenCmsModule(plugin, ideaModule);
			ocmsModule.init(moduleConfig);
			OCMSMODULE_BY_IDEAMODULE.put(ideaModule, ocmsModule);
		}
		// TODO: think about using an event instead
		OpenCmsMainMenu.getInstance(plugin).registerModuleActions();
	}

	public void unregisterModule(Module ideaModule) {
		LOG.info("unregistering module: " + ideaModule.getName());
		allExportPoints = null;
		OCMSMODULE_BY_IDEAMODULE.remove(ideaModule);
		// TODO: think about using an event instead
		OpenCmsMainMenu.getInstance(plugin).registerModuleActions();
	}

	public Collection<OpenCmsModule> getAllModules() {
		return OCMSMODULE_BY_IDEAMODULE.values();
	}

	public OpenCmsModule getModuleForIdeaVFile(VirtualFile ideaVFile) {
		if (ideaVFile == null || plugin.getProject() == null) {
			return null;
		}
		Module ideaModule = ModuleUtil.findModuleForFile(ideaVFile, plugin.getProject());
		return OCMSMODULE_BY_IDEAMODULE.get(ideaModule);
	}

	public OpenCmsModule getModuleForIdeaModule(Module ideaModule) {
		if (ideaModule == null) {
			return null;
		}
		return OCMSMODULE_BY_IDEAMODULE.get(ideaModule);
	}

	public boolean isIdeaVFileOpenCmsModuleResource(final VirtualFile file) {
		OpenCmsModule ocmsModule = getModuleForIdeaVFile(file);
		if (ocmsModule == null) {
			LOG.info("No module configured for the file " + file.getPath());
			return false;
		}
		return ocmsModule.isIdeaVFileModuleResource(file);
	}

	public OpenCmsModuleExportPoint getExportPointForVfsResource(String resourcePath) {
		for (OpenCmsModuleExportPoint exportPoint : getAllExportPoints()) {
			if (resourcePath.startsWith(exportPoint.getVfsSource())) {
				return exportPoint;
			}
		}
		return null;
	}

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
}
