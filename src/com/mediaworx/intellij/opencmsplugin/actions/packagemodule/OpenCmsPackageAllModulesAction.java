package com.mediaworx.intellij.opencmsplugin.actions.packagemodule;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPackageAllModulesAction extends OpenCmsPackageModuleAction {

	@Override
	protected VirtualFile[] getModuleFileArray(@NotNull AnActionEvent event) {
		return ActionTools.getAllModulesFileArray(plugin);
	}
}
