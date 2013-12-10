package com.mediaworx.intellij.opencmsplugin.actions.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMainMenu;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncModuleAction extends OpenCmsSyncAction {

	@Override
	protected VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event) {
		VirtualFile[] syncFiles = new VirtualFile[1];
		String actionId = event.getActionManager().getId(this);
		String moduleRoot = actionId.substring(OpenCmsMainMenu.SYNC_MODULE_ID_PREFIX.length());
		syncFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
		return syncFiles;
	}
}
