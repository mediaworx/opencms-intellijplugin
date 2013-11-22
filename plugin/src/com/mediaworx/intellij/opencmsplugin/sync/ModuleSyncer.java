package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.entities.OpenCmsModuleResource;
import com.mediaworx.intellij.opencmsplugin.entities.SyncFolder;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class ModuleSyncer {

	private static final Logger LOG = Logger.getInstance(ModuleSyncer.class);

	OpenCmsPlugin plugin;
	Collection<OpenCmsModule> ocmsModules;

	public ModuleSyncer(OpenCmsPlugin plugin) {
		this.plugin = plugin;
	}

	public void syncAllModules() {
		ocmsModules = plugin.getOpenCmsModules().getAllModules();
		syncModules();
	}

	private void syncModules() {

		// first sync all the module resources ...
		syncModuleResources();

		// ... then pull meta information for module resource ancestor folders (like /system and /system/modules)
		pullResourcePathAncestorMetaInfos();

		// ... lastly pull the module manifests
		pullModuleManifests();
	}

	private void syncModuleResources() {
		FileSyncer fileSyncer = new FileSyncer(plugin);

		// First put all valid module paths in a List
		List<VirtualFile> moduleResources = new ArrayList<VirtualFile>();
		List<OpenCmsModuleResource> moduleResourcesToBePulled = new ArrayList<OpenCmsModuleResource>();

		for (OpenCmsModule ocmsModule : ocmsModules) {

			for (String resourcePath : ocmsModule.getModuleResources()) {
				LOG.info("resource path: " + ocmsModule.getLocalVfsRoot() + resourcePath);
				VirtualFile resourceFile = LocalFileSystem.getInstance().findFileByIoFile(new File(ocmsModule.getLocalVfsRoot() + resourcePath));
				if (resourceFile != null) {
					LOG.info("vFolder path: " + resourceFile.getPath());
					moduleResources.add(resourceFile);
				}
				else {
					LOG.info("Resource path doesn't exist in the FS, it has to be pulled from the VFS");
					moduleResourcesToBePulled.add(new OpenCmsModuleResource(ocmsModule, resourcePath));
				}
			}
		}

		// pull resources that don't exist locally
		if (moduleResourcesToBePulled.size() > 0) {
			fileSyncer.setModuleResourcesToBePulled(moduleResourcesToBePulled);
		}

		// then sync all valid modules
		try {
			fileSyncer.syncFiles(moduleResources.toArray(new VirtualFile[moduleResources.size()]));
		}
		catch (Throwable t) {
			LOG.error("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	private void pullResourcePathAncestorMetaInfos() {
		List<OpenCmsModuleResource> resourcePathParents = new ArrayList<OpenCmsModuleResource>();
		for (OpenCmsModule ocmsModule : ocmsModules) {
			Set<String> handledParents = new HashSet<String>();
			for (String resourcePath : ocmsModule.getModuleResources()) {
				addParentFolderToResourcePaths(resourcePath, ocmsModule, resourcePathParents, handledParents);
			}
		}

		if (resourcePathParents.size() > 0) {
			try {
				Map<String,String> resourceInfos = plugin.getPluginConnector().getModuleResourceInfos(resourcePathParents);

				for (OpenCmsModuleResource resourceParent : resourcePathParents) {
					SyncFolder syncFolder = new SyncFolder();
					syncFolder.setOcmsModule(resourceParent.getOpenCmsModule());
					syncFolder.setSyncAction(SyncAction.PULL);
					syncFolder.setVfsPath(resourceParent.getResourcePath());

					String syncResult = SyncJob.doMetaInfoHandling(resourceInfos, syncFolder);
					LOG.info(syncResult);
				}
			}
			catch (IOException e) {
				Messages.showDialog("There was an error pulling the meta information for module resource ancestor folders from OpenCms.\nIs the connector module installed?",
						"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				LOG.error("There was an Exception pulling the meta information for module resource ancestor folders", e);
			}
		}
	}

	private void addParentFolderToResourcePaths(String resourcePath, OpenCmsModule ocmsModule,
	                                            List<OpenCmsModuleResource> resourcePathParents, Set<String> handledParents) {
		if (resourcePath.contains("/")) {
			String parentPath = resourcePath.substring(0, resourcePath.lastIndexOf("/"));
			if (!handledParents.contains(parentPath)) {
				resourcePathParents.add(new OpenCmsModuleResource(ocmsModule, parentPath));
				handledParents.add(parentPath);
				if (parentPath.contains("/")) {
					addParentFolderToResourcePaths(parentPath, ocmsModule, resourcePathParents, handledParents);
				}
			}
		}
	}

	private void pullModuleManifests() {

		// collect the module names in a List
		List<String> moduleNames = new ArrayList<String>(ocmsModules.size());
		for (OpenCmsModule ocmsModule : ocmsModules) {
			moduleNames.add(ocmsModule.getModuleName());
		}

		if (moduleNames.size() > 0) {
			try {
				// pull the module manifests
				Map<String,String> manifestInfos = plugin.getPluginConnector().getModuleManifests(moduleNames);

				for (OpenCmsModule ocmsModule : ocmsModules) {
					if (manifestInfos.containsKey(ocmsModule.getModuleName())) {
						// put the manifest to a file
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
