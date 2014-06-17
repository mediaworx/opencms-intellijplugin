/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
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

public abstract class OpenCmsMenu extends DefaultActionGroup {

	private static final Logger LOG = Logger.getInstance(OpenCmsMenu.class);

	protected OpenCmsPlugin plugin;
	protected ActionManager actionManager;

	protected OpenCmsMenu(OpenCmsPlugin plugin, String description, boolean popup) {
		super("_OpenCms", popup);
		getTemplatePresentation().setDescription(description);
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}

	protected abstract void registerActions();

	@Override
	public void actionPerformed(AnActionEvent event) {
		LOG.warn("menu's actionPerformed called for " + event.getPlace() + " " + event.getPresentation().getText());
	}

	@Override
	public void update(AnActionEvent event) {
		super.update(event);
		Project eventProject = event.getProject();
		if (eventProject == null) {
			return;
		}
		plugin = eventProject.getComponent(OpenCmsPlugin.class);
	}

	protected boolean isPluginEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isOpenCmsPluginEnabled();
	}

	/**
	 * the disableIfNoVisibleChildren mechanism is activated, and since all actions are automatically hidden if the
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

}
