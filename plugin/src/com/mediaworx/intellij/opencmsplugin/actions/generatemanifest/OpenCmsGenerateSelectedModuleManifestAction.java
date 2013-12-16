package com.mediaworx.intellij.opencmsplugin.actions.generatemanifest;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsGenerateSelectedModuleManifestAction extends OpenCmsGenerateManifestAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsGenerateSelectedModuleManifestAction.class);

	@Override
	protected VirtualFile[] getModuleFileArray(@NotNull AnActionEvent event) {
		return event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		ActionTools.setOnlyModulesSelectedPresentation(event, "_Generate manifest.xml for");
	}

}
