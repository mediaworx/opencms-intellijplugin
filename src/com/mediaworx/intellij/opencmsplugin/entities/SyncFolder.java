package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.SyncAction;
import com.mediaworx.opencms.moduleutils.manifestgenerator.OpenCmsModuleManifestGenerator;
import org.apache.chemistry.opencmis.client.api.CmisObject;

public class SyncFolder extends SyncEntity {

	public SyncFolder(OpenCmsModule ocmsModule, String vfsPath, VirtualFile ideaVFile, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		super(ocmsModule, vfsPath, ideaVFile, vfsObject, syncAction, replaceExistingEntity);
	}

	@Override
	public Type getType() {
		return Type.FOLDER;
	}

	public String getMetaInfoFilePath() {
		return OpenCmsModuleManifestGenerator.getMetaInfoPath(getOcmsModule().getManifestRoot(), getVfsPath(), true);
	}

	public String getMetaInfoFolderPath() {
		return getOcmsModule().getManifestRoot() + getVfsPath();
	}


}
