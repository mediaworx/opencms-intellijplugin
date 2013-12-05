package com.mediaworx.intellij.opencmsplugin.tools;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

public abstract class VfsFileAnalyzer {

	private static final Logger LOG = Logger.getInstance(VfsFileAnalyzer.class);

	protected final OpenCmsPlugin plugin;
	protected final VirtualFile[] files;
	protected final StringBuilder warnings;
	protected ProgressIndicator progressIndicator;

	public VfsFileAnalyzer(final OpenCmsPlugin plugin, final VirtualFile[] files) throws CmsConnectionException {
		this.files = files;
		this.plugin = plugin;
		warnings = new StringBuilder();
	}

	public void analyzeFiles() {

		if (files != null && files.length > 0) {
			for (VirtualFile file : files) {
				if (progressIndicator != null && progressIndicator.isCanceled()) {
					return;
				}

				// file/folder is ignored
				if (fileOrPathIsIgnored(file)) {
					// do nothing (filter VCS files and OpenCms Sync Metadata)
					LOG.info("file/folder is ignored");
					continue;
				}

				// if the file does not belong to a module, ignore
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(file);
				if (ocmsModule == null) {
					LOG.info("file/folder is not within a configured OpenCms module, ignore");
					continue;
				}

				// it's a folder that is a module root, so sync all corresponding module resources
				if (file.isDirectory() && ocmsModule.isIdeaVFileModuleRoot(file)) {
					LOG.info("Module root selected, handling the module " + ocmsModule.getModuleName());
					handleModule(ocmsModule);
				}

				// file/folder is within a module resource path, handle it
				else if (ocmsModule.isIdeaVFileModuleResource(file)) {
					LOG.info("Handling a module resource path, a folder or a file in a module");
					handleModuleResource(ocmsModule, file);
				}

				// if it is a folder that is not a resource path, but within the VFS path ...
				else if (file.isDirectory()  && ocmsModule.isIdeaVFileInVFSPath(file)) {
					LOG.info("Handling a VFS path outside of the module resources");
					String relativeFolderPath = file.getPath().substring(ocmsModule.getLocalVfsRoot().length());
					// ... get all module resources under the folder and add them
					for (String moduleResourceVfsPath : ocmsModule.getModuleResources()) {
						// if the module resource is within the selected folder ...
						if (moduleResourceVfsPath.startsWith(relativeFolderPath)) {
							// ... handle it
							LOG.info("- The module resource path " + moduleResourceVfsPath + " is child, so handle it");
							handleModuleResourcePath(ocmsModule, moduleResourceVfsPath);
						}
					}
				}

				// file/folder is neither a module resource nor a VFS resource parent, ignore
				else {
					warnings.append("Ignoring '").append(file.getPath()).append("' (not a module path).\n");
					LOG.info("File is not in the VFS path, ignore");
				}
			}
		}
	}

	protected void handleModule(OpenCmsModule ocmsModule) {
		for (String resourcePath : ocmsModule.getModuleResources()) {
			handleModuleResourcePath(ocmsModule, resourcePath);
		}
	}

	protected abstract void handleModuleResource(OpenCmsModule ocmsModule, VirtualFile file);

	protected abstract void handleModuleResourcePath(OpenCmsModule ocmsModule, String moduleResource);

	public static boolean fileOrPathIsIgnored(final VirtualFile virtualFile) {
		final String pathLC = virtualFile.getPath().toLowerCase();
		return pathLC.contains(".git")
				|| pathLC.contains(".svn")
				|| pathLC.contains(".cvs")
				|| pathLC.contains(".sass-cache")
				|| virtualFile.getName().equals("#SyncJob.txt")
				|| virtualFile.getName().equals("sass")
				|| virtualFile.getName().equals(".config")
				|| virtualFile.getName().equals("manifest.xml")
				|| virtualFile.getName().equals("log4j.properties")
				|| virtualFile.getName().equals(".gitignore");
	}

	public boolean hasWarnings() {
		return warnings.length() > 0;
	}

	public StringBuilder getWarnings() {
		return warnings;
	}

}
