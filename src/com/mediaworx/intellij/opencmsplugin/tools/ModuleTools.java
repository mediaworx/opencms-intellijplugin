package com.mediaworx.intellij.opencmsplugin.tools;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;

public class ModuleTools {

	public static String getModuleContentRoot(Module module) {
		String moduleContentRoot = null;
		if (module != null) {
			VirtualFile[] moduleRoots = ModuleRootManager.getInstance(module).getContentRoots();
			if (moduleRoots.length == 0) {
				return null;
			}
			// we assume that for OpenCms modules there is only one content root
			moduleContentRoot = moduleRoots[0].getPath();
		}
		return moduleContentRoot;
	}
}
