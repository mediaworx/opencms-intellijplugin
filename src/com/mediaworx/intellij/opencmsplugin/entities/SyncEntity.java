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

public abstract class SyncEntity {

	private OpenCmsModule ocmsModule;
	private String vfsPath;
	private VirtualFile ideaVFile;
	private File realFile;
	private CmisObject vfsObject;
	private SyncAction syncAction;
	private boolean replaceExistingEntity;

	public SyncEntity(OpenCmsModule ocmsModule, String vfsPath, VirtualFile ideaVFile, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		setOcmsModule(ocmsModule);
		setVfsPath(vfsPath);
		setIdeaVFile(ideaVFile);
		setVfsObject(vfsObject);
		setSyncAction(syncAction);
		setReplaceExistingEntity(replaceExistingEntity);
	}

	public abstract Type getType();

	public abstract String getMetaInfoFilePath();

	public OpenCmsModule getOcmsModule() {
		return ocmsModule;
	}

	public void setOcmsModule(OpenCmsModule ocmsModule) {
		this.ocmsModule = ocmsModule;
	}

	public String getVfsPath() {
		return vfsPath;
	}

	public void setVfsPath(String vfsPath) {
		this.vfsPath = vfsPath;
	}

	public String getRfsPath() {
		return ocmsModule.getLocalVfsRoot() + vfsPath;
	}

	public File getRealFile() {
		return this.realFile;
	}

	public void setRealFile(File realFile) {
		this.realFile = realFile;
	}

	public VirtualFile getIdeaVFile() {
		return ideaVFile;
	}

	public void setIdeaVFile(VirtualFile ideaVFile) {
		if (ideaVFile != null) {
			this.ideaVFile = ideaVFile;
			this.realFile = new File(ideaVFile.getPath());
		}
	}

	public CmisObject getVfsObject() {
		return vfsObject;
	}

	public void setVfsObject(CmisObject vfsObject) {
		this.vfsObject = vfsObject;
	}

	public SyncAction getSyncAction() {
		return syncAction;
	}

	public void setSyncAction(SyncAction syncAction) {
		this.syncAction = syncAction;
	}

	public boolean replaceExistingEntity() {
		return replaceExistingEntity;
	}

	public void setReplaceExistingEntity(boolean replaceExistingEntity) {
		this.replaceExistingEntity = replaceExistingEntity;
	}

	public boolean isFile() {
		return getType() == Type.FILE;
	}

	public boolean isFolder() {
		return getType() == Type.FOLDER;
	}

	public static enum Type {
		FILE,
		FOLDER
	}
}
