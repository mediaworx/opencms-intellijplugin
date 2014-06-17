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

import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

public class OpenCmsModuleResource {

	private OpenCmsModule openCmsModule;
	private String resourcePath;

	public OpenCmsModuleResource(OpenCmsModule openCmsModule, String resourcePath) {
		this.openCmsModule = openCmsModule;
		this.resourcePath = resourcePath;
	}

	public OpenCmsModule getOpenCmsModule() {
		return openCmsModule;
	}

	public String getResourcePath() {
		return resourcePath.replaceFirst("/$", ""); // strip trailing slash
	}

}
