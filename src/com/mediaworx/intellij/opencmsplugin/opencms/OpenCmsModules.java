package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.module.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.tools.PathTools;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class OpenCmsModules {

	OpenCmsPlugin plugin;
	List<ModuleExportPoint> allExportPoints;

	private Map<Module, OpenCmsModule> OCMSMODULE_BY_IDEAMODULE = new LinkedHashMap<Module, OpenCmsModule>();
	private Map<String, OpenCmsModule> OCMSMODULE_BY_NAME = new HashMap<String, OpenCmsModule>();

	public OpenCmsModules(OpenCmsPlugin plugin) {
		this.plugin = plugin;
	}

	public void registerModule(Module ideaModule, OpenCmsModuleConfigurationData moduleConfig) {
		System.out.println("registreing module: " + moduleConfig.getModuleName());
		unregisterModule(ideaModule);
		if (!moduleConfig.isOpenCmsModuleEnabled()) {
			return;
		}
		OpenCmsModule ocmsModule;
		if (OCMSMODULE_BY_IDEAMODULE.containsKey(ideaModule)) {
			ocmsModule = OCMSMODULE_BY_IDEAMODULE.get(ideaModule);
			ocmsModule.refresh(plugin, moduleConfig);
		}
		else {
			ocmsModule = new OpenCmsModule(plugin, ideaModule);
			ocmsModule.init(moduleConfig);
			OCMSMODULE_BY_IDEAMODULE.put(ideaModule, ocmsModule);
			OCMSMODULE_BY_NAME.put(moduleConfig.getModuleName(), ocmsModule);
		}

	}

	public void unregisterModule(Module ideaModule) {
		System.out.println("unregistreing module: " + ideaModule.getName());
		allExportPoints = null;
		OpenCmsModule ocmsModule = OCMSMODULE_BY_IDEAMODULE.get(ideaModule);
		if (ocmsModule == null) {
			return;
		}
		OCMSMODULE_BY_IDEAMODULE.remove(ideaModule);
		// find out the name of the module to be removed
		String moduleName = null;
		for (Map.Entry<String, OpenCmsModule> entry : OCMSMODULE_BY_NAME.entrySet()) {
			if (entry.getValue() == ocmsModule) {
				moduleName = entry.getKey();
				break;
			}
		}
		if (moduleName != null) {
			OCMSMODULE_BY_NAME.remove(moduleName);
		}
	}

	public Collection<OpenCmsModule> getAllModules() {
		return OCMSMODULE_BY_IDEAMODULE.values();
	}

	public OpenCmsModule getModuleForIdeaVFile(VirtualFile ideaVFile) {
		Module ideaModule = ModuleUtil.findModuleForFile(ideaVFile, plugin.getProject());
		return OCMSMODULE_BY_IDEAMODULE.get(ideaModule);
	}

	public OpenCmsModule getOpenCmsModule(String moduleName) {
		return OCMSMODULE_BY_NAME.get(moduleName);
	}


	// TODO: Umbau auf Verwendung der Module Resources!
	public boolean isIdeaVFileOpenCmsModuleResource(final VirtualFile file) {
		OpenCmsModule ocmsModule = getModuleForIdeaVFile(file);
		if (ocmsModule == null) {
			System.out.println("No module configured for the file " + file.getPath());
			return false;
		}
		String modulesPath = (PathTools.getLocalModulesParentPath(ocmsModule) + File.separator).replace('\\', '/');
		System.out.println("moduleName: "+ocmsModule.getModuleName());
		System.out.println("modulesPath: "+modulesPath);
		System.out.println("filePath: "+file.getPath().replace('\\', '/'));
		return file.getPath().replace('\\', '/').matches(Pattern.quote(modulesPath) + ".+");
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
