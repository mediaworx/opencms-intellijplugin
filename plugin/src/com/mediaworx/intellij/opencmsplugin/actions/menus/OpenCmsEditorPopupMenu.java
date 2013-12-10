package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorPopupMenu extends OpenCmsMenu {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.EditorPopupSyncAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.EditorPopupPublishAction";

	public OpenCmsEditorPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, true);
	}

	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
	}

	@Override
	public void unregisterActions() {
		actionManager.unregisterAction(SYNC_FILE_ID);
		actionManager.unregisterAction(PUBLISH_FILE_ID);
	}

}
