package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.util.ArrayList;

public class FileTypeCounter {

	OpenCmsPlugin plugin;
	int numModules;
	int numFolders;
	int numFiles;

	FileTypeCounter(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		numModules = 0;
		numFolders = 0;
		numFiles = 0;
	}

	void count(VirtualFile[] selectedFiles) {
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
	}

	boolean hasEntities() {
		return numModules + numFolders + numFiles > 0;
	}

	String getEntityNames() {
		StringBuilder entityNames = new StringBuilder();
		ArrayList<String> textElements = new ArrayList<String>(3);
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
					entityNames.append("/");
				}
				entityNames.append(textElements.get(i));
			}
		}
		else {
			entityNames.append("Modules/Folders/Files");
		}
		return entityNames.toString();
	}
}
