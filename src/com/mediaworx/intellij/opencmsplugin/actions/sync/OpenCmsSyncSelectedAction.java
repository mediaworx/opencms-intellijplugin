package com.mediaworx.intellij.opencmsplugin.actions.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncSelectedAction extends OpenCmsSyncAction {

	@Override
	protected VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event) {
		return event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		if (isPluginEnabled()) {
			ActionTools.setSelectionSpecificActionText(event, plugin, "_Sync");
		}
	}

	/**
	 * sets the syncer option "showConfirmDialog" to true
	 * @param syncer    the OpenCmsSyncer to configure
	 */
	@Override
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
		syncer.setShowConfirmDialog(true);
	}
}
