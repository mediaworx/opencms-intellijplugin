package com.mediaworx.intellij.opencmsplugin.actions.groups;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullSelectedModuleMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsProjectPopupMenu extends DefaultActionGroup {

	private static final String SYNC_SELECTED_ID = "OpenCmsPlugin.ProjectPopupSyncAction";
	private static final String PULL_METADATA_ID = "OpenCmsPlugin.ProjectPopupPullModuleMetaDataAction";
	private static final String PUBLISH_SELECTED_ID = "OpenCmsPlugin.ProjectPopupPublishAction";

	private OpenCmsPlugin plugin;
	private ActionManager actionManager;

	public OpenCmsProjectPopupMenu(OpenCmsPlugin plugin, String shortName) {
		super(shortName, true);
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}

	private void registerActions() {
		plugin.addAction(this, SYNC_SELECTED_ID, new OpenCmsSyncSelectedAction(), "_Sync selected Modules/Folders/Files");
		plugin.addAction(this, PULL_METADATA_ID, new OpenCmsPullSelectedModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
		plugin.addAction(this, PUBLISH_SELECTED_ID, new OpenCmsPublishSelectedAction(), "_Publish selected Modules/Folders/Files");
	}

	public void unregisterActions() {
		actionManager.unregisterAction(SYNC_SELECTED_ID);
		actionManager.unregisterAction(PULL_METADATA_ID);
		actionManager.unregisterAction(PUBLISH_SELECTED_ID);
	}

}
