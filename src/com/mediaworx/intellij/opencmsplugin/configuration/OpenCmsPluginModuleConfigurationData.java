package com.mediaworx.intellij.opencmsplugin.configuration;

import java.io.File;

public class OpenCmsPluginModuleConfigurationData {

    private String syncRootLocal;

    public String getSyncRootLocal() {
	    return stripTrailingSeparator(syncRootLocal);
    }

    public void setSyncRootLocal(String syncRootLocal) {
        this.syncRootLocal = syncRootLocal;
    }

	private String stripTrailingSeparator(String s) {
		if (s != null && s.endsWith(File.separator)) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}
}
