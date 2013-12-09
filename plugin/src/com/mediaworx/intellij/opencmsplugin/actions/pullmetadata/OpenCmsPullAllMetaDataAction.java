package com.mediaworx.intellij.opencmsplugin.actions.pullmetadata;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPullAllMetaDataAction extends OpenCmsPullMetaDataAction {

	@Override
	protected VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event) {
		return ActionTools.getAllModulesFileArray(plugin);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		if (isEnabled()) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}
}
