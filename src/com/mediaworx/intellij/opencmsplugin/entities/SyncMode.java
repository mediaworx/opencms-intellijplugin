package com.mediaworx.intellij.opencmsplugin.entities;

import java.util.HashMap;

/**
 * represents the SyncMode configured by the user (PUSH, SYNC, PULL)
 */
public enum SyncMode {

	PUSH("PUSH"),
	SYNC("SYNC"),
	PULL("PULL");

	/**
	 * HashMap used to lookup the PreviewType during creation of PreviewTypes from Strings
	 * @see #fromString(String)
	 */
	private static final HashMap<String, SyncMode> MODES = new HashMap<String, SyncMode>();
	static {
		MODES.put("PUSH", SyncMode.PUSH);
		MODES.put("SYNC", SyncMode.SYNC);
		MODES.put("PULL", SyncMode.PULL);
	}

	private final String syncModeStr;

	private SyncMode(final String syncModeStr) {
		this.syncModeStr = syncModeStr;
	}

	/** creates a new sync mode from a String ("PUSH", "SYNC" or "PULL") */
	public static SyncMode fromString(String syncModeStr) throws IllegalArgumentException {
		syncModeStr = syncModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		if (!MODES.containsKey(syncModeStr)) {
			throw new IllegalArgumentException(syncModeStr + " is not a legal previewType");
		}
		else {
			return MODES.get(syncModeStr);
		}
	}

	/**
	 * returns the String representation of the sync mode
	 * @return String representation of the sync mode, "PUSH", "SYNC" or "PULL"
	 */
	@Override
	public String toString() {
		return syncModeStr;
	}

}
