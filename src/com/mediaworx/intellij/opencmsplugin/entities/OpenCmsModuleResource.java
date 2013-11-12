package com.mediaworx.intellij.opencmsplugin.entities;

import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

public class OpenCmsModuleResource {

	private OpenCmsModule openCmsModule;
	private String resourcePath;

	public OpenCmsModuleResource(OpenCmsModule openCmsModule, String resourcePath) {
		this.openCmsModule = openCmsModule;
		this.resourcePath = resourcePath;
	}

	public OpenCmsModule getOpenCmsModule() {
		return openCmsModule;
	}

	public String getResourcePath() {
		return resourcePath.replaceFirst("/$", ""); // strip trailing slash
	}

}
