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

package com.mediaworx.intellij.opencmsplugin.actions.pullmetadata;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncAction;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

/**
 * Parent action for all actions used to pull meta data for OpenCms modules. The funtionality to pull meta data is
 * contained in the sync actions (meta data is pulled during sync as well), so all "Pull Meta Data" action actually
 * trigger a Sync action with a the Syncer option "PullMetaDataOnly" set to <code>true</code>.
 */
@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsPullMetaDataAction extends OpenCmsSyncAction {

	/**
	 * Sets the Syncer option "PullMetaDataOnly" to <code>true</code>.
	 * @param syncer    the OpenCmsSyncer to configure
	 */
	@Override
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
		syncer.setPullMetaDataOnly(true);
	}

	/**
	 * Enables the action if the "Pull Meta Data" is enabled in the plugin's configration.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		if (isPluginEnabled() && isPullMetaDataEnabled()) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}

}
