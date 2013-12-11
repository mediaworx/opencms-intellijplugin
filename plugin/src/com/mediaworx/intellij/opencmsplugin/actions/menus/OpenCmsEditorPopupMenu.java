package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsEditorPopupMenu extends OpenCmsMenu {

	private static final String SYNC_FILE_ID = "OpenCmsPlugin.EditorPopupSyncAction";
	private static final String PUBLISH_FILE_ID = "OpenCmsPlugin.EditorPopupPublishAction";

	public OpenCmsEditorPopupMenu(OpenCmsPlugin plugin) {
		super(plugin, "Editor specific OpenCms actions", true);
	}

	@Override
	protected void registerActions() {
		plugin.addAction(this, SYNC_FILE_ID, new OpenCmsSyncSelectedAction(), "_Sync File");
		plugin.addAction(this, PUBLISH_FILE_ID, new OpenCmsPublishSelectedAction(), "_Publish File");
	}

	@Override
	public void update(AnActionEvent event) {
		super.update(event);
		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		boolean enableMenu;
		if (selectedFiles != null && selectedFiles.length == 1) {
			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(selectedFiles[0]);
			enableMenu = ocmsModule.isIdeaVFileInVFSPath(selectedFiles[0]);
		}
		else {
			enableMenu = false;
		}
		Logger.getInstance(OpenCmsEditorPopupMenu.class).info("presentation: " + event.getPresentation() + " - enabled: " + enableMenu);
		event.getPresentation().setEnabled(enableMenu);
	}

}
