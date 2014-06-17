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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;

import java.util.List;

public class OpenCmsSyncer {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncer.class);

	OpenCmsPlugin plugin;
	OpenCmsPluginConfigurationData config;

	boolean showConfirmDialog = false;
	boolean pullMetaDataOnly = false;

	public OpenCmsSyncer(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();

		FileDocumentManager.getInstance().saveAllDocuments();
		FileDocumentManager.getInstance().reloadFiles();
	}


	public void syncFiles(VirtualFile[] syncFiles) {

		SyncFileAnalyzer analyzer;

		try {
			analyzer = new SyncFileAnalyzer(plugin, syncFiles, pullMetaDataOnly);
		}
		catch (CmsConnectionException e) {
			Messages.showDialog(e.getMessage(), "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
			return;
		}

		ProgressManager.getInstance().runProcessWithProgressSynchronously(analyzer, "Analyzing local and VFS syncFiles and folders ...", true, plugin.getProject());

		if (!analyzer.isExecuteSync()) {
			return;
		}

		int numSyncEntities = analyzer.getSyncList().size();

		boolean proceed = numSyncEntities > 0;
		LOG.info("proceed? " + proceed);

		StringBuilder message = new StringBuilder();
		if (analyzer.hasWarnings()) {
			message.append("Infos/Warnings during file analysis:\n").append(analyzer.getWarnings().append("\n"));
		}
		if (proceed) {
			SyncJob syncJob = new SyncJob(plugin, analyzer.getSyncList());
			if (showConfirmDialog && !pullMetaDataOnly && ((numSyncEntities == 1 && message.length() > 0) || numSyncEntities > 1)) {
				assembleConfirmMessage(message, syncJob.getSyncList());
				int dlgStatus = Messages.showOkCancelDialog(plugin.getProject(), message.toString(), "Start OpenCms VFS Sync?", Messages.getQuestionIcon());
				proceed = dlgStatus == 0;
			}
			if (proceed) {
				plugin.showConsole();
				new Thread(syncJob).start();
			}
		}
		else {
			message.append("Nothing to sync");
			Messages.showMessageDialog(message.toString(), "OpenCms VFS Sync", Messages.getInformationIcon());
		}
	}

	private void assembleConfirmMessage(StringBuilder message, List<SyncEntity> syncEntities) {
		int numSyncEntities = syncEntities.size();
		if (message.length() > 0) {
			message.append("\n");
		}
		message.append("The following ").append(numSyncEntities).append(" syncFiles or folders will be synced to or from OpenCms VFS:\n\n");
		for (SyncEntity syncEntity : syncEntities) {
			String suffix = syncEntity.replaceExistingEntity() ? "(changed)" : syncEntity.getSyncAction().isDeleteAction() ? "(obsolete)" : "(new)";
			message.append(syncEntity.getSyncAction().getDescription()).append(" ").append(syncEntity.getVfsPath()).append(" ").append(suffix).append("\n");
		}
		message.append("\nProceed?");
	}

	public void setShowConfirmDialog(boolean showConfirmDialog) {
		this.showConfirmDialog = showConfirmDialog;
	}

	public void setPullMetaDataOnly(boolean pullMetaDataOnly) {
		this.pullMetaDataOnly = pullMetaDataOnly;
	}

}
