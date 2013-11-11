package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class OpenCmsPluginConfigurationForm implements ActionListener, FocusListener {

	private JPanel rootComponent;

	private JCheckBox activeCheckBox;
	private JTextField repository;
	private JTextField username;
	private JTextField password;
	private JTextField webappRoot;
	private JTextField defaultLocalVfsRoot;
	private JComboBox defaultSyncMode;

	private JPanel formPanel;

//	private void createUIComponents() {
//	}

	public OpenCmsPluginConfigurationForm() {
		formPanel.setVisible(false);
		activeCheckBox.addActionListener(this);
		webappRoot.addFocusListener(this);
		defaultLocalVfsRoot.addFocusListener(this);
	}

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

	public void setData(OpenCmsPluginConfigurationData data) {
		activeCheckBox.setSelected(data.isOpenCmsPluginActive());
		if (data.isOpenCmsPluginActive()) {
			formPanel.setVisible(true);
		}
		else {
			formPanel.setVisible(false);
		}
		FormTools.setConfiguredOrKeepDefault(repository, data.getRepository());
		FormTools.setConfiguredOrKeepDefault(username, data.getUsername());
		FormTools.setConfiguredOrKeepDefault(password, data.getPassword());
		FormTools.setConfiguredOrKeepDefault(webappRoot, data.getWebappRoot());
		FormTools.setConfiguredOrKeepDefault(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot());
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
		data.setDefaultSyncMode(FormTools.getSyncModeFromComboBox(defaultSyncMode));
	}


	public boolean isModified(OpenCmsPluginConfigurationData data) {
		if (activeCheckBox.isSelected() != data.isOpenCmsPluginActive()) return true;
		if (repository.getText() != null ? !repository.getText().equals(data.getRepository()) : data.getRepository() != null) return true;
		if (username.getText() != null ? !username.getText().equals(data.getUsername()) : data.getUsername() != null) return true;
		if (password.getText() != null ? !password.getText().equals(data.getPassword()) : data.getPassword() != null) return true;
		if (webappRoot.getText() != null ? !webappRoot.getText().equals(data.getWebappRoot()) : data.getWebappRoot() != null) return true;
		if (defaultLocalVfsRoot.getText() != null ? !defaultLocalVfsRoot.getText().equals(data.getDefaultLocalVfsRoot()) : data.getDefaultLocalVfsRoot() != null) return true;
		if (defaultSyncMode.getSelectedItem() != null ? !FormTools.getSyncModeFromComboBox(defaultSyncMode).equals(data.getDefaultSyncMode()) : data.getDefaultSyncMode() != null) return true;
		return false;
	}

	private void createUIComponents() {
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

	public void focusGained(FocusEvent e) {
		// do nothing
	}

	public void focusLost(FocusEvent e) {
		JTextField textField = (JTextField)e.getSource();
		if (textField == webappRoot) {
			FormTools.clearPathField(webappRoot, false);
		}
		if (textField == defaultLocalVfsRoot) {
			FormTools.clearPathField(defaultLocalVfsRoot, true);
		}
	}
}
