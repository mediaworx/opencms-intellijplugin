package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.tools.FileTypeCounter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsPublishSelectedAction extends OpenCmsPublishAction {

	@Override
	protected VirtualFile[] getPublishFileArray(AnActionEvent event) {
		return event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);

		if (isPluginEnabled()) {
			boolean enableAction = false;
			VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

			FileTypeCounter fileTypeCounter = new FileTypeCounter(plugin);

			if (selectedFiles != null && selectedFiles.length > 0) {

				fileTypeCounter.count(selectedFiles);

				if (fileTypeCounter.hasEntities()) {
					enableAction = true;
				}
			}

			String actionText = "_Publish selected " + fileTypeCounter.getEntityNames();
			event.getPresentation().setText(actionText);
			if (enableAction) {
				event.getPresentation().setEnabled(true);
			}
			else {
				event.getPresentation().setEnabled(false);
			}
		}
	}
}
