package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.AutoPublishMode;
import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class FormTools {

	public static SyncMode getSyncModeFromComboBox(JComboBox comboBox) {
		String syncModeStr = (String)comboBox.getSelectedItem();
		if (syncModeStr == null || syncModeStr.length() == 0) {
			return SyncMode.SYNC;
		}
		syncModeStr = syncModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		return SyncMode.valueOf(syncModeStr);
	}

	public static AutoPublishMode getAutoPublishModeFromCombobox(JComboBox comboBox) {
		String autoPublishModeStr = (String)comboBox.getSelectedItem();
		if (autoPublishModeStr == null || autoPublishModeStr.length() == 0) {
			return AutoPublishMode.FILECHANGE;
		}
		autoPublishModeStr = autoPublishModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		return AutoPublishMode.valueOf(autoPublishModeStr);
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

	public static boolean isTextFieldModified(JTextField textField, String configuredText) {
		if (textField.getText() == null) {
			return configuredText != null;
		}
		else {
			return !textField.getText().equals(configuredText);
		}
	}

}
