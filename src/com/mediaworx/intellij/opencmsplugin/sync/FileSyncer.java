package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.*;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

import java.io.File;
import java.util.*;

public class FileSyncer {

	OpenCmsPlugin plugin;
	OpenCmsPluginConfigurationData config;
	private VfsAdapter vfsAdapter;
	private SyncJob syncJob;

	List<OpenCmsModuleResource> moduleResourcesToBePulled;

	public FileSyncer(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();
		vfsAdapter = plugin.getVfsAdapter();

		FileDocumentManager.getInstance().saveAllDocuments();
		FileDocumentManager.getInstance().reloadFiles();
	}

	public void setModuleResourcesToBePulled(List<OpenCmsModuleResource> moduleResourcesToBePulled) {
		this.moduleResourcesToBePulled = moduleResourcesToBePulled;
	}

	private SyncAction getRfsOnlySyncAction(SyncMode syncMode) {
		SyncAction syncAction;
		switch (syncMode) {
			case PULL:
				syncAction = SyncAction.DELETE_RFS;
				break;
			case PUSH:
			case SYNC:
			default:
				syncAction = SyncAction.PUSH;
				break;
		}
		return syncAction;
	}

	private SyncAction getVfsOnlySyncAction(SyncMode syncMode) {
		SyncAction syncAction;
		switch (syncMode) {
			case PUSH:
				syncAction = SyncAction.DELETE_VFS;
				break;
			case PULL:
			case SYNC:
			default:
				syncAction = SyncAction.PULL;
				break;
		}
		return syncAction;
	}

