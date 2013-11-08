package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OpenCmsPluginConfigurationForm implements ActionListener {

	private JPanel rootComponent;

	private JCheckBox activeCheckBox;
	private JTextField repository;
	private JTextField username;
	private JTextField password;
	private JTextField webappRoot;
	private JTextField defaultLocalVfsRoot;
	private JTextArea localModuleVfsRoots;
	private JComboBox defaultSyncMode;

	private JLabel moduleConfigurationHint;
	private JPanel formPanel;
	private JPanel basePanel;

//	private void createUIComponents() {
//	}

	public OpenCmsPluginConfigurationForm() {
		formPanel.setVisible(false);
		activeCheckBox.addActionListener(this);
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


	public void setData(OpenCmsPluginConfigurationData data) {
		activeCheckBox.setSelected(data.isOpenCmsPluginActive());
		if (data.isOpenCmsPluginActive()) {
			formPanel.setVisible(true);
		}
		else {
			formPanel.setVisible(false);
		}
		setConfiguredOrKeepDefault(repository, data.getRepository());
		setConfiguredOrKeepDefault(username, data.getUsername());
		setConfiguredOrKeepDefault(password, data.getPassword());
		setConfiguredOrKeepDefault(webappRoot, data.getWebappRoot());
		setConfiguredOrKeepDefault(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot());
		setConfiguredOrKeepDefault(localModuleVfsRoots, data.getLocalModuleVfsRoots());
		if (data.getDefaultSyncMode() == SyncMode.PUSH) {
			defaultSyncMode.setSelectedIndex(0);
		}
		else if (data.getDefaultSyncMode() == SyncMode.SYNC) {
			defaultSyncMode.setSelectedIndex(1);
		}
		else if (data.getDefaultSyncMode() == SyncMode.PULL) {
			defaultSyncMode.setSelectedIndex(2);
		}
	}


	public void getData(OpenCmsPluginConfigurationData data) {
		data.setOpenCmsPluginActive(activeCheckBox.isSelected());
		data.setRepository(repository.getText());
		data.setUsername(username.getText());
		data.setPassword(password.getText());
		data.setWebappRoot(webappRoot.getText());
		data.setDefaultLocalVfsRoot(defaultLocalVfsRoot.getText());
		data.setLocalModuleVfsRoots(localModuleVfsRoots.getText());
		data.setDefaultSyncMode(FormTools.getSyncModeFromComboBox(defaultSyncMode));
		data.initModuleConfiguration();
	}


	public boolean isModified(OpenCmsPluginConfigurationData data) {
		if (activeCheckBox.isSelected() != data.isOpenCmsPluginActive()) return true;
		if (repository.getText() != null ? !repository.getText().equals(data.getRepository()) : data.getRepository() != null) return true;
		if (username.getText() != null ? !username.getText().equals(data.getUsername()) : data.getUsername() != null) return true;
		if (password.getText() != null ? !password.getText().equals(data.getPassword()) : data.getPassword() != null) return true;
		if (webappRoot.getText() != null ? !webappRoot.getText().equals(data.getWebappRoot()) : data.getWebappRoot() != null) return true;
		if (defaultLocalVfsRoot.getText() != null ? !defaultLocalVfsRoot.getText().equals(data.getDefaultLocalVfsRoot()) : data.getDefaultLocalVfsRoot() != null) return true;
		if (localModuleVfsRoots.getText() != null ? !localModuleVfsRoots.getText().equals(data.getLocalModuleVfsRoots()) : data.getLocalModuleVfsRoots() != null) return true;
		if (defaultSyncMode.getSelectedItem() != null ? !FormTools.getSyncModeFromComboBox(defaultSyncMode).equals(data.getDefaultSyncMode()) : data.getDefaultSyncMode() != null) return true;
		return false;
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == activeCheckBox) {
			if (activeCheckBox.isSelected()) {
				formPanel.setVisible(true);
			}
			else {
				formPanel.setVisible(false);
			}
		}

	}
}
