package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class OpenCmsPluginConfigurationForm implements ActionListener, FocusListener {

	private JPanel rootComponent;

	private JPanel formPanel;

	private JCheckBox enabledCheckBox;
	private JTextField repository;
	private JTextField username;
	private JTextField password;
	private JTextField webappRoot;
	private JTextField defaultLocalVfsRoot;
	private JCheckBox usePluginConnectorCheckBox;
	private JTextField connectorUrl;
	private JTextField manifestRoot;
	private JComboBox defaultSyncMode;

	public OpenCmsPluginConfigurationForm() {
		formPanel.setVisible(false);
		enabledCheckBox.addActionListener(this);
		webappRoot.addFocusListener(this);
		defaultLocalVfsRoot.addFocusListener(this);
		usePluginConnectorCheckBox.addActionListener(this);
	}

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

	public void setData(OpenCmsPluginConfigurationData data) {
		enabledCheckBox.setSelected(data.isOpenCmsPluginEnabled());
		formPanel.setVisible(data.isOpenCmsPluginEnabled());
		FormTools.setConfiguredOrKeepDefault(repository, data.getRepository());
		FormTools.setConfiguredOrKeepDefault(username, data.getUsername());
		FormTools.setConfiguredOrKeepDefault(password, data.getPassword());
		FormTools.setConfiguredOrKeepDefault(webappRoot, data.getWebappRoot());
		FormTools.setConfiguredOrKeepDefault(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot());
		usePluginConnectorCheckBox.setSelected(data.isPluginConnectorEnabled());
		connectorUrl.setEnabled(data.isPluginConnectorEnabled());
		FormTools.setConfiguredOrKeepDefault(connectorUrl, data.getConnectorUrl());
		manifestRoot.setEnabled(data.isPluginConnectorEnabled());
		FormTools.setConfiguredOrKeepDefault(manifestRoot, data.getManifestRoot());
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
		data.setOpenCmsPluginEnabled(enabledCheckBox.isSelected());
		data.setRepository(repository.getText());
		data.setUsername(username.getText());
		data.setPassword(password.getText());
		data.setWebappRoot(webappRoot.getText());
		data.setDefaultLocalVfsRoot(defaultLocalVfsRoot.getText());
		data.setPluginConnectorEnabled(usePluginConnectorCheckBox.isSelected());
		data.setConnectorUrl(connectorUrl.getText());
		data.setManifestRoot(manifestRoot.getText());
		data.setDefaultSyncMode(FormTools.getSyncModeFromComboBox(defaultSyncMode));
	}


	public boolean isModified(OpenCmsPluginConfigurationData data) {
		return
			enabledCheckBox.isSelected() != data.isOpenCmsPluginEnabled() ||
			FormTools.isTextFieldModified(repository, data.getRepository()) ||
			FormTools.isTextFieldModified(username, data.getUsername()) ||
			FormTools.isTextFieldModified(password, data.getPassword()) ||
			FormTools.isTextFieldModified(webappRoot, data.getWebappRoot()) ||
			FormTools.isTextFieldModified(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot()) ||
			usePluginConnectorCheckBox.isSelected() != data.isPluginConnectorEnabled() ||
			FormTools.isTextFieldModified(connectorUrl, data.getConnectorUrl()) ||
			FormTools.isTextFieldModified(manifestRoot, data.getManifestRoot()) ||
			!FormTools.getSyncModeFromComboBox(defaultSyncMode).equals(data.getDefaultSyncMode())
		;
	}

	private void createUIComponents() {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == enabledCheckBox) {
			formPanel.setVisible(enabledCheckBox.isSelected());
		}
		else  if (source == usePluginConnectorCheckBox) {
			connectorUrl.setEnabled(usePluginConnectorCheckBox.isSelected());
			manifestRoot.setEnabled(usePluginConnectorCheckBox.isSelected());
		}

	}

	public void focusGained(FocusEvent e) {
		// do nothing
	}

	public void focusLost(FocusEvent e) {
		Object source = e.getSource();
		if (source == webappRoot) {
			FormTools.clearPathField(webappRoot, false);
		}
		else if (source == defaultLocalVfsRoot) {
			FormTools.clearPathField(defaultLocalVfsRoot, true);
		}
	}
}
