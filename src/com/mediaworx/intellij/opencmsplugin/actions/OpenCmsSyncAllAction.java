package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.entities.OpenCmsModuleResource;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.FileSyncer;

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
		List<VirtualFile> moduleResources = new ArrayList<VirtualFile>();
		List<OpenCmsModuleResource> moduleResourcesToBePulled = new ArrayList<OpenCmsModuleResource>();
		for (OpenCmsModule ocmsModule : ocmsModules) {

			for (String resourcePath : ocmsModule.getModuleResources()) {
				System.out.println("resource path: " + ocmsModule.getLocalVfsRoot() + resourcePath);
				VirtualFile resourceFile = LocalFileSystem.getInstance().findFileByIoFile(new File(ocmsModule.getLocalVfsRoot() + resourcePath));
				if (resourceFile != null) {
					System.out.println("vFolder path: " + resourceFile.getPath());
					moduleResources.add(resourceFile);
				}
				else {
					System.out.println("Resource doesn't exist in the FS, it has to be pulled from the VFS");
					moduleResourcesToBePulled.add(new OpenCmsModuleResource(ocmsModule, resourcePath));
				}
			}
		}
		if (moduleResourcesToBePulled.size() > 0) {
			fileSyncer.setModuleResourcesToBePulled(moduleResourcesToBePulled);
		}

		// pull resources that don't exist locally
		// TODO: implementieren, nachdenken, ob man nicht einfach grundsätzlich vom vfs-Root ausgehen sollte und die resource paths als Filter verwendet

		// then sync all valid modules
		try {
			fileSyncer.syncFiles(moduleResources.toArray(new VirtualFile[moduleResources.size()]));
		}
		catch (Throwable t) {
			System.out.println("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage());
			t.printStackTrace(System.out);
		}
	}
}
