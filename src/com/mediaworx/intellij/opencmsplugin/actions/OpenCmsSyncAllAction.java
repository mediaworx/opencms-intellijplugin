package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPluginComponent;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPluginConfigurationComponent;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.tools.PathTools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OpenCmsSyncAllAction extends AnAction {

	Project project;
	OpenCmsPluginConfigurationData config;
	VfsAdapter vfsAdapter;

	@Override
	public void actionPerformed(AnActionEvent event) {
		this.project = DataKeys.PROJECT.getData(event.getDataContext());
		this.config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();

		System.out.println("Event: " + event);
		System.out.println("Config: " + config);
		System.out.println("Plugin active? " + config.isOpenCmsPluginActive());

		if (config.isOpenCmsPluginActive()) {

			this.vfsAdapter = project.getComponent(OpenCmsPluginComponent.class).getVfsAdapter();

			if (vfsAdapter != null) {

				System.out.println("Sync all modules");

				FileSyncer fileSyncer = new FileSyncer(project, config, vfsAdapter);

				HashMap<String, String> modulePaths = config.getLocalModuleVfsRootMap();
				ArrayList<VirtualFile> moduleFolders = new ArrayList<VirtualFile>(modulePaths.size());

				Iterator it = modulePaths.keySet().iterator();

				// First put all valid module paths in
				for (String moduleName : modulePaths.keySet()) {
					String modulePath = PathTools.getLocalModulesParentPath(moduleName, config) + File.separator + moduleName;
					System.out.println("module path: " + modulePath);
					VirtualFile parentFolder = LocalFileSystem.getInstance().findFileByIoFile(new File(modulePath));
					if (parentFolder != null) {
						System.out.println("vFolder path: " + parentFolder.getPath());
						moduleFolders.add(parentFolder);
					}
					else {
						System.out.println("Configured module doesn't exist in the FS");
					}
				}

				try {
					fileSyncer.syncFiles(moduleFolders.toArray(new VirtualFile[moduleFolders.size()]));
				}
				catch (Exception e) {
					System.out.println("Exception in OpenCmsPlugin.actionPerformed: " + e.getMessage());
				}
			}
		}
	}
}
