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
 * represents the SyncMode configured by the user
 * <ul>
 *     <li>
 *         PUSH: If the sync mode PUSH is configured, the local file system is the master. All changes done to resources
 *         in OpenCms will be overwritten by the files from the local file system. VFS resources not existing locally
 *         will be deleted from the VFS. Properties set on resources in the VFS however will be left untouched by sync
 *         actions.
 *     </li>
 *     <li>
 *         SYNC: If the sync mode SYNC is configured, files are synced to and from the VFS based on the modification
 *         date. The local file system modificaton date is compared to the resources moduification date in the VFS.
 *         That way changes can be made to your local files and in OpenCms.
 *     </li>
 *     <li>
 *         PULL: If the sync mode PULL is configured, the OpenCms VFS is the master. All changes made to local files
 *         are overwritten by the corresponding files in the VFS when syncing. Local resources that don't exist in the
 *         VFS will be deleted from the real file system.
 *     </li>
 * </ul>
 */
public enum SyncMode {
	PUSH,
	SYNC,
	PULL
}
