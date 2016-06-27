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

package com.mediaworx.intellij.opencmsplugin.opencms;

import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;

/**
 * Bean linking a resource path to its corresponding OpenCms module
 */
public class OpenCmsModuleResource {

	private OpenCmsModule openCmsModule;
	private String resourcePath;

	/**
	 * @param openCmsModule the OpenCms module this resource belongs to
	 * @param resourcePath  the resource path (VFS relative path)
	 */
	public OpenCmsModuleResource(OpenCmsModule openCmsModule, String resourcePath) {
		this.openCmsModule = openCmsModule;
		this.resourcePath = PluginTools.ensureUnixPath(resourcePath);
	}

	/**
	 * @return the OpenCms module this resource belongs to
	 */
	public OpenCmsModule getOpenCmsModule() {
		return openCmsModule;
	}

	/**
	 * @return the resource path (VFS relative path)
	 */
	public String getResourcePath() {
		String resourcePath = this.resourcePath.replaceFirst("/$", ""); // strip trailing slash
		return PluginTools.addVfsSiteRootToLocalPath(openCmsModule, resourcePath);
	}

}
