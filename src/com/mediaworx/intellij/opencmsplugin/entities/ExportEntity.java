package com.mediaworx.intellij.opencmsplugin.entities;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 24.01.13
 * Time: 14:29
 * To change this template use File | Settings | File Templates.
 */
public class ExportEntity {

	public ExportEntity() {
	}

    String sourcePath;
	String targetPath;
    String vfsPath;
    String destination;

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

    public String getVfsPath() {
        return vfsPath;
    }

    public void setVfsPath(String vfsPath) {
        this.vfsPath = vfsPath;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
