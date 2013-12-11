package com.mediaworx.intellij.opencmsplugin.configuration;

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
	private JPanel connectorOptionsPanel;
	private JCheckBox pullMetaDataCheckbox;
	private JComboBox autoPublishMode;

	public OpenCmsPluginConfigurationForm() {
		formPanel.setVisible(false);
		enabledCheckBox.addActionListener(this);
		webappRoot.addFocusListener(this);
		defaultLocalVfsRoot.addFocusListener(this);
		usePluginConnectorCheckBox.addActionListener(this);
		pullMetaDataCheckbox.addActionListener(this);
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

		usePluginConnectorCheckBox.setSelected(data.isPluginConnectorEnabled());
		connectorOptionsPanel.setVisible(data.isPluginConnectorEnabled());
		FormTools.setConfiguredOrKeepDefault(connectorUrl, data.getConnectorUrl());
		pullMetaDataCheckbox.setSelected(data.isPullMetadataEnabled());
		manifestRoot.setEnabled(data.isPullMetadataEnabled());
		FormTools.setConfiguredOrKeepDefault(manifestRoot, data.getManifestRoot());

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
	}


	public void getData(OpenCmsPluginConfigurationData data) {
		data.setOpenCmsPluginEnabled(enabledCheckBox.isSelected());
		data.setRepository(repository.getText());
		data.setUsername(username.getText());
		data.setPassword(password.getText());
		data.setWebappRoot(webappRoot.getText());
		data.setDefaultLocalVfsRoot(defaultLocalVfsRoot.getText());
		data.setDefaultSyncMode(FormTools.getSyncModeFromComboBox(defaultSyncMode));
		data.setPluginConnectorEnabled(usePluginConnectorCheckBox.isSelected());
		data.setConnectorUrl(connectorUrl.getText());
		data.setPullMetadataEnabled(pullMetaDataCheckbox.isSelected());
		data.setManifestRoot(manifestRoot.getText());
		data.setAutoPublishMode(FormTools.getAutoPublishModeFromCombobox(autoPublishMode));
	}


	public boolean isModified(OpenCmsPluginConfigurationData data) {
		return
			isPluginActivationModified(data.isOpenCmsPluginEnabled()) ||
			FormTools.isTextFieldModified(repository, data.getRepository()) ||
			FormTools.isTextFieldModified(username, data.getUsername()) ||
			FormTools.isTextFieldModified(password, data.getPassword()) ||
			FormTools.isTextFieldModified(webappRoot, data.getWebappRoot()) ||
			FormTools.isTextFieldModified(defaultLocalVfsRoot, data.getDefaultLocalVfsRoot()) ||
			!FormTools.getSyncModeFromComboBox(defaultSyncMode).equals(data.getDefaultSyncMode()) ||
			usePluginConnectorCheckBox.isSelected() != data.isPluginConnectorEnabled() ||
			FormTools.isTextFieldModified(connectorUrl, data.getConnectorUrl()) ||
			pullMetaDataCheckbox.isSelected() != data.isPullMetadataEnabled() ||
			FormTools.isTextFieldModified(manifestRoot, data.getManifestRoot()) ||
			!FormTools.getAutoPublishModeFromCombobox(autoPublishMode).equals(data.getAutoPublishMode())
		;
	}

	public boolean isPluginActivationModified(boolean wasActivated) {
		return enabledCheckBox.isSelected() != wasActivated;
	}

	private void createUIComponents() {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == enabledCheckBox) {
			formPanel.setVisible(enabledCheckBox.isSelected());
		}
		else if (source == usePluginConnectorCheckBox) {
			connectorOptionsPanel.setVisible(usePluginConnectorCheckBox.isSelected());
		}
		else  if (source == pullMetaDataCheckbox) {
			manifestRoot.setEnabled(pullMetaDataCheckbox.isSelected());
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
