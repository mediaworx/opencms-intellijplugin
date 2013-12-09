package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.groups.OpenCmsMenu;
import com.mediaworx.intellij.opencmsplugin.actions.tools.ActionTools;
import com.mediaworx.intellij.opencmsplugin.actions.tools.FileTypeCounter;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPullMetaDataAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPullMetaDataAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] selectedFiles = getPullFileArray(event);

			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.setPullMetaDataOnly(true);
			ocmsSyncer.syncFiles(selectedFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsPullMetaDataAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	private VirtualFile[] getPullFileArray(AnActionEvent event) {
		VirtualFile[] pullFiles;

		String actionId = event.getActionManager().getId(this);

		// pull all meta data
		if (actionId.equals(OpenCmsMenu.PULL_ALL_METADATA_ID)) {
			pullFiles = ActionTools.getAllModulesFileArray(plugin);
		}
		// pull meta data for specific module
		else {
			pullFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		}
		return pullFiles;
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		if (!config.isPluginConnectorEnabled() || !config.isPullMetadataEnabled()) {
			// Stop if the connector or pulling meta data is disabled
			event.getPresentation().setEnabled(false);
			return;
		}

		String actionId = event.getActionManager().getId(this);

		// there's no update needed for the "pull all meta data" action, the action is always enabled
		if (actionId.equals(OpenCmsMenu.PULL_ALL_METADATA_ID)) {
			event.getPresentation().setEnabled(true);
			return;
		}

		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

		boolean enableAction = true;

		if (selectedFiles != null) {
			// check if only module roots have been selected
			for (VirtualFile ideaVFile : selectedFiles) {
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);
				if (ocmsModule == null || !ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
					enableAction = false;
					break;
				}
			}
		}
		else {
			enableAction = false;
		}

		if (enableAction) {
			FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);
			fileTypeCounter.count(selectedFiles);
			event.getPresentation().setText("_Pull Meta Data for selected " + fileTypeCounter.getEntityNames());
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}
}