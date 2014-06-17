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

package com.mediaworx.intellij.opencmsplugin.opencms;

public class OpenCmsModuleExportPoint {

	private String vfsSource;
	private String rfsTarget;

	public OpenCmsModuleExportPoint(String vfsSource, String rfsTarget) {
		this.vfsSource = vfsSource;
		this.rfsTarget = rfsTarget;
	}

	/**
	 * Returns the VFS resource path for this export point
	 * @return  the VFS resource path for this export point
	 */
	public String getVfsSource() {
		return vfsSource;
	}

	/**
	 * Returns the path in the real file system, relative to the webapp
	 * @return  the path in the real file system, relative to the webapp
	 */
	public String getRfsTarget() {
		return rfsTarget;
	}


	/**
	 * returns the export path for  the given resource relative to the webapp.
	 * @param resourcePath  path to the resource within the export point
	 * @return  export path for the resource relative to the webapp
	 */
	public String getTargetPathForVfsResource(String resourcePath) {
		if (!resourcePath.startsWith(vfsSource)) {
			return null;
		}
		return rfsTarget + resourcePath.substring(vfsSource.length());
	}
}
