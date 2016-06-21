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

package com.mediaworx.intellij.opencmsplugin.entities;

import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.SyncAction;
import com.mediaworx.intellij.opencmsplugin.tools.PluginTools;
import com.mediaworx.opencms.moduleutils.manifestgenerator.OpenCmsModuleManifestGenerator;
import org.apache.chemistry.opencmis.client.api.CmisObject;

import java.io.File;

public class SyncFile extends SyncEntity {

	/**
	 * Creates a new sync file
	 * @param ocmsModule the OpenCms module the file is contained in
	 * @param vfsPath the file's VFS path
	 * @param file The java file instance representing the file
	 * @param vfsObject The CMIS object representing the file
	 * @param syncAction The Sync Action to be used for the file (PUSH, PULL, DELETE_RFS or DELETE_VFS)
	 */
	public SyncFile(OpenCmsModule ocmsModule, String vfsPath, File file, CmisObject vfsObject, SyncAction syncAction, boolean replaceExistingEntity) {
		super(ocmsModule, vfsPath, file, vfsObject, syncAction, replaceExistingEntity);
	}

	/**
	 * Returns the Type (FILE)
	 * @return always returns Type.FILE
	 */
	@Override
	public Type getType() {
		return Type.FILE;
	}

	/**
	 * returns the path to the meta data file for this file
	 * @return the path to the meta data file for this file
	 */
	public String getMetaInfoFilePath() {
		String localPath = PluginTools.stripVfsSiteRootFromVfsPath(getOcmsModule(), getVfsPath());
		return OpenCmsModuleManifestGenerator.getMetaInfoPath(getOcmsModule().getManifestRoot(), localPath, false);
	}

}
