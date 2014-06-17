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

package com.mediaworx.intellij.opencmsplugin.sync;

/**
* Created with IntelliJ IDEA.
* User: widmann
* Date: 06.11.13
* Time: 15:21
* To change this template use File | Settings | File Templates.
*/
public enum SyncAction {
	PUSH("PUSH"),
	PULL("PULL"),
	DELETE_RFS("DELETE FROM RFS"),
	DELETE_VFS("DELETE FROM VFS");

	private String description;

	private SyncAction(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isDeleteAction() {
		return this == DELETE_RFS || this == DELETE_VFS;
	}
}