	public boolean syncFiles(VirtualFile[] syncFiles) {
		// Not connected yet (maybe OpenCms wasn't started when the project opened)
		if (!vfsAdapter.isConnected()) {
			// Try again to connect
			vfsAdapter.startSession();

			// Still not connected? Show an error message and stop
			if (!vfsAdapter.isConnected()) {
				Messages.showDialog("Connection to OpenCms VFS failed. Is OpenCms running?",
						"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());

				// Get the hell out of here
				return false;
			}
		}

		this.vfsAdapter.clearCache();

		this.syncJob = new SyncJob(plugin);

		StringBuilder message = new StringBuilder();

		executeFileAnalysis(syncFiles, message);
		if (syncJob == null) {
			return true;
		}
		int numSyncEntities = syncJob.numSyncEntities();

		boolean proceed = syncJob.hasSyncEntities();
		System.out.println("proceed? " + proceed);

		if (numSyncEntities == 0) {
			proceed = false;
			if (message.length() > 0) {
				message.append("\n");
			}
			message.append("Nothing to sync");
			Messages.showMessageDialog(message.toString(), "OpenCms VFS Sync", Messages.getInformationIcon());
		}
		else if ((numSyncEntities == 1 && message.length() > 0) || numSyncEntities > 1) {
			if (message.length() > 0) {
				message.append("\n");
			}
			message.append("The following ").append(numSyncEntities).append(" syncFiles or folders will be synced to or from OpenCms VFS:\n\n");
			List<SyncEntity> syncEntities = syncJob.getSyncList();
			for (SyncEntity syncEntity : syncEntities) {
				message.append(syncEntity.getSyncAction().getDescription()).append(" ");
				message.append(syncEntity.getVfsPath());
				if (!syncEntity.replaceExistingEntity()) {
					if (!syncEntity.getSyncAction().isDeleteAction()) {
						message.append(" (new)");
					}
					else {
						message.append(" (obsolete)");
					}
				}
				else {
					message.append(" (changed)");
				}
				message.append("\n");
			}
			message.append("\nProceed?");
			try {
				int dlgStatus = Messages.showOkCancelDialog(message.toString(), "Start OpenCms VFS Sync?", Messages.getQuestionIcon());
				proceed = dlgStatus == 0;
			}
			catch (Exception e) {
				System.out.println("There was an exception showing a dialog: ");
				e.printStackTrace(System.out);
			}
		}

		if (proceed) {
			syncJob.execute();
			if (syncJob.hasRefreshEntities()) {
				List<SyncEntity> pullEntityList = syncJob.getRefreshEntityList();
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
		}
		return false;
	}

	private void executeFileAnalysis(final VirtualFile[] syncFiles, final StringBuilder message) {
		Runnable deployRunner = new Runnable() {

			public void run() {

				ProgressIndicatorManager progressIndicatorManager = new ProgressIndicatorManager() {
					ProgressIndicator indicator;

					// public void setText(final String text) {
					// 	indicator.setText(text);
					// }

					public void init() {
						indicator = ProgressManager.getInstance().getProgressIndicator();
						indicator.setIndeterminate(true);
						indicator.setText("Please wait");
					}

					public boolean isCanceled() {
						return indicator.isCanceled();
					}
				};

				progressIndicatorManager.init();

				handleSyncFiles(syncFiles, message, progressIndicatorManager);
				if (moduleResourcesToBePulled != null && moduleResourcesToBePulled.size() > 0) {
					handleModuleResourcesToBePulled(moduleResourcesToBePulled, message, progressIndicatorManager);
				}
				if (progressIndicatorManager.isCanceled()) {
					syncJob = null;
				}
			}
		};

		ProgressManager.getInstance().runProcessWithProgressSynchronously(deployRunner, "Analyzing local and VFS syncFiles and folders ...", true, plugin.getProject());
	}

	private boolean fileOrPathIsIgnored(final VirtualFile virtualFile) {
		final String pathLC = virtualFile.getPath().toLowerCase();
		return pathLC.contains(".git")
				|| pathLC.contains(".svn")
				|| pathLC.contains(".cvs")
				|| pathLC.contains(".sass-cache")
				|| virtualFile.getName().equals("#SyncJob.txt")
				|| virtualFile.getName().equals("sass")
				|| virtualFile.getName().equals(".config")
				|| virtualFile.getName().equals("manifest.xml")
				|| virtualFile.getName().equals("log4j.properties")
				|| virtualFile.getName().equals(".gitignore");
	}

	private void handleSyncFiles(final VirtualFile[] syncFiles, StringBuilder message, ProgressIndicatorManager progressIndicatorManager) {
		for (VirtualFile syncFile : syncFiles) {
			if (progressIndicatorManager.isCanceled()) {
				syncJob = null;
				return;
			}

			if (fileOrPathIsIgnored(syncFile)) {
				// do nothing (filter VCS files and OpenCms Sync Metadata)
				System.out.println("File is ignored");
			}
			// File is not in the sync path, ignore
			else if (!plugin.getOpenCmsModules().isIdeaVFileOpenCmsModuleResource(syncFile)) {

				// TODO: check if there are module resources under the syncFile, if so, then add them

				message.append("Ignoring '").append(syncFile.getPath()).append("' (not a module path).\n");
				System.out.println("File is not in the sync path, ignore");
			}
			else {
				System.out.println("Handling a module or a file in a module");
				handleSyncFile(syncFile, FolderSyncMode.AUTO, progressIndicatorManager, message);
			}
		}
	}

	private void handleSyncFile(final VirtualFile file, FolderSyncMode folderSyncMode, ProgressIndicatorManager progressIndicatorManager, StringBuilder message) {

		if (progressIndicatorManager.isCanceled()) {
			syncJob = null;
			return;
		}
		System.out.println("Handle file/folder " + file.getPath());

		OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(file);
		System.out.println("Module: " + ocmsModule.getModuleName());
		walkFileTree(ocmsModule, file, folderSyncMode, progressIndicatorManager, message);
	}

	// TODO: handle cases where a folder on the vfs has the same name as a file on the rfs or vice versa
	private void walkFileTree(OpenCmsModule ocmsModule, VirtualFile ideaVFile, FolderSyncMode folderSyncMode, ProgressIndicatorManager progressIndicatorManager, StringBuilder message) {

		if (progressIndicatorManager.isCanceled()) {
			syncJob = null;
			return;
		}
		if (ocmsModule == null) {
			System.out.println("Skipping " + ideaVFile.getPath() + ", no module defined");
			return;
		}

		if (fileOrPathIsIgnored(ideaVFile)) {
			System.out.println("File is ignored: " + ideaVFile.getPath() + " " + ideaVFile.getName());
			return;
		}

		String vfsPath = ocmsModule.getVfsPathForIdeaVFile(ideaVFile);
		String rfsPath = ideaVFile.getPath();

		System.out.println("VFS path is " + vfsPath);

		boolean vfsObjectExists;
		CmisObject vfsObject = null;

		if (folderSyncMode != FolderSyncMode.PUSH) {
			// Get the corresponding vfs object (if it exists)
			try {
				vfsObject = vfsAdapter.getVfsObject(vfsPath);
			}
			catch (CmsPermissionDeniedException e) {
				message.append("Skipping ").append(vfsPath).append(", permission denied\n");
				return;
			}

			vfsObjectExists = vfsObject != null;
			System.out.println(vfsPath + (vfsObjectExists ? " exists" : " does not exist"));
		}
		else {
			vfsObjectExists = false;
		}

		// It's a folder, check if it is already there and compare contents
		if (ideaVFile.isDirectory()) {
			// The folder is not there, so push it with all child contents
			if (!vfsObjectExists) {
				System.out.println("It's a folder that does not exist on the VFS, PUSH recursively");
				addRfsOnlyFolderTreeToSyncJob(ocmsModule, vfsPath, ideaVFile, false, progressIndicatorManager, message);
			}
			// The Folder is there, compare contents of VFS and RFS
			else {
				System.out.println("It's a folder that does exist on the VFS, compare");

				// Get folder content from the vfs, put it in a set
				System.out.println("Getting VFS content");
				System.out.print("Children: ");
				ItemIterable<CmisObject> vfsChildren = ((Folder)vfsObject).getChildren();
				Map<String, CmisObject> vfsChildMap = new LinkedHashMap<String, CmisObject>();
				for (CmisObject vfsChild : vfsChildren) {
					vfsChildMap.put(vfsChild.getName(), vfsChild);
					System.out.print(vfsChild.getName() + " ");
				}
				System.out.println();

				System.out.println("Looping RFS children");
				VirtualFile[] rfsChildren = ideaVFile.getChildren();

				// handle resources in the RFS
				for (VirtualFile rfsChild : rfsChildren) {
					if (progressIndicatorManager.isCanceled()) {
						return;
					}
					String filename = rfsChild.getName();

					// The file/folder does not exist on the VFS, recurse in PUSH mode
					if (!vfsChildMap.containsKey(filename)) {
						System.out.println("RFS child " + rfsChild.getName() + " is not on the VFS, handle it in PUSH mode");
						walkFileTree(ocmsModule, rfsChild, FolderSyncMode.PUSH, progressIndicatorManager, message);
					}
					// The file/folder does exist on the VFS, recurse in AUTO mode
					else {
						System.out.println("RFS child " + rfsChild.getName() + " exists on the VFS, handle it in AUTO mode");
						walkFileTree(ocmsModule, rfsChild, FolderSyncMode.AUTO, progressIndicatorManager, message);

						// remove the file from the vfsChildren map, so that only files that exist only on the vfs will be left
						vfsChildMap.remove(filename);
					}
				}

				System.out.println("Handle files/folders that exist only on the vfs");
				// Handle files/folders that exist only on the vfs
				for (CmisObject vfsChild : vfsChildMap.values()) {
					if (progressIndicatorManager.isCanceled()) {
						return;
					}
					String childVfsPath = vfsPath + "/" + vfsChild.getName();
					String childRfsPath = rfsPath + "/" + vfsChild.getName();

					// files
					if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
						addVfsOnlyFileToSyncJob(ocmsModule, childVfsPath, childRfsPath, vfsChild, false);
					}
					// folders
					else if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
						addVfsOnlyFolderTreeToSyncJob(ocmsModule, childVfsPath, childRfsPath, vfsChild, false);
					}
				}

			}
		}
		// It's a file
		else if (!ideaVFile.isSpecialFile()) {
			// The file is not there, so push it
			if (!vfsObjectExists) {
				System.out.println("It's a file that does not exist on the VFS, PUSH");
				addRfsOnlyFileToSyncJob(ocmsModule, vfsPath, ideaVFile, null);
			}
			// The file exists, check which one is newer
			else {
				System.out.println("It's a file that exists on the VFS and the RFS");
				File realFile = new File(ideaVFile.getPath());
				Date localDate = new Date(realFile.lastModified());
				Date vfsDate = vfsObject.getLastModificationDate().getTime();

				if (ocmsModule.getSyncMode() == SyncMode.SYNC) {
					System.out.println("SyncMode is SYNC, so compare dates");
					if (localDate.after(vfsDate)) {
						System.out.println("RFS file is newer, PUSH");
						SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntity.Type.FILE, vfsPath, ideaVFile.getPath(), ideaVFile, vfsObject, SyncAction.PUSH, true);
						syncJob.addSyncEntity(ocmsModule, syncFile);
					}
					else if (vfsDate.after(localDate)) {
						System.out.println("VFS file is newer, PULL");
						SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntity.Type.FILE, vfsPath, ideaVFile.getPath(), ideaVFile, vfsObject, SyncAction.PULL, true);
						syncJob.addSyncEntity(ocmsModule, syncFile);
					}
					else {
						System.out.println("VFS file and RFS file have the same date, ignore");
					}
				}
				else if (ocmsModule.getSyncMode() == SyncMode.PUSH && vfsDate.compareTo(localDate) != 0) {
					System.out.println("SyncMode is PUSH and files are not equal, so force push");
					SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntity.Type.FILE, vfsPath, ideaVFile.getPath(), ideaVFile, vfsObject, SyncAction.PUSH, true);
					syncJob.addSyncEntity(ocmsModule, syncFile);
				}
				else if (ocmsModule.getSyncMode() == SyncMode.PULL && vfsDate.compareTo(localDate) != 0) {
					System.out.println("SyncMode is PULL and files are not equal, so force pull");
					SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntity.Type.FILE, vfsPath, ideaVFile.getPath(), ideaVFile, vfsObject, SyncAction.PULL, true);
					syncJob.addSyncEntity(ocmsModule, syncFile);
				}
			}
		}

	}

	private void handleModuleResourcesToBePulled(List<OpenCmsModuleResource> moduleResourcesToBePulled, StringBuilder message, ProgressIndicatorManager progressIndicatorManager) {
		for (OpenCmsModuleResource moduleResourceToBePulled : moduleResourcesToBePulled) {
			if (progressIndicatorManager.isCanceled()) {
				syncJob = null;
				return;
			}

			String vfsPath = moduleResourceToBePulled.getResourcePath();

			CmisObject vfsObject;
			try {
				vfsObject = vfsAdapter.getVfsObject(vfsPath);
			}
			catch (CmsPermissionDeniedException e) {
				message.append("Skipping ").append(vfsPath).append(", permission denied\n");
				continue;
			}
			if (vfsObject == null)  {
				message.append("Skipping ").append(vfsPath).append(", doesn't exist in the VFS\n");
				continue;
			}

			OpenCmsModule ocmsModule = moduleResourceToBePulled.getOpenCmsModule();
			String rfsPath = ocmsModule.getLocalVfsRoot() + vfsPath;

			// files
			if (vfsObject.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addModuleResourceFileToSyncJob(ocmsModule, vfsPath, rfsPath, vfsObject);
			}
			// folders
			else if (vfsObject.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addModuleResourceFolderTreeToSyncJob(ocmsModule, vfsPath, rfsPath, vfsObject);
			}
		}
	}

	private SyncEntity getSyncEntity(SyncEntity.Type type, String vfsPath, String rfsPath, VirtualFile file, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		SyncEntity entity;
		if (type == SyncEntity.Type.FILE) {
			entity = new SyncFile();
		}
		else {
			entity = new SyncFolder();
		}
		entity.setVfsPath(vfsPath);
		entity.setRfsPath(rfsPath);
		entity.setIdeaVFile(file);
		entity.setVfsObject(vfsObject);
		entity.setSyncAction(syncAction);
		entity.setReplaceExistingEntity(replaceExistingEntity);
		return entity;
	}

	private void addRfsOnlyFileToSyncJob(OpenCmsModule ocmsModule, String vfsPath, VirtualFile file, Document vfsFile) {
		System.out.println("Adding RFS only file " + vfsPath);
		SyncAction syncAction = getRfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntity.Type.FILE, vfsPath, file.getPath(), file, vfsFile, syncAction, vfsFile != null);
		syncJob.addSyncEntity(ocmsModule, syncFile);
	}

	private void addRfsOnlyFolderTreeToSyncJob(OpenCmsModule ocmsModule, String vfsPath, VirtualFile file, boolean replaceExistingEntity, ProgressIndicatorManager progressIndicatorManager, StringBuilder message) {
		System.out.println("Adding RFS only folder " + vfsPath);

		SyncAction syncAction = getRfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFolder syncFile = (SyncFolder)getSyncEntity(SyncEntity.Type.FOLDER, vfsPath, file.getPath(), file, null, syncAction, replaceExistingEntity);
		syncJob.addSyncEntity(ocmsModule, syncFile);

		if (syncAction != SyncAction.DELETE_RFS) {
			System.out.println("Get children of folder " + vfsPath);
			VirtualFile[] children = file.getChildren();
			for (VirtualFile child : children) {
				System.out.println("Handle PUSH child " + child.getPath());
				walkFileTree(ocmsModule, child, FolderSyncMode.PUSH, progressIndicatorManager, message);
			}
		}
	}

	private void addVfsOnlyFileToSyncJob(OpenCmsModule ocmsModule, String vfsPath, String rfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		System.out.println("Adding VFS only file " + vfsPath);
		SyncAction syncAction = getVfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFile syncFile = (SyncFile) getSyncEntity(SyncEntity.Type.FILE, vfsPath, rfsPath, null, vfsObject, syncAction, replaceExistingEntity);
		syncJob.addSyncEntity(ocmsModule, syncFile);
	}

	private void addVfsOnlyFolderTreeToSyncJob(OpenCmsModule ocmsModule, String vfsPath, String rfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		System.out.println("Adding VFS only folder " + vfsPath);

		SyncAction syncAction = getVfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFolder syncFile = (SyncFolder) getSyncEntity(SyncEntity.Type.FOLDER, vfsPath, rfsPath, null, vfsObject, syncAction, replaceExistingEntity);
		syncJob.addSyncEntity(ocmsModule, syncFile);

		if (syncAction != SyncAction.DELETE_VFS) {
			// traverse folder, add children to the SyncJob
			System.out.println("Get children of VFS folder " + vfsPath);
			ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
			for (CmisObject child : vfsChildren) {
				String childVfsPath = vfsPath + "/" + child.getName();
				String childRfsPath = rfsPath + "/" + child.getName();

				// files
				if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
					addVfsOnlyFileToSyncJob(ocmsModule, childVfsPath, childRfsPath, child, false);
				}
				// folders
				else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
					addVfsOnlyFolderTreeToSyncJob(ocmsModule, childVfsPath, childRfsPath, child, false);
				}
			}
		}
	}

	private void addModuleResourceFileToSyncJob(OpenCmsModule ocmsModule, String vfsPath, String rfsPath, CmisObject vfsObject) {
		System.out.println("Adding VFS module resource file " + vfsPath);
		SyncFile syncFile = (SyncFile) getSyncEntity(SyncEntity.Type.FILE, vfsPath, rfsPath, null, vfsObject, SyncAction.PULL, false);
		syncJob.addSyncEntity(ocmsModule, syncFile);
	}

	private void addModuleResourceFolderTreeToSyncJob(OpenCmsModule ocmsModule, String vfsPath, String rfsPath, CmisObject vfsObject) {
		System.out.println("Adding VFS module resource folder " + vfsPath);

		SyncFolder syncFile = (SyncFolder) getSyncEntity(SyncEntity.Type.FOLDER, vfsPath, rfsPath, null, vfsObject, SyncAction.PULL, false);
		syncJob.addSyncEntity(ocmsModule, syncFile);

		// traverse folder, add children to the SyncJob
		System.out.println("Get children of VFS folder " + vfsPath);
		ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
		for (CmisObject child : vfsChildren) {
			String childVfsPath = vfsPath + "/" + child.getName();
			String childRfsPath = rfsPath + "/" + child.getName();

			// files
			if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addModuleResourceFileToSyncJob(ocmsModule, childVfsPath, childRfsPath, child);
			}
			// folders
			else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addModuleResourceFolderTreeToSyncJob(ocmsModule, childVfsPath, childRfsPath, child);
			}
		}
	}



	public interface ProgressIndicatorManager {

		void init();

		boolean isCanceled();
	}

}
