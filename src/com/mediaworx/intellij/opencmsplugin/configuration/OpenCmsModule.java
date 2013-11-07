package com.mediaworx.intellij.opencmsplugin.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OpenCmsModule {

	private String name;

	private String localVfsRoot;

	private List<ModuleExportPoint> exportPoints = new ArrayList<ModuleExportPoint>();


	public OpenCmsModule(String name) {
		this.name = name;
	}

	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public void setLocalVfsRoot(String localVfsRoot) {
		this.localVfsRoot = localVfsRoot;
	}

	public void addExportPoint(String vfsSource, String rfsTarget) {
		exportPoints.add(new ModuleExportPoint(vfsSource, rfsTarget));
	}

	public List<ModuleExportPoint> getExportPoints() {
		return exportPoints;
	}

}
