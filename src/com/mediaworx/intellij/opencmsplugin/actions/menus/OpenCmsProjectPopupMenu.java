package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.generatemanifest.OpenCmsGenerateSelectedModuleManifestsAction;
import com.mediaworx.intellij.opencmsplugin.actions.packagemodule.OpenCmsPackageSelectedModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullSelectedModuleMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsProjectPopupMenu extends OpenCmsMenu {

	private static final String SYNC_SELECTED_ID = "OpenCmsPlugin.ProjectPopupSyncAction";
	private static final String PULL_METADATA_ID = "OpenCmsPlugin.ProjectPopupPullModuleMetaDataAction";
	private static final String PUBLISH_SELECTED_ID = "OpenCmsPlugin.ProjectPopupPublishAction";
	private static final String GENERATE_SELECTED_MODULE_MANIFEST_ID = "OpenCmsPlugin.ProjectPopupGenerateManifestAction";
	private static final String PACKAGE_SELECTED_MODULE_ID = "OpenCmsPlugin.ProjectPopupPackageModuleAction";

	public OpenCmsProjectPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, "Project specific OpenCms actions", true);
	}

	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_SELECTED_ID, new OpenCmsSyncSelectedAction(), "_Sync selected Modules/Folders/Files");
		plugin.addAction(this, PULL_METADATA_ID, new OpenCmsPullSelectedModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
		plugin.addAction(this, PUBLISH_SELECTED_ID, new OpenCmsPublishSelectedAction(), "_Publish selected Modules/Folders/Files");
		plugin.addAction(this, GENERATE_SELECTED_MODULE_MANIFEST_ID, new OpenCmsGenerateSelectedModuleManifestsAction(), "_Generate manifest.xml for selected modules");
		plugin.addAction(this, PACKAGE_SELECTED_MODULE_ID, new OpenCmsPackageSelectedModulesAction(), "Package Module _Zip for selected modules");
	}

}
