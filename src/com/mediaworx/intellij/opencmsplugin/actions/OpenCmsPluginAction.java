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

package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import com.mediaworx.opencms.ideconnector.client.IDEConnectorClient;
import org.jetbrains.annotations.NotNull;

/**
 * The "mother" of all menu actions for the OpenCms plugin. Implemented by all of the plugin's actions except menu
 * groups.
 */
public abstract class OpenCmsPluginAction extends AnAction {

	protected Project project;
	protected OpenCmsPlugin plugin;
	protected OpenCmsPluginConfigurationData config;
	protected OpenCmsToolWindowConsole console;
	protected IDEConnectorClient connectorClient;

	/**
	 * Method triggered by IntelliJ whenever a menu action is executed by the user. Does nothing by itself besides
	 * initializing some variables needed for action execution. The actual action functionality can be found in
	 * implementing classes.
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void actionPerformed(AnActionEvent event) {
		init(event);
	}

	/**
	 * Does nothing, called by IntelliJ
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void beforeActionPerformedUpdate(@NotNull AnActionEvent event) {
		// Do nothing to avoid calling update() twice (default behaviour for AnAction)
	}

	/**
	 * Update method that hides the action if the IntelliJ plugin is not enabled. That way the OpenCms menus containing
	 * the actions will be disabled automatically for projects that don't use the OpenCmsPlugin, because
	 * <code>disableIfNoVisibleChildren()</code> returns <code>true</code> (see
	 * {@link com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMenu#disableIfNoVisibleChildren()}).
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		init(event);
		event.getPresentation().setVisible(isPluginEnabled());
	}

	/**
	 * Initializes some references needed by the implementing actions (project, plugin, config)
	 * @param event the action event, provided by IntelliJ
	 */
	private void init(AnActionEvent event) {
		project = CommonDataKeys.PROJECT.getData(event.getDataContext());
		if (project == null) {
			return;
		}
		plugin = project.getComponent(OpenCmsPlugin.class);
		if (plugin == null) {
			return;
		}
		config = plugin.getPluginConfiguration();
		connectorClient = plugin.getConnectorClient();
	}

	/**
	 * clears the OpenCms console if there is one.
	 */
	protected void clearConsole() {
		if (config == null || config.isOpenCmsPluginEnabled()) {
			if (plugin.getConsole() != null) {
				plugin.getConsole().clear();
			}
		}
	}

	/**
	 * Can be used for checks if the plugin is enabled.
	 * @return  <code>true</code> if the OpenCms plugin is enabled in the project level configuration,
	 *          <code>false</code> otherwise
	 */
	protected boolean isPluginEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isOpenCmsPluginEnabled();
	}

	/**
	 * Can be used for checks if pulling module and resource meta data is enabled.
	 * @return  <code>true</code> if pulling meta data is enabled in the project level configuration,
	 *          <code>false</code> otherwise
	 */
	protected boolean isPullMetaDataEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isPluginConnectorEnabled() && config.isPullMetadataEnabled();
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}
}
