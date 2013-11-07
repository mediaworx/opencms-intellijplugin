package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class OpenCmsPluginConfigurationForm {

    private JPanel rootComponent;

	private JCheckBox activeCheckBox;
	private JTextField repository;
    private JTextField username;
    private JTextField password;
    private JTextField webappRoot;
    private JTextArea localModuleVfsRoots;
	private JComboBox syncMode;

	private JLabel activeCheckBoxLabel;
	private JLabel repositoryLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
	private JLabel webappRootLabel;
    private JLabel moduleConfigurationLabel;
	private JLabel moduleConfigurationHint;
	private JLabel syncModeLabel;

//    private void createUIComponents() {
//    }

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
		setConfiguredOrKeepDefault(repository, data.getRepository());
		setConfiguredOrKeepDefault(username, data.getUsername());
		setConfiguredOrKeepDefault(password, data.getPassword());
        setConfiguredOrKeepDefault(webappRoot, data.getWebappRoot());
        setConfiguredOrKeepDefault(localModuleVfsRoots, data.getLocalModuleVfsRoots());
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


	public void getData(OpenCmsPluginConfigurationData data) {
		data.setOpenCmsPluginActive(activeCheckBox.isSelected());
		data.setRepository(repository.getText());
		data.setUsername(username.getText());
		data.setPassword(password.getText());
        data.setWebappRoot(webappRoot.getText());
        data.setLocalModuleVfsRoots(localModuleVfsRoots.getText());
		data.setSyncMode((String)syncMode.getSelectedItem());
		data.initModuleConfiguration();
	}


	public boolean isModified(OpenCmsPluginConfigurationData data) {
		if (activeCheckBox.isSelected() != data.isOpenCmsPluginActive()) return true;
		if (repository.getText() != null ? !repository.getText().equals(data.getRepository()) : data.getRepository() != null) return true;
		if (username.getText() != null ? !username.getText().equals(data.getUsername()) : data.getUsername() != null) return true;
		if (password.getText() != null ? !password.getText().equals(data.getPassword()) : data.getPassword() != null) return true;
        if (webappRoot.getText() != null ? !webappRoot.getText().equals(data.getWebappRoot()) : data.getWebappRoot() != null) return true;
        if (localModuleVfsRoots.getText() != null ? !localModuleVfsRoots.getText().equals(data.getLocalModuleVfsRoots()) : data.getLocalModuleVfsRoots() != null) return true;
		if (syncMode.getSelectedItem() != null ? !syncMode.getSelectedItem().equals(data.getSyncMode()) : data.getSyncMode() != null) return true;
		return false;
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}
}
