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

package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.generatemanifest.OpenCmsGenerateSelectedModuleManifestsAction;
import com.mediaworx.intellij.opencmsplugin.actions.packagemodule.OpenCmsPackageSelectedModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullSelectedModuleMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsProjectPopupMenu extends OpenCmsMenu {

	private static final String SYNC_SELECTED_ID = "OpenCmsPlugin.ProjectPopupSyncAction";
	private static final String PULL_METADATA_ID = "OpenCmsPlugin.ProjectPopupPullModuleMetaDataAction";
	private static final String PUBLISH_SELECTED_ID = "OpenCmsPlugin.ProjectPopupPublishAction";
	private static final String GENERATE_SELECTED_MODULE_MANIFEST_ID = "OpenCmsPlugin.ProjectPopupGenerateManifestAction";
	private static final String PACKAGE_SELECTED_MODULE_ID = "OpenCmsPlugin.ProjectPopupPackageModuleAction";

	public OpenCmsProjectPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, "Project specific OpenCms actions", true);
	}

	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_SELECTED_ID, new OpenCmsSyncSelectedAction(), "_Sync selected Modules/Folders/Files");
		plugin.addAction(this, PULL_METADATA_ID, new OpenCmsPullSelectedModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
		plugin.addAction(this, PUBLISH_SELECTED_ID, new OpenCmsPublishSelectedAction(), "_Publish selected Modules/Folders/Files");
		plugin.addAction(this, GENERATE_SELECTED_MODULE_MANIFEST_ID, new OpenCmsGenerateSelectedModuleManifestsAction(), "_Generate manifest.xml for selected modules");
		plugin.addAction(this, PACKAGE_SELECTED_MODULE_ID, new OpenCmsPackageSelectedModulesAction(), "Package Module _Zip for selected modules");
	}

}
