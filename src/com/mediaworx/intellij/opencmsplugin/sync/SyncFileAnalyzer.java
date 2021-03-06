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
import com.intellij.openapi.progress.ProgressManager;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.entities.SyncFile;
import com.mediaworx.intellij.opencmsplugin.entities.SyncFolder;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModuleResource;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import com.mediaworx.intellij.opencmsplugin.tools.VfsFileAnalyzer;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.util.*;

class SyncFileAnalyzer extends VfsFileAnalyzer implements Runnable {

	private static final Logger LOG = Logger.getInstance(SyncFileAnalyzer.class);

	private SyncList syncList;
	List<OpenCmsModuleResource> moduleResourcesToBePulled;

	private VfsAdapter vfsAdapter;

	private static final String CLASSES_FOLDER = "/classes";

	private boolean pullAllMetaInformation;
	private boolean executeSync = true;

	SyncFileAnalyzer(final OpenCmsPlugin plugin, final List<File> syncFiles, boolean pullAllMetaInformation) throws CmsConnectionException {

		super(plugin, syncFiles);

		this.pullAllMetaInformation = pullAllMetaInformation;
		syncList = new SyncList();
		syncList.setPullMetaDataOnly(pullAllMetaInformation);

		vfsAdapter = plugin.getVfsAdapter();

		// Not connected yet (maybe OpenCms wasn't started when the project opened)
		if (!vfsAdapter.isConnected()) {
			// Try again to connect
			vfsAdapter.startSession();
		}
		this.vfsAdapter.clearCache();

		moduleResourcesToBePulled = new ArrayList<OpenCmsModuleResource>();
	}

	@Override
	public void run() {
		progressIndicator = ProgressManager.getInstance().getProgressIndicator();
		progressIndicator.setIndeterminate(true);
		progressIndicator.setText("Calculating resources to sync, please wait");

		analyzeFiles();

		if (!progressIndicator.isCanceled()) {
			if (moduleResourcesToBePulled != null && moduleResourcesToBePulled.size() > 0) {
				handleModuleResourcesToBePulled(moduleResourcesToBePulled);
			}
		}
		else {
			executeSync = false;
		}
	}

	boolean isExecuteSync() {
		return executeSync;
	}

	protected void handleModule(OpenCmsModule ocmsModule) {
		super.handleModule(ocmsModule);
		syncList.setSyncModuleMetaData(true);
		syncList.addOcmsModule(ocmsModule);
	}

	protected void handleModuleResourcePath(OpenCmsModule ocmsModule, String moduleResourceVfsPath) {
		LOG.info("resource path: " + ocmsModule.getLocalVfsRoot() + moduleResourceVfsPath);

		File resourceFile = new File(ocmsModule.getLocalVfsRoot() + moduleResourceVfsPath);
		if (resourceFile.exists()) {
			LOG.info("vFolder path: " + PluginTools.ensureUnixPath(resourceFile.getPath()));
			handleModuleResource(ocmsModule, resourceFile);
		}
		else {
			LOG.info("Resource path doesn't exist in the FS, it has to be pulled from the VFS");
			moduleResourcesToBePulled.add(new OpenCmsModuleResource(ocmsModule, moduleResourceVfsPath));
		}
	}

	protected void handleModuleResource(OpenCmsModule ocmsModule, File file) {
		walkFileTree(ocmsModule, file, FolderSyncMode.AUTO);
	}

