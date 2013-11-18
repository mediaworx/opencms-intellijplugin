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

	public abstract Type getType();

	public abstract String getMetaInfoFileSuffix();

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

	public String getMetaInfoPath() {
		return ocmsModule.getManifestRoot() + vfsPath + getMetaInfoFileSuffix();
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
