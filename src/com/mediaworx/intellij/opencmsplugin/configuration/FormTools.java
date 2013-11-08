package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;

public class FormTools {

	public static SyncMode getSyncModeFromComboBox(JComboBox comboBox) {
		String syncModeStr = (String)comboBox.getSelectedItem();
		if (syncModeStr == null || syncModeStr.length() == 0) {
			syncModeStr = SyncMode.PUSH.name();
		}
		syncModeStr = syncModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		return SyncMode.valueOf(syncModeStr);
	}

}
