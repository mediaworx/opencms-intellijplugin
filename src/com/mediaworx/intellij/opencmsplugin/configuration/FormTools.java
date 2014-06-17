/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.connector.AutoPublishMode;
import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;

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

	public static boolean isTextFieldModified(JTextComponent textField, String configuredText) {
		if (textField.getText() == null) {
			return configuredText != null;
		}
		else {
			return !textField.getText().equals(configuredText);
		}
	}

}
