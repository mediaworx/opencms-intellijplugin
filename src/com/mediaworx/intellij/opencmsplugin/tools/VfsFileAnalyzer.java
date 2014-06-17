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

package com.mediaworx.intellij.opencmsplugin.tools;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.HashSet;

public abstract class VfsFileAnalyzer {

	private static final Logger LOG = Logger.getInstance(VfsFileAnalyzer.class);

	protected final OpenCmsPlugin plugin;
	protected final VirtualFile[] files;
	protected final StringBuilder warnings;
	protected HashSet<String> handledPaths;
	protected ProgressIndicator progressIndicator;


	public VfsFileAnalyzer(final OpenCmsPlugin plugin, final VirtualFile[] files) throws CmsConnectionException {
		this.files = files;
		this.plugin = plugin;
		warnings = new StringBuilder();
		handledPaths = new HashSet<String>();
	}

	public void analyzeFiles() {

		if (files != null && files.length > 0) {
			for (VirtualFile file : files) {
				if (progressIndicator != null && progressIndicator.isCanceled()) {
					return;
				}

				if (handledPaths.contains(file.getPath())) {
					continue;
				}

				// if the file does not belong to a module, ignore
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(file);
				if (ocmsModule == null) {
					LOG.info("file/folder is not within a configured OpenCms module, ignore");
					handledPaths.add(file.getPath());
					continue;
				}

				// file/folder is ignored
				if (fileOrPathIsIgnored(plugin.getPluginConfiguration(), file)) {
					// do nothing (filter files defined in the file and folder ignore lists)
					plugin.getConsole().info("File or path is ignored: " + file.getPath());
					handledPaths.add(file.getPath());
					continue;
				}

				// it's a folder that is a module root, so sync all corresponding module resources
				if (file.isDirectory() && ocmsModule.isIdeaVFileModuleRoot(file)) {
					LOG.info("Module root selected, handling the module " + ocmsModule.getModuleName());
					handleModule(ocmsModule);
				}

				// file/folder is within a module resource path, handle it
				else if (ocmsModule.isIdeaVFileModuleResource(file)) {
					LOG.info("Handling a module resource path, a folder or a file in a module");
					handleModuleResource(ocmsModule, file);
				}

				// if it is a folder that is not a resource path, but within the VFS path ...
				else if (file.isDirectory()  && ocmsModule.isIdeaVFileInVFSPath(file)) {
					LOG.info("Handling a VFS path outside of the module resources");
					String relativeFolderPath = file.getPath().substring(ocmsModule.getLocalVfsRoot().length());
					// ... get all module resources under the folder and add them
					for (String moduleResourceVfsPath : ocmsModule.getModuleResources()) {
						// if the module resource is within the selected folder ...
						if (moduleResourceVfsPath.startsWith(relativeFolderPath)) {
							// ... handle it
							LOG.info("- The module resource path " + moduleResourceVfsPath + " is child, so handle it");
							handleModuleResourcePath(ocmsModule, moduleResourceVfsPath);
						}
					}
				}

				// file/folder is neither a module resource nor a VFS resource parent, ignore
				else {
					warnings.append("Ignoring '").append(file.getPath()).append("' (not a module path).\n");
					LOG.info("File is not in the VFS path, ignore");
				}
			}
		}
	}

	protected void handleModule(OpenCmsModule ocmsModule) {
		for (String resourcePath : ocmsModule.getModuleResources()) {
			handleModuleResourcePath(ocmsModule, resourcePath);
		}
	}

	protected abstract void handleModuleResource(OpenCmsModule ocmsModule, VirtualFile file);

	protected abstract void handleModuleResourcePath(OpenCmsModule ocmsModule, String moduleResource);

	public static boolean fileOrPathIsIgnored(OpenCmsPluginConfigurationData config, final VirtualFile ideaVFile) {
		final String path = ideaVFile.getPath();
		for (String ignoredPath : config.getIgnoredPathsArray()) {
			if (path.matches(".*/"+ignoredPath+"(/.*)?")) {
				LOG.info("path " + ignoredPath + " is ignored");
				return true;
			}
		}

		final String filename = ideaVFile.getName();
		for (String ignoredFilename : config.getIgnoredFilesArray()) {
			if (filename.matches(ignoredFilename)) {
				LOG.info("file " + ignoredFilename + " is ignored");
				return true;
			}
		}
		return false;
	}

	public boolean hasWarnings() {
		return warnings.length() > 0;
	}

	public StringBuilder getWarnings() {
		return warnings;
	}

}
