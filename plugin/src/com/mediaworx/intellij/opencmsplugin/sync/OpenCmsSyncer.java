package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpenCmsSyncer {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncer.class);

	OpenCmsPlugin plugin;
	OpenCmsPluginConfigurationData config;

	boolean skipConfirmDialog = false;
	boolean pullMetaDataOnly = false;

	public OpenCmsSyncer(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		config = plugin.getPluginConfiguration();

		FileDocumentManager.getInstance().saveAllDocuments();
		FileDocumentManager.getInstance().reloadFiles();
	}


	public void syncAllModules() {

		skipConfirmDialog = true;

		Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();

		// First put all valid module paths in a List
		List<VirtualFile> moduleRoots = new ArrayList<VirtualFile>();

		for (OpenCmsModule ocmsModule : ocmsModules) {
			VirtualFile moduleRoot = LocalFileSystem.getInstance().findFileByPath(ocmsModule.getIntelliJModuleRoot());
			moduleRoots.add(moduleRoot);
		}

		// then sync all valid modules
		try {
			syncFiles(moduleRoots.toArray(new VirtualFile[moduleRoots.size()]));
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage(), t);
		}
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

		SyncJob syncJob = new SyncJob(plugin, analyzer.getSyncList());
		int numSyncEntities = syncJob.numSyncEntities();

		boolean proceed = syncJob.hasSyncEntities();
		LOG.info("proceed? " + proceed);

		StringBuilder message = new StringBuilder();
		if (analyzer.hasWarnings()) {
			message.append("Infos/Warnings during file analysis:\n").append(analyzer.getWarnings().append("\n"));
		}
		if (proceed) {
			if (!skipConfirmDialog && !pullMetaDataOnly && ((numSyncEntities == 1 && message.length() > 0) || numSyncEntities > 1)) {
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

	public void setPullMetaDataOnly(boolean pullMetaDataOnly) {
		this.pullMetaDataOnly = pullMetaDataOnly;
	}

}
