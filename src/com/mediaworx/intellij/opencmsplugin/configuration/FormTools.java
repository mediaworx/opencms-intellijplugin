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

/**
 * Some helper methods used by configuration forms
 */
public class FormTools {

	/**
	 * Reads the content of the given combo box and returns the corresponding sync mode
	 * @param comboBox the combo box to check
	 * @return  the sync mode that corresponds to the value selected in the combo box
	 */
	public static SyncMode getSyncModeFromComboBox(JComboBox comboBox) {
		String syncModeStr = (String)comboBox.getSelectedItem();
		if (syncModeStr == null || syncModeStr.length() == 0) {
			return SyncMode.SYNC;
		}
		syncModeStr = syncModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		return SyncMode.valueOf(syncModeStr);
	}

	/**
	 * Reads the content of the given combo box and returns the corresponding auto publish mode
	 * @param comboBox the combo box to check
	 * @return the auto publish mode that corresponds to the value selected in the combo box
	 */
	public static AutoPublishMode getAutoPublishModeFromCombobox(JComboBox comboBox) {
		String autoPublishModeStr = (String)comboBox.getSelectedItem();
		if (autoPublishModeStr == null || autoPublishModeStr.length() == 0) {
			return AutoPublishMode.FILECHANGE;
		}
		autoPublishModeStr = autoPublishModeStr.replaceAll("([A-Z]+) ?.*", "$1");
		return AutoPublishMode.valueOf(autoPublishModeStr);
	}


	/**
	 * If a value is configured (not null AND not empty), it is set as text of the field; if the configured value is
	 * null or empty, the field is unchanged and so the default value is kept
	 * @param field the text field
	 * @param configured the configured value (may be null or empty)
	 */
	public static void setConfiguredOrKeepDefault(JTextComponent field, String configured) {
		if (configured != null && configured.length() > 0 && field != null) {
			field.setText(configured);
		}
	}

	/**
	 * Does some cleanup in path fields. Backslashes are replaced by forward slashes, if <code>removeLeadingSlash</code>
	 * is <code>true</code>, leading slashes are removed, trailing slashes are always removed
	 * @param field the path field
	 * @param removeLeadingSlash <code>true</code>, if the leading slash should be removed, <code>false</code> otherwise
	 */
	public static void cleanupPathField(JTextComponent field, boolean removeLeadingSlash) {
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

	/**
	 * checks if the textfield's content differs from the configured text
	 * @param textField the text field to check
	 * @param configuredText the configured text to compare with the text field content
	 * @return  <code>true</code> if the configured text is not equal to the text field content, <code>false</code>
	 *          otherwise
	 */
	public static boolean isTextFieldModified(JTextComponent textField, String configuredText) {
		if (textField.getText() == null) {
			return configuredText != null;
		}
		else {
			return !textField.getText().equals(configuredText);
		}
	}

}
