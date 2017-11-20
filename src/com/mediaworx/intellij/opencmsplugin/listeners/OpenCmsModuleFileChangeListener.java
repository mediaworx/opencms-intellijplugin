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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener/handler for delete, move and rename events in the IntelliJ file system. Analyzes changes and uses the
 * {@link OpenCmsModuleFileChangeHandler} to ask the user if those changes should be reflected in the OpenCms VFS
 */
// TODO: handle cases where moves or deletions of parents of vfs resources take place (or don't handle those cases, whatever, at least think about it)
public class OpenCmsModuleFileChangeListener implements BulkFileListener {

	private static final Logger LOG = Logger.getInstance(OpenCmsModuleFileChangeListener.class);

	private OpenCmsPlugin plugin;
	private OpenCmsModuleFileChangeHandler changeHandler;
	private VfsAdapter vfsAdapter;
	private OpenCmsPluginConfigurationData config;
	private OpenCmsModules openCmsModules;

	private Map<VirtualFile, Module> deletedFileModuleLookup;

	/**
	 * Creates a new listener/handler for delete, move and rename events in the IntelliJ file system
	 * @param plugin the OpenCms plugin instance
	 */
	public OpenCmsModuleFileChangeListener(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		changeHandler = new OpenCmsModuleFileChangeHandler(plugin);
		config = plugin.getPluginConfiguration();
		openCmsModules = plugin.getOpenCmsModules();

		deletedFileModuleLookup = new HashMap<VirtualFile, Module>();
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
	 * Handler method that is called after the file change has been executed by IntelliJ, analyzes file deletes, moves
	 * and renames and calls the change handler (in a separate thread) that handles all the changes and is also used
	 * to present a dialog asking the user if the file change should be reflected in the OpenCms VFS as well.
	 * @param vFileEvents   List of file events, provided by IntelliJ
	 */
	public void after(@NotNull List<? extends VFileEvent> vFileEvents) {

		try {
			if (config == null || !config.isOpenCmsPluginEnabled()) {
				return;
			}

			for (VFileEvent event : vFileEvents) {
				handleFileEvent(event);
			}

			if (changeHandler.hasAffectedFiles()) {
				ApplicationManager.getApplication().invokeLater(changeHandler);
			}

		}
		catch (CmsConnectionException e) {
			LOG.error("Error syncing file deletion/move/rename to OpenCms:\n" + e.getMessage(), e);
		}
		finally {
			deletedFileModuleLookup.clear();
		}
	}

	/**
	 * Returns the VFS adapter (initializing it if it wasn't initialized before)
	 *
	 * @return the VFS adapter
	 *
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
	 * {@link OpenCmsModuleFileChangeHandler#handleChanges()}
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
						changeHandler.addFileToBeDeleted(ocmsModule, vfsPath, ideaVFile.isDirectory());
					}
				}
				catch (CmisPermissionDeniedException e) {
					throw new CmsConnectionException("A local file has been deleted, but it can't be checked if the file exists in the VFS (permission denied).\nPlease check manually: " + vfsPath);
				}
			}
		}
	}

	/**
	 * Internal handler for file move events fills a list of files to be moved that is handled later in
	 * {@link OpenCmsModuleFileChangeHandler#handleChanges()}
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
						changeHandler.addFileToBeMoved(oldOcmsModule, newOcmsModule, ideaVFile, ideaVFile.getName(), oldParentVfsPath, newParentVfsPath);
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
					changeHandler.addFileToBeDeleted(oldOcmsModule, oldVfsPath, ideaVFile.isDirectory());
				}
			}
		}
	}

	/**
	 * Internal handler for file rename events fills a list of files to be renamed that is handled later in
	 * {@link OpenCmsModuleFileChangeHandler#handleChanges()}
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
					changeHandler.addFileToBeRenamed(ocmsModule, ideaVFile, oldVfsPath, newVfsPath, newName);
				}
			}
		}
	}

}
