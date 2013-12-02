package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

public class OpenCmsPullAllMetaDataAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPullAllMetaDataAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		super.actionPerformed(event);
		LOG.info("actionPerformed - event: " + event);

		try {
			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.setPullMetaDataOnly(true);
			ocmsSyncer.syncAllModules();
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);

		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		if (config.isPluginConnectorEnabled() && config.isPullMetadataEnabled()) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}
}
