package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.ExportEntity;
import com.mediaworx.intellij.opencmsplugin.entities.OpenCmsModuleResource;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncFolder;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPushException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class SyncJob implements Runnable {

	private static final Logger LOG = Logger.getInstance(SyncJob.class);
	private static final String ERROR_PREFIX = "ERROR: ";

	private OpenCmsPlugin plugin;
	private OpenCmsToolWindowConsole console;
    private OpenCmsPluginConfigurationData config;
	private VfsAdapter adapter;

	private SyncList syncList;
	private boolean pullMetadataOnly;
	private List<SyncEntity> refreshEntityList;
	private List<ExportEntity> exportList;

	public SyncJob(OpenCmsPlugin plugin, SyncList syncList) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();
		adapter = plugin.getVfsAdapter();
		this.refreshEntityList = new ArrayList<SyncEntity>();
		this.exportList = new ArrayList<ExportEntity>();
		setSyncList(syncList);
		this.pullMetadataOnly = syncList.isPullMetaDataOnly();
	}

	public void run() {
		console = plugin.getConsole();

		int step = 1;
		int numSteps = 0;
		if (!pullMetadataOnly) {
			numSteps += 1;
		}
		if (config.isPluginConnectorEnabled() && config.isPullMetadataEnabled()) {
			numSteps += 1;
		}
		if (syncList.isSyncModuleMetaData()) {
			numSteps += 2;
		}
		if (!pullMetadataOnly && numExportEntities() > 0) {
			numSteps += 1;
		}

		// ######## SYNC FILES / FOLDERS ################################
		if (!pullMetadataOnly) {
			console.info("Step " + (step++) + "/" + numSteps + ": Syncing files and folders");
			for (SyncEntity entity : syncList) {
				doSync(entity);
			}
			console.info("---- Sync finished ----\n");
		}

		if (syncList.isSyncModuleMetaData()) {
			// ######## PULL MODULE MANIFESTS ################################
			console.info("Step " + (step++) + "/" + numSteps + ": Pull module manifests");
			pullModuleManifests();
			console.info("---- Module manifest pull finished ----\n");

			// ######## PULL META INFOS FOR MODULE RESOURCE PARENTS ################################
			console.info("Step " + (step++) + "/" + numSteps + ": Pulling resource meta data for module resource path ancestors from OpenCms");
			pullModuleResourcePathAncestorMetaInfos();
			console.info("---- Pull of meta data for module resource path ancestors finished ----\n");
		}

		// ######## PULL RESOURCE VFS META INFORMATION ################################
		if (config.isPluginConnectorEnabled() && config.isPullMetadataEnabled()) {
			console.info("Step " + (step++) + "/" + numSteps + ": Pulling resource meta data from OpenCms");
			pullResourceMetaInfos();
			console.info("---- Resource meta info pull finished ----\n");
		}

		// ######## EXPORT POINT HANDLING ################################
              if (!pullMetadataOnly && numExportEntities() > 0) {
               console.info("Step " + step + "/" + numSteps + ": Handling export points");

                  for (ExportEntity entity : exportList) {
                      doExportPointHandling(entity);
                  }
                  console.info("---- Copying of ExportPoints finished ----\n");
              }

		// ######## REFRESH IDEA FILESYSTEM ##############################
		if (hasRefreshEntities()) {
			List<SyncEntity> pullEntityList = getRefreshEntityList();
			List<File> refreshFiles = new ArrayList<File>(pullEntityList.size());

			for (SyncEntity entity : pullEntityList) {
				refreshFiles.add(entity.getRealFile());
			}

			try {
				LocalFileSystem.getInstance().refreshIoFiles(refreshFiles);
			}
			catch (Exception e) {
				// if there's an exception then the file was not found.
			}
		}
		console.info("#### SYNC FINISHED ####");
	}

	public void setSyncList(SyncList syncList) {
		this.syncList = syncList;

		if (!syncList.isPullMetaDataOnly()) {
			for (SyncEntity entity : syncList) {
				if (entity.getSyncAction() == SyncAction.PULL || entity.getSyncAction() == SyncAction.DELETE_RFS) {
					this.refreshEntityList.add(entity);
				}
				if (entity.getSyncAction() != SyncAction.DELETE_VFS) {
		            addSyncEntityToExportListIfNecessary(entity);
				}
			}
		}
	}

	public List<SyncEntity> getSyncList() {
		return syncList;
	}

    private void addSyncEntityToExportListIfNecessary(SyncEntity syncEntity) {

	    List<ModuleExportPoint> exportPoints = syncEntity.getOcmsModule().getExportPoints();

        if (exportPoints != null) {

	        String localModuleVfsRoot = syncEntity.getOcmsModule().getLocalVfsRoot();
	        String entityVfsPath = syncEntity.getVfsPath();

		    for (ModuleExportPoint exportPoint : exportPoints) {
			    String vfsSource = exportPoint.getVfsSource();
	            if (entityVfsPath.startsWith(vfsSource)) {
	                String destination = exportPoint.getRfsTarget();
	                String relativePath = entityVfsPath.substring(vfsSource.length());
	                ExportEntity exportEntity = new ExportEntity();
	                exportEntity.setSourcePath(localModuleVfsRoot+entityVfsPath);
	                exportEntity.setTargetPath(config.getWebappRoot() + "/" + destination + relativePath);
	                exportEntity.setVfsPath(entityVfsPath);
	                exportEntity.setDestination(destination);
		            exportEntity.setToBeDeleted(syncEntity.getSyncAction() == SyncAction.DELETE_RFS);
	                addExportEntity(exportEntity);
	            }
	        }
        }
    }

	private void addExportEntity(ExportEntity entity) {
		exportList.add(entity);
	}

	public int numSyncEntities() {
		return syncList.size();
	}

	public boolean hasSyncEntities() {
		return numSyncEntities() > 0;
	}

	public List<SyncEntity> getRefreshEntityList() {
		return refreshEntityList;
	}

	public int getNumRefreshEntities() {
		return refreshEntityList.size();
	}

	public boolean hasRefreshEntities() {
		return getNumRefreshEntities() > 0;
	}

	public int numExportEntities() {
		return exportList.size();
	}


	private void doSync(SyncEntity entity) {
		if (entity.getSyncAction() == SyncAction.PUSH) {
			doPush(entity);
		}
		else if (entity.getSyncAction() == SyncAction.PULL) {
			doPull(entity);
		}
		else if (entity.getSyncAction() == SyncAction.DELETE_RFS) {
			doDeleteFromRfs(entity);
		}
		else if (entity.getSyncAction() == SyncAction.DELETE_VFS) {
			doDeleteFromVfs(entity);
		}
	}

	private void doPush(SyncEntity entity) {

		boolean success = false;
		String errormessage = null;

		if (entity.isFolder()) {
			try {
				adapter.createFolder(entity.getVfsPath());
				success = true;
			}
			catch (Exception e) {
				errormessage = "Error pushing Folder "+entity.getVfsPath()+"\n"+e.getMessage();
			}
		}
		else if (entity.isFile()) {
			try {
				adapter.pushFile(entity);
				success = true;
			}
			catch (CmsPushException e) {
				errormessage = e.getMessage();
			}
		}

		if (success) {
			StringBuilder confirmation = new StringBuilder();
			confirmation.append("PUSH: ").append(entity.getVfsPath()).append(" pushed to VFS");
			if (entity.replaceExistingEntity()) {
				confirmation.append(" replacing an existing entity");
			}
			console.info(confirmation.toString());
		}
		else {
			console.error("PUSH FAILED! " + errormessage);
		}
	}

	private void doPull(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder();

        if (entity.isFolder()) {
            try {
	            FileUtils.forceMkdir(new File(entity.getRfsPath()));
            } catch (IOException e) {
	            console.error("ERROR: couldn't create local directory " + entity.getRfsPath());
	            LOG.warn("There was an Exception creating a local directory", e);
           }
        }
        else {
            adapter.pullFile(entity);
        }

		confirmation.append("PULL: ").append(entity.getVfsPath()).append(" pulled from VFS to ").append(entity.getOcmsModule().getLocalVfsRoot());
		if (entity.replaceExistingEntity()) {
			confirmation.append(" replacing an existing entity");
		}

		console.info(confirmation.toString());
	}

	private void doDeleteFromRfs(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder("DELETE ").append(entity.getVfsPath()).append(" from ").append(entity.getOcmsModule().getLocalVfsRoot()).append(" (not in the VFS) - ");
		File rfsFile = entity.getRealFile();
		if (FileUtils.deleteQuietly(rfsFile)) {
			confirmation.append(" SUCCESS");
			console.info(confirmation.toString());
		}
		else {
			confirmation.insert(0, "ERROR: ");
			confirmation.append(" FAILED!");
			console.error(confirmation.toString());
		}
	}

	private void doDeleteFromVfs(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder("DELETE ").append(entity.getVfsPath()).append(" (not in the RFS) - ");
		if (adapter.deleteResource(entity.getVfsPath())) {
			confirmation.append(" SUCCESS");
			console.info(confirmation.toString());
		}
		else {
			confirmation.insert(0, "ERROR: ");
			confirmation.append(" FAILED!");
			console.error(confirmation.toString());
		}
	}

	private void pullResourceMetaInfos() {
		// build lists with all resources for which meta information is to be pulled / deleted
		ArrayList<String> pullEntityList = new ArrayList<String>();
		for (SyncEntity entity : syncList) {
			if (!entity.getSyncAction().isDeleteAction()) {
				pullEntityList.add(entity.getVfsPath());
			}
		}

		HashMap<String, String> metaInfos;

		try {
			metaInfos = plugin.getPluginConnector().getResourceInfos(pullEntityList);
		}
		catch (IOException e) {
			console.error("There was an error retrieving resource meta infos from OpenCms");
			LOG.warn("IOException while trying to retrieve meta infos", e);
			return;
		}

		int numMetaEntities = syncList.size();

		if (numMetaEntities > 0) {
			for (SyncEntity entity : syncList) {
				doMetaInfoHandling(console, metaInfos, entity);
			}
		}
	}

	public void doMetaInfoHandling(OpenCmsToolWindowConsole console, Map<String,String> metaInfos, SyncEntity entity) {
		String metaInfoFilePath = entity.getMetaInfoFilePath();
		File metaInfoFile = new File(metaInfoFilePath);

		if (entity.getSyncAction().isDeleteAction()) {
			FileUtils.deleteQuietly(metaInfoFile);
			console.info("DELETE: " + metaInfoFilePath);
			return;
		}

		if (metaInfos.containsKey(entity.getVfsPath())) {
			if (entity instanceof SyncFolder) {
				String metaFolderPath = ((SyncFolder)entity).getMetaInfoFolderPath();
				File metaFolder = new File(metaFolderPath);
				if (!metaFolder.exists()) {
					try {
						FileUtils.forceMkdir(metaFolder);
					}
					catch (IOException e) {
						String message = "ERROR: cant create meta info directory " + metaFolderPath;
						console.error(message);
						LOG.warn(message, e);
						return;
					}
				}
			}
			try {
				FileUtils.writeStringToFile(metaInfoFile, metaInfos.get(entity.getVfsPath()), Charset.forName("UTF-8"));
			}
			catch (IOException e) {
				String message = "ERROR: cant create meta info file " + metaInfoFilePath;
				console.error(message);
				LOG.warn(message, e);
				return;
			}

		}
		else {
			String message = entity.getVfsPath() + " not found in meta info map.";
			console.error(message);
			return;
		}
		console.info("PULL: Meta info file pulled: " + metaInfoFilePath);
	}

	private void pullModuleResourcePathAncestorMetaInfos() {
		List<OpenCmsModuleResource> resourcePathParents = new ArrayList<OpenCmsModuleResource>();
		for (OpenCmsModule ocmsModule : syncList.getOcmsModules()) {
			Set<String> handledParents = new HashSet<String>();
			for (String resourcePath : ocmsModule.getModuleResources()) {
				addParentFolderToResourcePaths(resourcePath, ocmsModule, resourcePathParents, handledParents);
			}
		}

		if (resourcePathParents.size() > 0) {
			try {
				Map<String,String> resourceInfos = plugin.getPluginConnector().getModuleResourceInfos(resourcePathParents);

				for (OpenCmsModuleResource resourceParent : resourcePathParents) {
					SyncFolder syncFolder = new SyncFolder(resourceParent.getOpenCmsModule(), resourceParent.getResourcePath(), null, null, SyncAction.PULL, false);
					doMetaInfoHandling(console, resourceInfos, syncFolder);
				}
			}
			catch (IOException e) {
				Messages.showDialog("There was an error pulling the meta information for module resource ancestor folders from OpenCms.\nIs the connector module installed?",
						"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				LOG.warn("There was an Exception pulling the meta information for module resource ancestor folders", e);
			}
		}
	}

	private void addParentFolderToResourcePaths(String resourcePath, OpenCmsModule ocmsModule,
	                                            List<OpenCmsModuleResource> resourcePathParents, Set<String> handledParents) {
		if (resourcePath.endsWith("/")) {
			resourcePath = resourcePath.substring(0, resourcePath.length() - 1);
		}
		if (resourcePath.lastIndexOf("/") > 0) {  // > 0 is right because the "/" at position 0 has to be ignored
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
		List<String> moduleNames = new ArrayList<String>(syncList.getOcmsModules().size());
		for (OpenCmsModule ocmsModule : syncList.getOcmsModules()) {
			moduleNames.add(ocmsModule.getModuleName());
		}

		if (moduleNames.size() > 0) {
			try {
				// pull the module manifests
				Map<String,String> manifestInfos = plugin.getPluginConnector().getModuleManifests(moduleNames);

				for (OpenCmsModule ocmsModule : syncList.getOcmsModules()) {
					if (manifestInfos.containsKey(ocmsModule.getModuleName())) {
						// put the manifest to a file
						String manifestPath = ocmsModule.getManifestRoot() + "/manifest_stub.xml";
						FileUtils.writeStringToFile(new File(manifestPath), manifestInfos.get(ocmsModule.getModuleName()), Charset.forName("UTF-8"));
						console.info("PULL: " + manifestPath + " pulled from OpenCms");
					}
					else {
						LOG.warn("No manifest found for module " + ocmsModule.getModuleName());
					}
				}
			}
			catch (IOException e) {
				Messages.showDialog("There was an error pulling the module manifest files from OpenCms.\nIs the connector module installed?",
						"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				LOG.warn("There was an Exception pulling the module manifests", e);
			}
		}
	}

	private void doExportPointHandling(ExportEntity entity) {
        StringBuilder confirmation = new StringBuilder();

		if (!entity.isToBeDeleted()) {
			confirmation.append("Copy of ").append(entity.getVfsPath()).append(" to ").append(entity.getDestination()).append(" - ");
	        File file = new File(entity.getSourcePath());
	        if (file.exists()) {
	            if (file.isFile()) {
	                try {
	                    FileUtils.copyFile(file, new File(entity.getTargetPath()));
	                    confirmation.append("SUCCESS");
	                } catch (IOException e) {
		                confirmation.insert(0, "ERROR: ");
	                    confirmation.append("FAILED (").append(e.getMessage()).append(")");
	                }
	            }
	            else if (file.isDirectory()) {
	                try {
	                    FileUtils.copyDirectory(file, new File(entity.getTargetPath()));
	                    confirmation.append("SUCCESS");
	                } catch (IOException e) {
		                confirmation.insert(0, "ERROR: ");
	                    confirmation.append("FAILED (").append(e.getMessage()).append(")");
	                }
	            }
	        }
	        else {
	            confirmation.append(" - FILE NOT FOUND");
	        }
		}
		else {
			confirmation.append("Resource ").append(entity.getVfsPath()).append(" removed, deletion of exported file ")
					.append(entity.getDestination()).append(" - ");
			File file = new File(entity.getTargetPath());
			if (file.exists()) {
				if (FileUtils.deleteQuietly(file)) {
					confirmation.append("SUCCESS");
				}
				else {
					confirmation.insert(0, "ERROR: ");
					confirmation.append("FAILED");
				}
			}
			else {
				confirmation.append("NOT NECESSARY (doesn't exist)");
			}
		}
		if (confirmation.indexOf(ERROR_PREFIX) > -1) {
			console.error(confirmation.toString());
		}
		else {
			console.info(confirmation.toString());
		}
    }

}
