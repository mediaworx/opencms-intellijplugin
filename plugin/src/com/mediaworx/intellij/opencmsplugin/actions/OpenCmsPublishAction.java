package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.connector.PublishFileAnalyzer;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPublishAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPublishAction.class);

	private static final HashSet<String> SYNC_SELECTED_ACTION_IDS = new HashSet<String>();
	static {
		SYNC_SELECTED_ACTION_IDS.add(OpenCmsMenu.PUBLISH_ID);
		SYNC_SELECTED_ACTION_IDS.add(OpenCmsPlugin.PROJECT_POPUP_PUBLISH_ID);
	}

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			OpenCmsToolWindowConsole console = plugin.getConsole();
			VirtualFile[] publishFiles = getPublishFileArray(event);

			if (publishFiles == null || publishFiles.length == 0) {
				console.info("nothing to publish");
			}

			PublishFileAnalyzer analyzer = new PublishFileAnalyzer(plugin, publishFiles);
			analyzer.analyzeFiles();
			List<String> publishList = analyzer.getPublishList();

			plugin.showConsole();

			if (publishList.size() > 0) {
				console.info("Starting direct publish session for the following resources (and contained sub resources): ");
				for (String vfsPath : publishList) {
					console.info("  " + vfsPath);
				}

				OpenCmsPluginConnector connector = plugin.getPluginConnector();
				try {
					if (connector.publishResources(publishList, true)) {
						console.info("Direct publish session started");
					}
					else {
						console.error(connector.getMessage());
					}
				}
				catch (IOException e) {
					LOG.warn("There was an exception while publishing resources", e);
					console.error("There was an exception while publishing resources. Is OpenCms running? Please have a look at the OpenCms log file and/or the IntelliJ log file.");
				}
			}
			else {
				console.info("nothing to publish");
			}
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	private VirtualFile[] getPublishFileArray(AnActionEvent event) {
		VirtualFile[] publishFiles = null;

		String actionId = event.getActionManager().getId(this);

		// Publish all open Tabs
		if (actionId.equals(OpenCmsMenu.PUBLISH_OPEN_TABS_ID) || actionId.equals(OpenCmsPlugin.TAB_POPUP_PUBLISH_OPEN_TABS_ID)) {
			Editor[] editors = EditorFactory.getInstance().getAllEditors();
			if (editors.length > 0) {
				ArrayList<VirtualFile> openFiles = new ArrayList<VirtualFile>(editors.length);
				FileDocumentManager fileDocManager = FileDocumentManager.getInstance();
				for (Editor editor : editors) {
					VirtualFile vf = fileDocManager.getFile(editor.getDocument());
					if (vf != null) {
						openFiles.add(vf);
					}
				}
				publishFiles = openFiles.toArray(new VirtualFile[openFiles.size()]);
			}
		}
		// Publish all Modules
		else if (actionId.equals(OpenCmsMenu.PUBLSH_ALL_MODULES_ID)) {
			Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
			publishFiles = new VirtualFile[ocmsModules.size()];
			int i = 0;
			for (OpenCmsModule ocmsModule : ocmsModules) {
				publishFiles[i++] = LocalFileSystem.getInstance().findFileByPath(ocmsModule.getIntelliJModuleRoot());
			}
		}
		// publish specific modules
		else if (actionId.startsWith(OpenCmsMenu.PUBLISH_MODULE_ID_PREFIX)) {
			publishFiles = new VirtualFile[1];
			String moduleRoot = actionId.substring(OpenCmsMenu.PUBLISH_MODULE_ID_PREFIX.length());
			publishFiles[0] = LocalFileSystem.getInstance().findFileByPath(moduleRoot);
		}
		// publish selected modules/folders/files
		else {
			publishFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		}
		return publishFiles;
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);

		String actionId = event.getActionManager().getId(this);

		if (actionId == null) {
			return;
		}

		if (SYNC_SELECTED_ACTION_IDS.contains(actionId)) {
			boolean enableAction = false;
			VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

			FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);

			if (selectedFiles != null && selectedFiles.length > 0) {

				fileTypeCounter.count(selectedFiles);

				if (fileTypeCounter.hasEntities()) {
					enableAction = true;
				}
			}

			String actionText = "_Publish selected " + fileTypeCounter.getEntityNames();
			event.getPresentation().setText(actionText);
			if (enableAction) {
				event.getPresentation().setEnabled(true);
			}
			else {
				event.getPresentation().setEnabled(false);
			}
		}
	}

}
