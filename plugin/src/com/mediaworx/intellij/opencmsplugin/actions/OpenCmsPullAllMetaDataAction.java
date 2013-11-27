package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;

public class OpenCmsPullAllMetaDataAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPullAllMetaDataAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		super.actionPerformed(event);
		LOG.info("actionPerformed - event: " + event);

		try {
			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.pullAllMetaData();
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage(), t);
		}
	}
}
