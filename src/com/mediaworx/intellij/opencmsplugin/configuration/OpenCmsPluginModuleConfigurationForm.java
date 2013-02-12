package com.mediaworx.intellij.opencmsplugin.configuration;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 07.11.12
 * Time: 10:30
 * To change this template use File | Settings | File Templates.
 */
public class OpenCmsPluginModuleConfigurationForm {

	private JPanel rootComponent;

	private JTextField syncRootLocal;

	private JLabel syncRootLocalLabel;

//    private void createUIComponents() {
//    }

	// Method returns the root component of the form
	public JComponent getRootComponent() {
		return rootComponent;
	}

	private void setConfiguredOrKeepDefault(JTextField field, String configured) {
        if (configured != null && configured.length() > 0 && field != null) {
			field.setText(configured);
		}
	}


	public void setData(OpenCmsPluginModuleConfigurationData data) {
		setConfiguredOrKeepDefault(syncRootLocal, data.getSyncRootLocal());
	}


	public void getData(OpenCmsPluginModuleConfigurationData data) {
		data.setSyncRootLocal(syncRootLocal.getText());
	}


	public boolean isModified(OpenCmsPluginModuleConfigurationData data) {
		if (syncRootLocal.getText() != null ? !syncRootLocal.getText().equals(data.getSyncRootLocal()) : data.getSyncRootLocal() != null) return true;
		return false;
	}
}
