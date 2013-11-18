package com.mediaworx.intellij.opencmsplugin.entities;

import java.util.List;

public class SyncFolder extends SyncEntity {

	public static final String METAINFO_FILE_SUFFIX = ".folder.xml";

	private List<SyncEntity> children;

	@Override
	public Type getType() {
		return Type.FOLDER;
	}

	@Override
	public String getMetaInfoFileSuffix() {
		return METAINFO_FILE_SUFFIX;
	}

	public List<SyncEntity> getChildren() {
		return children;
	}

	public void setChildren(List<SyncEntity> children) {
		this.children = children;
	}

	public String getMetaInfoFolderPath() {
		return getOcmsModule().getManifestRoot() + getVfsPath();
	}


}
