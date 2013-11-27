package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import org.jetbrains.annotations.NotNull;

public abstract class OpenCmsPluginAction extends AnAction {

	protected Project project;
	protected OpenCmsPlugin plugin;

	@Override
	public void actionPerformed(AnActionEvent event) {
		init(event);
	}

	@Override
	public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
		// Do nothing to avoid calling update() twice (default behaviour for AnAction)
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		init(event);
		super.update(event);
	}

	private void init(AnActionEvent event) {
		project = DataKeys.PROJECT.getData(event.getDataContext());
		if (project == null) {
			return;
		}
		plugin = project.getComponent(OpenCmsPlugin.class);
		if (plugin.getConsole() != null) {
			plugin.getConsole().clear();
		}
	}
}
