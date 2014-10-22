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

import com.intellij.openapi.module.Module;
import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.regex.Pattern;

/**
 * The Swing form used for the module level configuration of the OpenCms plugin. This form is included for all
 * IntelliJ modules under File > Project Structure > Project Settings > Modules > [Module Name] > Tab "OpenCms Module".
 */
public class OpenCmsModuleConfigurationForm implements ActionListener, FocusListener {

	private OpenCmsPluginConfigurationData config;
	private JPanel rootComponent;
	private JCheckBox isOpenCmsModuleCheckbox;
	private JRadioButton useProjectDefaultModuleNameRadioButton;
	private JRadioButton useModuleSpecificMoudleNameRadioButton;
	private JTextField moduleName;
	private JPanel formPanel;
	private JRadioButton useProjectDefaultVfsRootRadioButton;
	private JRadioButton useModuleSpecificVfsRootRadioButton;
	private JTextField localVfsRoot;
	private JRadioButton useProjectDefaultSyncModeRadioButton;
	private JRadioButton useModuleSpecificSyncModeRadioButton;
	private JComboBox syncMode;
	private JPanel moduleVersionPanel;
	private JCheckBox setSpecificModuleVersionCheckbox;
	private JTextField moduleVersion;


	/**
	 * Creates a new module level configuration form and initializes listeners for form actions
	 */
	public OpenCmsModuleConfigurationForm(OpenCmsPluginConfigurationData config, Module module) {
		this.config = config;
		formPanel.setVisible(false);
		isOpenCmsModuleCheckbox.addActionListener(this);
		useProjectDefaultModuleNameRadioButton.addActionListener(this);
		String defaultModuleName = config.getModuleNamingScheme().replaceAll(Pattern.quote("${modulename}"), module.getName());
		useProjectDefaultModuleNameRadioButton.setText("Use default (" + defaultModuleName + ")");
		useModuleSpecificMoudleNameRadioButton.addActionListener(this);
		if (config != null) {
			useProjectDefaultVfsRootRadioButton.setText("Use project default path (" + config.getDefaultLocalVfsRoot() + ")");
		}
		useProjectDefaultVfsRootRadioButton.addActionListener(this);
		useModuleSpecificVfsRootRadioButton.addActionListener(this);
		if (config != null) {
			String defaultSyncMode;
			switch (config.getDefaultSyncMode()) {
				case PUSH: defaultSyncMode = "PUSH"; break;
				case PULL: defaultSyncMode = "PULL"; break;
				default: defaultSyncMode = "SYNC";
			}
			useProjectDefaultSyncModeRadioButton.setText("Use project default sync mode (" + defaultSyncMode + ")");
		}
		useProjectDefaultSyncModeRadioButton.addActionListener(this);
		useModuleSpecificSyncModeRadioButton.addActionListener(this);
		localVfsRoot.addFocusListener(this);
		if (config != null) {
			moduleVersionPanel.setVisible(config.isPluginConnectorEnabled() && config.isPullMetadataEnabled());
		}
		setSpecificModuleVersionCheckbox.addActionListener(this);
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
	public void setData(OpenCmsModuleConfigurationData data) {
		if (data != null) {
			isOpenCmsModuleCheckbox.setSelected(data.isOpenCmsModuleEnabled());
			formPanel.setVisible(data.isOpenCmsModuleEnabled());

			if (data.isUseProjectDefaultModuleNameEnabled()) {
				useProjectDefaultModuleNameRadioButton.setSelected(true);
				useModuleSpecificMoudleNameRadioButton.setSelected(false);
				moduleName.setEnabled(false);
			}
			else {
				useProjectDefaultModuleNameRadioButton.setSelected(false);
				useModuleSpecificMoudleNameRadioButton.setSelected(true);
				moduleName.setEnabled(true);
			}
			FormTools.setConfiguredOrKeepDefault(moduleName, data.getModuleName());

			if (data.isUseProjectDefaultVfsRootEnabled()) {
				useProjectDefaultVfsRootRadioButton.setSelected(true);
				useModuleSpecificVfsRootRadioButton.setSelected(false);
				localVfsRoot.setEnabled(false);
			}
			else {
				useProjectDefaultVfsRootRadioButton.setSelected(false);
				useModuleSpecificVfsRootRadioButton.setSelected(true);
				localVfsRoot.setEnabled(true);
			}
			FormTools.setConfiguredOrKeepDefault(localVfsRoot, data.getLocalVfsRoot());

			if (data.isUseProjectDefaultSyncModeEnabled()) {
				useProjectDefaultSyncModeRadioButton.setSelected(true);
				useModuleSpecificSyncModeRadioButton.setSelected(false);
				syncMode.setEnabled(false);
			}
			else {
				useProjectDefaultSyncModeRadioButton.setSelected(false);
				useModuleSpecificSyncModeRadioButton.setSelected(true);
				syncMode.setEnabled(true);
			}
			if (data.getSyncMode() == SyncMode.PUSH) {
				syncMode.setSelectedIndex(0);
			}
			else if (data.getSyncMode() == SyncMode.SYNC) {
				syncMode.setSelectedIndex(1);
			}
			else if (data.getSyncMode() == SyncMode.PULL) {
				syncMode.setSelectedIndex(2);
			}

			if (config != null) {
				moduleVersionPanel.setVisible(config.isPluginConnectorEnabled() && config.isPullMetadataEnabled());
			}
			setSpecificModuleVersionCheckbox.setSelected(data.isSetSpecificModuleVersionEnabled());

			moduleVersion.setEnabled(data.isSetSpecificModuleVersionEnabled());
			moduleVersion.setText(data.getModuleVersion());
		}
	}


	/**
	 * Reads the configuration data from the form and sets it to the given data object
	 * @param data the configuration data object to be filled with the form data
	 */
	public void getData(OpenCmsModuleConfigurationData data) {
		data.setOpenCmsModuleEnabled(isOpenCmsModuleCheckbox.isSelected());
		if (useProjectDefaultModuleNameRadioButton.isSelected()) {
			data.setUseProjectDefaultModuleNameEnabled(true);
		}
		else {
			data.setUseProjectDefaultModuleNameEnabled(false);
		}
		data.setModuleName(moduleName.getText());
		if (useProjectDefaultVfsRootRadioButton.isSelected()) {
			data.setUseProjectDefaultVfsRootEnabled(true);
		}
		else {
			data.setUseProjectDefaultVfsRootEnabled(false);
		}
		data.setLocalVfsRoot(localVfsRoot.getText());
		if (useProjectDefaultSyncModeRadioButton.isSelected()) {
			data.setUseProjectDefaultSyncModeEnabled(true);
		}
		else {
			data.setUseProjectDefaultSyncModeEnabled(false);
		}
		data.setSyncMode(FormTools.getSyncModeFromComboBox(syncMode));

		data.setSetSpecificModuleVersionEnabled(setSpecificModuleVersionCheckbox.isSelected());
		data.setModuleVersion(moduleVersion.getText());
	}


	/**
	 * Checks if any configuration settings in the form have been modified since the last save
	 * @param data the last saved data object
	 * @return  <code>true</code> if the configuration form has been modified, <code>false</code> otherwise
	 */
	public boolean isModified(OpenCmsModuleConfigurationData data) {
		return isOpenCmsModuleCheckbox.isSelected() != data.isOpenCmsModuleEnabled() ||
				useProjectDefaultModuleNameRadioButton.isSelected() != data.isUseProjectDefaultModuleNameEnabled() ||
				FormTools.isTextFieldModified(moduleName, data.getModuleName()) ||
				useProjectDefaultVfsRootRadioButton.isSelected() != data.isUseProjectDefaultVfsRootEnabled() ||
				FormTools.isTextFieldModified(localVfsRoot, data.getLocalVfsRoot()) ||
				useProjectDefaultSyncModeRadioButton.isSelected() != data.isUseProjectDefaultSyncModeEnabled() ||
				!FormTools.getSyncModeFromComboBox(syncMode).equals(data.getSyncMode()) ||
				setSpecificModuleVersionCheckbox.isSelected() != data.isSetSpecificModuleVersionEnabled() ||
				FormTools.isTextFieldModified(moduleVersion, data.getModuleVersion())
		;
	}

	/**
	 * does nothing
	 */
	private void createUIComponents() {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == isOpenCmsModuleCheckbox) {
			formPanel.setVisible(isOpenCmsModuleCheckbox.isSelected());
		}
		if (source == useProjectDefaultModuleNameRadioButton && useProjectDefaultModuleNameRadioButton.isSelected()) {
			moduleName.setEnabled(false);
		}
		if (source == useModuleSpecificMoudleNameRadioButton && useModuleSpecificMoudleNameRadioButton.isSelected()) {
			moduleName.setEnabled(true);
		}
		if (source == useProjectDefaultVfsRootRadioButton && useProjectDefaultVfsRootRadioButton.isSelected()) {
			localVfsRoot.setEnabled(false);
		}
		if (source == useModuleSpecificVfsRootRadioButton && useModuleSpecificVfsRootRadioButton.isSelected()) {
			localVfsRoot.setEnabled(true);
		}
		if (source == useProjectDefaultSyncModeRadioButton && useProjectDefaultSyncModeRadioButton.isSelected()) {
			syncMode.setEnabled(false);
		}
		if (source == useModuleSpecificSyncModeRadioButton && useModuleSpecificSyncModeRadioButton.isSelected()) {
			syncMode.setEnabled(true);
		}
		if (source == setSpecificModuleVersionCheckbox) {
			moduleVersion.setEnabled(setSpecificModuleVersionCheckbox.isSelected());
		}
	}

	/**
	 * does nothing
	 */
	public void focusGained(FocusEvent e) {
		// do nothing
	}

	/**
	 * Does some cleanup after fields containing paths lose focus
	 * @param e the action event provided by IntelliJ
	 */
	public void focusLost(FocusEvent e) {
		JTextField textField = (JTextField)e.getSource();
		if (textField == localVfsRoot) {
			FormTools.clearPathField(localVfsRoot, true);
		}
	}
}
