package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorTabPopupMenu extends OpenCmsMenu {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.TabsPopupSyncAction";
	private static final String SYNC_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupSyncOpenTabsAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.TabsPopupPublishAction";
	private static final String PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupPublishOpenTabsAction";

	public OpenCmsEditorTabPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, true);
	}

	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenEditorTabsAction(), "Sync all open Editor _Tabs");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
		plugin.addAction(this, PUBLISH_OPEN_TABS_ID, new OpenCmsPublishOpenEditorTabsAction(), "Publish all open Editor Tabs");
	}

}
