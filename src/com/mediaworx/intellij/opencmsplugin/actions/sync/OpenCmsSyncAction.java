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
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Parent action for all actions used to sync module resources to/from the OpenCms VFS
 */
@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);

	/**
	 * Triggers the sync of module resources depending on the menu entry the user chose. Which resources are to
	 * be synced is determined by calling the abstract method
	 * {@link #getSyncFiles(com.intellij.openapi.actionSystem.AnActionEvent)} that's implemented by subclasses.
	 */
	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			List<File> syncFiles = getSyncFiles(event);
			if (syncFiles.size() > 0) {
				clearConsole();
			}
			OpenCmsSyncer syncer = new OpenCmsSyncer(plugin);
			setSyncerOptions(syncer);

			syncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	/**
	 * Abstract method to determine the module resources to be synced, implemented in subclasses.
	 * @param event the action event, provided by IntelliJ
	 * @return  An array with virtual files representing OpenCms module resources
	 */
	protected abstract List<File> getSyncFiles(@NotNull AnActionEvent event);

	/**
	 * Sets options for the given OpenCmsSyncer. Does nothing by default (the syncer works with default actions)
	 * but may be overridden by implementing classes.
	 * @param syncer    the OpenCmsSyncer to configure
	 */
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
	}

}
