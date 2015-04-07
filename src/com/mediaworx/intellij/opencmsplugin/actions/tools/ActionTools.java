/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.actions.tools;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection of static utility methods used by actions
 */
public class ActionTools {

	/**
	 * @return  A file array containing all files open in editor tabs
	 */
	public static List<File> getOpenTabsFileList() {
		Editor[] editors = EditorFactory.getInstance().getAllEditors();
		List<File> openFiles = new ArrayList<>(editors.length);
		if (editors.length > 0) {
			FileDocumentManager fileDocManager = FileDocumentManager.getInstance();
			for (Editor editor : editors) {
				VirtualFile vf = fileDocManager.getFile(editor.getDocument());
				if (vf != null) {
					openFiles.add(new File(vf.getPath()));
				}
			}
		}
		return openFiles;
	}

	/**
	 * @param plugin the current OpenCms plugin instance
	 * @return  A file array containing files representing all the OpenCms modules in the current project.
	 */
	public static List<File> getAllModulesFileList(OpenCmsPlugin plugin) {
		List<File> moduleFiles;
		Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
		moduleFiles = new ArrayList<>(ocmsModules.size());
		int i = 0;
		for (OpenCmsModule ocmsModule : ocmsModules) {
			moduleFiles.add(new File(ocmsModule.getModuleBasePath()));
		}
		return moduleFiles;
	}

	/**
	 * Sets the text for the event's action depending on the user's selection in the project tree. E.g. if the user
	 * selects mutiple files the resulting text is "[textPrefix] selected Files", if the user selects multiple folders
	 * and one file the resulting text is "[textPrefix] selected Folders/File", if the user selects modules only the
	 * resulting text is "[textPrefix] selected Modules".
	 * @param event the action event provided by IntelliJ
	 * @param plugin the current OpenCms plugin instance
	 * @param textPrefix the text prefix to use
	 */
	public static void setSelectionSpecificActionText(AnActionEvent event, OpenCmsPlugin plugin, String textPrefix) {
		boolean enableAction = false;
		String actionPlace = event.getPlace();

		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);

		if (selectedFiles != null && selectedFiles.length > 0) {
			fileTypeCounter.count(selectedFiles);
			if (fileTypeCounter.hasEntities()) {
				enableAction = true;
			}
		}

		if (!actionPlace.equals(ActionPlaces.EDITOR_POPUP) && !actionPlace.equals(ActionPlaces.EDITOR_TAB_POPUP)) {
			String actionText = textPrefix + " selected " + fileTypeCounter.getEntityNames();
			event.getPresentation().setText(actionText);
		}

		if (enableAction) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}

	/**
	 * Checks if only OpenCms modules are selected and enables or disables the event's action accordingly (if only
	 * modules are selected the action is enabled, otherwise it is disabled). At the same time the action text is set
	 * to "[textPrefix] selected module" if only one module is selected and to "[textPrefix] selected modules" if
	 * multiple modules are selected.
	 * @param event the action event provided by IntelliJ
	 * @param textPrefix the text prefix to use
	 */
	public static void setOnlyModulesSelectedPresentation(AnActionEvent event, String textPrefix) {
		boolean enableAction = true;

		Project project = event.getProject();
		if (project == null) {
			return;
		}

		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);
		VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

		if (selectedFiles != null) {
			// check if only module roots have been selected
			for (VirtualFile ideaVFile : selectedFiles) {
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForPath(ideaVFile.getPath());
				if (ocmsModule == null || !ocmsModule.isPathModuleRoot(ideaVFile.getPath())) {
					enableAction = false;
					break;
				}
			}
		}
		else {
			enableAction = false;
		}

		if (enableAction) {
			FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);
			fileTypeCounter.count(selectedFiles);
			event.getPresentation().setText(textPrefix + " selected " + fileTypeCounter.getEntityNames());
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}

}
