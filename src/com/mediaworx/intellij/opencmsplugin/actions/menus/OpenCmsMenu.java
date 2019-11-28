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

package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;

/**
 * Parent for all OpenCms menus in IntelliJ. Right now there are four OpenCms menus implemented. They can be found at
 * the following locations:
 * <ul>
 *     <li>In the main menu</li>
 *     <li>In the Project window</li>
 *     <li>In the editor's right click popup menu</li>
 *     <li>In the editor tab's right click popup menu</li>
 * </ul>
 * All menus are context aware, e.g. there's a difference if you click on a folder, a file or a module.
 */
public abstract class OpenCmsMenu extends DefaultActionGroup {

	private static final Logger LOG = Logger.getInstance(OpenCmsMenu.class);

	protected OpenCmsPlugin plugin;
	protected ActionManager actionManager;

	public OpenCmsMenu(boolean popup, String description) {
		super("_OpenCms", popup);
		getTemplatePresentation().setDescription(description);
	}

	/**
	 * Creates a new OpenCms menu
	 * @param plugin the OpenCms plugin instance
	 * @param description A description of the menu that's displayed in the status bar
	 * @param popup <code>true</code> if the menu is a popup menu, <code>false</code> otherwise
	 */
	protected OpenCmsMenu(OpenCmsPlugin plugin, String description, boolean popup) {
		super("_OpenCms", popup);
		getTemplatePresentation().setDescription(description);
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}

	/**
	 * Registers the menu's actions, abstract method that must be implemented by subclasses
	 */
	protected abstract void registerActions();

	/**
	 * Is called during updates. Used to update the plugin instance on project switches (if a different project is
	 * opened or activated in another project window)
	 * @param event the action event, provided by IntelliJ
	 */
	@Override
	public void update(AnActionEvent event) {
		super.update(event);
		Project eventProject = event.getProject();
		if (eventProject == null) {
			return;
		}
		plugin = eventProject.getComponent(OpenCmsPlugin.class);
	}

	/**
	 * Checks if the OpenCms plugin is enabled.
	 * @return  <code>true</code> if the OpenCms plugin is enabled for the current project, <code>false</code> otherwise
	 */
	protected boolean isPluginEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isOpenCmsPluginEnabled();
	}

	/**
	 * The disableIfNoVisibleChildren mechanism is activated, and since all actions are automatically hidden if the
	 * IntelliJ plugin is not enabled (see
	 * {@link com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction#update(AnActionEvent)}),
	 * the OpenCms menus containing the actions will be disabled automatically for projects that don't use the
	 * OpenCmsPlugin. Because of a suspected bug in IntelliJ the OpenCms menu in the main menu ist treated differently,
	 * see {@link OpenCmsMainMenu#update(AnActionEvent)}.
	 * @return  always returns <code>true</code>
	 */
	@Override
	public boolean disableIfNoVisibleChildren() {
		return true;
	}

	public void setPlugin(OpenCmsPlugin plugin) {
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}
}
