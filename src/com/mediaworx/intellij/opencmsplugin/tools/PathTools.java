package com.mediaworx.intellij.opencmsplugin.tools;

import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

// TODO: nach Umbau auf Verwendung von Module Resoureces entfernen!
public class PathTools {

	public static final String VFS_SYSTEMFOLDER = "/system";
	public static final String VFS_MODULESFOLDER = "/modules";

	public static String getLocalModulesParentPath(final OpenCmsModule ocmsModule) {
		StringBuilder modulePath = new StringBuilder();
		modulePath.append(ocmsModule.getLocalVfsRoot())
				.append(VFS_SYSTEMFOLDER).append(VFS_MODULESFOLDER).append("/")
				.append(ocmsModule.getModuleName());
		return modulePath.toString();
	}

}
