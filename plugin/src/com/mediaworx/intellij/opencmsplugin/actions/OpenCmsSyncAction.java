package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.sync.FileSyncer;

public class OpenCmsSyncAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsEditorPopupAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);

		Project project = DataKeys.PROJECT.getData(event.getDataContext());
		if (project == null) {
			return;
		}
		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);
        FileSyncer fileSyncer = new FileSyncer(plugin);

        try {
	        fileSyncer.syncFiles(event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY));
        }
        catch (Throwable t) {
	        LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
        }
	}

}
