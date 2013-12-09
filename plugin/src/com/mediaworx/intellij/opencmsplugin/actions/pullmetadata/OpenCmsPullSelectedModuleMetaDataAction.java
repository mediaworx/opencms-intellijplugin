package com.mediaworx.intellij.opencmsplugin.actions.pullmetadata;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.FileTypeCounter;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPullSelectedModuleMetaDataAction extends OpenCmsPullMetaDataAction {

	@Override
	protected VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event) {
		return event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);

		if (!isEnabled()) {
			event.getPresentation().setEnabled(false);
			return;
		}

		VirtualFile[] selectedFiles = getSyncFileArray(event);

		boolean enableAction = true;

		if (selectedFiles != null) {
			// check if only module roots have been selected
			for (VirtualFile ideaVFile : selectedFiles) {
				OpenCmsModule ocmsModule = plugin.getOpenCmsModules().getModuleForIdeaVFile(ideaVFile);
				if (ocmsModule == null || !ocmsModule.isIdeaVFileModuleRoot(ideaVFile)) {
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
			event.getPresentation().setText("_Pull Meta Data for selected " + fileTypeCounter.getEntityNames());
			event.getPresentation().setEnabled(true);
		}
		else {
			event.getPresentation().setEnabled(false);
		}
	}
}
