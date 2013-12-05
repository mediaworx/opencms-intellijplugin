package com.mediaworx.intellij.opencmsplugin.actions.tools;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;
import java.util.Collection;

public class ActionTools {

	public static VirtualFile[] getOpenTabsFileArray() {
		VirtualFile[] publishFiles = null;
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
		return publishFiles;
	}

	public static VirtualFile[] getAllModulesFileArray(OpenCmsPlugin plugin) {
		VirtualFile[] publishFiles;Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
		publishFiles = new VirtualFile[ocmsModules.size()];
		int i = 0;
		for (OpenCmsModule ocmsModule : ocmsModules) {
			publishFiles[i++] = LocalFileSystem.getInstance().findFileByPath(ocmsModule.getIntelliJModuleRoot());
		}
		return publishFiles;
	}

}
