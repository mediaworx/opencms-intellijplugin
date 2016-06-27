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
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMainMenu;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Action to sync a specific module that was selected from the OpenCms "Sync Module" menu.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncModuleAction extends OpenCmsSyncAction {

	/**
	 * Determines the module selected by the user and returns a corresponding file array containing one entry.
	 * @param event the action event, provided by IntelliJ
	 * @return Virtual file array containing exactly one file representing the module selected by the user
	 */
	@Override
	protected List<File> getSyncFiles(@NotNull AnActionEvent event) {
		List<File> syncFiles = new ArrayList<>(1);
		String actionId = event.getActionManager().getId(this);
		// the module's root path is contained in the action id (after a prefix)
		String moduleRoot = actionId.substring(OpenCmsMainMenu.SYNC_MODULE_ID_PREFIX.length());
		syncFiles.add(new File(moduleRoot));
		return syncFiles;
	}
}
