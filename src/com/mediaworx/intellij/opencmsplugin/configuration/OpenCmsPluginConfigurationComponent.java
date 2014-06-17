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
public class OpenCmsPluginConfigurationComponent implements ProjectComponent, Configurable, PersistentStateComponent<OpenCmsPluginConfigurationData> {

   	private OpenCmsPluginConfigurationForm form;
	private OpenCmsPluginConfigurationData configurationData;

	Project project;

	public OpenCmsPluginConfigurationComponent(Project project) {
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
		return "OpenCmsPlugin.ConfigurationComponent";
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
				}
				else {
					plugin.setPluginConnector(new OpenCmsPluginConnector(configurationData.getConnectorUrl(), configurationData.getUsername(), configurationData.getPassword()));
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
