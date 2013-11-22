package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.sync.ModuleSyncer;

public class OpenCmsSyncAllAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAllAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);

		Project project = DataKeys.PROJECT.getData(event.getDataContext());

		if (project == null) {
			return;
		}
		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);

		LOG.info("Sync all modules");

		ModuleSyncer moduleSyncer = new ModuleSyncer(plugin);
		moduleSyncer.syncAllModules();

	}
}
