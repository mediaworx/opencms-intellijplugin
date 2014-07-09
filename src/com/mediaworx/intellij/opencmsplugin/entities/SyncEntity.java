/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
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

package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.SyncAction;
import org.apache.chemistry.opencmis.client.api.CmisObject;

import java.io.File;

/**
 * Parent class for resource beans used for syncing.
 */
public abstract class SyncEntity {

	private OpenCmsModule ocmsModule;
	private String vfsPath;
	private VirtualFile ideaVFile;
	private File realFile;
	private CmisObject vfsObject;
	private SyncAction syncAction;
	private boolean replaceExistingEntity;

	/**
	 * Creates a new sync entity
	 * @param ocmsModule the OpenCms module the entity is contained in
	 * @param vfsPath the entity's VFS path
	 * @param ideaVFile IntelliJs virtual file representing the entity
	 * @param vfsObject The CMIS object representing the entity
	 * @param syncAction The Sync Action to be used for the entity (PUSH, PULL, DELETE_RFS or DELETE_VFS)
	 * @param replaceExistingEntity <code>true</code> if an existing entity is replaced during sync
	 */
	public SyncEntity(OpenCmsModule ocmsModule, String vfsPath, VirtualFile ideaVFile, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		setOcmsModule(ocmsModule);
		setVfsPath(vfsPath);
		setIdeaVFile(ideaVFile);
		setVfsObject(vfsObject);
		setSyncAction(syncAction);
		setReplaceExistingEntity(replaceExistingEntity);
	}

	/**
	 * Returns the entity's type (FILE or FOLDER)
	 * @return the entity's type (FILE or FOLDER)
	 */
	public abstract Type getType();

	/**
	 * returns the path to the meta data file for this entity
	 * @return the path to the meta data file for this entity
	 */
	public abstract String getMetaInfoFilePath();

	/**
	 * Returns the OpenCms module.
	 * @return the OpenCms module
	 */
	public OpenCmsModule getOcmsModule() {
		return ocmsModule;
	}

	/**
	 * Sets the OpenCms module
	 * @param ocmsModule the OpenCms module
	 */
	public void setOcmsModule(OpenCmsModule ocmsModule) {
		this.ocmsModule = ocmsModule;
	}

	/**
	 * Returns the entity's VFS path.
	 * @return the entity's VFS path
	 */
	public String getVfsPath() {
		return vfsPath;
	}

	/**
	 * Sets the entity's VFS path.
	 * @param vfsPath the entity's VFS path
	 */
	public void setVfsPath(String vfsPath) {
		this.vfsPath = vfsPath;
	}

	/**
	 * Returns the entity's path in the real file System.
	 * @return the entity's RFS path
	 */
	public String getRfsPath() {
		return ocmsModule.getLocalVfsRoot() + vfsPath;
	}

	/**
	 * Returns a File handle for the entity in the real file system
	 * @return a File handle for the entity in the RFS
	 */
	public File getRealFile() {
		return this.realFile;
	}

	/**
	 * Sets the File handle handle for the entity in the real file system
	 * @param realFile the File handle for the entity in the RFS
	 */
	public void setRealFile(File realFile) {
		this.realFile = realFile;
	}

	/**
	 * Returns the IntelliJ virtual file for the entity
	 * @return the IntelliJ virtual file for the entity
	 */
	public VirtualFile getIdeaVFile() {
		return ideaVFile;
	}

	/**
	 * Sets the IntelliJ virtual file for the entity
	 * @param ideaVFile the IntelliJ virtual file for the entity
	 */
	public void setIdeaVFile(VirtualFile ideaVFile) {
		if (ideaVFile != null) {
			this.ideaVFile = ideaVFile;
			this.realFile = new File(ideaVFile.getPath());
		}
	}

	/**
	 * Returns the CMIS object representing this entity in the OpenCms VFS
	 * @return the CMIS object representing this entity in the OpenCms VFS
	 */
	public CmisObject getVfsObject() {
		return vfsObject;
	}

	/**
	 * Sets the CMIS object representing this entity in the OpenCms VFS
	 * @param vfsObject the CMIS object representing this entity in the OpenCms VFS
	 */
	public void setVfsObject(CmisObject vfsObject) {
		this.vfsObject = vfsObject;
	}

	/**
	 * Returns the entity's SyncAction (PUSH, PULL, DELETE_RFS or DELETE_VFS)
	 * @return the entity's SyncAction (PUSH, PULL, DELETE_RFS or DELETE_VFS)
	 */
	public SyncAction getSyncAction() {
		return syncAction;
	}

	/**
	 * Sets the entity's SyncAction
	 * @param syncAction the entity's SyncAction (PUSH, PULL, DELETE_RFS or DELETE_VFS)
	 */
	public void setSyncAction(SyncAction syncAction) {
		this.syncAction = syncAction;
	}

	/**
	 * Flag denoting if an existing entity is replaced during sync
	 * @return <code>true</code> if an existing entity is replaced during sync, <code>false</code> otherwise
	 */
	public boolean replaceExistingEntity() {
		return replaceExistingEntity;
	}

	/**
	 * Sets the flag denoting if an existing entity is replaced during sync
	 * @param replaceExistingEntity <code>true</code> if an existing entity is replaced during sync,
	 *                              <code>false</code> otherwise
	 */
	public void setReplaceExistingEntity(boolean replaceExistingEntity) {
		this.replaceExistingEntity = replaceExistingEntity;
	}

	/**
	 * Checks if the entity is a file
	 * @return <code>true</code> if the entity is a file, <code>false</code> otherwise
	 */
	public boolean isFile() {
		return getType() == Type.FILE;
	}

	/**
	 * Checks if the entity is a folder
	 * @return <code>true</code> if the entity is a folder, <code>false</code> otherwise
	 */
	public boolean isFolder() {
		return getType() == Type.FOLDER;
	}

	/**
	 * Possible Types of the entity, FILE and FOLDER
	 */
	public static enum Type {
		FILE,
		FOLDER
	}
}
