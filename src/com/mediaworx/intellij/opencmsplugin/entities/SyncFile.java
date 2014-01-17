package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.SyncAction;
import com.mediaworx.opencms.moduleutils.manifestgenerator.OpenCmsModuleManifestGenerator;
import org.apache.chemistry.opencmis.client.api.CmisObject;

import java.util.Date;

public class SyncFile extends SyncEntity {

	private Date lastChangeDate;

	public SyncFile(OpenCmsModule ocmsModule, String vfsPath, VirtualFile ideaVFile, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		super(ocmsModule, vfsPath, ideaVFile, vfsObject, syncAction, replaceExistingEntity);
	}

	@Override
	public Type getType() {
		return Type.FILE;
	}

	public String getMetaInfoFilePath() {
		return OpenCmsModuleManifestGenerator.getMetaInfoPath(getOcmsModule().getManifestRoot(), getVfsPath(), false);
	}

	public Date getLastChangeDate() {
		return (Date) lastChangeDate.clone();
	}

	public void setLastChangeDate(Date lastChangeDate) {
		this.lastChangeDate = (Date)lastChangeDate.clone();
	}
}
