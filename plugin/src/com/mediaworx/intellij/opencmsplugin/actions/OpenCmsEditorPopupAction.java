package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.sync.FileSyncer;

/**
 * defines the action for the editor popup menu
 */
public class OpenCmsEditorPopupAction extends AnAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsEditorPopupAction.class);

	/**
	 * syncs the file in the editor with OpenCms
	 * @param event the event, provided by IntelliJ
	 */
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);

		Project project = DataKeys.PROJECT.getData(event.getDataContext());
		if (project == null) {
			return;
		}
		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);
		FileSyncer fileSyncer = new FileSyncer(plugin);

		try {
			VirtualFile[] syncFiles = new VirtualFile[1];
			syncFiles[0] = event.getData(PlatformDataKeys.VIRTUAL_FILE);
			fileSyncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.error("Exception in OpenCmsEditorPopupAction.actionPerformed: " + t.getMessage(), t);
		}
	}
}
