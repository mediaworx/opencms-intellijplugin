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

package com.mediaworx.intellij.opencmsplugin.actions.packagemodule;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.opencms.moduleutils.packager.OpenCmsModulePackager;
import com.mediaworx.opencms.moduleutils.packager.exceptions.OpenCmsModulePackagerException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Parent action for all actions used to package module zip files
 */
@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsPackageModuleAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPackageModuleAction.class);

	/**
	 * Triggers the packaging of module zips depending on the menu entry the user chose. Which modules are to
	 * be packaged is determined by calling the abstract method
	 * {@link #getModuleFileArray(com.intellij.openapi.actionSystem.AnActionEvent)} that's implemented by subclasses.
	 * For the module packaging the OpenCmsModulePackager is used that is provided as a separate library.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		List<File> moduleFiles = getModuleFileArray(event);
		final List<File> filesToBeRefreshed = new ArrayList<File>(moduleFiles.size());

		OpenCmsModulePackager packager = new OpenCmsModulePackager();
		plugin.showConsole();
		clearConsole();
		for (File moduleFile : moduleFiles) {
			if (!moduleFile.exists()) {
				continue;
			}
			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForFile(moduleFile);
			if (ocmsModule == null || !ocmsModule.isFileModuleRoot(moduleFile)) {
				continue;
			}

			String zipTargetPath = ocmsModule.getModuleBasePath() + "/" + config.getModuleZipTargetFolderPath();
			try {
				String packageName = packager.packageModule(ocmsModule.getManifestRoot(), ocmsModule.getLocalVfsRoot(), zipTargetPath);
				plugin.getConsole().info(packageName + " was saved at " + zipTargetPath);
			}
			catch (OpenCmsModulePackagerException e) {
				String message = "Error packaging module " + ocmsModule.getModuleName();
				LOG.warn(message, e);
				plugin.getConsole().error(message + "\n" + e.getMessage());
			}

			filesToBeRefreshed.add(new File(zipTargetPath));
		}
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				LocalFileSystem.getInstance().refreshIoFiles(filesToBeRefreshed, true, false, null);
			}
		}, 1000);
	}

	/**
	 * Abstract method to determine the modules to be packaged, implemented in subclasses.
	 * @param event the action event, provided by IntelliJ
	 * @return  An array with virtual files representing OpenCms modules
	 */
	protected abstract List<File> getModuleFileArray(@NotNull AnActionEvent event);

	/**
	 * Enables or disables the "package module zip" actions. If "pull meta data" is enabled in the plugin configuration,
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
