package com.mediaworx.intellij.opencmsplugin.configuration;

public class ModuleExportPoint {

	private String vfsSource;
	private String rfsTarget;

	public ModuleExportPoint(String vfsSource, String rfsTarget) {
		this.vfsSource = vfsSource;
		this.rfsTarget = rfsTarget;
	}

	/**
	 * Returns the VFS resource path for this export point
	 * @return  the VFS resource path for this export point
	 */
	public String getVfsSource() {
		return vfsSource;
	}

	/**
	 * Returns the path in the real file system, relative to the webapp
	 * @return  the path in the real file system, relative to the webapp
	 */
	public String getRfsTarget() {
		return rfsTarget;
	}


	/**
	 * returns the export path for  the given resource relative to the webapp.
	 * @param resourcePath  path to the resource within the export point
	 * @return  export path for the resource relative to the webapp
	 */
	public String getTargetPathForVfsResource(String resourcePath) {
		if (!resourcePath.startsWith(vfsSource)) {
			return null;
		}
		return rfsTarget + resourcePath.substring(vfsSource.length());
	}
}
