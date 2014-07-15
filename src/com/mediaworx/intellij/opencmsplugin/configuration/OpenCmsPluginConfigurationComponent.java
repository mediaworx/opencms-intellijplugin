/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.configuration;

import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
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
/**
 * Component for the project level configuration of the OpenCms plugin. The configuration data is stored in the file
 * <code>opencms.xml</code> in the IntelliJ configuration folder (<code>.idea</code>).
 */
public class OpenCmsPluginConfigurationComponent implements ProjectComponent, Configurable, PersistentStateComponent<OpenCmsPluginConfigurationData> {

   	private OpenCmsPluginConfigurationForm form;
	private OpenCmsPluginConfigurationData configurationData;

	Project project;

	/**
	 * Creates a new project level configuration component.
	 * @param project the IntelliJ project
	 */
	public OpenCmsPluginConfigurationComponent(Project project) {
		this.project = project;
	}

	/**
	 * Method called by IntelliJ whenever a project is opened, does nothing.
	 */
	public void projectOpened() {
		// Do nothing
	}

	/**
	 * Method called by IntelliJ whenever a project is closed, does nothing.
	 */
	public void projectClosed() {
		// Do nothing
	}

	/**
	 * Method called by IntelliJ whenever the project level configuration component is initialized, does nothing.
	 */
	public void initComponent() {
		// Do nothing
	}

	/**
	 * Method called by IntelliJ whenever the project level configuration component is disposed, does some cleanup.
	 */
	public void disposeComponent() {
		form = null;
		configurationData = null;
	}

	/**
	 * Returns the component's name.
	 * @return the component's name "OpenCmsPlugin.ConfigurationComponent"
	 */
	@NotNull
	public String getComponentName() {
		return "OpenCmsPlugin.ConfigurationComponent";
	}

	/**
	 * Returns the component's display name that is used in the Settings dialog.
	 * @return  the component's display name "OpenCms Plugin"
	 */
	@Nls
	public String getDisplayName() {
		return "OpenCms Plugin";
	}

	/**
	 * There's no help topic for the OpenCms plugin, so <code>null</code> is returned.
	 * @return  always returns <code>null</code>
	 */
	public String getHelpTopic() {
		return null;  // Do nothing
	}

	/**
	 * Creates the project level configuration component and initializes the corresponding configuration data object.
	 * @return the project level configuration component
	 */
	public JComponent createComponent() {
		if (configurationData == null) {
			configurationData = new OpenCmsPluginConfigurationData();
		}
		if (form == null) {
			form = new OpenCmsPluginConfigurationForm();
		}
		return form.getRootComponent();
	}

	/**
	 * Checks if the project level configuration was modified
	 * @return  <code>true</code> if the project level configuration was modified, <code>false</code> otherwise
	 */
	public boolean isModified() {
		return form != null && form.isModified(configurationData);
	}

	/**
	 * Applies the modifications made to the project level configuration.
	 * @throws ConfigurationException required by the interface but never thrown
	 */
	public void apply() throws ConfigurationException {
		if (form != null) {
			boolean pluginActivationWasModified = form.isPluginActivationModified(configurationData.isOpenCmsPluginEnabled());

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
					plugin.getPluginConnector().setUseMetaVariables(configurationData.isUseMetaVariablesEnabled());
				}
				else {
					plugin.setPluginConnector(new OpenCmsPluginConnector(configurationData.getConnectorUrl(), configurationData.getUsername(), configurationData.getPassword(), configurationData.isUseMetaVariablesEnabled()));
				}
			}
			else {
				plugin.setPluginConnector(null);
			}

			if (pluginActivationWasModified) {
				if (configurationData.isOpenCmsPluginEnabled()) {
					plugin.enable();
				}
				else {
					plugin.disable();
				}
			}
		}
	}

	/**
	 * Resets the configuration form to the last saved state after modifications were made.
	 */
	public void reset() {
		if (form != null) {
			// Reset form data from component
			form.setData(configurationData);
		}
	}

	/**
	 * Clears UI resources used by the project level configuration component.
	 */
	public void disposeUIResources() {
		form = null;
	}

	/**
	 * Returns the current project level configuration state.
	 * @return the OpenCmsPluginConfigurationData object
	 */
	@Nullable
	public OpenCmsPluginConfigurationData getState() {
		return configurationData;
	}

	/**
	 * Loads the project level configuration state contained in the given configuration data.
	 * @param configurationData the project level configuration data to load
	 */
	public void loadState(OpenCmsPluginConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsPluginConfigurationData();
		}
		XmlSerializerUtil.copyBean(configurationData, this.configurationData);
	}

}
