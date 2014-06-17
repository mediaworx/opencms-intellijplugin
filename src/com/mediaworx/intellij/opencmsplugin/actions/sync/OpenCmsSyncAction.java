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

package com.mediaworx.intellij.opencmsplugin.actions.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] syncFiles = getSyncFileArray(event);
			OpenCmsSyncer syncer = new OpenCmsSyncer(plugin);
			setSyncerOptions(syncer);
			syncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	protected abstract VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event);

	/**
	 * sets options for the given OpenCmsSyncer. Does nothing by default (the syncer works with default actions)
	 * but may be overridden by implementing classes
	 * @param syncer    the OpenCmsSyncer to configure
	 */
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
	}

}
