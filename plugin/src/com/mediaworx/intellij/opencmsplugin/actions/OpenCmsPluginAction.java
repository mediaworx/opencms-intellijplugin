package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;

public abstract class OpenCmsPluginAction extends AnAction {

	protected Project project;
	protected OpenCmsPlugin plugin;

	@Override
	public void actionPerformed(AnActionEvent event) {
		project = DataKeys.PROJECT.getData(event.getDataContext());
		if (project == null) {
			return;
		}
		plugin = project.getComponent(OpenCmsPlugin.class);
	}
}
