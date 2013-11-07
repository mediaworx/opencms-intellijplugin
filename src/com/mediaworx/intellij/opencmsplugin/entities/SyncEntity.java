package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.sync.SyncAction;
import org.apache.chemistry.opencmis.client.api.CmisObject;

import java.io.File;
import java.io.IOException;

public class SyncEntity {

    private String vfsPath;
    private String rfsPath;
    private VirtualFile ideaVFile;
    private File realFile;
    private CmisObject vfsObject;
    private SyncAction syncAction;
    private boolean replaceExistingEntity;

    public Type getType() {
        Object test = this;
        if (test instanceof SyncFolder) {
            return Type.FOLDER;
        }
        else {
            return Type.FILE;
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
		if (!realFile.exists()) {
			try {
				if (isFolder()) {
					if (!realFile.mkdirs()) {
						System.out.println("The directories for " + getRfsPath() + " could not be created");
					}
				}
				else {
					if (realFile.createNewFile()) {
						System.out.println("The file " + getRfsPath() + " could not be created");
					}
				}
			}
			catch (IOException e) {
				System.out.println("There was an Exception creating the local file" + getRfsPath() + ": " + e + "\n" + e.getMessage());
			}
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
