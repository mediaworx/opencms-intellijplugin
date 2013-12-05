package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.groups.OpenCmsMenu;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import com.mediaworx.intellij.opencmsplugin.actions.tools.FileTypeCounter;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);

	private static final HashSet<String> SYNC_SELECTED_ACTION_IDS = new HashSet<String>();
	static {
		SYNC_SELECTED_ACTION_IDS.add(OpenCmsMenu.SYNC_ID);
		SYNC_SELECTED_ACTION_IDS.add(OpenCmsPlugin.PROJECT_POPUP_SYNC_ID);
	}

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] syncFiles = getSyncFileArray(event);
			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			String actionId = event.getActionManager().getId(this);
			if (!SYNC_SELECTED_ACTION_IDS.contains(actionId)) {
				ocmsSyncer.setSkipConfirmDialog(true);
			}
			ocmsSyncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	private VirtualFile[] getSyncFileArray(AnActionEvent event) {
		VirtualFile[] syncFiles;

		String actionId = event.getActionManager().getId(this);

		// sync all open Tabs
		if (actionId.equals(OpenCmsMenu.SYNC_OPEN_TABS_ID) || actionId.equals(OpenCmsPlugin.TAB_POPUP_SYNC_OPEN_TABS_ID)) {
			syncFiles = ActionTools.getOpenTabsFileArray();
		}
		// sync all Modules
		else if (actionId.equals(OpenCmsMenu.SYNC_ALL_MODULES_ID)) {
			syncFiles = ActionTools.getAllModulesFileArray(plugin);
		}
		// sync a specific module
		else if (actionId.startsWith(OpenCmsMenu.PUBLISH_MODULE_ID_PREFIX)) {
			syncFiles = new VirtualFile[1];
			String moduleRoot = actionId.substring(OpenCmsMenu.PUBLISH_MODULE_ID_PREFIX.length());
			syncFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
		}
		// sync selected modules/folders/files
		else {
			syncFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		}
		return syncFiles;
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		String actionId = event.getActionManager().getId(this);

		if (actionId == null) {
			return;
		}

		if (SYNC_SELECTED_ACTION_IDS.contains(actionId)) {
			boolean enableAction = false;
			VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

			FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);

			if (selectedFiles != null && selectedFiles.length > 0) {

				fileTypeCounter.count(selectedFiles);

				if (fileTypeCounter.hasEntities()) {
					enableAction = true;
				}
			}

			String actionText = "_Sync selected " + fileTypeCounter.getEntityNames();
			event.getPresentation().setText(actionText);
			if (enableAction) {
				event.getPresentation().setEnabled(true);
			}
			else {
				event.getPresentation().setEnabled(false);
			}
		}
	}

}
