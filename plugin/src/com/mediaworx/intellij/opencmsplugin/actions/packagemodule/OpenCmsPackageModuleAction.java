package com.mediaworx.intellij.opencmsplugin.actions.packagemodule;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.opencms.moduleutils.packager.OpenCmsModulePackager;
import com.mediaworx.opencms.moduleutils.packager.exceptions.OpenCmsModulePackagerException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsPackageModuleAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPackageModuleAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		VirtualFile[] ideaVFiles = getModuleFileArray(event);
		final List<File> filesToBeRefreshed = new ArrayList<File>(ideaVFiles.length);

		OpenCmsModulePackager packager = new OpenCmsModulePackager();
		plugin.showConsole();
		for (VirtualFile ideaVFile : ideaVFiles) {
			OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);
			if (ocmsModule == null || !ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
				continue;
			}

			String zipTargetPath = ocmsModule.getIntelliJModuleRoot();
			try {
				String packageName = packager.packageModule(ocmsModule.getManifestRoot(), ocmsModule.getLocalVfsRoot(), zipTargetPath);
				plugin.getConsole().info(packageName + " was saved at " + zipTargetPath);
			}
			catch (OpenCmsModulePackagerException e) {
				String message = "Error packaging module " + ocmsModule.getModuleName();
				LOG.warn(message, e);
				plugin.getConsole().error(message + "\n" + e.getMessage());
			}

			filesToBeRefreshed.add(new File(zipTargetPath));
		}
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				LocalFileSystem.getInstance().refreshIoFiles(filesToBeRefreshed, true, false, null);
			}
		}, 1000);
	}

	protected abstract VirtualFile[] getModuleFileArray(@NotNull AnActionEvent event);

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		if (isPluginEnabled() && isPullMetaDataEnabled()) {
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}
}
