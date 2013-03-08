package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.chemistry.opencmis.client.api.CmisObject;

import java.io.File;
import java.io.IOException;

public class SyncEntity {
    private String vfsPath;
    private String rfsPath;
    private VirtualFile ideaVFile;
    private File realFile;
    private CmisObject vfsObject;
    private SyncMode syncMode;
    private boolean replaceExistingEntity;

    public SyncEntityType getType() {
        Object test = this;
        if (test instanceof SyncFolder) {
            return SyncEntityType.FOLDER;
        }
        else {
            return SyncEntityType.FILE;
        }
    }

    public String getVfsPath() {
        return vfsPath;
    }

    public void setVfsPath(String vfsPath) {
        this.vfsPath = vfsPath;
    }

    public String getRfsPath() {
        return rfsPath;
    }

    public void setRfsPath(String rfsPath) {
        this.rfsPath = rfsPath;
    }

    public VirtualFile getIdeaVFile() {
        return ideaVFile;
    }

    public File getRealFile() {
        return this.realFile;
    }

    public void setIdeaVFile(VirtualFile ideaVFile) {
        if (ideaVFile != null) {
            this.ideaVFile = ideaVFile;
            this.realFile = new File(ideaVFile.getPath());
        }
    }

	public File createRealFile() {
		File realFile = new File(getRfsPath());
		try {
			realFile.createNewFile();
		}
		catch (IOException e) {
			System.out.println("There was an Exception creating the local file" + getRfsPath() + ": " + e + "\n" + e.getMessage());
		}
		this.realFile = realFile;
		return realFile;
	}

	public CmisObject getVfsObject() {
        return vfsObject;
    }

    public void setVfsObject(CmisObject vfsObject) {
        this.vfsObject = vfsObject;
    }

    public SyncMode getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    public boolean replaceExistingEntity() {
        return replaceExistingEntity;
    }

    public void setReplaceExistingEntity(boolean replaceExistingEntity) {
        this.replaceExistingEntity = replaceExistingEntity;
    }

    public boolean isFile() {
        return getType() == SyncEntityType.FILE;
    }

    public boolean isFolder() {
        return getType() == SyncEntityType.FOLDER;
    }
}
