package com.mediaworx.intellij.opencmsplugin.entities;

import java.util.Date;

public class SyncFile extends SyncEntity {

	public static final String METAINFO_FILE_SUFFIX = ".xml";

	private Date lastChangeDate;

	@Override
	public Type getType() {
		return Type.FILE;
	}

	@Override
	public String getMetaInfoFileSuffix() {
		return METAINFO_FILE_SUFFIX;
	}

	public Date getLastChangeDate() {
		return (Date) lastChangeDate.clone();
	}

	public void setLastChangeDate(Date lastChangeDate) {
		this.lastChangeDate = (Date) lastChangeDate.clone();
	}
}
