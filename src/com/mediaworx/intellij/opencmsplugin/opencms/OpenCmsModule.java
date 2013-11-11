package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.module.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import java.util.List;

public class OpenCmsModule {

	OpenCmsPlugin plugin;
	Module ideaModule;
	OpenCmsModuleConfigurationData moduleConfig;

	List<ModuleExportPoint> exportPoints;
	String localVfsRoot;
	SyncMode syncMode;

	public OpenCmsModule(OpenCmsPlugin plugin, Module ideaModule) {
		this.plugin = plugin;
		this.ideaModule = ideaModule;
	}

	public void init(OpenCmsModuleConfigurationData moduleConfig) {
		this.moduleConfig = moduleConfig;
		exportPoints = plugin.getOpenCmsConfiguration().getExportPointsForModule(moduleConfig.getModuleName());

		String relativeVfsRoot;

		if (moduleConfig.isUseProjectDefaultVfsRootEnabled()) {
			relativeVfsRoot = plugin.getPluginConfiguration().getDefaultLocalVfsRoot();
		}
		else {
			relativeVfsRoot = moduleConfig.getLocalVfsRoot();
		}

		VirtualFile[] moduleRoots = ModuleRootManager.getInstance(ideaModule).getContentRoots();
		if (moduleRoots.length == 0) {
			localVfsRoot = null;
		}
		StringBuilder vfsRootBuilder = new StringBuilder();
		vfsRootBuilder.append(moduleRoots[0].getPath()).append("/").append(relativeVfsRoot);
		localVfsRoot = vfsRootBuilder.toString();

		if (moduleConfig.isUseProjectDefaultSyncModeEnabled()) {
			syncMode = plugin.getPluginConfiguration().getDefaultSyncMode();
		}
		else {
			syncMode = moduleConfig.getSyncMode();
		}
	}

	public void refresh(OpenCmsModuleConfigurationData moduleConfig) {
		// for now a refresh just does the same as init
		init(moduleConfig);
	}

	public String getModuleName() {
		return moduleConfig.getModuleName();
	}

	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public SyncMode getSyncMode() {
		return syncMode;
	}

	public List<ModuleExportPoint> getExportPoints() {
		return exportPoints;
	}

	public String getVfsPathForIdeaVFile(final VirtualFile ideaVFile) {
		String filepath = ideaVFile.getPath().replace('\\', '/');
		OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);
		String syncRoot = ocmsModule.getLocalVfsRoot();
		String relativeName = filepath.substring(filepath.indexOf(syncRoot) + syncRoot.length(), filepath.length());
		if (relativeName.length() == 0) {
			relativeName = "/";
		}
		return relativeName;
	}
}
