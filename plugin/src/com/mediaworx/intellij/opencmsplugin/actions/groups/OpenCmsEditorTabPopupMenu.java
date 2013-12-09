package com.mediaworx.intellij.opencmsplugin.actions.groups;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorTabPopupMenu extends DefaultActionGroup {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.TabsPopupSyncAction";
	private static final String SYNC_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupSyncOpenTabsAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.TabsPopupPublishAction";
	private static final String PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupPublishOpenTabsAction";

	private OpenCmsPlugin plugin;
	private ActionManager actionManager;

	public OpenCmsEditorTabPopupMenu(OpenCmsPlugin plugin, String shortName) {
		super(shortName, true);
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}

	private void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenEditorTabsAction(), "Sync all open Editor _Tabs");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
		plugin.addAction(this, PUBLISH_OPEN_TABS_ID, new OpenCmsPublishOpenEditorTabsAction(), "Publish all open Editor Tabs");
	}

	public void unregisterActions() {
		actionManager.unregisterAction(SYNC_FILE_ID);
		actionManager.unregisterAction(SYNC_OPEN_TABS_ID);
		actionManager.unregisterAction(PUBLISH_FILE_ID);
		actionManager.unregisterAction(PUBLISH_OPEN_TABS_ID);
	}

}
