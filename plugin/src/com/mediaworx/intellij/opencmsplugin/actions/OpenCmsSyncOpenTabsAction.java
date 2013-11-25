package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;

import java.util.ArrayList;

public class OpenCmsSyncOpenTabsAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncOpenTabsAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			Editor[] editors = EditorFactory.getInstance().getAllEditors();
			if (editors.length > 0) {
				ArrayList<VirtualFile> openFiles = new ArrayList<VirtualFile>(editors.length);
				FileDocumentManager fileDocManager = FileDocumentManager.getInstance();
				for (Editor editor : editors) {
					VirtualFile vf = fileDocManager.getFile(editor.getDocument());
					if (vf != null && !OpenCmsSyncer.fileOrPathIsIgnored(vf) && plugin.getOpenCmsModules().isIdeaVFileOpenCmsModuleResource(vf)) {
						openFiles.add(vf);
					}
				}

				OpenCmsSyncer fileSyncer = new OpenCmsSyncer(plugin);
				fileSyncer.syncFiles(openFiles.toArray(new VirtualFile[openFiles.size()]));
			}
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncOpenTabsAction.actionPerformed: " + t.getMessage(), t);
		}
	}
}
