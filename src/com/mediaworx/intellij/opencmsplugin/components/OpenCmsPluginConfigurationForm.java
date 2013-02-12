package com.mediaworx.intellij.opencmsplugin.components;

import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 07.11.12
 * Time: 10:30
 * To change this template use File | Settings | File Templates.
 */
public class OpenCmsPluginConfigurationForm {

    private JPanel rootComponent;

    private JTextField repository;
    private JTextField username;
    private JTextField password;
    private JTextField syncRootLocal;
    private JTextField webappRoot;

    private JLabel repositoryLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel syncRootLocalLabel;
    private JLabel webappRootLabel;

//    private void createUIComponents() {
//    }

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

    private void setConfiguredOrKeepDefault(JTextField field, String configured) {
        if (configured != null && configured.length() > 0) {
            field.setText(configured);
        }
    }


	public void setData(OpenCmsPluginConfigurationData data) {
		setConfiguredOrKeepDefault(repository, data.getRepository());
		setConfiguredOrKeepDefault(username, data.getUsername());
		setConfiguredOrKeepDefault(password, data.getPassword());
		setConfiguredOrKeepDefault(syncRootLocal, data.getSyncRootLocal());
		setConfiguredOrKeepDefault(webappRoot, data.getWebappRoot());
	}


	public void getData(OpenCmsPluginConfigurationData data) {
		data.setRepository(repository.getText());
		data.setUsername(username.getText());
		data.setPassword(password.getText());
		data.setSyncRootLocal(syncRootLocal.getText());
		data.setWebappRoot(webappRoot.getText());
	}


	public boolean isModified(OpenCmsPluginConfigurationData data) {
		if (repository.getText() != null ? !repository.getText().equals(data.getRepository()) : data.getRepository() != null) return true;
		if (username.getText() != null ? !username.getText().equals(data.getUsername()) : data.getUsername() != null) return true;
		if (password.getText() != null ? !password.getText().equals(data.getPassword()) : data.getPassword() != null) return true;
		if (syncRootLocal.getText() != null ? !syncRootLocal.getText().equals(data.getSyncRootLocal()) : data.getSyncRootLocal() != null) return true;
		if (webappRoot.getText() != null ? !webappRoot.getText().equals(data.getWebappRoot()) : data.getWebappRoot() != null) return true;
		return false;
	}
}
