package com.mediaworx.intellij.opencmsplugin.actions.groups;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorPopupMenu extends DefaultActionGroup {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.EditorPopupSyncAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.EditorPopupPublishAction";

	private OpenCmsPlugin plugin;
	private ActionManager actionManager;

	public OpenCmsEditorPopupMenu(OpenCmsPlugin plugin, String shortName) {
		super(shortName, true);
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}

	private void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
	}

	public void unregisterActions() {
		actionManager.unregisterAction(SYNC_FILE_ID);
		actionManager.unregisterAction(PUBLISH_FILE_ID);
	}

}
