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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@State(
    name = "OpenCmsModuleConfigurationData",
    storages = {
        @Storage(id = "OpenCmsPluginModuleConfiguration", file = "$MODULE_FILE$")
    }
)
public class OpenCmsModuleConfigurationComponent implements ModuleComponent, Configurable, PersistentStateComponent<OpenCmsModuleConfigurationData> {

	private OpenCmsPlugin plugin;
	private Module module;
	private OpenCmsModuleConfigurationForm form;
	private OpenCmsModuleConfigurationData configurationData;

	public OpenCmsModuleConfigurationComponent(Module module) {
		this.module = module;
		plugin = module.getProject().getComponent(OpenCmsPlugin.class);
	}

	public void initComponent() {
	}

	public void disposeComponent() {
		plugin = null;
		module = null;
		form = null;
		configurationData = null;
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPlugin.ModuleConfigurationComponent";
	}

	public void projectOpened() {
		// called when project is opened
	}

	public void projectClosed() {
		// called when project is being closed
	}

	public void moduleAdded() {
		// Invoked when the module corresponding to this component instance has been completely
		// loaded and added to the project.
	}

	@Nls
	public String getDisplayName() {
		return "OpenCms Module";
	}

	public Icon getIcon() {
        return IconLoader.getIcon("/icons/opencms_13.png");
	}

	@Nullable
	public String getHelpTopic() {
		return null;  // Do nothing
	}

	@Nullable
	public JComponent createComponent() {
		if (configurationData == null) {
			configurationData = new OpenCmsModuleConfigurationData();
		}
		if (form == null) {
			form = new OpenCmsModuleConfigurationForm(plugin.getPluginConfiguration());
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
			handleOcmsModuleRegistration();
		}
	}

	private void handleOcmsModuleRegistration() {
		if (configurationData.isOpenCmsModuleEnabled()) {
			plugin.getOpenCmsModules().registerModule(module, configurationData);
		}
		else {
			plugin.getOpenCmsModules().unregisterModule(module);
		}
	}

	public void reset() {
		if (form != null && configurationData != null) {
			// Reset form data from component
			form.setData(configurationData);
		}
	}

	public void disposeUIResources() {
		form = null;
	}

	@Nullable
	public OpenCmsModuleConfigurationData getState() {
		return this.configurationData;
	}

	public void loadState(OpenCmsModuleConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsModuleConfigurationData();
		}
        XmlSerializerUtil.copyBean(configurationData, this.configurationData);
		handleOcmsModuleRegistration();
	}
}
