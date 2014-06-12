package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class OpenCmsModuleConfigurationForm implements ActionListener, FocusListener {

	private OpenCmsPluginConfigurationData config;
	private JPanel rootComponent;
	private JCheckBox isOpenCmsModuleCheckbox;
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


	public OpenCmsModuleConfigurationForm(OpenCmsPluginConfigurationData config) {
		this.config = config;
		formPanel.setVisible(false);
		isOpenCmsModuleCheckbox.addActionListener(this);
		useProjectDefaultVfsRootRadioButton.addActionListener(this);
		useModuleSpecificVfsRootRadioButton.addActionListener(this);
		useProjectDefaultSyncModeRadioButton.addActionListener(this);
		useModuleSpecificSyncModeRadioButton.addActionListener(this);
		localVfsRoot.addFocusListener(this);
		if (config != null) {
			moduleVersionPanel.setVisible(config.isPluginConnectorEnabled() && config.isPullMetadataEnabled());
		}
		setSpecificModuleVersionCheckbox.addActionListener(this);
	}

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

	public void setData(OpenCmsModuleConfigurationData data) {
		if (data != null) {
			isOpenCmsModuleCheckbox.setSelected(data.isOpenCmsModuleEnabled());
			formPanel.setVisible(data.isOpenCmsModuleEnabled());
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


	public void getData(OpenCmsModuleConfigurationData data) {
		data.setOpenCmsModuleEnabled(isOpenCmsModuleCheckbox.isSelected());
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


	public boolean isModified(OpenCmsModuleConfigurationData data) {
		return
				isOpenCmsModuleCheckbox.isSelected() != data.isOpenCmsModuleEnabled() ||
				FormTools.isTextFieldModified(moduleName, data.getModuleName()) ||
				useProjectDefaultVfsRootRadioButton.isSelected() && !data.isUseProjectDefaultVfsRootEnabled() ||
				FormTools.isTextFieldModified(localVfsRoot, data.getLocalVfsRoot()) ||
				useProjectDefaultSyncModeRadioButton.isSelected() && !data.isUseProjectDefaultSyncModeEnabled() ||
				!FormTools.getSyncModeFromComboBox(syncMode).equals(data.getSyncMode())
		;
	}

	private void createUIComponents() {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == isOpenCmsModuleCheckbox) {
			formPanel.setVisible(isOpenCmsModuleCheckbox.isSelected());
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

	public void focusGained(FocusEvent e) {
		// do nothing
	}

	public void focusLost(FocusEvent e) {
		JTextField textField = (JTextField)e.getSource();
		if (textField == localVfsRoot) {
			FormTools.clearPathField(localVfsRoot, true);
		}
	}
}
