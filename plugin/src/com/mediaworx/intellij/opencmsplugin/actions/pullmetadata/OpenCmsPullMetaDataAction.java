package com.mediaworx.intellij.opencmsplugin.actions.pullmetadata;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncAction;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsPullMetaDataAction extends OpenCmsSyncAction {

	@Override
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
		syncer.setPullMetaDataOnly(true);
	}

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
