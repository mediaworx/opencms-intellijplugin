package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.sync.OpenCmsSyncer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsSyncAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsSyncAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			VirtualFile[] syncFiles = getSyncFileArray(event);
			OpenCmsSyncer syncer = new OpenCmsSyncer(plugin);
			setSyncerOptions(syncer);
			syncer.syncFiles(syncFiles);
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsSyncAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	protected abstract VirtualFile[] getSyncFileArray(@NotNull AnActionEvent event);

	/**
	 * sets options for the given OpenCmsSyncer. Does nothing by default (the syncer works with default actions)
	 * but may be overridden by implementing classes
	 * @param syncer    the OpenCmsSyncer to configure
	 */
	protected void setSyncerOptions(@NotNull OpenCmsSyncer syncer) {
	}

}
