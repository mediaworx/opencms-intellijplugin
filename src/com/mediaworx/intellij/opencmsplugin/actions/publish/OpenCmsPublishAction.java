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

package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsConnectionAction;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.connector.PublishFileAnalyzer;
import com.mediaworx.intellij.opencmsplugin.exceptions.OpenCmsConnectorException;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Parent action for all actions used to publish OpenCms resources
 */
@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsPublishAction extends OpenCmsConnectionAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPublishAction.class);

	/**
	 * Triggers the publishing of module resources depending on the menu entry the user chose. Which resources are to
	 * be published is determined by calling the abstract method
	 * {@link #getPublishFileArray(com.intellij.openapi.actionSystem.AnActionEvent)} that's implemented by subclasses.
	 */
	@Override
	public void executeAction(AnActionEvent event) {
		LOG.info("executeAction - event: " + event);

		try {
			OpenCmsToolWindowConsole console = plugin.getConsole();
			List<File> publishFiles = getPublishFileArray(event);

			if (publishFiles == null || publishFiles.size() == 0) {
				console.info("nothing to publish");
			}

			PublishFileAnalyzer analyzer = new PublishFileAnalyzer(plugin, publishFiles);
			analyzer.analyzeFiles();
			List<String> publishList = analyzer.getPublishList();

			plugin.showConsole();
			clearConsole();

			if (publishList.size() > 0) {
				console.info("Starting direct publish session for the following resources (and contained sub resources): ");
				for (String vfsPath : publishList) {
					console.info("  " + vfsPath);
				}

				OpenCmsPluginConnector connector = plugin.getPluginConnector();
				try {
					connector.publishResources(publishList, true);
					console.info("Direct publish session started");
				}
				catch (IOException e) {
					LOG.warn("There was an exception while publishing resources", e);
					console.error("There was an exception while publishing resources. Is OpenCms running? Please have a look at the OpenCms log file and/or the IntelliJ log file.");
				}
				catch (OpenCmsConnectorException e) {
					console.error(e.getMessage());
				}
			}
			else {
				console.info("nothing to publish");
			}
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsPublishAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	/**
	 * Abstract method to determine the module resources to be published, implemented in subclasses.
	 * @param event the action event, provided by IntelliJ
	 * @return  An array with virtual files representing OpenCms module resources
	 */
	protected abstract List<File> getPublishFileArray(AnActionEvent event);

	/**
	 * Enables the action if the OpenCms plugin connector is enabled in the plugin's configuration.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		event.getPresentation().setEnabled(isConnectorEnabled());
	}

	/**
	 * Determines if the plugin connector is enabled in the plugin's configuration
	 * @return <code>true</code> if the plugin connector is enabled, <code>false</code> otherwise
	 */
	protected boolean isConnectorEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isPluginConnectorEnabled();
	}
}
