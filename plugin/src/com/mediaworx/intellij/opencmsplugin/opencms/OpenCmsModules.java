package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.module.OpenCmsModuleConfigurationData;

import java.util.*;

public class OpenCmsModules {

	private static final Logger LOG = Logger.getInstance(OpenCmsModules.class);

	OpenCmsPlugin plugin;
	List<ModuleExportPoint> allExportPoints;

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

	}

	public void unregisterModule(Module ideaModule) {
		LOG.info("unregistering module: " + ideaModule.getName());
		allExportPoints = null;
		OCMSMODULE_BY_IDEAMODULE.remove(ideaModule);
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

	public boolean isIdeaVFileOpenCmsModuleResource(final VirtualFile file) {
		OpenCmsModule ocmsModule = getModuleForIdeaVFile(file);
		if (ocmsModule == null) {
			LOG.info("No module configured for the file " + file.getPath());
			return false;
		}
		return ocmsModule.isIdeaVFileModuleResource(file);
	}

	public ModuleExportPoint getExportPointForVfsResource(String resourcePath) {
		for (ModuleExportPoint exportPoint : getAllExportPoints()) {
			if (resourcePath.startsWith(exportPoint.getVfsSource())) {
				return exportPoint;
			}
		}
		return null;
	}

	public List<ModuleExportPoint> getAllExportPoints() {
		if (allExportPoints == null) {
			allExportPoints = new ArrayList<ModuleExportPoint>();
			for (OpenCmsModule ocmsModule : getAllModules()) {
				List<ModuleExportPoint> exportPoints = ocmsModule.getExportPoints();
				if (exportPoints != null) {
					for (ModuleExportPoint exportPoint : exportPoints) {
						allExportPoints.add(exportPoint);
					}
				}
			}
		}
		return allExportPoints;
	}
}
