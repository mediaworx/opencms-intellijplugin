package com.mediaworx.intellij.opencmsplugin.actions.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsSyncAction;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsSyncOpenEditorTabsAction extends OpenCmsSyncAction {

	@Override
	protected VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event) {
		return ActionTools.getOpenTabsFileArray();
	}

}
