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

package com.mediaworx.intellij.opencmsplugin.sync;

/**
 * Action to take during sync
 * <ul>
 *     <li>PUSH - local file will be copied to OpenCms VFS</li>
 *     <li>PULL - OpenCms VFS file will be copied to the local file system</li>
 *     <li>DELETE_RFS - the file is to be deleted from the real file system</li>
 *     <li>DELETE_VFS - the file is to be deleted from the OpenCms VFS</li>
 * </ul>
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

	/**
	 * @return <code>true</code> if the SyncAction is either DELETE_RFS or DELETE_VFS, <code>false</code> otherwise
	 */
	public boolean isDeleteAction() {
		return this == DELETE_RFS || this == DELETE_VFS;
	}
}
