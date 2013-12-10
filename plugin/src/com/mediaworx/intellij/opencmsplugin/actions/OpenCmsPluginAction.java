package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import org.jetbrains.annotations.NotNull;

public abstract class OpenCmsPluginAction extends AnAction {

	protected Project project;
	protected OpenCmsPlugin plugin;
	protected OpenCmsPluginConfigurationData config;

	@Override
	public void actionPerformed(AnActionEvent event) {
		init(event);
	}

	@Override
	public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
		// Do nothing to avoid calling update() twice (default behaviour for AnAction)
	}

	/**
	 * update method that hides the action if the IntelliJ plugin is not enabled. That way the OpenCms menus containing
	 * the actions will be disabled automatically for projects that don't use the OpenCmsPlugin, because
	 * <code>disableIfNoVisibleChildren()</code> returns <code>true</code> (see
	 * {@link com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMenu#disableIfNoVisibleChildren()}).
	 * @param event the event (provided by IntelliJ)
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		super.update(event);
		init(event);
		event.getPresentation().setVisible(isPluginEnabled());
	}

	private void init(AnActionEvent event) {
		project = DataKeys.PROJECT.getData(event.getDataContext());
		if (project == null) {
			return;
		}
		plugin = project.getComponent(OpenCmsPlugin.class);
		config = plugin.getPluginConfiguration();
		if (plugin.getConsole() != null) {
			plugin.getConsole().clear();
		}
	}

	protected boolean isPluginEnabled() {
		OpenCmsPluginConfigurationData config = plugin.getPluginConfiguration();
		return config != null && config.isPluginConnectorEnabled() && config.isPullMetadataEnabled();
	}
}
