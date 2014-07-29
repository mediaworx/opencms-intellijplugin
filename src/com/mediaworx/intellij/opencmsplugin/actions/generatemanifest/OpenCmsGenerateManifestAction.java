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

package com.mediaworx.intellij.opencmsplugin.actions.generatemanifest;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.opencms.moduleutils.manifestgenerator.OpenCmsModuleManifestGenerator;
import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlFileWriteException;
import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlParseException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent action for all actions used to generate module manifest files (manifest.xml)
 */
@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsGenerateManifestAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsGenerateManifestAction.class);

	/**
	 * Triggers the creation of module manifests depending on the menu entry the user chose. Which manifests are to
	 * be created is determined by calling the abstract method
	 * {@link #getModuleFileArray(com.intellij.openapi.actionSystem.AnActionEvent)} that's implemented by subclasses.
	 * For the manifest creation the OpenCmsModuleManifestGenerator is used that is provided as a separate library.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		VirtualFile[] ideaVFiles = getModuleFileArray(event);
		final List<File> filesToBeRefreshed = new ArrayList<File>(ideaVFiles.length);

		OpenCmsModuleManifestGenerator manifestGenerator = new OpenCmsModuleManifestGenerator();
		manifestGenerator.setReplaceMetaVariables(config.isUseMetaVariablesEnabled());
		plugin.showConsole();
		clearConsole();

		for (VirtualFile ideaVFile : ideaVFiles) {
			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);
			if (ocmsModule == null || !ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
				continue;
			}
			try {
				if (ocmsModule.isSetSpecificModuleVersionEnabled()) {
					manifestGenerator.setModuleVersion(ocmsModule.getModuleVersion());
				}
				else {
					manifestGenerator.setModuleVersion(null);
				}
				manifestGenerator.generateManifest(new File(ocmsModule.getManifestRoot()));
				plugin.getConsole().info("manifest.xml created at " + ocmsModule.getManifestRoot());
				VirtualFile manifestRoot = LocalFileSystem.getInstance().findFileByPath(ocmsModule.getManifestRoot());
				if (manifestRoot != null) {
					manifestRoot.refresh(true, false);
				}
			}
			catch (OpenCmsMetaXmlParseException e) {
				String message = "There was an error parsing the meta information for module " + ocmsModule.getModuleName();
				plugin.getConsole().error(message);
				LOG.error(message, e);
			}
			catch (OpenCmsMetaXmlFileWriteException e) {
				String message = "There was an error writing the manifest.xml for module " + ocmsModule.getModuleName();
				plugin.getConsole().error(message);
				LOG.error(message, e);
			}
			catch (Exception e) {
				String message = "Error generating manifest.xml for the module " + ocmsModule.getModuleName() + " (" + e.getMessage() + ")";
				plugin.getConsole().error(message + "; Please have a look at the IntelliJ log file and/or the OpenCms log file.");
				LOG.error(message, e);
			}
		}
	}

	/**
	 * Abstract method to determine the manifests for what modules are to be created, implemented in subclasses.
	 * @param event the action event, provided by IntelliJ
	 * @return  An array with virtual files representing OpenCms modules
	 */
	protected abstract VirtualFile[] getModuleFileArray(@NotNull AnActionEvent event);

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
