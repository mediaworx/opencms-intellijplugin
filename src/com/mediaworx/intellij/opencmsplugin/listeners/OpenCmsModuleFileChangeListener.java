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

package com.mediaworx.intellij.opencmsplugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.AutoPublishMode;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.exceptions.OpenCmsConnectorException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.SyncJob;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import com.mediaworx.opencms.moduleutils.manifestgenerator.OpenCmsModuleManifestGenerator;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Listener/handler for delete, move and rename events in the IntelliJ file system
 */
// TODO: handle cases where moves or deletions of parents of vfs resources take place (or don't handle those cases, whatever, at least think about it)
public class OpenCmsModuleFileChangeListener implements BulkFileListener {

	private static final Logger LOG = Logger.getInstance(OpenCmsModuleFileChangeListener.class);

	private OpenCmsPlugin plugin;
	private OpenCmsPluginConfigurationData config;
	private OpenCmsModules openCmsModules;
	private OpenCmsToolWindowConsole console;

	private VfsAdapter vfsAdapter;
	Map<VirtualFile, Module> deletedFileModuleLookup;
	List<VfsFileDeleteInfo> vfsFilesToBeDeleted;
	List<VfsFileMoveInfo> vfsFilesToBeMoved;
	List<VfsFileRenameInfo> vfsFilesToBeRenamed;
	private List<File> refreshFiles;

	/**
	 * Creates a new listener/handler for delete, move and rename events in the IntelliJ file system
	 * @param plugin the OpenCms plugin instance
	 */
	public OpenCmsModuleFileChangeListener(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();
		openCmsModules = plugin.getOpenCmsModules();

		deletedFileModuleLookup = new HashMap<VirtualFile, Module>();
		vfsFilesToBeDeleted = new ArrayList<VfsFileDeleteInfo>();
		vfsFilesToBeMoved = new ArrayList<VfsFileMoveInfo>();
		vfsFilesToBeRenamed = new ArrayList<VfsFileRenameInfo>();
		refreshFiles = new ArrayList<File>();
	}

	/**
	 * Handler method that is called before IntelliJ executes the file change, stores a lookup of IntelliJ modules for
	 * deleted files because the module can't be determined after the delete is executed
	 * @param vFileEvents   List of file events, provided by IntelliJ
	 */
	public void before(@NotNull List<? extends VFileEvent> vFileEvents) {

		// sometimes file events occur before the plugin was initialized, so lets make sure we have a plugin, a project and a configuration
		if (plugin == null || plugin.getProject() == null || config == null || !config.isOpenCmsPluginEnabled()) {
			return;
		}

		// save all modules for deleted files in a lookup map, because IntelliJ can't find the module after the
		// deletion of directories (ModuleUtil.findModuleForFile returns null in that case)
		for (VFileEvent event : vFileEvents) {
			if (event instanceof VFileDeleteEvent) {
				VirtualFile ideaVFile = event.getFile();
				if (ideaVFile == null) {
					continue;
				}
				Module ideaModule = ModuleUtil.findModuleForFile(ideaVFile, plugin.getProject());
				if (ideaModule == null) {
					continue;
				}
				deletedFileModuleLookup.put(ideaVFile, ideaModule);
			}
		}
	}

