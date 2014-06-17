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
 * The FolderSyncMode describes the way folders are treated during sync. If a folder exists on both VFS and RFS, its
 * FolderSyncMode is AUTO, so all child resources will be checked for existence; if it exists only on the RFS, the sync
 * mode is PUSH (and hence all child resources can be pushed without checking if they exist on the VFS); if the folder
 * exists only on the VFS the sync mode is PULL (and hence all child resources can be pulled without checking if they
 * exist on the RFS).
 */
public enum FolderSyncMode {
	AUTO,
	PUSH,
	PULL
}
