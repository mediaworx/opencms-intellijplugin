/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * The Swing form used for the project level configuration of the OpenCms plugin. This form is contained in the
 * IntellIJ configuration (File > Settings > Project Settings > OpenCms Plugin).
 */
public class OpenCmsPluginConfigurationForm implements ActionListener, FocusListener {

	private static final Logger LOG = Logger.getInstance(OpenCmsPluginConfigurationForm.class);

	private JPanel rootComponent;
	private JPanel formPanel;
	private JCheckBox enabledCheckBox;
	private JTextField repository;
	private JTextField username;
	private JTextField password;
	private JTextField webappRoot;
	private JTextField defaultLocalVfsRoot;
	private JTextField moduleNamingScheme;
	private JComboBox defaultSyncMode;
	private JTextArea ignoredFiles;
	private JTextArea ignoredPaths;
	private JCheckBox usePluginConnectorCheckBox;
	private JTextField connectorUrl;
	private JCheckBox usePluginConnectorServiceCheckBox;
	private JTextField connectorServiceUrl;
	private JTextField manifestRoot;
	private JPanel connectorOptionsPanel;
	private JCheckBox pullMetaDataCheckbox;
	private JComboBox autoPublishMode;
	private JLabel pluginVersionLabel;
	private JPanel pullMetaDataOptionsPanel;
	private JCheckBox useMetaDateVariablesCheckbox;
	private JCheckBox useMetaIdVariablesCheckbox;
	private JTextField moduleZipTargetFolderPath;
	private JTextArea consultThePluginWikiTextArea;

	private static String pluginVersion = null;

	/**
	 * Creates a new project level configuration form and initializes listeners for form actions
	 */
	public OpenCmsPluginConfigurationForm() {
		formPanel.setVisible(false);
		enabledCheckBox.addActionListener(this);
		webappRoot.addFocusListener(this);
		defaultLocalVfsRoot.addFocusListener(this);
		moduleZipTargetFolderPath.addFocusListener(this);
		usePluginConnectorCheckBox.addActionListener(this);
		pullMetaDataCheckbox.addActionListener(this);

		if (pluginVersion == null) {
			PluginId pluginId = PluginId.getId("com.mediaworx.intellij.opencmsplugin");
			IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
			if (pluginDescriptor != null) {
				pluginVersion = pluginDescriptor.getVersion();
			}
			if (pluginVersion == null) {
				pluginVersion = "unknown";
			}
		}
		pluginVersionLabel.setText("V " + pluginVersion);
	}

	/**
	 * Returns the root component of the configuration form
	 * @return  the root component of the configuration form
	 */
	public JComponent getRootComponent() {
		return rootComponent;
	}

	/**
	 * Fills the configuration form with the given data
	 * @param data  the configuration data to be used to fill the form
	 */
	public void setData(OpenCmsPluginConfigurationData data) {
		enabledCheckBox.setSelected(data.isOpenCmsPluginEnabled());
		formPanel.setVisible(data.isOpenCmsPluginEnabled());
		FormTools.setConfiguredOrKeepDefault(repository, data.getRepository());
		FormTools.setConfiguredOrKeepDefault(username, data.getUsername());
		FormTools.setConfiguredOrKeepDefault(password, data.getPassword());
		FormTools.setConfiguredOrKeepDefault(webappRoot, data.getWebappRoot());
		FormTools.setConfiguredOrKeepDefault(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot());
		FormTools.setConfiguredOrKeepDefault(moduleNamingScheme, data.getModuleNamingScheme());

		switch (data.getDefaultSyncMode()) {
			case PUSH:
				defaultSyncMode.setSelectedIndex(0);
				break;
			case SYNC:
				defaultSyncMode.setSelectedIndex(1);
				break;
			case PULL:
				defaultSyncMode.setSelectedIndex(2);
		}

		switch (data.getAutoPublishMode()) {
			case OFF:
				autoPublishMode.setSelectedIndex(0);
				break;
			case FILECHANGE:
				autoPublishMode.setSelectedIndex(1);
				break;
			case ALL:
				autoPublishMode.setSelectedIndex(2);
		}

		FormTools.setConfiguredOrKeepDefault(ignoredFiles, data.getIgnoredFiles());
		FormTools.setConfiguredOrKeepDefault(ignoredPaths, data.getIgnoredPaths());

		FormTools.setConfiguredOrKeepDefault(moduleZipTargetFolderPath, data.getModuleZipTargetFolderPath());

		usePluginConnectorCheckBox.setSelected(data.isPluginConnectorEnabled());
		usePluginConnectorServiceCheckBox.setSelected(data.isPluginConnectorServiceEnabled());
		connectorOptionsPanel.setVisible(data.isPluginConnectorEnabled());
		FormTools.setConfiguredOrKeepDefault(connectorUrl, data.getConnectorUrl());
		FormTools.setConfiguredOrKeepDefault(connectorServiceUrl, data.getConnectorServiceUrl());
		pullMetaDataCheckbox.setSelected(data.isPullMetadataEnabled());
		pullMetaDataOptionsPanel.setVisible(data.isPullMetadataEnabled());
		FormTools.setConfiguredOrKeepDefault(manifestRoot, data.getManifestRoot());
		useMetaDateVariablesCheckbox.setSelected(data.isUseMetaDateVariablesEnabled());
		useMetaIdVariablesCheckbox.setSelected(data.isUseMetaIdVariablesEnabled());
	}