	// TODO: handle cases where a folder on the vfs has the same name as a file on the rfs or vice versa
	private void walkFileTree(OpenCmsModule ocmsModule, File file, FolderSyncMode folderSyncMode) {

		if (progressIndicator.isCanceled()) {
			executeSync = false;
			return;
		}

		// the file has already been handled, so skip
		if (fileWasHandled(file)) {
			LOG.info("already handled " + file.getPath() + ", skipping");
			return;
		}
		else {
			addHandledFile(file);
		}

		if (fileOrPathIsIgnored(plugin.getPluginConfiguration(), file)) {
			return;
		}

		String vfsPath = ocmsModule.getVfsPathForFile(file);

		LOG.info("VFS path is " + vfsPath);

		boolean vfsObjectExists;
		CmisObject vfsObject = null;

		// it is known, that the VFS resource does not exist (because a descendant of a nonexistent folder is handled)
		if (folderSyncMode == FolderSyncMode.PUSH) {
			vfsObjectExists = false;
		}
		// the VFS resource might exist
		else {
			// get the corresponding vfs object (if it exists)
			try {
				vfsObject = vfsAdapter.getVfsObject(vfsPath);
			}
			catch (CmsPermissionDeniedException e) {
				String message = "Skipping " + vfsPath + ", permission denied\n";
				LOG.info(message, e);
				warnings.append(message).append("\n");
				return;
			}

			vfsObjectExists = vfsObject != null;
			LOG.debug(vfsPath + (vfsObjectExists ? " exists" : " does not exist"));
		}

		// It's a folder, check if it is already there and compare contents
		if (file.isDirectory()) {
			// The folder is not there, so push it with all child contents
			if (!vfsObjectExists) {
				if (!pullAllMetaInformation) {
					LOG.info("It's a folder that does not exist on the VFS, PUSH recursively");
					addRfsOnlyFolderTreeToSyncList(ocmsModule, vfsPath, file, false);
				}
			}
			// The Folder is there, compare contents of VFS and RFS
			else {
				handleExistingFolder(ocmsModule, file, (Folder)vfsObject, vfsPath);
			}
		}
		// It's a file
		else {
			// The file is not there, so push it
			if (!vfsObjectExists) {
				if (!pullAllMetaInformation) {
					LOG.info("It's a file that does not exist on the VFS, PUSH");
					addRfsOnlyFileToSyncList(ocmsModule, vfsPath, file, null);
				}
			}
			// The file exists, check which one is newer
			else {
				handleExistingFile(ocmsModule, file, vfsObject, vfsPath);
			}
		}

	}

	private void handleExistingFolder(OpenCmsModule ocmsModule, File file, Folder vfsObject, String vfsPath) {
		LOG.info("It's a folder that does exist on the VFS, compare");

		// Get folder content from the vfs, put it in a set
		LOG.info("Getting VFS content");
		LOG.info("Children:");

		if (pullAllMetaInformation) {
			syncList.add(new SyncFolder(ocmsModule, vfsPath, file, vfsObject, SyncAction.PULL, true));
		}

		ItemIterable<CmisObject> vfsChildren = vfsObject.getChildren();
		Map<String, CmisObject> vfsChildMap = new LinkedHashMap<String, CmisObject>();
		for (CmisObject vfsChild : vfsChildren) {
			vfsChildMap.put(vfsChild.getName(), vfsChild);
			LOG.info("    " + vfsChild.getName());
		}

		LOG.info("Looping RFS children");



		File[] rfsChildren = file.listFiles();

		if (rfsChildren != null && rfsChildren.length > 0) {
			// handle resources in the RFS
			for (File rfsChild : rfsChildren) {
				if (progressIndicator.isCanceled()) {
					return;
				}
				String filename = rfsChild.getName();

				// The file/folder does not exist on the VFS, recurse in PUSH mode (all children will be pushed)
				if (!vfsChildMap.containsKey(filename)) {
					if (!pullAllMetaInformation) {
						LOG.info("RFS child " + rfsChild.getName() + " is not on the VFS, handle it in PUSH mode");
						walkFileTree(ocmsModule, rfsChild, FolderSyncMode.PUSH);
					}
				}
				// The file/folder does exist on the VFS, recurse in AUTO mode (children will be pushed or pulled depending on their date)
				else {
					LOG.info("RFS child " + rfsChild.getName() + " exists on the VFS, handle it in AUTO mode");
					walkFileTree(ocmsModule, rfsChild, FolderSyncMode.AUTO);

					// remove the file from the vfsChildren map, so that only files that exist only on the vfs will be left
					vfsChildMap.remove(filename);
				}
			}
		}

		// Handle files/folders that exist only on the vfs
		if (!pullAllMetaInformation && vfsChildMap.size() > 0) {
			handleVfsOnlyChildren(ocmsModule, vfsObject.getPath(), vfsChildMap);
		}
	}


