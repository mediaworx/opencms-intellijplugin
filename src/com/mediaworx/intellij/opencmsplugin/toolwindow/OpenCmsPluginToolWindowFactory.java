package com.mediaworx.intellij.opencmsplugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;

import javax.swing.*;

public class OpenCmsPluginToolWindowFactory implements ToolWindowFactory {

	private static final Logger LOG = Logger.getInstance(OpenCmsPluginToolWindowFactory.class);

	private OpenCmsPlugin plugin;
	private ToolWindow toolWindow;
	private JPanel toolWindowContent;
	private OpenCmsToolWindowConsole console;
	private JScrollPane scrollPane;

	@Override
	public void createToolWindowContent(Project project, ToolWindow toolWindow) {
		plugin = project.getComponent(OpenCmsPlugin.class);

		this.toolWindow = toolWindow;

		ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);

		console.setScrollPane(scrollPane);
		plugin.setConsole(console);
	}

	private void createUIComponents() {
		console = new OpenCmsToolWindowConsole();
	}
}
