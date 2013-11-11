package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.FileSyncer;
import com.mediaworx.intellij.opencmsplugin.tools.PathTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpenCmsSyncAllAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent event) {
		System.out.println("Event: " + event);

		Project project = DataKeys.PROJECT.getData(event.getDataContext());

		if (project == null) {
			return;
		}
		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);

		System.out.println("Sync all modules");

		FileSyncer fileSyncer = new FileSyncer(plugin);

		Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();

		// First put all valid module paths in a List
		List<VirtualFile> moduleFolders = new ArrayList<VirtualFile>();
		for (OpenCmsModule ocmsModule : ocmsModules) {
			String modulePath = PathTools.getLocalModulesParentPath(ocmsModule);
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

		// then sync all valid modules
		try {
			fileSyncer.syncFiles(moduleFolders.toArray(new VirtualFile[moduleFolders.size()]));
		}
		catch (Exception e) {
			System.out.println("Exception in OpenCmsSyncAllAction.actionPerformed: " + e.getMessage());
		}
	}
}
