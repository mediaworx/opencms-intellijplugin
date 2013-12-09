package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;

public class OpenCmsPublishAllModulesAction extends OpenCmsPublishAction {

	@Override
	protected VirtualFile[] getPublishFileArray(AnActionEvent event) {
		return ActionTools.getAllModulesFileArray(plugin);
	}
}
