package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.entities.OpenCmsModuleResource;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.FileSyncer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OpenCmsSyncAllAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAllAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);

		Project project = DataKeys.PROJECT.getData(event.getDataContext());

		if (project == null) {
			return;
		}
		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);

		LOG.info("Sync all modules");

		FileSyncer fileSyncer = new FileSyncer(plugin);

		Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();

		// First put all valid module paths in a List
		List<VirtualFile> moduleResources = new ArrayList<VirtualFile>();
		List<OpenCmsModuleResource> moduleResourcesToBePulled = new ArrayList<OpenCmsModuleResource>();
		List<String> moduleManifestsToBePulled = new ArrayList<String>(ocmsModules.size());
		for (OpenCmsModule ocmsModule : ocmsModules) {

			for (String resourcePath : ocmsModule.getModuleResources()) {
				LOG.info("resource path: " + ocmsModule.getLocalVfsRoot() + resourcePath);
				VirtualFile resourceFile = LocalFileSystem.getInstance().findFileByIoFile(new File(ocmsModule.getLocalVfsRoot() + resourcePath));
				if (resourceFile != null) {
					LOG.info("vFolder path: " + resourceFile.getPath());
					moduleResources.add(resourceFile);
				}
				else {
					LOG.info("Resource doesn't exist in the FS, it has to be pulled from the VFS");
					moduleResourcesToBePulled.add(new OpenCmsModuleResource(ocmsModule, resourcePath));
				}
				moduleManifestsToBePulled.add(ocmsModule.getModuleName());
			}
		}

		// pull resources that don't exist locally
		if (moduleResourcesToBePulled.size() > 0) {
			fileSyncer.setModuleResourcesToBePulled(moduleResourcesToBePulled);
		}

		// TODO: nachdenken, ob man nicht einfach grundsätzlich vom vfs-Root ausgehen sollte und die resource paths als Filter verwendet

		// then sync all valid modules
		try {
			fileSyncer.syncFiles(moduleResources.toArray(new VirtualFile[moduleResources.size()]));
		}
		catch (Throwable t) {
			LOG.error("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage(), t);
		}

		// pull the module manifests
		if (moduleManifestsToBePulled.size() > 0) {
			try {
				Map<String,String> manifestInfos = plugin.getPluginConnector().getModuleManifests(moduleManifestsToBePulled);

				for (OpenCmsModule ocmsModule : ocmsModules) {
					if (manifestInfos.containsKey(ocmsModule.getModuleName())) {
						String manifestPath = ocmsModule.getManifestRoot() + "/manifest_stub.xml";
						FileUtils.writeStringToFile(new File(manifestPath), manifestInfos.get(ocmsModule.getModuleName()), Charset.forName("UTF-8"));
					}
					else {
						LOG.warn("No manifest found for module " + ocmsModule.getModuleName());
					}
				}
			}
			catch (IOException e) {
				Messages.showDialog("There was an error pulling the module manifest files from OpenCms.\nIs the connector module installed?",
						"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());

				LOG.error("There was an Exception pulling the module manifests", e);
			}
		}
	}
}
