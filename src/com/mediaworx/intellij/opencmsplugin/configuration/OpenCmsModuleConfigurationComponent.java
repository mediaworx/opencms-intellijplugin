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
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
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
/**
 * Component for the module level configuration of the OpenCms plugin. The configuration data is stored in IntelliJ's
 * module file (<code>[modulename].iml</code>.
 */
public class OpenCmsModuleConfigurationComponent implements ModuleComponent, Configurable, PersistentStateComponent<OpenCmsModuleConfigurationData> {

	private OpenCmsPlugin plugin;
	private Module module;
	private OpenCmsModuleConfigurationForm form;
	private OpenCmsModuleConfigurationData configurationData;

	/**
	 * Creates a new module level configuration component.
	 * @param module the IntelliJ module
	 */
	public OpenCmsModuleConfigurationComponent(Module module) {
		this.module = module;
		plugin = module.getProject().getComponent(OpenCmsPlugin.class);
	}

	/**
	 * Method called by IntelliJ whenever the module level configuration component is initialized, does nothing.
	 */
	public void initComponent() {
	}

	/**
	 * Method called by IntelliJ whenever the module level configuration component is disposed, does some cleanup.
	 */
	public void disposeComponent() {
		plugin = null;
		module = null;
		form = null;
		configurationData = null;
	}

	/**
	 * Returns the component's name.
	 * @return the component's name "OpenCmsPlugin.ModuleConfigurationComponent"
	 */
	@NotNull
	public String getComponentName() {
		return "OpenCmsPlugin.ModuleConfigurationComponent";
	}

	/**
	 * Method called by IntelliJ whenever a project is opened, does nothing.
	 */
	public void projectOpened() {
		// called when project is opened
	}

	/**
	 * Method called by IntelliJ whenever a project is closed, does nothing.
	 */
	public void projectClosed() {
		// called when project is being closed
	}

	/**
	 * Method called by IntelliJ whenever a module is added to the project, does nothing.
	 */
	public void moduleAdded() {
		// Invoked when the module corresponding to this component instance has been completely
		// loaded and added to the project.
	}

	/**
	 * Returns the component's display name that is used in the Settings dialog.
	 * @return  the component's display name "OpenCms Module"
	 */
	@Nls
	public String getDisplayName() {
		return "OpenCms Module";
	}

	/**
	 * Method called by IntelliJ to display an icon on the module configuration tab.
	 * @return  path to the OpenCms icon, "/icons/opencms_13.png"
	 */
	public Icon getIcon() {
        return IconLoader.getIcon("/icons/opencms_13.png");
	}

	/**
	 * There's no help topic for the OpenCms plugin, so <code>null</code> is returned.
	 * @return  always returns <code>null</code>
	 */
	@Nullable
	public String getHelpTopic() {
		return null;  // Do nothing
	}

	/**
	 * Creates the module level configuration component and initializes the corresponding configuration data object.
	 * @return the module level configuration component
	 */
	@Nullable
	public JComponent createComponent() {
		if (configurationData == null) {
			configurationData = new OpenCmsModuleConfigurationData();
		}
		if (form == null) {
			form = new OpenCmsModuleConfigurationForm(plugin.getPluginConfiguration(), module);
		}
		return form.getRootComponent();
	}

	/**
	 * Checks if the module level configuration was modified
	 * @return  <code>true</code> if the module level configuration was modified, <code>false</code> otherwise
	 */
	public boolean isModified() {
		return form != null && form.isModified(configurationData);
	}

	/**
	 * Applies the modifications made to the module level configuration.
	 * @throws ConfigurationException required by the interface but never thrown
	 */
	public void apply() throws ConfigurationException {

		if (form != null) {
			// Get data from editor to component
			form.getData(configurationData);
			handleOcmsModuleRegistration();
		}
	}

	/**
	 * Registers and unregisters OpenCms modules depending on the setting "Is OpenCms Module". The OpenCms module 
	 * registry (class <code>OpenCmsModules</code>) is a singleton kept by the plugin instance.
	 * @see com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules
	 */
	private void handleOcmsModuleRegistration() {
		module.getModuleFilePath();
		if (configurationData.isOpenCmsModuleEnabled()) {
			String moduleBasePath = PluginTools.getModuleContentRoot(module);
			plugin.getOpenCmsModules().registerModule(moduleBasePath, configurationData);
		}
		else {
			plugin.getOpenCmsModules().unregisterModule(module);
		}
	}

	/**
	 * Resets the configuration form to the last saved state after modifications were made.
	 */
	public void reset() {
		if (form != null && configurationData != null) {
			// Reset form data from component
			form.setData(configurationData);
		}
	}

	/**
	 * Clears UI resources used by the module level configuration component.
	 */
	public void disposeUIResources() {
		form = null;
	}

	/**
	 * Returns the current module level configuration state.
	 * @return the OpenCmsPluginConfigurationData object
	 */
	@Nullable
	public OpenCmsModuleConfigurationData getState() {
		return this.configurationData;
	}

	/**
	 * Loads the module level configuration state contained in the given configuration data.
	 * @param configurationData the module level configuration data to load
	 */
	public void loadState(OpenCmsModuleConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsModuleConfigurationData();
		}
        XmlSerializerUtil.copyBean(configurationData, this.configurationData);
		handleOcmsModuleRegistration();
	}
}
