package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;

public class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsEditorPopupAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
	        OpenCmsSyncer fileSyncer = new OpenCmsSyncer(plugin);
			fileSyncer.syncFiles(event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY));
        }
        catch (Throwable t) {
	        LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
        }
	}
}
