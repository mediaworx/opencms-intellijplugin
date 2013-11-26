package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.SyncAction;
import org.apache.chemistry.opencmis.client.api.CmisObject;

public class SyncFolder extends SyncEntity {

	public static final String METAINFO_FILE_SUFFIX = ".folder.xml";

	public SyncFolder(OpenCmsModule ocmsModule, String vfsPath, VirtualFile ideaVFile, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		super(ocmsModule, vfsPath, ideaVFile, vfsObject, syncAction, replaceExistingEntity);
	}

	@Override
	public Type getType() {
		return Type.FOLDER;
	}

	@Override
	public String getMetaInfoFileSuffix() {
		return METAINFO_FILE_SUFFIX;
	}

	public String getMetaInfoFolderPath() {
		return getOcmsModule().getManifestRoot() + getVfsPath();
	}


}
