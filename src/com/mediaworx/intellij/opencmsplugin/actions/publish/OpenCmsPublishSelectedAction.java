package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPublishSelectedAction extends OpenCmsPublishAction {

	@Override
	protected VirtualFile[] getPublishFileArray(AnActionEvent event) {
		return event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		if (isPluginEnabled() && isConnectorEnabled()) {
			ActionTools.setSelectionSpecificActionText(event, plugin, "_Publish");
		}
	}
}
