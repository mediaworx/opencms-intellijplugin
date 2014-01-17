package com.mediaworx.intellij.opencmsplugin.sync;

import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;
import java.util.Collection;

public class SyncList extends ArrayList<SyncEntity> {

	private boolean syncModuleMetaData = false;
	private Collection<OpenCmsModule> ocmsModules;
	private boolean pullMetaDataOnly = false;

	public boolean isPullMetaDataOnly() {
		return pullMetaDataOnly;
	}

	public void setPullMetaDataOnly(boolean pullMetaDataOnly) {
		this.pullMetaDataOnly = pullMetaDataOnly;
	}

	public boolean isSyncModuleMetaData() {
		return syncModuleMetaData;
	}

	public void setSyncModuleMetaData(boolean syncModuleMetaData) {
		this.syncModuleMetaData = syncModuleMetaData;
	}

	public Collection<OpenCmsModule> getOcmsModules() {
		return ocmsModules;
	}

	public void addOcmsModule(OpenCmsModule ocmsModule) {
		if (ocmsModules == null) {
			ocmsModules = new ArrayList<OpenCmsModule>();
		}
		if (!ocmsModules.contains(ocmsModule)) {
			ocmsModules.add(ocmsModule);
		}
	}
}
