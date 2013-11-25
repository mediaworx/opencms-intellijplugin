package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;

/** defines the action for the editor popup menu */
public class OpenCmsEditorPopupAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsEditorPopupAction.class);

	/**
	 * syncs the file in the editor with OpenCms
	 *
	 * @param event the event, provided by IntelliJ
	 */
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] syncFiles = new VirtualFile[1];
			syncFiles[0] = event.getData(PlatformDataKeys.VIRTUAL_FILE);
			OpenCmsSyncer fileSyncer = new OpenCmsSyncer(plugin);
			fileSyncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsEditorPopupAction.actionPerformed: " + t.getMessage(), t);
		}
	}
}
