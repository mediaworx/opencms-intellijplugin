package com.mediaworx.intellij.opencmsplugin.actions.publish;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.connector.PublishFileAnalyzer;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("ComponentNotRegistered")
public abstract class OpenCmsPublishAction extends OpenCmsPluginAction {

	private static final Logger LOG = Logger.getInstance(OpenCmsPublishAction.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.info("actionPerformed - event: " + event);
		super.actionPerformed(event);

		try {
			OpenCmsToolWindowConsole console = plugin.getConsole();
			VirtualFile[] publishFiles = getPublishFileArray(event);

			if (publishFiles == null || publishFiles.length == 0) {
				console.info("nothing to publish");
			}

			PublishFileAnalyzer analyzer = new PublishFileAnalyzer(plugin, publishFiles);
			analyzer.analyzeFiles();
			List<String> publishList = analyzer.getPublishList();

			plugin.showConsole();

			if (publishList.size() > 0) {
				console.info("Starting direct publish session for the following resources (and contained sub resources): ");
				for (String vfsPath : publishList) {
					console.info("  " + vfsPath);
				}

				OpenCmsPluginConnector connector = plugin.getPluginConnector();
				try {
					if (connector.publishResources(publishList, true)) {
						console.info("Direct publish session started");
					}
					else {
						console.error(connector.getMessage());
					}
				}
				catch (IOException e) {
					LOG.warn("There was an exception while publishing resources", e);
					console.error("There was an exception while publishing resources. Is OpenCms running? Please have a look at the OpenCms log file and/or the IntelliJ log file.");
				}
			}
			else {
				console.info("nothing to publish");
			}
		}
		catch (Throwable t) {
			LOG.warn("Exception in OpenCmsPublishAction.actionPerformed: " + t.getMessage(), t);
		}
	}

	protected abstract VirtualFile[] getPublishFileArray(AnActionEvent event);

	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		event.getPresentation().setEnabled(isConnectorEnabled());
	}

	protected boolean isConnectorEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isPluginConnectorEnabled() && config.isPullMetadataEnabled();
	}
}
