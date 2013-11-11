package com.mediaworx.intellij.opencmsplugin.configuration.module;

import com.mediaworx.intellij.opencmsplugin.configuration.FormTools;
import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OpenCmsModuleConfigurationForm implements ActionListener {

	private JPanel rootComponent;
	private JCheckBox isOpenCmsModule;
	private JTextField moduleName;
	private JPanel formPanel;
	private JRadioButton useProjectDefaultVfsRootRadioButton;
	private JRadioButton useModuleSpecificVfsRootRadioButton;
	private JTextField localVfsRoot;
	private JRadioButton useProjectDefaultSyncModeRadioButton;
	private JRadioButton useModuleSpecificSyncModeRadioButton;
	private JComboBox syncMode;


	public OpenCmsModuleConfigurationForm() {
		formPanel.setVisible(false);
		isOpenCmsModule.addActionListener(this);
		useProjectDefaultVfsRootRadioButton.addActionListener(this);
		useModuleSpecificVfsRootRadioButton.addActionListener(this);
		useProjectDefaultSyncModeRadioButton.addActionListener(this);
		useModuleSpecificSyncModeRadioButton.addActionListener(this);
	}

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

	private void setConfiguredOrKeepDefault(JTextComponent field, String configured) {
		if (configured != null && configured.length() > 0 && field != null) {
			field.setText(configured);
		}
	}

	public void setData(OpenCmsModuleConfigurationData data) {
		isOpenCmsModule.setSelected(data.isOpenCmsModuleEnabled());
		if (data.isOpenCmsModuleEnabled()) {
			formPanel.setVisible(true);
		}
		else {
			formPanel.setVisible(false);
		}
		setConfiguredOrKeepDefault(moduleName, data.getModuleName());

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
		setConfiguredOrKeepDefault(localVfsRoot, data.getLocalVfsRoot());

		if (data.isUseProjectDefaultSyncModeEnabled()) {
			useProjectDefaultSyncModeRadioButton.setSelected(true);
			useModuleSpecificSyncModeRadioButton.setSelected(false);
			syncMode.setEnabled(false);
		}
		else {
			useProjectDefaultSyncModeRadioButton.setSelected(false);
			useModuleSpecificSyncModeRadioButton.setSelected(true);
			syncMode.setEnabled(true);
			if (data.getSyncMode() == SyncMode.PUSH) {
				syncMode.setSelectedIndex(0);
			}
			else if (data.getSyncMode() == SyncMode.SYNC) {
				syncMode.setSelectedIndex(1);
			}
			else if (data.getSyncMode() == SyncMode.PULL) {
				syncMode.setSelectedIndex(2);
			}
		}
	}


	public void getData(OpenCmsModuleConfigurationData data) {
		data.setOpenCmsModuleEnabled(isOpenCmsModule.isSelected());
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
	}


	public boolean isModified(OpenCmsModuleConfigurationData data) {
		if (isOpenCmsModule.isSelected() != data.isOpenCmsModuleEnabled()) return true;
		if (moduleName.getText() != null ? !moduleName.getText().equals(data.getModuleName()) : data.getModuleName() != null) return true;
		if (useProjectDefaultVfsRootRadioButton == null || (useProjectDefaultVfsRootRadioButton.isSelected() && !data.isUseProjectDefaultVfsRootEnabled())) return true;
		if (localVfsRoot.getText() != null ? !localVfsRoot.getText().equals(data.getLocalVfsRoot()) : data.getLocalVfsRoot() != null) return true;
		if (useProjectDefaultSyncModeRadioButton == null || (useProjectDefaultSyncModeRadioButton.isSelected() && !data.isUseProjectDefaultSyncModeEnabled())) return true;
		if (syncMode.getSelectedItem() != null ? !FormTools.getSyncModeFromComboBox(syncMode).equals(data.getSyncMode()) : data.getSyncMode() != null) return true;
		return false;
	}

	private void createUIComponents() {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == isOpenCmsModule) {
			if (isOpenCmsModule.isSelected()) {
				formPanel.setVisible(true);
			}
			else {
				formPanel.setVisible(false);
			}
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
	}
}
