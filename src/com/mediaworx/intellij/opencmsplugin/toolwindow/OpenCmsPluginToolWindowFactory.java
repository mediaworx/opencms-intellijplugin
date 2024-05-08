/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
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

		ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(toolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);

		console.setScrollPane(scrollPane);
		plugin.setConsole(console);
	}

	private void createUIComponents() {
		console = new OpenCmsToolWindowConsole();
	}

	public void setData(OpenCmsToolWindowConsole data) {
	}

	public void getData(OpenCmsToolWindowConsole data) {
	}

	public boolean isModified(OpenCmsToolWindowConsole data) {
		return false;
	}
}
