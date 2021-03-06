/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import com.mediaworx.opencms.ideconnector.client.IDEConnectorClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * Configurable for the project level configuration of the OpenCms plugin. The configuration data is stored in the file
 * <code>opencms.xml</code> in the IntelliJ configuration folder (<code>.idea</code>). Persistence is handled via
 * {@link OpenCmsPlugin}.
 */
public class OpenCmsPluginConfigurable implements Configurable {

	private static final Logger LOG = Logger.getInstance(OpenCmsPluginConfigurable.class);

	private OpenCmsPluginConfigurationForm form;

	private Project project;
	private OpenCmsPlugin plugin;
	private OpenCmsPluginConfigurationData configurationData;


	/**
	 * Creates a new project level configuration component.
	 * @param project the IntelliJ project
	 */
	public OpenCmsPluginConfigurable(Project project) {
		this.project = project;
		plugin = project.getComponent(OpenCmsPlugin.class);
		configurationData = plugin.getPluginConfiguration();
	}

	/**
	 * Returns the component's display name that is used in the Settings dialog.
	 * @return  the component's display name "OpenCms Plugin"
	 */
	@Nls
	@Override
	public String getDisplayName() {
		return "OpenCms Plugin";
	}

	/**
	 * There's no help topic for the OpenCms plugin, so <code>null</code> is returned.
	 * @return  always returns <code>null</code>
	 */
	@Override
	public String getHelpTopic() {
		return null;  // Do nothing
	}

	/**
	 * Creates the project level configuration component and initializes the corresponding configuration data object.
	 * @return the project level configuration component
	 */
	@Override
	public JComponent createComponent() {
		LOG.info("OpenCmsPlugin: OpenCmsPluginConfigurable.createComponent called. Project: " + project.getName());
		if (form == null) {
			form = new OpenCmsPluginConfigurationForm();
		}
		return form.getRootComponent();
	}

	/**
	 * Checks if the project level configuration was modified
	 * @return  <code>true</code> if the project level configuration was modified, <code>false</code> otherwise
	 */
	@Override
	public boolean isModified() {
		return form != null && form.isModified(configurationData);
	}

	/**
	 * Applies the modifications made to the project level configuration.
	 * @throws ConfigurationException required by the interface but never thrown
	 */
	@Override
	public void apply() throws ConfigurationException {
		LOG.info("OpenCmsPlugin: OpenCmsPluginConfigurable.apply called. Project: " + project.getName());
		if (form != null) {
			boolean pluginActivationWasModified = form.isPluginActivationModified(configurationData.isOpenCmsPluginEnabled());

			// Get data from editor to component
			form.getData(configurationData);

			final OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);

			if (!plugin.checkWebappRootConfiguration(false)) {
				throw new ConfigurationException("The Webapp Root or OpenCms configuration folder was not found. Please check the OpenCms Webapp Root in the OpenCms Plugin settings.", "Configuration error!");
			}

			if (plugin.getOpenCmsModules() != null && plugin.getOpenCmsModules().getAllModules().size() > 0) {
				plugin.refreshOpenCmsModules();
			}

			VfsAdapter vfsAdapter = plugin.getVfsAdapter();

			if (vfsAdapter != null) {
				vfsAdapter.setUser(configurationData.getUsername());
				vfsAdapter.setPassword(configurationData.getPassword());
			}

			if (configurationData.isPluginConnectorEnabled()) {
				if (plugin.getPluginConnector() != null) {
					plugin.getPluginConnector().setConnectorUrl(configurationData.getConnectorUrl());
					plugin.getPluginConnector().setUser(configurationData.getUsername());
					plugin.getPluginConnector().setPassword(configurationData.getPassword());
					plugin.getPluginConnector().setUseMetaDateVariables(configurationData.isUseMetaDateVariablesEnabled());
					plugin.getPluginConnector().setUseMetaIdVariables(configurationData.isUseMetaIdVariablesEnabled());
				}
				else {
					plugin.setPluginConnector(
							new OpenCmsPluginConnector(
									configurationData.getConnectorUrl(),
									configurationData.getUsername(),
									configurationData.getPassword(),
									configurationData.isUseMetaDateVariablesEnabled(),
									configurationData.isUseMetaIdVariablesEnabled()
							)
					);
				}
			}
			else {
				plugin.setPluginConnector(null);
			}

			if (configurationData.isPluginConnectorServiceEnabled() && StringUtils.isNotBlank(configurationData.getConnectorServiceUrl())) {
				IDEConnectorClient connectorClient = plugin.getConnectorClient();
				if (connectorClient != null) {
					connectorClient.getConfiguration().setConnectorServiceBaseUrl(configurationData.getConnectorServiceUrl());
				}
			}
			else {
				plugin.setConnectorClient(null);
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
	@Override
	public void reset() {
		LOG.info("OpenCmsPlugin: OpenCmsPluginConfigurable.reset called. Project: " + project.getName());
		if (form != null) {
			// Reset form data from component
			form.setData(configurationData);
		}
	}

	/**
	 * Clears UI resources used by the project level configuration component.
	 */
	@Override
	public void disposeUIResources() {
		form = null;
	}

}