	/**
	 * Reads the configuration data from the form and sets it to the given data object
	 * @param data the configuration data object to be filled with the form data
	 */
	public void getData(OpenCmsPluginConfigurationData data) {
		data.setOpenCmsPluginEnabled(enabledCheckBox.isSelected());
		data.setRepository(repository.getText());
		data.setUsername(username.getText());
		data.setPassword(password.getText());
		data.setWebappRoot(webappRoot.getText());
		data.setDefaultLocalVfsRoot(defaultLocalVfsRoot.getText());
		data.setModuleNamingScheme(moduleNamingScheme.getText());
		data.setDefaultSyncMode(FormTools.getSyncModeFromComboBox(defaultSyncMode));
		data.setIgnoredFiles(ignoredFiles.getText());
		data.setIgnoredPaths(ignoredPaths.getText());
		data.setModuleZipTargetFolderPath(moduleZipTargetFolderPath.getText());
		data.setPluginConnectorEnabled(usePluginConnectorCheckBox.isSelected());
		data.setPluginConnectorServiceEnabled(usePluginConnectorServiceCheckBox.isSelected());
		data.setConnectorUrl(connectorUrl.getText());
		data.setConnectorServiceUrl(connectorServiceUrl.getText());
		data.setAutoPublishMode(FormTools.getAutoPublishModeFromCombobox(autoPublishMode));
		data.setPullMetadataEnabled(pullMetaDataCheckbox.isSelected());
		data.setManifestRoot(manifestRoot.getText());
		// data.setUseMetaVariablesEnabled(useMetaDateVariablesCheckbox.isSelected() && useMetaIdVariablesCheckbox.isSelected());
		data.setUseMetaDateVariablesEnabled(useMetaDateVariablesCheckbox.isSelected());
		data.setUseMetaIdVariablesEnabled(useMetaIdVariablesCheckbox.isSelected());
	}


	/**
	 * Checks if any configuration settings in the form have been modified since the last save
	 * @param data the last saved data object
	 * @return  <code>true</code> if the configuration form has been modified, <code>false</code> otherwise
	 */
	public boolean isModified(OpenCmsPluginConfigurationData data) {
		return
			isPluginActivationModified(data.isOpenCmsPluginEnabled()) ||
			FormTools.isTextFieldModified(repository, data.getRepository()) ||
			FormTools.isTextFieldModified(username, data.getUsername()) ||
			FormTools.isTextFieldModified(password, data.getPassword()) ||
			FormTools.isTextFieldModified(webappRoot, data.getWebappRoot()) ||
			FormTools.isTextFieldModified(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot()) ||
			FormTools.isTextFieldModified(moduleNamingScheme, data.getModuleNamingScheme()) ||
			!FormTools.getSyncModeFromComboBox(defaultSyncMode).equals(data.getDefaultSyncMode()) ||
			FormTools.isTextFieldModified(ignoredFiles, data.getIgnoredFiles()) ||
			FormTools.isTextFieldModified(ignoredPaths, data.getIgnoredPaths()) ||
			FormTools.isTextFieldModified(moduleZipTargetFolderPath, data.getModuleZipTargetFolderPath()) ||
			usePluginConnectorCheckBox.isSelected() != data.isPluginConnectorEnabled() ||
			usePluginConnectorServiceCheckBox.isSelected() != data.isPluginConnectorServiceEnabled() ||
			FormTools.isTextFieldModified(connectorUrl, data.getConnectorUrl()) ||
			FormTools.isTextFieldModified(connectorServiceUrl, data.getConnectorServiceUrl()) ||
			!FormTools.getAutoPublishModeFromCombobox(autoPublishMode).equals(data.getAutoPublishMode()) ||
			pullMetaDataCheckbox.isSelected() != data.isPullMetadataEnabled() ||
			FormTools.isTextFieldModified(manifestRoot, data.getManifestRoot()) ||
			useMetaDateVariablesCheckbox.isSelected() != data.isUseMetaDateVariablesEnabled() ||
			useMetaIdVariablesCheckbox.isSelected() != data.isUseMetaIdVariablesEnabled()
		;
	}

	/**
	 * Helper method to check if the plugin activation checkbox was modified
	 * @param wasActivated  the previous selection state of the plugin activation checkbox
	 * @return  <code>true</code> if the plugin activation was modified, <code>false</code> otherwise
	 */
	public boolean isPluginActivationModified(boolean wasActivated) {
		return enabledCheckBox.isSelected() != wasActivated;
	}

	/**
	 * does nothing
	 */
	private void createUIComponents() {
	}

	/**
	 * Event hanlder for form action, handles changes to checkboxes in the form
	 * @param e the action event provided by IntelliJ
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == enabledCheckBox) {
			formPanel.setVisible(enabledCheckBox.isSelected());
		}
		else if (source == usePluginConnectorCheckBox) {
			connectorOptionsPanel.setVisible(usePluginConnectorCheckBox.isSelected());
		}
		else  if (source == pullMetaDataCheckbox) {
			pullMetaDataOptionsPanel.setVisible(pullMetaDataCheckbox.isSelected());
		}
	}

	/**
	 * does nothing
	 * @param e the action event provided by IntelliJ
	 */
	public void focusGained(FocusEvent e) {
		// do nothing
	}

	/**
	 * Does some cleanup after fields containing paths lose focus
	 * @param e the action event provided by IntelliJ
	 */
	public void focusLost(FocusEvent e) {
		Object source = e.getSource();
		if (source == webappRoot) {
			FormTools.cleanupPathField(webappRoot, false);
		}
		else if (source == defaultLocalVfsRoot) {
			FormTools.cleanupPathField(defaultLocalVfsRoot, true);
		}
		else if (source == moduleZipTargetFolderPath) {
			FormTools.cleanupPathField(moduleZipTargetFolderPath, true);
		}
	}
}
