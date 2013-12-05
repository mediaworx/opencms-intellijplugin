package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class OpenCmsModuleConfigurationForm implements ActionListener, FocusListener {

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
		localVfsRoot.addFocusListener(this);
	}

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

	public void setData(OpenCmsModuleConfigurationData data) {
		isOpenCmsModule.setSelected(data.isOpenCmsModuleEnabled());
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
		return
				isOpenCmsModule.isSelected() != data.isOpenCmsModuleEnabled() ||
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
		if (source == isOpenCmsModule) {
			formPanel.setVisible(isOpenCmsModule.isSelected());
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
