package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			String actionId = event.getActionManager().getId(this);

			VirtualFile[] syncFiles;

			if (!actionId.startsWith(OpenCmsMenu.SYNC_MODULE_ID_PREFIX)) {
				syncFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
			}
			else {
				syncFiles = new VirtualFile[1];
				String moduleRoot = actionId.substring(OpenCmsMenu.SYNC_MODULE_ID_PREFIX.length());
				syncFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
			}

			OpenCmsSyncer ocmsSyncer = new OpenCmsSyncer(plugin);
			ocmsSyncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	@Override
	public void update(@NotNull AnActionEvent event) {

		super.update(event);

		String actionId = event.getActionManager().getId(this);

		if (actionId == null) {
			return;
		}

		if (!actionId.startsWith(OpenCmsMenu.SYNC_MODULE_ID_PREFIX)) {
			boolean enableAction = false;
			VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

			int numFiles = 0;
			int numFolders = 0;
			int numModules = 0;

			if (selectedFiles != null && selectedFiles.length > 0) {

				// calculate the number of selected modules, folders and files
				for (VirtualFile ideaVFile : selectedFiles) {

					OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);

					if (ocmsModule == null) {
						continue;
					}

					if (ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
						numModules += 1;
					}
					else if (ocmsModule.isIdeaVFileInVFSPath(ideaVFile)) {
						if (ideaVFile.isDirectory()) {
							numFolders += 1;
						}
						else {
							numFiles += 1;
						}
					}
					// if we know that there are multiple modules, multiple folders and multiple files, then there's no reason to go on
					if (numModules > 1 && numFolders > 1 && numFiles > 1) {
						break;
					}
				}

				if (numModules + numFolders + numFiles > 0) {
					enableAction = true;
				}
			}

			String actionText = getActionText(event, numFiles, numFolders, numModules);
			event.getPresentation().setText(actionText);
			if (enableAction) {
				event.getPresentation().setEnabled(true);
			}
			else {
				event.getPresentation().setEnabled(false);
			}
		}
	}

	private String getActionText(@NotNull AnActionEvent event, int numFiles, int numFolders, int numModules) {
		ArrayList<String> textElements = new ArrayList<String>(3);

		StringBuilder actionText = new StringBuilder();
		if (event.getPlace().equals("ProjectViewPopup")) {
			actionText.append("OpenCms: ");
		}
		actionText.append("_Sync selected ");

		if (numModules + numFolders + numFiles > 0) {
			if (numModules > 0) {
				textElements.add(numModules > 1 ? "Modules" : "Module");
			}
			if (numFolders > 0) {
				textElements.add(numFolders > 1 ? "Folders" : "Folder");
			}
			if (numFiles > 0) {
				textElements.add(numFiles > 1 ? "Files" : "File");
			}

			for (int i = 0; i < textElements.size(); i++) {
				if (i > 0) {
					actionText.append("/");
				}
				actionText.append(textElements.get(i));
			}
		}
		else {
			actionText.append("Modules/Folders/Files");
		}

		return actionText.toString();
	}

}
