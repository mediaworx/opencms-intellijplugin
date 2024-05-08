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

package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.jetbrains.annotations.NotNull;

/**
 * Used to create the OpenCms menu located in the editor popup menu containing the following actions:
 * <ul>
 *     <li>Sync File</li>
 *     <li>Publish File</li>
 * </ul>
 *
 * The OpenCms menu is context aware, it is disabled if the file in the editor is not within an OpenCms module's
 * resource path.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorPopupMenu extends OpenCmsMenu {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.EditorPopupSyncAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.EditorPopupPublishAction";

	/**
	 * Creates the OpenCms menu for the editor popup menu
	 * @param plugin the OpenCms plugin instance
	 */
	public OpenCmsEditorPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, "Editor specific OpenCms actions", true);
	}

	/**
	 * Registers the actions for the OpenCms menu in the editor popup menu (Sync, Pulish).
	 */
	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
	}

	/**
	 * Checks if the file in the editor is within an OpenCms module's resource path and disables the actions if not.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		boolean enableMenu;
		if (selectedFiles != null && selectedFiles.length == 1) {
			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForPath(selectedFiles[0].getPath());
			enableMenu = ocmsModule != null && ocmsModule.isPathInVFSPath(selectedFiles[0].getPath());
		}
		else {
			enableMenu = false;
		}
		Logger.getInstance(OpenCmsEditorPopupMenu.class).info("presentation: " + event.getPresentation() + " - enabled: " + enableMenu);
		event.getPresentation().setEnabled(enableMenu);
	}

}
