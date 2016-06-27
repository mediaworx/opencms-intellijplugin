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

package com.mediaworx.intellij.opencmsplugin.actions.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Action to sync all OpenCms resources selected in the project tree to/from the OpenCms VFS.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncSelectedAction extends OpenCmsSyncAction {

	/**
	 * @param event the action event, provided by IntelliJ
	 * @return An Array containing the file(s) selected in the project tree
	 */
	@Override
	protected List<File> getSyncFiles(@NotNull AnActionEvent event) {
		return PluginTools.getRealFilesFromVirtualFiles(event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY));
	}

	/**
	 * Sets the action text depending on the selection (e.g. "Sync selected Files", "Sync selected Folder",
	 * "Sync selected Modules/Folders/Files").
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		if (isPluginEnabled()) {
			ActionTools.setSelectionSpecificActionText(event, plugin, "_Sync");
		}
	}

	/**
	 * Sets the syncer option "showConfirmDialog" to <code>true</code> so that the user has to confirm the files to
	 * be synced.
	 * @param syncer    the OpenCmsSyncer to configure
	 */
	@Override
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
		syncer.setShowConfirmDialog(true);
	}
}
