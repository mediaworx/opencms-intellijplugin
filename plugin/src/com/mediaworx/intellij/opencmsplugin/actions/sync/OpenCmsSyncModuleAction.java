package com.mediaworx.intellij.opencmsplugin.actions.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsSyncAction;
import com.mediaworx.intellij.opencmsplugin.actions.groups.OpenCmsMenu;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncModuleAction extends OpenCmsSyncAction {

	@Override
	protected VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event) {
		String actionId = event.getActionManager().getId(this);
		VirtualFile[] syncFiles = new VirtualFile[1];
		String moduleRoot = actionId.substring(OpenCmsMenu.SYNC_MODULE_ID_PREFIX.length());
		syncFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
		return syncFiles;
	}
}
