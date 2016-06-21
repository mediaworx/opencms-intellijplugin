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

package com.mediaworx.intellij.opencmsplugin.actions.importmodule;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.toolwindow.ConsolePrinter;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import com.mediaworx.opencms.ideconnector.data.ModuleImportInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent action for all actions used to import module zips into the local OpenCms instance
 */
@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsImportAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsImportAction.class);

	/**
	 * Triggers the import of modules depending on the menu entry the user chose. Which modules are to
	 * be imported is determined by calling the abstract method
	 * {@link #getModuleFileList(AnActionEvent)} that's implemented by subclasses.
	 * For the moduel import the IDEConnectorClient is used that is provided as a separate library.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		if (config.isPluginConnectorServiceEnabled() && StringUtils.isNotBlank(config.getConnectorServiceUrl())) {
			final List<File> moduleFiles = getModuleFileList(event);

			final OpenCmsToolWindowConsole console = plugin.getConsole();

			final List<ModuleImportInfo> moduleImportInfos = new ArrayList<>();
			for (File moduleFile : moduleFiles) {
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForFile(moduleFile);
				if (ocmsModule == null || !ocmsModule.isFileModuleRoot(moduleFile)) {
					continue;
				}
				String moduleZipPath = ocmsModule.findNewestModuleZipPath();
				if (StringUtils.isNotBlank(moduleZipPath)) {
					ModuleImportInfo importInfo = new ModuleImportInfo();
					importInfo.setModuleZipPath(moduleZipPath);
					importInfo.setImportSiteRoot(ocmsModule.getExportImportSiteRoot());
					moduleImportInfos.add(importInfo);
				}
				else {
					console.error("No module zip for module " + ocmsModule.getModuleName() + " found in target folder " + config.getModuleZipTargetFolderPath());
				}
			}
			if (moduleImportInfos.size() > 0) {

				plugin.showConsole();
				clearConsole();

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						try {
							connectorClient.login(config.getUsername(), config.getPassword());
							connectorClient.importModules(moduleImportInfos, new ConsolePrinter(console));
							connectorClient.logout();
						}
						catch (Exception e) {
							SwingUtilities.invokeLater(
								new Runnable() {
									@Override
									public void run() {
										Messages.showDialog("This function is only available if the IDE Connector module 1.5 is installed and configured in OpenCms and if OpenCms is running. Consult the Plugin Wiki for mor information.", "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
									}
								}
							);
						}
					}
				};
				Thread thread = new Thread(runnable);
				thread.start();
			}
		}
		else {
			Messages.showDialog("This function is only available if the IDE Connector Service URL is provided and the Connector Service is activated (IDE connector module 1.5 required).", "Error - Please Check Your OpenCms Plugin Configuration.", new String[]{"Ok"}, 0, Messages.getErrorIcon());
		}
	}

	/**
	 * Abstract method to determine the manifests for what modules are to be created, implemented in subclasses.
	 * @param event the action event, provided by IntelliJ
	 * @return  An array with virtual files representing OpenCms modules
	 */
	protected abstract List<File> getModuleFileList(@NotNull AnActionEvent event);

	/**
	 * Enables or disables the generate manifest actions. If "pull meta data" is enabled in the plugin configuration,
	 * the actions are enabled, otherwise they are disabled.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		if (isPluginEnabled() && isPullMetaDataEnabled()) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}

}
