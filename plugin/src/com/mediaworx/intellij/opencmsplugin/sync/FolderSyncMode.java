package com.mediaworx.intellij.opencmsplugin.sync;

/**
 * The FolderSyncMode describes the way folders are treated during sync. If a folder exists on both VFS and RFS, its
 * FolderSyncMode is AUTO, so all child resources will be checked for existence; if it exists only on the RFS, the sync
 * mode is PUSH (and hence all child resources can be pushed without checking if they exist on the VFS); if the folder
 * exists only on the VFS the sync mode is PULL (and hence all child resources can be pulled without checking if they
 * exist on the RFS).
 */
public enum FolderSyncMode {
	AUTO,
	PUSH,
	PULL
}
