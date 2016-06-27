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

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

/**
 * Used to create the OpenCms menu located in the editor tab popup menu containing the following actions:
 * <ul>
 *     <li>Sync File</li>
 *     <li>Sync all open Editor Tabs</li>
 *     <li>Publish File</li>
 *     <li>Publish all open Editor Tabs</li>
 * </ul>
 *
 * The file actions are context aware, they are disabled if the right clicked tab's file is not within an OpenCms
 * module's resource path.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorTabPopupMenu extends OpenCmsMenu {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.TabsPopupSyncAction";
	private static final String SYNC_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupSyncOpenTabsAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.TabsPopupPublishAction";
	private static final String PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupPublishOpenTabsAction";

	/**
	 * Creates the OpenCms menu for the editor tab popup menu
	 * @param plugin the OpenCms plugin instance
	 */
	public OpenCmsEditorTabPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, "Editor Tab specific OpenCms Actions", true);
	}

	/**
	 * Registers the actions for the OpenCms menu in the editor tab popup menu (Sync file / open tabs, Pulish file /
	 * open tabs).
	 */
	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenEditorTabsAction(), "Sync all open Editor _Tabs");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
		plugin.addAction(this, PUBLISH_OPEN_TABS_ID, new OpenCmsPublishOpenEditorTabsAction(), "Publish all open Editor Tabs");
	}

}
