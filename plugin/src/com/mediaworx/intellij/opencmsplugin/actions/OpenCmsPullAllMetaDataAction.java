package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import com.mediaworx.intellij.opencmsplugin.sync.SyncJob;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPullAllMetaDataAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPullAllMetaDataAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		super.actionPerformed(event);
		LOG.info("actionPerformed - event: " + event);

		try {
			cleanupMetaFolders();

			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.setPullMetaDataOnly(true);
			ocmsSyncer.syncAllModules();
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAllAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	private void cleanupMetaFolders() {
		Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
		for (OpenCmsModule ocmsModule : ocmsModules) {
			SyncJob.cleanupModuleMetaFolder(ocmsModule);
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
