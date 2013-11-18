package com.mediaworx.intellij.opencmsplugin.entities;

public class ExportEntity {

	public ExportEntity() {
	}

	String sourcePath;
	String targetPath;
	String vfsPath;
	String destination;
	boolean toBeDeleted;

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

	public boolean isToBeDeleted() {
		return toBeDeleted;
	}

	public void setToBeDeleted(boolean toBeDeleted) {
		this.toBeDeleted = toBeDeleted;
	}

	@Override
	public String toString() {
		return "ExportEntity {\n" +
				"  sourcePath:  " + sourcePath + "\n" +
				"  targetPath:  " + targetPath + "\n" +
				"  vfsPath:     " + vfsPath + "\n" +
				"  destination: " + destination + "\n" +
				"  toBeDeleted: " + toBeDeleted + "\n" +
				"} " + super.toString();
	}
}
