package com.mediaworx.intellij.opencmsplugin.connector;

import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.sync.VfsFileAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class PublishFileAnalyzer extends VfsFileAnalyzer {

	List<String> publishList;

	public PublishFileAnalyzer(OpenCmsPlugin plugin, VirtualFile[] files) throws CmsConnectionException {
		super(plugin, files);

		publishList = new ArrayList<String>();
	}

	@Override
	protected void handleModuleResource(OpenCmsModule ocmsModule, VirtualFile file) {
		publishList.add(ocmsModule.getVfsPathForIdeaVFile(file));
	}

	@Override
	protected void handleModuleResourcePath(OpenCmsModule ocmsModule, String moduleResourceVfsPath) {
		publishList.add(moduleResourceVfsPath);
	}

	public List<String> getPublishList() {
		return publishList;
	}
}
