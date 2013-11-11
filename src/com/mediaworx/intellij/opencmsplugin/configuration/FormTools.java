package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class FormTools {

	public static SyncMode getSyncModeFromComboBox(JComboBox comboBox) {
		String syncModeStr = (String)comboBox.getSelectedItem();
		if (syncModeStr == null || syncModeStr.length() == 0) {
			syncModeStr = SyncMode.PUSH.name();
		}
		syncModeStr = syncModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		return SyncMode.valueOf(syncModeStr);
	}

	public static void setConfiguredOrKeepDefault(JTextComponent field, String configured) {
		if (configured != null && configured.length() > 0 && field != null) {
			field.setText(configured);
		}
	}

	public static void clearPathField(JTextComponent field, boolean removeLeadingSlash) {
		String fieldValue = field.getText();
		fieldValue = fieldValue.replace("\\", "/");
		int startIndex = 0;
		int endIndex = fieldValue.length();
		if (removeLeadingSlash && fieldValue.startsWith("/")) {
			startIndex = 1;
		}
		if (fieldValue.endsWith("/")) {
			endIndex = endIndex - 1;
		}
		if (startIndex != 0 || endIndex != fieldValue.length()) {
			fieldValue = fieldValue.substring(startIndex, endIndex);
		}
		field.setText(fieldValue);
	}
}