	/**
	 * Handler method that is called after the file change has been executed by IntelliJ, handles file deletes, moves
	 * and renames: Is used to present a dialog asking the user if the file change should be reflected in the OpenCms
	 * VFS as well.
	 * @param vFileEvents   List of file events, provided by IntelliJ
	 */
	public void after(@NotNull List<? extends VFileEvent> vFileEvents) {

		if (config == null || !config.isOpenCmsPluginEnabled()) {
			return;
		}

		ToolWindow toolWindow = plugin.getToolWindow();
		if (toolWindow == null) {
			return;
		}

		console = plugin.getConsole();

		try {
			for (VFileEvent event : vFileEvents) {
				handleFileEvent(event);
			}
			if (getNumAffected() > 0) {
				boolean wasExecuted = handleAffectedFiles();

				// Publish the affected VFS resources (if publish is enabled)
				if (wasExecuted && config.isPluginConnectorEnabled() && config.getAutoPublishMode() != AutoPublishMode.OFF) {
					publishAffectedVfsResources();
				}
			}
		}
		catch (CmsConnectionException e) {
			Messages.showDialog("Error syncing file deletion/move/rename to OpenCms:\n"+e.getMessage(), "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
		}
		finally {
			vfsFilesToBeDeleted.clear();
			vfsFilesToBeMoved.clear();
			vfsFilesToBeRenamed.clear();
			deletedFileModuleLookup.clear();
			refreshFiles.clear();
		}
	}

	/**
	 * Determines the number of files affected by deletes, moves and renames
	 * @return the number of files affected by deletes, moves and renames
	 */
	private int getNumAffected() {
		return vfsFilesToBeDeleted.size() + vfsFilesToBeMoved.size() + vfsFilesToBeRenamed.size();
	}

	/**
	 * Returns the VFS adapter (initializing it if it wasn't initialized before)
	 * @return the VFS adapter
	 * @throws CmsConnectionException
	 */
	private VfsAdapter getVfsAdapter() throws CmsConnectionException {

		if (vfsAdapter == null) {
			vfsAdapter = plugin.getVfsAdapter();
		}

		// Not connected yet (maybe OpenCms wasn't started when the project opened)
		if (!vfsAdapter.isConnected()) {
			// Try to connect
			vfsAdapter.startSession();
		}
		return vfsAdapter;
	}


	/**
	 * Internal handler for file delete, move and rename events
	 * @param event IntelliJ's file change event
	 * @throws CmsConnectionException if the connection to OpenCms fails
	 */
	private void handleFileEvent(VFileEvent event) throws CmsConnectionException {
		// File is deleted
		if (event instanceof VFileDeleteEvent) {
			handleFileDeleteEvent(event);
		}
		// File is moved
		if (event instanceof VFileMoveEvent) {
			handleFileMoveEvent(event);
		}

		// File is renamed
		if (event instanceof VFilePropertyChangeEvent) {
			String propertyName = ((VFilePropertyChangeEvent)event).getPropertyName();
			if ("name".equals(propertyName)) {
				handleFileRenameEvent(event);
			}
		}
	}

	/**
	 * Internal handler for file delete events, fills a list of files to be deleted that is handled later in
	 * {@link #handleAffectedFiles()}
	 * @param event IntelliJ's file change event
	 * @throws CmsConnectionException if the connection to OpenCms fails
	 */
	private void handleFileDeleteEvent(VFileEvent event) throws CmsConnectionException {
		VirtualFile ideaVFile = event.getFile();
		if (ideaVFile != null) {
			String moduleBasePath = PluginTools.getModuleContentRoot(deletedFileModuleLookup.get(ideaVFile));
			OpenCmsModule ocmsModule = openCmsModules.getModuleForBasePath(moduleBasePath);

			// check if the file belongs to an OpenCms module
			if (ocmsModule  != null && ocmsModule.isPathModuleResource(ideaVFile.getPath())) {
				LOG.info("The following module resource was deleted: " + ideaVFile.getPath());
				String vfsPath = ocmsModule.getVfsPathForRealPath(ideaVFile.getPath());
				try {
					if (getVfsAdapter().exists(vfsPath)) {
						vfsFilesToBeDeleted.add(new VfsFileDeleteInfo(ocmsModule, vfsPath, ideaVFile.isDirectory()));
					}
				}
				catch (CmisPermissionDeniedException e) {
					throw new
							CmsConnectionException("A local file has been deleted, but it can't be checked if the file exists in the VFS (permission denied).\nPlease check manually: " + vfsPath);
				}
			}
		}
	}

	/**
	 * Internal handler for file move events fills a list of files to be moved that is handled later in
	 * {@link #handleAffectedFiles()}
	 * @param event IntelliJ's file change event
	 * @throws CmsConnectionException if the connection to OpenCms fails
	 */
	private void handleFileMoveEvent(VFileEvent event) throws CmsConnectionException {
		VirtualFile ideaVFile = event.getFile();

		if (ideaVFile != null) {


			VirtualFile oldParent = ((VFileMoveEvent)event).getOldParent();
			String oldParentPath = oldParent.getPath();

			VirtualFile newParent = ((VFileMoveEvent)event).getNewParent();
			String newParentPath = newParent.getPath();

			OpenCmsModule oldOcmsModule = openCmsModules.getModuleForPath(oldParentPath);
			OpenCmsModule newOcmsModule = openCmsModules.getModuleForPath(newParent.getPath());

			// old and new parent are in a module -> move the file in the OpenCms VFS
			if (oldOcmsModule != null && oldOcmsModule.isPathModuleResource(oldParentPath)
					&& newOcmsModule != null && newOcmsModule.isPathModuleResource(newParent.getPath())) {
				String oldParentVfsPath = oldOcmsModule.getVfsPathForRealPath(oldParentPath);
				String oldVfsPath = oldParentVfsPath + "/" + ideaVFile.getName();
				if (getVfsAdapter().exists(oldVfsPath)) {
					String newParentVfsPath = newOcmsModule.getVfsPathForRealPath(newParentPath);
					if (!oldParentVfsPath.equals(newParentVfsPath)) {
						LOG.debug("A file was moved from " + oldParentVfsPath + " to " + newParentVfsPath);
						vfsFilesToBeMoved.add(new VfsFileMoveInfo(oldOcmsModule, newOcmsModule, ideaVFile, ideaVFile.getName(), oldParentVfsPath, newParentVfsPath));
					}
				}
			}

			// if the new parent path is not inside a module, remove it
			else if (oldOcmsModule != null && oldOcmsModule.isPathModuleResource(oldParentPath)
						&& (newOcmsModule == null || !newOcmsModule.isPathModuleResource(newParentPath))) {
				String oldParentVfsPath = oldOcmsModule.getVfsPathForRealPath(oldParentPath);
				String oldVfsPath = oldParentVfsPath + "/" + ideaVFile.getName();

				LOG.info("File was moved out of the module path, deleting " + oldVfsPath);

				if (getVfsAdapter().exists(oldVfsPath)) {
					vfsFilesToBeDeleted.add(new VfsFileDeleteInfo(oldOcmsModule, oldVfsPath, ideaVFile.isDirectory()));
				}
			}
		}
	}

	/**
	 * Internal handler for file rename events fills a list of files to be renamed that is handled later in
	 * {@link #handleAffectedFiles()}
	 * @param event IntelliJ's file change event
	 * @throws CmsConnectionException if the connection to OpenCms fails
	 */
	private void handleFileRenameEvent(VFileEvent event) throws CmsConnectionException {
		VirtualFile ideaVFile = event.getFile();
		if (ideaVFile != null) {
			String renameFilePath = ideaVFile.getPath();
			OpenCmsModule ocmsModule = openCmsModules.getModuleForPath(renameFilePath);
			if (ocmsModule != null) {
				LOG.debug("The following file was renamed: " + ideaVFile.getPath());
				String oldName = (String)((VFilePropertyChangeEvent)event).getOldValue();
				String newName = (String)((VFilePropertyChangeEvent)event).getNewValue();
				String newVfsPath = ocmsModule.getVfsPathForRealPath(renameFilePath);
				String oldVfsPath = newVfsPath.replaceFirst(newName, oldName);

				if (!oldVfsPath.equals(newVfsPath) && ocmsModule.isPathModuleResource(ocmsModule.getLocalVfsRoot() + oldVfsPath) && getVfsAdapter().exists(oldVfsPath)) {
					vfsFilesToBeRenamed.add(new VfsFileRenameInfo(ocmsModule, ideaVFile, oldVfsPath, newVfsPath, newName));
				}
			}
		}
	}

	/**
	 * Handles all the lists of deleted, moved and renamed files collected in
	 * {@link #handleFileDeleteEvent(com.intellij.openapi.vfs.newvfs.events.VFileEvent)},
	 * {@link #handleFileMoveEvent(com.intellij.openapi.vfs.newvfs.events.VFileEvent)} and
	 * {@link #handleFileRenameEvent(com.intellij.openapi.vfs.newvfs.events.VFileEvent)}
	 * @return <code>true</code> if any action was executed
	 * @throws CmsConnectionException
	 */
	private boolean handleAffectedFiles() throws CmsConnectionException {

		boolean wasExecuted = false;

		// Delete files
		if (vfsFilesToBeDeleted.size() > 0) {
			wasExecuted = deleteFiles();
		}
		// Move files
		if (vfsFilesToBeMoved.size() > 0) {
			wasExecuted = moveFiles();
		}
		// Rename files
		if (vfsFilesToBeRenamed.size() > 0) {
			wasExecuted = renameFiles();
		}

		// Refresh the affected files in the IDEA VFS after a short delay (to avoid event collision)
		if (refreshFiles.size() > 0) {
			final List<File> filesToBeRefreshedLater = new ArrayList<File>(refreshFiles);
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					LocalFileSystem.getInstance().refreshIoFiles(filesToBeRefreshedLater, true, false, null);
				}
			}, 2000);
		}

		return wasExecuted;
	}

	/**
	 * Internal helper method constructing the meta data file path without the corresponding file suffix (used to
	 * remove meta data folders)
	 * @param ocmsModule    the OpenCms module containing the resource
	 * @param vfsPath       VFS path of the resource whose meta data path is to be returned
	 * @return the meta data file path without the corresponding file suffix
	 */
	private String getMetaDataFilePathWithoutSuffix(OpenCmsModule ocmsModule, String vfsPath) {
		return ocmsModule.getManifestRoot() + vfsPath;
	}

	/**
	 * Internal helper method constructing the path to the meta data XML file
	 * @param ocmsModule    the OpenCms module containing the resource
	 * @param vfsPath       VFS path of the resource whose meta data path is to be returned
	 * @param isDirectory <code>true</code> if the resource is a folder, <code>false</code> otherwise
	 * @return the path to the meta data XML file
	 */
	private String getMetaDataFilePath(OpenCmsModule ocmsModule, String vfsPath, boolean isDirectory) {
		return OpenCmsModuleManifestGenerator.getMetaInfoPath(ocmsModule.getManifestRoot(), vfsPath, isDirectory);
	}

	/**
	 * Presents a dialog asking the user if files are to be deleted from the VFS and handles the deletions if the user
	 * chooses to do so
	 * @return  <code>true</code> if the user elected to delete files, <code>false</code> if the user cancelled the
	 *          deletion
	 * @throws CmsConnectionException if the connection to OpenCms failed
	 */
	private boolean deleteFiles() throws CmsConnectionException {
		StringBuilder msg = new StringBuilder("Do you want to delete the following files/folders from the OpenCms VFS?");
		for (VfsFileDeleteInfo vfsFileToBeDeleted : vfsFilesToBeDeleted) {
			msg.append("\n").append(vfsFileToBeDeleted.vfsPath);
		}

		int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Delete Files/Folders?", Messages.getQuestionIcon());

		if (dlgStatus == 0) {
			console.clear();
			plugin.showConsole();
			for (VfsFileDeleteInfo deleteInfo : vfsFilesToBeDeleted) {
				console.info("DELETE: " + deleteInfo.vfsPath);
				getVfsAdapter().deleteResource(deleteInfo.vfsPath);
				// check export points
				deleteExportedFileIfNecessary(deleteInfo.vfsPath);

				// handle meta data files
				boolean isDirectory = deleteInfo.isDirectory;
				String metaDataFilePath = getMetaDataFilePath(deleteInfo.ocmsModule, deleteInfo.vfsPath, isDirectory);
				console.info("Remove meta data file " + metaDataFilePath);
				File metaDataFile = new File(metaDataFilePath);
				FileUtils.deleteQuietly(metaDataFile);
				refreshFiles.add(metaDataFile);

				if (isDirectory) {
					String metaFolderPath = getMetaDataFilePathWithoutSuffix(deleteInfo.ocmsModule, deleteInfo.vfsPath);
					console.info("Remove meta data folder " + metaFolderPath);
					File metaFolder = new File(metaFolderPath);
					FileUtils.deleteQuietly(metaFolder);
					refreshFiles.add(metaFolder);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Presents a dialog asking the user if files are to be moved in the VFS and handles the moves if the user
	 * chooses to do so
	 * @return  <code>true</code> if the user elected to move files, <code>false</code> if the user cancelled the
	 *          move
	 * @throws CmsConnectionException if the connection to OpenCms failed
	 */
	private boolean moveFiles() throws CmsConnectionException {
		StringBuilder msg = new StringBuilder("Do you want to move the following files/folders in the OpenCms VFS as well?");
		for (VfsFileMoveInfo vfsFileToBeMoved : vfsFilesToBeMoved) {
			msg.append("\n").append(vfsFileToBeMoved.oldVfsPath);
		}

		int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move Files/Folders?", Messages.getQuestionIcon());

		if (dlgStatus == 0) {
			console.clear();
			plugin.showConsole();
			for (VfsFileMoveInfo moveInfo : vfsFilesToBeMoved) {
				try {
					console.info("MOVE: " + moveInfo.oldVfsPath + " to " + moveInfo.newParentPath);
					Folder oldParent = (Folder)getVfsAdapter().getVfsObject(moveInfo.oldParentPath);
					Folder newParent = (Folder)getVfsAdapter().getVfsObject(moveInfo.newParentPath);
					if (newParent == null) {
						newParent = getVfsAdapter().createFolder(moveInfo.newParentPath);
					}
					FileableCmisObject resource = (FileableCmisObject)getVfsAdapter().getVfsObject(moveInfo.oldVfsPath);
					resource.move(oldParent, newParent);

					// handle export points
					handleExportPointsForMovedResources(moveInfo.oldVfsPath, moveInfo.newVfsPath, moveInfo.newIdeaVFile.getPath());

					// handle meta data files
					handleMetaDataForMovedResources(moveInfo.oldOcmsModule, moveInfo.newOcmsModule, moveInfo.oldVfsPath, moveInfo.newVfsPath, moveInfo.newIdeaVFile.isDirectory());
				}
				catch (CmsPermissionDeniedException e) {
					Messages.showDialog("Error moving files/folders." + e.getMessage(),
							"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Presents a dialog asking the user if files are to be renamed in the VFS and handles the renames if the user
	 * chooses to do so
	 * @return  <code>true</code> if the user elected to rename files, <code>false</code> if the user cancelled the
	 *          rename
	 * @throws CmsConnectionException if the connection to OpenCms failed
	 */
	private boolean renameFiles() throws CmsConnectionException {
		StringBuilder msg = new StringBuilder("Do you want to rename the following files/folders in the OpenCms VFS as well?");
		for (VfsFileRenameInfo vfsFileToBeRenamed : vfsFilesToBeRenamed) {
			msg.append("\n").append(vfsFileToBeRenamed.oldVfsPath).append(" -> ").append(vfsFileToBeRenamed.newName);
		}

		int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move Files/Folders?", Messages.getQuestionIcon());

		if (dlgStatus == 0) {
			console.clear();
			plugin.showConsole();
			for (VfsFileRenameInfo renameInfo : vfsFilesToBeRenamed) {
				console.info("RENAME: " + renameInfo.oldVfsPath + " to " + renameInfo.newName);
				try {
					CmisObject file = getVfsAdapter().getVfsObject(renameInfo.oldVfsPath);
					if (file == null) {
						LOG.warn("Error renaming " + renameInfo.oldVfsPath + ": the resource could not be loaded through CMIS");
						continue;
					}
					HashMap<String, Object> properties = new HashMap<String, Object>();
					properties.put(PropertyIds.NAME, renameInfo.newName);
					file.updateProperties(properties);

					// handle export points
					handleExportPointsForMovedResources(renameInfo.oldVfsPath, renameInfo.newVfsPath, renameInfo.newIdeaVFile.getPath());

					// handle meta data files
					handleMetaDataForMovedResources(renameInfo.ocmsModule, renameInfo.ocmsModule, renameInfo.oldVfsPath, renameInfo.newVfsPath, renameInfo.newIdeaVFile.isDirectory());
				}
				catch (CmsPermissionDeniedException e) {
					LOG.warn("Exception moving files - permission denied", e);
					Messages.showDialog("Error moving files/folders. " + e.getMessage(),
							"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Handles moves in export points for a moved resource
	 * @param oldVfsPath the VFS path before the move
	 * @param newVfsPath the VFS path after the move
	 * @param newRfsPath the RFS path after the move
	 */
	private void handleExportPointsForMovedResources(String oldVfsPath, String newVfsPath, String newRfsPath) {
		// if the old parent path was inside an export point, remove the exported file
		deleteExportedFileIfNecessary(oldVfsPath);

		// if the new path is inside an export point, handle it
		OpenCmsModuleExportPoint newPathExportPoint = openCmsModules.getExportPointForVfsResource(newVfsPath);
		if (newPathExportPoint != null) {
			File newRfsFile = new File(newRfsPath);
			String exportTargetPath = config.getWebappRoot() + "/" + newPathExportPoint.getTargetPathForVfsResource(newVfsPath);
			File exportTargetFile = new File(exportTargetPath);
			try {
				console.info("Copying export point from " + newRfsPath + " to " + exportTargetPath);
				FileUtils.copyFile(newRfsFile, exportTargetFile);
				refreshFiles.add(exportTargetFile);
			}
			catch (IOException e) {
				LOG.warn("Exception exporting a file for an ExportPoint", e);
			}
		}
	}

	/**
	 * Moves meta data xml files for a moved resource
	 * @param oldOcmsModule the source OpenCms module
	 * @param newOcmsModule the target OpenCms module
	 * @param oldVfsPath the VFS path before the move
	 * @param newVfsPath the VFS path after the move
	 * @param isDirectory <code>true</code> if the resource is a folder, <code>false</code> otherwise
	 */
	private void handleMetaDataForMovedResources(OpenCmsModule oldOcmsModule, OpenCmsModule newOcmsModule, String oldVfsPath, String newVfsPath, boolean isDirectory) {
		String oldMetaDataFilePath = getMetaDataFilePath(oldOcmsModule, oldVfsPath, isDirectory);
		String newMetaDataFilePath = getMetaDataFilePath(newOcmsModule, newVfsPath, isDirectory);
		try {
			console.info("Move meta data file from " + oldMetaDataFilePath + " to " + newMetaDataFilePath);
			File oldMetaDataFile = new File(oldMetaDataFilePath);
			File newMetaDataFile = new File(newMetaDataFilePath);
			FileUtils.moveFile(oldMetaDataFile, newMetaDataFile);
			File oldParentFile = oldMetaDataFile.getParentFile();
			File newParentFile = newMetaDataFile.getParentFile();
			refreshFiles.add(oldParentFile);
			if (!FileUtil.filesEqual(oldParentFile, newParentFile)) {
				refreshFiles.add(newParentFile);
			}
		}
		catch (IOException e) {
			LOG.warn("Exception while moving " + oldMetaDataFilePath + " to " + newMetaDataFilePath, e);
		}
		if (isDirectory) {
			String oldMetaFolderPath = getMetaDataFilePathWithoutSuffix(oldOcmsModule, oldVfsPath);
			String newMetaFolderPath = getMetaDataFilePathWithoutSuffix(newOcmsModule, newVfsPath);
			try {
				console.info("Move meta data folder from " + oldMetaFolderPath + " to " + newMetaFolderPath);
				File oldMetaFolder = new File(oldMetaFolderPath);
				File newMetaFolder = new File(newMetaFolderPath);
				FileUtils.moveDirectory(oldMetaFolder, newMetaFolder);
				File oldParentFolder = oldMetaFolder.getParentFile();
				File newParentFolder = newMetaFolder.getParentFile();
				refreshFiles.add(oldParentFolder);
				if (!FileUtil.filesEqual(oldParentFolder, newParentFolder)) {
					refreshFiles.add(newParentFolder);
				}
			}
			catch (IOException e) {
				LOG.warn("Exception while moving " + oldMetaFolderPath + " to " + newMetaFolderPath, e);
			}
		}
	}

	/**
	 * checks if vfsPath is inside an ExportPoint and removes a previously exported file if necessary
	 * @param vfsPath	   VFS path to check
	 */
	private void deleteExportedFileIfNecessary(String vfsPath) {
		OpenCmsModuleExportPoint exportPoint = openCmsModules.getExportPointForVfsResource(vfsPath);
		if (exportPoint != null) {
			String exportPath = config.getWebappRoot() + "/" + exportPoint.getTargetPathForVfsResource(vfsPath);
			File exportedFileToBeDeleted = new File(exportPath);
			if (exportedFileToBeDeleted.exists()) {
				StringBuilder confirmation = new StringBuilder();
				StringBuilder notice = new StringBuilder();

				SyncJob.deleteExportedResource(vfsPath, exportPath, confirmation, notice);

				if (confirmation.indexOf(SyncJob.ERROR_PREFIX) > -1) {
					console.error(confirmation.toString());
				}
				else {
					console.info(confirmation.toString());
				}
				if (notice.length() > 0) {
					console.notice(notice.toString());
				}

				refreshFiles.add(exportedFileToBeDeleted);
			}
		}
	}

	/**
	 * Starts an OpenCms direct publish session for resources that were affected by the deletes/moves/renames
	 */
	private void publishAffectedVfsResources() {
		ArrayList <String> affectedResourcePaths = new ArrayList<String>(getNumAffected());
		for (VfsFileDeleteInfo deletedInfo : vfsFilesToBeDeleted) {
			affectedResourcePaths.add(deletedInfo.vfsPath);
		}
		for (VfsFileMoveInfo moveInfo : vfsFilesToBeMoved) {
			affectedResourcePaths.add(moveInfo.newVfsPath);
		}
		for (VfsFileRenameInfo renameInfo : vfsFilesToBeRenamed) {
			affectedResourcePaths.add(renameInfo.newVfsPath);
		}
		if (affectedResourcePaths.size() > 0) {
			try {
				plugin.getPluginConnector().publishResources(affectedResourcePaths, false);
				console.info("PUBLISH: A direct publish session was started successfully");
			}
			catch (OpenCmsConnectorException e) {
				console.error(e.getMessage());
			}
			catch (IOException e) {
				LOG.warn("There was an exception while publishing resources after a file change event", e);
				Messages.showDialog("There was an Error during publish.\nIs OpenCms running?\n\n" + e.getMessage(),
						"OpenCms Publish Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
			}
		}
	}

	/** Internal bean to store infos for deleted resources */
	private static class VfsFileDeleteInfo {
		OpenCmsModule ocmsModule;
		String vfsPath;
		boolean isDirectory;

		private VfsFileDeleteInfo(OpenCmsModule ocmsModule, String vfsPath, boolean isDirectory) {
			this.ocmsModule = ocmsModule;
			this.vfsPath = vfsPath;
			this.isDirectory = isDirectory;
		}
	}

	/** Internal bean to store infos for moved resources */
	private static class VfsFileMoveInfo {
		OpenCmsModule oldOcmsModule;
		OpenCmsModule newOcmsModule;
		VirtualFile newIdeaVFile;
		String resourceName;
		String oldParentPath;
		String oldVfsPath;
		String newParentPath;
		String newVfsPath;

		private VfsFileMoveInfo(OpenCmsModule oldOcmsModule, OpenCmsModule newOcmsModule, VirtualFile newIdeaVFile, String resourceName, String oldParentPath, String newParentPath) {
			this.oldOcmsModule = oldOcmsModule;
			this.newOcmsModule = newOcmsModule;
			this.newIdeaVFile = newIdeaVFile;
			this.resourceName = resourceName;
			this.oldParentPath = oldParentPath;
			oldVfsPath = oldParentPath + "/" + resourceName;
			this.newParentPath = newParentPath;
			newVfsPath = newParentPath + "/" + resourceName;
		}
	}

	/** Internal bean to store infos for renamed resources */
	private static class VfsFileRenameInfo {
		private OpenCmsModule ocmsModule;
		private VirtualFile newIdeaVFile;
		String oldVfsPath;
		String newVfsPath;
		String newName;

		private VfsFileRenameInfo(OpenCmsModule ocmsModule, VirtualFile newIdeaVFile, String oldVfsPath, String newVfsPath, String newName) {
			this.ocmsModule = ocmsModule;
			this.newIdeaVFile = newIdeaVFile;
			this.oldVfsPath = oldVfsPath;
			this.newVfsPath = newVfsPath;
			this.newName = newName;
		}
	}
}
