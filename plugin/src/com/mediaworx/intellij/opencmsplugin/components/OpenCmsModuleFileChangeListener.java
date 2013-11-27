package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// TODO: handle cases where moves or deletions of parents of vfs resources take place (or don't handle those cases, whatever, at least think about it)
public class OpenCmsModuleFileChangeListener implements BulkFileListener {

	private static final Logger LOG = Logger.getInstance(OpenCmsModuleFileChangeListener.class);

	private OpenCmsPlugin plugin;
	private OpenCmsPluginConfigurationData config;
	private OpenCmsModules openCmsModules;

	private VfsAdapter vfsAdapter;
	private List<File> refreshFiles = new ArrayList<File>();
	ArrayList<String> vfsFilesToBeDeleted = new ArrayList<String>();
	ArrayList<VfsFileMoveInfo> vfsFilesToBeMoved = new ArrayList<VfsFileMoveInfo>();
	ArrayList<VfsFileRenameInfo> vfsFilesToBeRenamed = new ArrayList<VfsFileRenameInfo>();

	public OpenCmsModuleFileChangeListener(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();
		openCmsModules = plugin.getOpenCmsModules();
	}

	public void before(@NotNull List<? extends VFileEvent> vFileEvents) {
	}

	public void after(@NotNull List<? extends VFileEvent> vFileEvents) {

		try {
			for (VFileEvent event : vFileEvents) {
				handleFileEvent(event);
			}

			handleAffectedFiles();

			// Refresh the affected files in the IDEA VFS
			if (refreshFiles.size() > 0) {
				LocalFileSystem.getInstance().refreshIoFiles(refreshFiles);
			}
		}
		catch (CmsConnectionException e) {
			Messages.showDialog(e.getMessage(), "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
		}
	}

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

	private void handleFileEvent(VFileEvent event) throws CmsConnectionException {
		// File is deleted
		if (event instanceof VFileDeleteEvent) {
			handleFileDeleteEvent(vfsFilesToBeDeleted, event);
		}
		// File is moved
		if (event instanceof VFileMoveEvent) {
			handleFileMoveEvent(vfsFilesToBeDeleted, vfsFilesToBeMoved, event);
		}

		// File is renamed
		if (event instanceof VFilePropertyChangeEvent) {
			String propertyName = ((VFilePropertyChangeEvent)event).getPropertyName();
			if ("name".equals(propertyName)) {
				handleFileRenameEvent(vfsFilesToBeRenamed, event);
			}
		}
	}

	private void handleFileDeleteEvent(ArrayList<String> vfsFilesToBeDeleted, VFileEvent event) throws CmsConnectionException {
		VirtualFile ideaVFile = event.getFile();

		if (ideaVFile != null) {
			LOG.info("The following file was deleted: " + ideaVFile.getPath());

			OpenCmsModule ocmsModule = openCmsModules.getModuleForIdeaVFile(ideaVFile);

			// check if the file belongs to an OpenCms module
			if (ocmsModule  != null) {
				String vfsPath = ocmsModule.getVfsPathForIdeaVFile(ideaVFile);
				if (getVfsAdapter().exists(vfsPath)) {
					vfsFilesToBeDeleted.add(vfsPath);
				}
			}
		}
	}

	private void handleFileMoveEvent(ArrayList<String> vfsFilesToBeDeleted, ArrayList<VfsFileMoveInfo> vfsFilesToBeMoved, VFileEvent event) throws CmsConnectionException {
		VirtualFile ideaVFile = event.getFile();

		if (ideaVFile != null) {
			String newRfsPath = ideaVFile.getPath();
			LOG.info("The following file was moved: " + newRfsPath);

			VirtualFile oldParent = ((VFileMoveEvent)event).getOldParent();
			VirtualFile newParent = ((VFileMoveEvent)event).getNewParent();
			OpenCmsModule oldParentOcmsModule = openCmsModules.getModuleForIdeaVFile(oldParent);
			OpenCmsModule newParentOcmsModule = openCmsModules.getModuleForIdeaVFile(newParent);

			// old and new parent are in a module -> move the file in the OpenCms VFS
			if (oldParentOcmsModule != null && newParentOcmsModule != null) {
				String oldParentPath = oldParentOcmsModule.getVfsPathForIdeaVFile(oldParent);
				String oldVfsPath = oldParentPath + "/" + ideaVFile.getName();
				if (getVfsAdapter().exists(oldVfsPath)) {
					String newParentPath = newParentOcmsModule.getVfsPathForIdeaVFile(newParent);
					vfsFilesToBeMoved.add(new VfsFileMoveInfo(ideaVFile, ideaVFile.getName(), oldParentPath, newParentPath));
				}
			}

			// if the new parent path is not inside a module, remove it
			else if (oldParentOcmsModule != null && newParentOcmsModule == null) {
				String oldParentPath = oldParentOcmsModule.getVfsPathForIdeaVFile(oldParent);
				String oldVfsPath = oldParentPath + "/" + ideaVFile.getName();

				LOG.info("File was moved out of the module path, deleting " + oldVfsPath);

				if (getVfsAdapter().exists(oldVfsPath)) {
					vfsFilesToBeDeleted.add(oldVfsPath);
				}
			}
		}
	}

	private void handleFileRenameEvent(ArrayList<VfsFileRenameInfo> vfsFilesToBeRenamed, VFileEvent event) throws CmsConnectionException {
		VirtualFile ideaVFile = event.getFile();
		if (ideaVFile != null) {
			LOG.info("The following file was renamed: " + ideaVFile.getPath());

			OpenCmsModule ocmsModule = openCmsModules.getModuleForIdeaVFile(ideaVFile);

			if (ocmsModule != null) {
				String oldName = (String)((VFilePropertyChangeEvent) event).getOldValue();
				String newName = (String)((VFilePropertyChangeEvent) event).getNewValue();
				String newVfsPath = ocmsModule.getVfsPathForIdeaVFile(ideaVFile);
				String oldVfsPath = newVfsPath.replaceFirst(newName, oldName);

				if (getVfsAdapter().exists(oldVfsPath)) {
					vfsFilesToBeRenamed.add(new VfsFileRenameInfo(ideaVFile, oldVfsPath, newVfsPath, newName));
				}

			}
		}
	}

	private void handleAffectedFiles() throws CmsConnectionException {
		// Delete files
		if (vfsFilesToBeDeleted.size() > 0) {
			deleteFiles();
		}
		// Move files
		if (vfsFilesToBeMoved.size() > 0) {
			moveFiles();
		}
		// Rename files
		if (vfsFilesToBeRenamed.size() > 0) {
			renameFiles();
		}
	}

	private void deleteFiles() throws CmsConnectionException {
		StringBuilder msg = new StringBuilder("Do you want to delete the following files/folders from the OpenCms VFS as well?");
		for (String vfsFileToBeDeleted : vfsFilesToBeDeleted) {
			msg.append("\n").append(vfsFileToBeDeleted);
		}

		int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Delete Files/Folders?", Messages.getQuestionIcon());

		if (dlgStatus == 0) {
			for (String vfsFileToBeDeleted : vfsFilesToBeDeleted) {
				getVfsAdapter().deleteResource(vfsFileToBeDeleted);
				// check export points
				deleteExportedFileIfNecessary(vfsFileToBeDeleted, refreshFiles);
			}
		}
	}

	private void moveFiles() throws CmsConnectionException {
		StringBuilder msg = new StringBuilder("Do you want to move the following files/folders in the OpenCms VFS as well?");
		for (VfsFileMoveInfo vfsFileToBeMoved : vfsFilesToBeMoved) {
			msg.append("\n").append(vfsFileToBeMoved.getOldVfsPath());
		}

		int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move Files/Folders?", Messages.getQuestionIcon());

		if (dlgStatus == 0) {
			for (VfsFileMoveInfo moveInfo : vfsFilesToBeMoved) {
				try {
					LOG.info("Moving " + moveInfo.getOldVfsPath() + " to " + moveInfo.getNewParentPath());
					Folder oldParent = (Folder)getVfsAdapter().getVfsObject(moveInfo.getOldParentPath());
					Folder newParent = (Folder)getVfsAdapter().getVfsObject(moveInfo.getNewParentPath());
					if (newParent == null) {
						newParent = getVfsAdapter().createFolder(moveInfo.getNewParentPath());
					}
					FileableCmisObject resource = (FileableCmisObject)getVfsAdapter().getVfsObject(moveInfo.getOldVfsPath());
					resource.move(oldParent, newParent);

					// handle export points
					handleExportPointsForMovedResources(moveInfo.getOldVfsPath(), moveInfo.getNewVfsPath(), moveInfo.getNewIdeaVFile().getPath(), refreshFiles);

				}
				catch (CmsPermissionDeniedException e) {
					Messages.showDialog("Error moving files/folders." + e.getMessage(),
							"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				}
			}
		}
	}

	private void renameFiles() throws CmsConnectionException {
		StringBuilder msg = new StringBuilder("Do you want to rename the following files/folders in the OpenCms VFS as well?");
		for (VfsFileRenameInfo vfsFileToBeRenamed : vfsFilesToBeRenamed) {
			msg.append("\n").append(vfsFileToBeRenamed.getOldVfsPath()).append(" -> ").append(vfsFileToBeRenamed.getNewName());
		}

		int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move Files/Folders?", Messages.getQuestionIcon());

		if (dlgStatus == 0) {
			for (VfsFileRenameInfo renameInfo : vfsFilesToBeRenamed) {
				LOG.info("Renaming " + renameInfo.getOldVfsPath() + " to " + renameInfo.getNewName());
				try {
					CmisObject file = getVfsAdapter().getVfsObject(renameInfo.oldVfsPath);
					HashMap<String, Object> properties = new HashMap<String, Object>();
					properties.put(PropertyIds.NAME, renameInfo.getNewName());
					file.updateProperties(properties);

					// handle export points
					handleExportPointsForMovedResources(renameInfo.getOldVfsPath(), renameInfo.getNewVfsPath(), renameInfo.getNewIdeaVFile().getPath(), refreshFiles);
				}
				catch (CmsPermissionDeniedException e) {
					LOG.warn("Exception moving files - permission denied", e);
					Messages.showDialog("Error moving files/folders. " + e.getMessage(),
							"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				}
			}
		}
	}

	private void handleExportPointsForMovedResources(String oldVfsPath, String newVfsPath, String newRfsPath, List<File> refreshFiles) {
		// if the old parent path was inside an export point, remove the exported file
		deleteExportedFileIfNecessary(oldVfsPath, refreshFiles);

		// if the new path is inside an export point, handle it
		ModuleExportPoint newPathExportPoint = openCmsModules.getExportPointForVfsResource(newVfsPath);
		if (newPathExportPoint != null) {
			File newRfsFile = new File(newRfsPath);
			String exportTargetPath = config.getWebappRoot() + "/" + newPathExportPoint.getTargetPathForVfsResource(newVfsPath);
			File exportTargetFile = new File(exportTargetPath);
			try {
				FileUtils.copyFile(newRfsFile, exportTargetFile);
				refreshFiles.add(exportTargetFile);
			}
			catch (IOException e) {
				LOG.warn("Exception exporting a file for an ExportPoint", e);
			}
		}
	}

	/**
	 * checks if vfsPath is inside an ExportPoint and removes a previously exported file if necessary
	 * @param vfsPath	   VFS path to check
	 * @param refreshFiles  List of files that are to be refreshed
	 */
	private void deleteExportedFileIfNecessary(String vfsPath, List<File> refreshFiles) {
		ModuleExportPoint exportPoint = openCmsModules.getExportPointForVfsResource(vfsPath);
		if (exportPoint != null) {
			String exportPath = config.getWebappRoot() + "/" + exportPoint.getTargetPathForVfsResource(vfsPath);
			File exportedFileToBeDeleted = new File(exportPath);
			if (exportedFileToBeDeleted.exists()) {
				FileUtils.deleteQuietly(exportedFileToBeDeleted);
				refreshFiles.add(exportedFileToBeDeleted);
			}
		}
	}

	private static class VfsFileMoveInfo {

		private VirtualFile newIdeaVFile;
		private String resourceName;
		private String oldParentPath;
		private String newParentPath;

		private VfsFileMoveInfo(VirtualFile newIdeaVFile, String resourceName, String oldParentPath, String newParentPath) {
			this.newIdeaVFile = newIdeaVFile;
			this.resourceName = resourceName;
			this.oldParentPath = oldParentPath;
			this.newParentPath = newParentPath;
		}

		private VirtualFile getNewIdeaVFile() {
			return newIdeaVFile;
		}

		public String getOldParentPath() {
			return oldParentPath;
		}

		public String getNewParentPath() {
			return newParentPath;
		}

		public String getOldVfsPath() {
			return oldParentPath + "/" + resourceName;
		}

		public String getNewVfsPath() {
			return newParentPath + "/" + resourceName;
		}
	}

	private static class VfsFileRenameInfo {
		private VirtualFile newIdeaVFile;
		String oldVfsPath;
		String newVfsPath;
		String newName;

		private VfsFileRenameInfo(VirtualFile newIdeaVFile, String oldVfsPath, String newVfsPath, String newName) {
			this.newIdeaVFile = newIdeaVFile;
			this.oldVfsPath = oldVfsPath;
			this.newVfsPath = newVfsPath;
			this.newName = newName;
		}

		private VirtualFile getNewIdeaVFile() {
			return newIdeaVFile;
		}

		public String getOldVfsPath() {
			return oldVfsPath;
		}

		private String getNewVfsPath() {
			return newVfsPath;
		}

		public String getNewName() {
			return newName;
		}
	}
}