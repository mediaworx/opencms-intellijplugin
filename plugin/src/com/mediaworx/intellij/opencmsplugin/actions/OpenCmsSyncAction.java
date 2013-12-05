package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			String actionId = event.getActionManager().getId(this);

			VirtualFile[] syncFiles;

			if (!actionId.startsWith(OpenCmsMenu.SYNC_MODULE_ID_PREFIX)) {
				syncFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
			}
			else {
				syncFiles = new VirtualFile[1];
				String moduleRoot = actionId.substring(OpenCmsMenu.SYNC_MODULE_ID_PREFIX.length());
				syncFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
			}

			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		String actionId = event.getActionManager().getId(this);

		if (actionId == null) {
			return;
		}

		if (!actionId.startsWith(OpenCmsMenu.SYNC_MODULE_ID_PREFIX) && !actionId.equals(OpenCmsPlugin.TAB_POPUP_SYNC_ID)) {
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
