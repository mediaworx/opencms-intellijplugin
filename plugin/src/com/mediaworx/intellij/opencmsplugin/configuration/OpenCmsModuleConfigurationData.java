package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;

public class OpenCmsModuleConfigurationData {

	private boolean openCmsModuleEnabled = false;
    private String moduleName;
	private boolean useProjectDefaultVfsRootEnabled = true;
    private String localVfsRoot;
	private boolean useProjectDefaultSyncModeEnabled = true;
	private SyncMode syncMode;

	public boolean isOpenCmsModuleEnabled() {
		return openCmsModuleEnabled;
	}

	public void setOpenCmsModuleEnabled(boolean openCmsModule) {
		this.openCmsModuleEnabled = openCmsModule;
	}

	public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

	public boolean isUseProjectDefaultVfsRootEnabled() {
		return useProjectDefaultVfsRootEnabled;
	}

	public void setUseProjectDefaultVfsRootEnabled(boolean useProjectDefaultVfsRootEnabled) {
		this.useProjectDefaultVfsRootEnabled = useProjectDefaultVfsRootEnabled;
	}

	public String getLocalVfsRoot() {
        return localVfsRoot;
    }

    public void setLocalVfsRoot(String localVfsRoot) {
        this.localVfsRoot = localVfsRoot;
    }

	public boolean isUseProjectDefaultSyncModeEnabled() {
		return useProjectDefaultSyncModeEnabled;
	}

	public void setUseProjectDefaultSyncModeEnabled(boolean useProjectDefaultSyncModeEnabled) {
		this.useProjectDefaultSyncModeEnabled = useProjectDefaultSyncModeEnabled;
	}

	public SyncMode getSyncMode() {
		return syncMode != null ? syncMode : SyncMode.PUSH;
	}

	public void setSyncMode(SyncMode syncMode) {
		this.syncMode = syncMode;
	}
}
