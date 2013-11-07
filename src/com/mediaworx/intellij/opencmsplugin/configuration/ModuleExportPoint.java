package com.mediaworx.intellij.opencmsplugin.configuration;

public class ModuleExportPoint {

	private String vfsSource;
	private String rfsTarget;

	public ModuleExportPoint(String vfsSource, String rfsTarget) {
		this.vfsSource = vfsSource;
		this.rfsTarget = rfsTarget;
	}

	public String getVfsSource() {
		return vfsSource;
	}

	public String getRfsTarget() {
		return rfsTarget;
	}
}
