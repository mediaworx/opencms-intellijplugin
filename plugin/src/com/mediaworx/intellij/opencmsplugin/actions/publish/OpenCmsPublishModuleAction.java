package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPublishAction;
import com.mediaworx.intellij.opencmsplugin.actions.groups.OpenCmsMenu;

public class OpenCmsPublishModuleAction extends OpenCmsPublishAction {

	@Override
	protected VirtualFile[] getPublishFileArray(AnActionEvent event) {
		VirtualFile[] publishFiles = new VirtualFile[1];
		String actionId = event.getActionManager().getId(this);
		String moduleRoot = actionId.substring(OpenCmsMenu.PUBLISH_MODULE_ID_PREFIX.length());
		publishFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
		return publishFiles;
	}
}
