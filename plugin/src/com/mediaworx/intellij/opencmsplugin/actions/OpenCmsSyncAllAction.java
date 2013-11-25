package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;

public class OpenCmsSyncAllAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAllAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		super.actionPerformed(event);
		LOG.info("actionPerformed - event: " + event);

		OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
		ocmsSyncer.syncAllModules();
	}
}
