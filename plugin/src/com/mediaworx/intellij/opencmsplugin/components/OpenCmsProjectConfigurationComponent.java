package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationForm;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

@State(
	name = "OpenCmsPluginConfigurationData",
	storages = {
		@Storage( file = "$WORKSPACE_FILE$"),
		@Storage( file = "$PROJECT_CONFIG_DIR$/opencms.xml", scheme = StorageScheme.DIRECTORY_BASED)
	}
)
public class OpenCmsProjectConfigurationComponent implements ProjectComponent, Configurable, PersistentStateComponent<OpenCmsPluginConfigurationData> {

   	private OpenCmsPluginConfigurationForm form;
	private OpenCmsPluginConfigurationData configurationData;

	Project project;

	public OpenCmsProjectConfigurationComponent(Project project) {
		this.project = project;
	}

	public void projectOpened() {
		// Do nothing
	}

	public void projectClosed() {
		// Do nothing
	}

	public void initComponent() {
		// Do nothing
	}

	public void disposeComponent() {
		// Do nothing
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPluginConfigurationComponent";
	}

	@Nls
	public String getDisplayName() {
		return "OpenCms Plugin";
	}

	public String getHelpTopic() {
		return null;  // Do nothing
	}

	public JComponent createComponent() {
		if (configurationData == null) {
			configurationData = new OpenCmsPluginConfigurationData();
		}
		if (form == null) {
			form = new OpenCmsPluginConfigurationForm();
		}
		return form.getRootComponent();
	}

	public boolean isModified() {
		return form != null && form.isModified(configurationData);
	}

	public void apply() throws ConfigurationException {
		if (form != null) {
			// Get data from editor to component
			form.getData(configurationData);

			OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);
			if (plugin.getOpenCmsModules() != null && plugin.getOpenCmsModules().getAllModules().size() > 0) {
				Collection<OpenCmsModule> openCmsModules = plugin.getOpenCmsModules().getAllModules();
				for (OpenCmsModule openCmsModule : openCmsModules) {
					openCmsModule.refresh();
				}
			}

			if (configurationData.isPluginConnectorEnabled()) {
				if (plugin.getPluginConnector() != null) {
					plugin.getPluginConnector().setConnectorUrl(configurationData.getConnectorUrl());
					plugin.getPluginConnector().setUser(configurationData.getUsername());
					plugin.getPluginConnector().setPassword(configurationData.getPassword());
				}
				else {
					plugin.setPluginConnector(new OpenCmsPluginConnector(configurationData.getConnectorUrl(), configurationData.getUsername(), configurationData.getPassword()));
				}
			}
			else {
				plugin.setPluginConnector(null);
			}
		}
	}

	public void reset() {
		if (form != null) {
			// Reset form data from component
			form.setData(configurationData);
		}
	}

	public void disposeUIResources() {
		form = null;
	}

	@Nullable
	public OpenCmsPluginConfigurationData getState() {
		return this.configurationData;
	}

	public void loadState(OpenCmsPluginConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsPluginConfigurationData();
		}
		XmlSerializerUtil.copyBean(configurationData, this.configurationData);
	}

	public OpenCmsPluginConfigurationData getConfigurationData() {
		return configurationData;
	}
}
