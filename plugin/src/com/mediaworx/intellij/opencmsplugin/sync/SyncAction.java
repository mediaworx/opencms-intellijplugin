package com.mediaworx.intellij.opencmsplugin.sync;

/**
* Created with IntelliJ IDEA.
* User: widmann
* Date: 06.11.13
* Time: 15:21
* To change this template use File | Settings | File Templates.
*/
public enum SyncAction {
	PUSH("PUSH"),
	PULL("PULL"),
	DELETE_RFS("DELETE FROM RFS"),
	DELETE_VFS("DELETE FROM VFS");

	private String description;

	private SyncAction(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isDeleteAction() {
		return this == DELETE_RFS || this == DELETE_VFS;
	}
}
