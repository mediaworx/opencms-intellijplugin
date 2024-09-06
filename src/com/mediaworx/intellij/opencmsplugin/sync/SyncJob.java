/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
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

package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.AutoPublishMode;
import com.mediaworx.intellij.opencmsplugin.entities.ExportEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncFolder;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPushException;
import com.mediaworx.intellij.opencmsplugin.exceptions.OpenCmsConnectorException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModuleResource;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Syncs OpenCms and the local file system, is run as a separate thread
 */
public class SyncJob implements Runnable {

	private static final Logger LOG = Logger.getInstance(SyncJob.class);
	public static final String ERROR_PREFIX = "ERROR: ";

	private static final String CLASSES_PATH = "WEB-INF/classes";
	private static final String IDE_CONNECTOR_PACKAGE = "com.mediaworx.opencms.ideconnector";
	private static final Set<String> IDE_CONNECTOR_PARENT_PATHS = new HashSet<String>();
	static {
		String[] ideConnectorPathFolders = IDE_CONNECTOR_PACKAGE.split("\\.");
		StringBuilder ideConnectorParentPath = new StringBuilder(CLASSES_PATH);
		IDE_CONNECTOR_PARENT_PATHS.add(ideConnectorParentPath.toString());
		for (String ideConnectorPathFolder : ideConnectorPathFolders) {
			ideConnectorParentPath.append("/").append(ideConnectorPathFolder);
			IDE_CONNECTOR_PARENT_PATHS.add(ideConnectorParentPath.toString());
		}
	}

	private OpenCmsPlugin plugin;
	private OpenCmsToolWindowConsole console;
	private OpenCmsPluginConfigurationData config;
	private VfsAdapter adapter;

	private SyncList syncList;
	private boolean pullMetadataOnly;
	private List<SyncEntity> refreshEntityList;
	private List<ExportEntity> exportList;
	private List<String> publishList;
	private boolean publish;

