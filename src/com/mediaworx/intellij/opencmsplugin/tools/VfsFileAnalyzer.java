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
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Abstract class used for analyzing a user selection in IntelliJ's project tree. All selected entities are checked
 * if they
 *
 * <ul>
 *     <li>are contained in an OpenCms module resource path or within a module's VFS path</li>
 *     <li>are not ignored due to the ignored files/folders configuration</li>
 * </ul>
 *
 * Those entities passing that test, are handled depending on their type:
 *
 * <ul>
 *     <li>
 *         if the selected entity is an IntelliJ module, it is handled by calling
 *         {@link #handleModule(OpenCmsModule)} that calls {@link #handleModuleResourcePath(OpenCmsModule, String)}
 *         for all of the module's resource paths.
 *     </li>
 *     <li>
 *         if the selected entity is a file or a folder contained in an OpenCms module resource path, it is handled
 *         by calling {@link #handleModuleResource(OpenCmsModule, File)}
 *     </li>
 *     <li>
 *         if the selected entity is a folder that's an ancestor to an OpenCms module resource path, it is handled
 *         by calling {@link #handleModuleResourcePath(OpenCmsModule, String)}
 *     </li>
 * </ul>
 *
 * The methods <code>handleModuleResource</code> and <code>handleModuleResourcePath</code> are abstract and are
 * implemented by subclasses. VfsFileAnalyzers are used to determine which resources are to be synced
 * ({@link com.mediaworx.intellij.opencmsplugin.sync.SyncFileAnalyzer} or which resources are to be published
 * ({@link com.mediaworx.intellij.opencmsplugin.connector.PublishFileAnalyzer}.
 */
public abstract class VfsFileAnalyzer {

	private static final Logger LOG = Logger.getInstance(VfsFileAnalyzer.class);

	protected final OpenCmsPlugin plugin;
	protected final List<File> files;
	protected final StringBuilder warnings;
	protected HashSet<String> handledPaths;
	protected ProgressIndicator progressIndicator;


	public VfsFileAnalyzer(final OpenCmsPlugin plugin, final List<File> files) throws CmsConnectionException {
		this.files = files;
		this.plugin = plugin;
		warnings = new StringBuilder();
		handledPaths = new HashSet<String>();
	}

	public void analyzeFiles() {

		if (files != null && files.size() > 0) {
			for (File file : files) {
				if (progressIndicator != null && progressIndicator.isCanceled()) {
					return;
				}

				if (handledPaths.contains(file.getPath())) {
					continue;
				}

				// if the file does not belong to a module, ignore
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForFile(file);
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

				// it's a folder that is a module root, so handle all corresponding module resources
				if (file.isDirectory() && ocmsModule.isFileModuleRoot(file)) {
					LOG.info("Module root selected, handling the module " + ocmsModule.getModuleName());
					handleModule(ocmsModule);
				}

				// file/folder is within a module resource path, handle it
				else if (ocmsModule.isFileModuleResource(file)) {
					LOG.info("Handling a module resource path, a folder or a file in a module");
					handleModuleResource(ocmsModule, file);
				}

				// if it is a folder that is not a resource path, but within the VFS path ...
				else if (file.isDirectory()  && ocmsModule.isFileInVFSPath(file)) {
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

	/**
	 * Handles selected modules, calls the abstract method {@link #handleModuleResourcePath(OpenCmsModule, String)}
	 * for all of the module's resource paths.
	 * @param ocmsModule the OpenCms module
	 */
	protected void handleModule(OpenCmsModule ocmsModule) {
		for (String resourcePath : ocmsModule.getModuleResources()) {
			handleModuleResourcePath(ocmsModule, resourcePath);
		}
	}

	/**
	 * Abstract method handling resources contained in OpenCms module resource paths
	 * @param ocmsModule the OpenCms module the resource is contained in
	 * @param file IntelliJ file representing the resource in the real file system
	 */
	protected abstract void handleModuleResource(OpenCmsModule ocmsModule, File file);

	/**
	 * Abstract method handling resource paths
	 * @param ocmsModule the OpenCms module containing the file
	 * @param moduleResourceVfsPath the VFS file path
	 */
	protected abstract void handleModuleResourcePath(OpenCmsModule ocmsModule, String moduleResourceVfsPath);

	/**
	 * Utility method used to check if a resource is ignored due to the ignored files/folders configuration
	 * @param config the project level plugin configuration data
	 * @param file Java file instance representing the resource to be checked
	 * @return <code>true</code> if the resource is ignored, <code>false</code> otherwise
	 */
	public static boolean fileOrPathIsIgnored(OpenCmsPluginConfigurationData config, final File file) {
		final String path = file.getPath();
		final String filename = file.getName();
		return fileOrPathIsIgnored(config, path, filename);
	}

	/**
	 * Utility method used to check if a resource is ignored due to the ignored files/folders configuration
	 * @param config the project level plugin configuration data
	 * @param path full file system path of the file to check
	 * @param filename name of the file to check
	 * @return <code>true</code> if the resource is ignored, <code>false</code> otherwise
	 */
	public static boolean fileOrPathIsIgnored(OpenCmsPluginConfigurationData config, final String path, String filename) {
		for (String ignoredPath : config.getIgnoredPathsArray()) {
			if (path.matches(".*/"+ignoredPath+"(/.*)?")) {
				LOG.info("path " + ignoredPath + " is ignored");
				return true;
			}
		}

		for (String ignoredFilename : config.getIgnoredFilesArray()) {
			if (filename.matches(ignoredFilename)) {
				LOG.info("file " + ignoredFilename + " is ignored");
				return true;
			}
		}
		return false;
	}

	/**
	 * Used to check if there were warnings during file analysis and handling.
	 * @return <code>true</code> if there were warnings, <code>false</code> otherwise
	 * @see #getWarnings()
	 */
	public boolean hasWarnings() {
		return warnings.length() > 0;
	}

	/**
	 * Returns the warnings collected during file analysis and handling.
	 * @return StringBuilder containing warning messages, empty SringBuilder if no warnings were generated
	 * @see #hasWarnings()
	 */
	public StringBuilder getWarnings() {
		return warnings;
	}

}
