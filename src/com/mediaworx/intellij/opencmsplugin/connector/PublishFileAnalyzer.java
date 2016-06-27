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

package com.mediaworx.intellij.opencmsplugin.connector;

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.tools.VfsFileAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS file analyzer used to analyze which of the entities selected in the IntelliJ project tree are to be published
 */
public class PublishFileAnalyzer extends VfsFileAnalyzer {

	List<String> publishList;

	/**
	 * Creates a new PublishFileAnalyzer
	 * @param plugin the OpenCms plugin instance
	 * @param files File array containing the files selected in IntelliJ's project tree
	 * @throws CmsConnectionException is needed because of the abstract classe's interface, but never thrown
	 */
	public PublishFileAnalyzer(OpenCmsPlugin plugin, List<File> files) throws CmsConnectionException {
		super(plugin, files);

		publishList = new ArrayList<String>();
	}

	/**
	 * handles module resources, adds the file's VFS path to the publish list
	 * @param ocmsModule the OpenCms module containing the file
	 * @param file IntelliJ's virtual file
	 */
	@Override
	protected void handleModuleResource(OpenCmsModule ocmsModule, File file) {
		publishList.add(ocmsModule.getVfsPathForFile(file));
		addHandledFile(file);
	}

	/**
	 * handles module resource paths, adds the given VFS path to the publish list
	 * @param ocmsModule the OpenCms module containing the file
	 * @param moduleResourceVfsPath the VFS file path
	 */
	@Override
	protected void handleModuleResourcePath(OpenCmsModule ocmsModule, String moduleResourceVfsPath) {
		publishList.add(moduleResourceVfsPath);
		addHandledFilePath(ocmsModule.getLocalVfsRoot() + moduleResourceVfsPath);
	}
	
	/**
	 * returns the list of VFS resource paths to be published
	 * @return the list of VFS resource paths to be published
	 */
	public List<String> getPublishList() {
		return publishList;
	}
}
