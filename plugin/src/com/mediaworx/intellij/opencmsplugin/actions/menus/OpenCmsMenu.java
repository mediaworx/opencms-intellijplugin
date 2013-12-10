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

	protected OpenCmsMenu(OpenCmsPlugin plugin, boolean popup) {
		super("_OpenCms", popup);
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
