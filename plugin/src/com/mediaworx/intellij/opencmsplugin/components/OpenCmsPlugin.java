package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.mediaworx.intellij.opencmsplugin.actions.*;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsConfiguration;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsPluginToolWindowFactory;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class OpenCmsPlugin implements ProjectComponent {

	private static final Logger LOG = Logger.getInstance(OpenCmsPlugin.class);

	public static final String TOOLWINDOW_ID = "OpenCms";

	private static final String OPENCMS_MENU_ID = "OpenCmsPlugin.ActionMenu";

	private static final String PROJECT_POPUP_GROUP_ID = "OpenCmsPlugin.ProjectPopupGroup";
	private static final String PROJECT_POPUP_SYNC_ID = "OpenCmsPlugin.ProjectPopupSyncAction";
	private static final String PROJECT_POPUP_PULL_METADATA_ID = "OpenCmsPlugin.ProjectPopupPullModuleMetaDataAction";
	public static final String PROJECT_POPUP_PUBLISH_ID = "OpenCmsPlugin.ProjectPopupPublishAction";

	private static final String EDITOR_POPUP_GROUP_ID = "OpenCmsPlugin.EditorPopupGroup";
	private static final String EDITOR_POPUP_SYNC_ID = "OpenCmsPlugin.EditorPopupSyncAction";
	public static final String EDITOR_POPUP_PUBLISH_ID = "OpenCmsPlugin.EditorPopupPublishAction";

	private static final String TAB_POPUP_GROUP_ID = "OpenCmsPlugin.TabsPopupGroup";
	private static final String TAB_POPUP_SYNC_ID = "OpenCmsPlugin.TabsPopupSyncAction";
	private static final String TAB_POPUP_SYNC_OPEN_TAB_ID = "OpenCmsPlugin.TabsPopupSyncOpenTabsAction";
	public static final String TAB_POPUP_PUBLISH_ID = "OpenCmsPlugin.TabsPopupPublishAction";
	public static final String TAB_POPUP_PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupPublishOpenTabsAction";

	private static final Icon MENU_ICON = new ImageIcon(OpenCmsPlugin.class.getResource("/icons/opencms_menu.png"));

	private Project project;
	private OpenCmsConfiguration openCmsConfiguration;
	private OpenCmsModules openCmsModules;
	private VfsAdapter vfsAdapter;
	private OpenCmsPluginConfigurationData config;
	private OpenCmsPluginConnector pluginConnector;
	private ToolWindow toolWindow;
	private OpenCmsToolWindowConsole console;
	private ActionManager actionManager;
	private OpenCmsMenu openCmsMenu;

	public OpenCmsPlugin(Project project) {
		this.project = project;
		openCmsModules = new OpenCmsModules(this);
	}

	public void projectOpened() {
	}

	public void projectClosed() {
	}

	public void initComponent() {
		LOG.warn("initComponent called, project: " + project.getName());
		config = project.getComponent(OpenCmsProjectConfigurationComponent.class).getConfigurationData();
		if (config != null && config.isOpenCmsPluginEnabled()) {
			openCmsConfiguration = new OpenCmsConfiguration(config.getWebappRoot());

			if (config.isPluginConnectorEnabled()) {
				pluginConnector = new OpenCmsPluginConnector(config.getConnectorUrl(), config.getUsername(), config.getPassword());
			}
			registerActions();
		}
		else {
			unregisterActions();
		}
	}

	private void registerActions() {
		actionManager = ActionManager.getInstance();
		registerMainMenuActions();
		registerProjectPopupActions();
		registerEditorPopupActions();
		registerEditorTabPopupActions();
	}

	public void addAction(DefaultActionGroup group, String id, AnAction action, String text) {
		addAction(group, id, action, text, null, null);
	}

	private void addAction(DefaultActionGroup group, String id, AnAction action, String text, Icon icon) {
		addAction(group, id, action, text, icon, null);
	}

	private void addAction(DefaultActionGroup group, String id, AnAction action, String text, Icon icon, Constraints constraints) {
		action.getTemplatePresentation().setText(text);
		if (icon != null) {
			action.getTemplatePresentation().setIcon(icon);
		}
		actionManager.registerAction(id, action);
		if (constraints == null) {
			group.add(action);
		}
		else {
			group.add(action, constraints);
		}
	}

	private void registerMainMenuActions() {
		openCmsMenu = (OpenCmsMenu)actionManager.getAction(OPENCMS_MENU_ID);
		if (openCmsMenu == null) {
			DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_MAIN_MENU);
			openCmsMenu = new OpenCmsMenu(this, "_OpenCms", false);
			actionManager.registerAction(OPENCMS_MENU_ID, openCmsMenu);
			mainMenu.addAction(openCmsMenu, new Constraints(Anchor.BEFORE, "HelpMenu"));
		}
	}

	private void registerProjectPopupActions() {

		DefaultActionGroup group = (DefaultActionGroup)actionManager.getAction(PROJECT_POPUP_GROUP_ID);

		if (group == null) {
			DefaultActionGroup projectPopupMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);

			group = new DefaultActionGroup();
			group.setPopup(true);
			addAction(projectPopupMenu, PROJECT_POPUP_GROUP_ID, group, "_OpenCms", MENU_ICON, new Constraints(Anchor.BEFORE, "RevealIn"));
			projectPopupMenu.add(Separator.getInstance(), new Constraints(Anchor.AFTER, PROJECT_POPUP_GROUP_ID));

			addAction(group, PROJECT_POPUP_SYNC_ID, new OpenCmsSyncAction(), "_Sync selected Modules/Folders/Files");
			addAction(group, PROJECT_POPUP_PULL_METADATA_ID, new OpenCmsPullModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
			addAction(group, PROJECT_POPUP_PUBLISH_ID, new OpenCmsPublishAction(), "_Publish selected Modules/Folders/Files");
		}
	}

	private void registerEditorPopupActions() {
		DefaultActionGroup group = (DefaultActionGroup)actionManager.getAction(EDITOR_POPUP_GROUP_ID);

		if (group == null) {

			DefaultActionGroup editorPopupMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_POPUP);

			group = new DefaultActionGroup();
			group.setPopup(true);
			addAction(editorPopupMenu, EDITOR_POPUP_GROUP_ID, group, "_OpenCms", MENU_ICON, new Constraints(Anchor.BEFORE, "IDEtalk.SendCodePointer"));
			editorPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, EDITOR_POPUP_GROUP_ID));

			addAction(group, EDITOR_POPUP_SYNC_ID, new OpenCmsEditorPopupSyncAction(), "_Sync File");
			addAction(group, EDITOR_POPUP_PUBLISH_ID, new OpenCmsPublishAction(), "_Publish selected Modules/Folders/Files");
		}
	}

	private void registerEditorTabPopupActions() {
		DefaultActionGroup group = (DefaultActionGroup)actionManager.getAction(TAB_POPUP_GROUP_ID);

		if (group == null) {

			DefaultActionGroup tabPopupMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_TAB_POPUP);

			group = new DefaultActionGroup();
			group.setPopup(true);
			tabPopupMenu.addAction(Separator.getInstance());
			addAction(tabPopupMenu, TAB_POPUP_GROUP_ID, group, "_OpenCms", MENU_ICON);

			addAction(group, TAB_POPUP_SYNC_ID, new OpenCmsSyncAction(), "_Sync File");
			addAction(group, TAB_POPUP_SYNC_OPEN_TAB_ID, new OpenCmsSyncOpenTabsAction(), "Sync all open Editor _Tabs");
			addAction(group, TAB_POPUP_PUBLISH_ID, new OpenCmsPublishAction(), "_Publish selected Modules/Folders/Files");
			addAction(group, TAB_POPUP_PUBLISH_OPEN_TABS_ID, new OpenCmsPublishAction(), "Publish all open Editor Tabs");
		}
	}

	private void unregisterActions() {
		ActionManager actionManager = ActionManager.getInstance();
		openCmsMenu.unregisterActions();
		actionManager.unregisterAction(OPENCMS_MENU_ID);
		actionManager.unregisterAction(PROJECT_POPUP_SYNC_ID);
		actionManager.unregisterAction(PROJECT_POPUP_PULL_METADATA_ID);

		actionManager.unregisterAction(EDITOR_POPUP_SYNC_ID);

		actionManager.unregisterAction(TAB_POPUP_SYNC_ID);
		actionManager.unregisterAction(TAB_POPUP_SYNC_OPEN_TAB_ID);
	}

	public void disposeComponent() {
		project = null;
		vfsAdapter = null;
		config = null;
		openCmsConfiguration = null;
		openCmsModules = null;
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPlugin.MainComponent";
	}

	public Project getProject() {
		return project;
	}

	public OpenCmsConfiguration getOpenCmsConfiguration() {
		return openCmsConfiguration;
	}

	public OpenCmsPluginConfigurationData getPluginConfiguration() {
		return config;
	}

	public OpenCmsModules getOpenCmsModules() {
		return openCmsModules;
	}

	public VfsAdapter getVfsAdapter() {
		if (vfsAdapter == null) {
			if (config != null && config.isOpenCmsPluginEnabled() && config.getPassword() != null && config.getPassword().length() > 0) {
				vfsAdapter = new VfsAdapter(config.getRepository(), config.getUsername(), config.getPassword());
			}
		}
		return vfsAdapter;
	}

	public OpenCmsPluginConnector getPluginConnector() {
		return pluginConnector;
	}

	public void setPluginConnector(OpenCmsPluginConnector pluginConnector) {
		this.pluginConnector = pluginConnector;
	}

	private void initToolWindow() {
		toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(OpenCmsPlugin.TOOLWINDOW_ID, false, ToolWindowAnchor.BOTTOM);
		toolWindow.setIcon(new ImageIcon(this.getClass().getResource("/icons/opencms_13.png")));
		OpenCmsPluginToolWindowFactory toolWindowFactory = new OpenCmsPluginToolWindowFactory();
		toolWindowFactory.createToolWindowContent(project, toolWindow);
	}

	public ToolWindow getToolWindow() {
		if (toolWindow == null) {
			initToolWindow();
		}
		return toolWindow;
	}

	public void showConsole() {
		if (toolWindow == null || !toolWindow.isActive()) {
			getToolWindow().activate(null);
		}
	}

	public OpenCmsToolWindowConsole getConsole() {
		// if the console has not been initialized ...
		if (console == null) {
			initToolWindow();  // ... initialize the tool window (containing the console)
		}
		return console;
	}

	public void setConsole(OpenCmsToolWindowConsole console) {
		this.console = console;
	}
}