	/**
	 * Creates a new SyncJob for the given SyncList
	 * @param plugin   the current plugin instance
	 * @param syncList list of SyncEntities to be synced by this SyncJob
	 */
	public SyncJob(OpenCmsPlugin plugin, SyncList syncList) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();
		publish = config.isPluginConnectorEnabled() && config.getAutoPublishMode() == AutoPublishMode.ALL;
		adapter = plugin.getVfsAdapter();
		this.refreshEntityList = new ArrayList<SyncEntity>();
		this.exportList = new ArrayList<ExportEntity>();
		this.publishList = new ArrayList<String>();
		setSyncList(syncList);
	}

	/**
	 * Starts the SyncJob (done as a separate thread)
	 */
	public void run() {
		console = plugin.getConsole();

		int step = 1;
		int numSteps = 1;
		if (config.isPluginConnectorEnabled() && config.isPullMetadataEnabled()) {
			numSteps += 1;
		}
		if (syncList.isSyncModuleMetaData()) {
			numSteps += 2;
		}
		if (!pullMetadataOnly && publish) {
			numSteps += 1;
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
		// ######## OR CLEAN UP META DATA FOLDERS ################################
		else {
			console.info("Step " + (step++) + "/" + numSteps + ": Clean meta data folders for affected modules");
			for (OpenCmsModule ocmsModule : syncList.getOcmsModules()) {
				console.info("Cleaning meta data folder for " + ocmsModule.getModuleName());
				cleanupModuleMetaFolder(ocmsModule);
			}
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

		if (!pullMetadataOnly) {
			// ######## PUBLISHING ###########################################
			if (publish) {
				console.info("Step " + step + "/" + numSteps + ": Publishing");

				if (publishList.size() > 0) {
					try {
						plugin.getPluginConnector().publishResources(publishList, false);
						console.info("A direct publish session was started successfully");
					}
					catch (OpenCmsConnectorException e) {
						console.error(e.getMessage());
					}
					catch (IOException e) {
						LOG.warn("There was an exception while publishing resources after sync", e);
						console.error("There was an exception while publishing resources after syncing. Is OpenCms running? Please have a look at the OpenCms log file and/or the IntelliJ log file.");
					}
				}
				else {
					console.info("No resources need publishing");
				}

				console.info("---- Publish finished ----\n");
			}

			// ######## EXPORT POINT HANDLING ################################
			if (numExportEntities() > 0) {
				console.info("Step " + step + "/" + numSteps + ": Handling export points");

				for (ExportEntity entity : exportList) {
					doExportPointHandling(entity);
				}
				console.info("---- Copying of ExportPoints finished ----\n");
			}
		}

		// ######## REFRESH IDEA FILESYSTEM ##############################
		if (hasRefreshEntities()) {
			List<SyncEntity> pullEntityList = getRefreshEntityList();
			List<File> refreshFiles = new ArrayList<File>(pullEntityList.size());

			for (SyncEntity entity : pullEntityList) {
				refreshFiles.add(entity.getFile());
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

	private void setSyncList(SyncList syncList) {
		this.syncList = syncList;
		this.pullMetadataOnly = syncList.isPullMetaDataOnly();

		if (!syncList.isPullMetaDataOnly()) {
			for (SyncEntity entity : syncList) {
				if (entity.getSyncAction() == SyncAction.PULL || entity.getSyncAction() == SyncAction.DELETE_RFS) {
					this.refreshEntityList.add(entity);
				}
				addSyncEntityToExportListIfNecessary(entity);
			}
		}
	}

	/**
	 * @return the List of SyncEntities handled by this SyncJob
	 */
	public SyncList getSyncList() {
		return syncList;
	}

	private void addSyncEntityToExportListIfNecessary(SyncEntity syncEntity) {

		// if publishing is enabled, pushed and deleted entities don't have to be handled (this is done by OpenCms)
		if (publish && (syncEntity.getSyncAction() == SyncAction.PUSH || syncEntity.getSyncAction() == SyncAction.DELETE_VFS)) {
			return;
		}

		List<OpenCmsModuleExportPoint> exportPoints = syncEntity.getOcmsModule().getExportPoints();

		if (exportPoints != null) {

			String localModuleVfsRoot = syncEntity.getOcmsModule().getLocalVfsRoot();
			String entityVfsPath = syncEntity.getVfsPath();

			for (OpenCmsModuleExportPoint exportPoint : exportPoints) {
				String vfsSource = exportPoint.getVfsSource();
				if (entityVfsPath.startsWith(vfsSource)) {
					String destination = exportPoint.getRfsTarget();
					String relativePath = entityVfsPath.substring(vfsSource.length());
					ExportEntity exportEntity = new ExportEntity();
					exportEntity.setSourcePath(localModuleVfsRoot+entityVfsPath);
					exportEntity.setTargetPath(config.getWebappRoot() + "/" + destination + relativePath);
					exportEntity.setVfsPath(entityVfsPath);
					exportEntity.setDestination(destination);
					exportEntity.setToBeDeleted(syncEntity.getSyncAction().isDeleteAction());
					addExportEntity(exportEntity);
				}
			}
		}
	}

	private void addExportEntity(ExportEntity entity) {
		exportList.add(entity);
	}

	private List<SyncEntity> getRefreshEntityList() {
		return refreshEntityList;
	}

	private int getNumRefreshEntities() {
		return refreshEntityList.size();
	}

	private boolean hasRefreshEntities() {
		return getNumRefreshEntities() > 0;
	}

	private int numExportEntities() {
		return exportList.size();
	}


	private void doSync(SyncEntity entity) {
		if (entity.getSyncAction() == SyncAction.PUSH) {
			doPush(entity);
			if (publish) {
				publishList.add(entity.getVfsPath());
			}
		}
		else if (entity.getSyncAction() == SyncAction.PULL) {
			doPull(entity);
		}
		else if (entity.getSyncAction() == SyncAction.DELETE_RFS) {
			doDeleteFromRfs(entity);
		}
		else if (entity.getSyncAction() == SyncAction.DELETE_VFS) {
			doDeleteFromVfs(entity);
			if (publish) {
				publishList.add(entity.getVfsPath());
			}
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
		File rfsFile = entity.getFile();
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

		try {
			HashMap<String, String> metaInfos = plugin.getPluginConnector().getResourceInfos(pullEntityList);
			for (SyncEntity entity : syncList) {
				doMetaInfoHandling(console, metaInfos, entity);
			}
		}
		catch (OpenCmsConnectorException e) {
			console.error(e.getMessage());
		}
		catch (IOException e) {
			console.error("There was an error retrieving resource meta infos from OpenCms");
			LOG.warn("IOException while trying to retrieve meta infos", e);
		}
	}

	private void doMetaInfoHandling(OpenCmsToolWindowConsole console, Map<String,String> metaInfos, SyncEntity entity) {
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
				String metaInfoStr = PluginTools.ensureUnixNewline(metaInfos.get(entity.getVfsPath())) + "\n";
				FileUtils.writeStringToFile(metaInfoFile, metaInfoStr, "UTF-8");
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
			catch (OpenCmsConnectorException e) {
				console.error(e.getMessage());
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
						String manifest = manifestInfos.get(ocmsModule.getModuleName());
						if (ocmsModule.isSetSpecificModuleVersionEnabled() && StringUtils.isNotEmpty(ocmsModule.getModuleVersion())) {
							manifest = manifest.replaceFirst("<version>[^<]*</version>", "<version>" + Matcher.quoteReplacement(ocmsModule.getModuleVersion()) + " </version>");
						}
						manifest = PluginTools.ensureUnixNewline(manifest) + "\n";
						FileUtils.writeStringToFile(new File(manifestPath), manifest, StandardCharsets.UTF_8);
						console.info("PULL: " + manifestPath + " pulled from OpenCms");
					}
					else {
						LOG.warn("No manifest found for module " + ocmsModule.getModuleName());
					}
				}
			}
			catch (OpenCmsConnectorException e) {
				console.error(e.getMessage());
			}
			catch (IOException e) {
				Messages.showDialog("There was an error pulling the module manifest files from OpenCms.\nIs the connector module installed?",
						"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				LOG.warn("There was an Exception pulling the module manifests", e);
			}
		}
	}

	private static boolean isParentOfIdeConnectorPath(String path) {
		for (String ideConnectorParentPath : IDE_CONNECTOR_PARENT_PATHS) {
			if (path.endsWith(ideConnectorParentPath)) {
				return true;
			}
		}
		return false;
	}

	private void doExportPointHandling(ExportEntity entity) {
		StringBuilder confirmation = new StringBuilder();
		StringBuilder notice = new StringBuilder();

		if (!entity.isToBeDeleted()) {
			confirmation.append("Copy of ").append(entity.getVfsPath()).append(" to ").append(entity.getDestination()).append(" - ");
			File file = new File(entity.getSourcePath());
			if (file.exists()) {
				if (file.isFile()) {
					try {
						FileUtils.copyFile(file, new File(entity.getTargetPath()));
						confirmation.append("SUCCESS");
					} catch (IOException e) {
						confirmation.insert(0, ERROR_PREFIX);
						confirmation.append("FAILED (").append(e.getMessage()).append(")");
					}
				}
				else if (file.isDirectory()) {
					try {
						FileUtils.copyDirectory(file, new File(entity.getTargetPath()));
						confirmation.append("SUCCESS");
					} catch (IOException e) {
						confirmation.insert(0, ERROR_PREFIX);
						confirmation.append("FAILED (").append(e.getMessage()).append(")");
					}
				}
			}
			else {
				confirmation.append(" - FILE NOT FOUND");
			}
		}
		else {
			String targetPath = entity.getTargetPath();
			String vfsPath = entity.getVfsPath();

			deleteExportedResource(vfsPath, targetPath, confirmation, notice);
		}
		if (confirmation.indexOf(ERROR_PREFIX) > -1) {
			console.error(confirmation.toString());
		}
		else {
			console.info(confirmation.toString());
		}
		if (notice.length() > 0) {
			console.notice(notice.toString());
		}
	}

	/**
	 * Deletes a resource file that was exported via export point handling
	 * @param vfsPath       the resource's VFS path
	 * @param targetPath    the export point's target path within the WebApp
	 * @param confirmation  StringBuilder the log is added to
	 * @param notice        String builder for an additional notice that may be displayed under the log. This is used
	 *                      for a warning message if the deletion was skipper because a path containing the IDE
	 *                      connector would be deleted.
	 */
	public static void deleteExportedResource(String vfsPath, String targetPath, StringBuilder confirmation, StringBuilder notice) {
		confirmation.append("Resource ").append(vfsPath).append(" removed, deletion of exported file ")
				.append(targetPath).append(" - ");
		// check if the target path is something that shouldn't be deleted
		if (isParentOfIdeConnectorPath(targetPath)) {
			confirmation.append("SKIPPED (paths containing the IDE connector must not be deleted)");
			notice.append("Deletion of the folder ").append(targetPath).append(" was skipped. Please delete any unwanted exported sub resources manually.");
		}
		else {
			File file = new File(targetPath);
			if (file.exists()) {
				if (FileUtils.deleteQuietly(file)) {
					confirmation.append("SUCCESS");
				}
				else {
					confirmation.insert(0, ERROR_PREFIX);
					confirmation.append("FAILED");
				}
			}
			else {
				confirmation.append("NOT NECESSARY (doesn't exist)");
			}
		}
	}

	private void cleanupModuleMetaFolder(OpenCmsModule ocmsModule) {
		if (ocmsModule != null) {
			File metaFolder = new File(ocmsModule.getManifestRoot());
			if (!metaFolder.isDirectory()) {
				return;
			}
			File[] metaFiles = metaFolder.listFiles();
			if (metaFiles == null || metaFiles.length == 0) {
				return;
			}
			for (File metaFile : metaFiles) {
				FileUtils.deleteQuietly(metaFile);
			}
		}
	}

}