	private void handleVfsOnlyChildren(OpenCmsModule ocmsModule, String parentVfsPath, Map<String, CmisObject> vfsChildMap) {
		LOG.info("Handle files/folders that exist only on the vfs");
		for (CmisObject vfsChild : vfsChildMap.values()) {
			if (progressIndicator.isCanceled()) {
				return;
			}
			String childVfsPath = parentVfsPath + vfsChild.getName();

			// files
			if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addVfsOnlyFileToSyncList(ocmsModule, childVfsPath, vfsChild, false);
			}
			// folders
			else if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addVfsOnlyFolderTreeToSyncList(ocmsModule, childVfsPath, vfsChild, false);
			}
		}
	}

	private void handleExistingFile(OpenCmsModule ocmsModule, File file, CmisObject vfsObject, String vfsPath) {
		LOG.info("It's a file that exists on the VFS and the RFS");

		if (pullAllMetaInformation) {
			syncList.add(new SyncFile(ocmsModule, vfsPath, file, vfsObject, SyncAction.PULL, true));
		}
		else {
			Date localDate = new Date(file.lastModified());
			Date vfsDate = vfsObject.getLastModificationDate().getTime();

			if (ocmsModule.getSyncMode() == SyncMode.SYNC) {
				LOG.info("SyncMode is SYNC, so compare dates");
				if (localDate.after(vfsDate)) {
					LOG.info("RFS file is newer, PUSH");
					syncList.add(new SyncFile(ocmsModule, vfsPath, file, vfsObject, SyncAction.PUSH, true));
				}
				else if (vfsDate.after(localDate)) {
					LOG.info("VFS file is newer, PULL");
					syncList.add(new SyncFile(ocmsModule, vfsPath, file, vfsObject, SyncAction.PULL, true));
				}
				else {
					LOG.info("VFS file and RFS file have the same date, ignore");
				}
			}
			// if the dates are different, add the resource with PUSH or PULL action, depending on the module's syncMode
			else if (vfsDate.compareTo(localDate) != 0) {
				SyncAction syncAction = ocmsModule.getSyncMode() == SyncMode.PUSH ? SyncAction.PUSH : SyncAction.PULL;
				LOG.info("SyncMode is " + ocmsModule.getSyncMode() + " and files are not equal, so force " + syncAction);
				syncList.add(new SyncFile(ocmsModule, vfsPath, file, vfsObject, syncAction, true));
			}
		}
	}

	private void handleModuleResourcesToBePulled(List<OpenCmsModuleResource> moduleResourcesToBePulled) {
		for (OpenCmsModuleResource moduleResourceToBePulled : moduleResourcesToBePulled) {
			if (progressIndicator.isCanceled()) {
				executeSync = false;
				return;
			}

			String vfsPath = moduleResourceToBePulled.getResourcePath();

			CmisObject vfsObject;
			try {
				vfsObject = vfsAdapter.getVfsObject(vfsPath);
			}
			catch (CmsPermissionDeniedException e) {
				warnings.append("Skipping ").append(vfsPath).append(", permission denied\n");
				continue;
			}
			if (vfsObject == null)  {
				warnings.append("Skipping ").append(vfsPath).append(", doesn't exist in the VFS\n");
				continue;
			}

			OpenCmsModule ocmsModule = moduleResourceToBePulled.getOpenCmsModule();

			// files
			if (vfsObject.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addModuleResourceFileToSyncList(ocmsModule, vfsPath, vfsObject);
			}
			// folders
			else if (vfsObject.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addModuleResourceFolderTreeToSyncList(ocmsModule, vfsPath, vfsObject);
			}
		}
	}

	private void addRfsOnlyFileToSyncList(OpenCmsModule ocmsModule, String vfsPath, File file, Document vfsFile) {
		LOG.info("Adding RFS only file " + vfsPath);
		SyncAction syncAction = getRfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, file, vfsFile, syncAction, vfsFile != null);
		syncList.add(syncFile);
	}

	private void addRfsOnlyFolderTreeToSyncList(OpenCmsModule ocmsModule, String vfsPath, File file, boolean replaceExistingEntity) {
		LOG.info("Adding RFS only folder " + vfsPath);

		SyncAction syncAction = getRfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFolder syncFolder = new SyncFolder(ocmsModule, vfsPath, file, null, syncAction, replaceExistingEntity);
		syncList.add(syncFolder);

		if (syncAction != SyncAction.DELETE_RFS) {
			LOG.info("Get children of folder " + vfsPath);
			Collection<File> rfsChildren = FileUtils.listFiles(file, TrueFileFilter.INSTANCE, null);
			for (File rfsChild : rfsChildren) {
				LOG.info("Handle PUSH child " + rfsChild.getPath());
				walkFileTree(ocmsModule, rfsChild, FolderSyncMode.PUSH);
			}
		}
	}

	private void addVfsOnlyFileToSyncList(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		LOG.info("Adding VFS only file " + vfsPath);
		SyncAction syncAction = getVfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, null, vfsObject, syncAction, replaceExistingEntity);
		syncList.add(syncFile);
	}

	private void addVfsOnlyFolderTreeToSyncList(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		LOG.info("Adding VFS only folder " + vfsPath);

		SyncAction syncAction = getVfsOnlySyncAction(ocmsModule.getSyncMode());
		SyncFolder syncFolder = new SyncFolder(ocmsModule, vfsPath, null, vfsObject, syncAction, replaceExistingEntity);
		syncList.add(syncFolder);

		if (syncAction != SyncAction.DELETE_VFS) {
			// traverse folder, add children to the SyncJob
			LOG.info("Get children of VFS folder " + vfsPath);
			ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
			for (CmisObject child : vfsChildren) {
				String childVfsPath = vfsPath + "/" + child.getName();

				// files
				if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
					addVfsOnlyFileToSyncList(ocmsModule, childVfsPath, child, false);
				}
				// folders
				else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
					addVfsOnlyFolderTreeToSyncList(ocmsModule, childVfsPath, child, false);
				}
			}
		}
	}

	private void addModuleResourceFileToSyncList(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject) {
		LOG.info("Adding VFS module resource file " + vfsPath);
		SyncFile syncFile = new SyncFile(ocmsModule, vfsPath, null, vfsObject, SyncAction.PULL, false);
		syncList.add(syncFile);
	}

	private void addModuleResourceFolderTreeToSyncList(OpenCmsModule ocmsModule, String vfsPath, CmisObject vfsObject) {
		LOG.info("Adding VFS module resource folder " + vfsPath);

		SyncFolder syncFolder = new SyncFolder(ocmsModule, vfsPath, null, vfsObject, SyncAction.PULL, false);
		syncList.add(syncFolder);

		// traverse folder, add children to the SyncJob
		LOG.info("Get children of VFS folder " + vfsPath);
		ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
		for (CmisObject child : vfsChildren) {
			if (progressIndicator.isCanceled()) {
				executeSync = false;
				return;
			}
			String childVfsPath = vfsPath + "/" + child.getName();

			// files
			if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addModuleResourceFileToSyncList(ocmsModule, childVfsPath, child);
			}
			// folders
			else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addModuleResourceFolderTreeToSyncList(ocmsModule, childVfsPath, child);
			}
		}
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

	SyncList getSyncList() {
		return syncList;
	}
}
