package com.mediaworx.intellij.opencmsplugin;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.mediaworx.intellij.opencmsplugin.actions.menus.*;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationComponent;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.listeners.OpenCmsModuleFileChangeListener;
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
	private static final String PROJECT_POPUP_MENU_ID = "OpenCmsPlugin.ProjectPopupMenu";
	private static final String EDITOR_POPUP_MENU_ID = "OpenCmsPlugin.EditorPopupMenu";
	private static final String TAB_POPUP_MENU_ID = "OpenCmsPlugin.TabsPopupGroup";

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
		actionManager = ActionManager.getInstance();
		config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();
		if (config != null && config.isOpenCmsPluginEnabled()) {
			openCmsConfiguration = new OpenCmsConfiguration(config.getWebappRoot());

			if (config.isPluginConnectorEnabled()) {
				pluginConnector = new OpenCmsPluginConnector(config.getConnectorUrl(), config.getUsername(), config.getPassword());
			}
			registerActions();
			registerListeners();
		}
		else {
			unregisterActions();
		}
	}

	private void registerActions() {
		registerMainMenu();
		registerProjectPopupMenu();
		registerEditorPopupActions();
		registerEditorTabPopupActions();
	}

	private void registerListeners() {
		MessageBus bus = ApplicationManager.getApplication().getMessageBus();
		MessageBusConnection connection = bus.connect();
		OpenCmsModuleFileChangeListener fileChangeListener = new OpenCmsModuleFileChangeListener(this);
		connection.subscribe(VirtualFileManager.VFS_CHANGES, fileChangeListener);
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

	private void registerMainMenu() {
		OpenCmsMainMenu openCmsMainMenu = (OpenCmsMainMenu) actionManager.getAction(OPENCMS_MENU_ID);
		if (openCmsMainMenu == null) {
			DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_MAIN_MENU);
			openCmsMainMenu = new OpenCmsMainMenu(this);
			addAction(mainMenu, OPENCMS_MENU_ID, openCmsMainMenu, "_OpenCms", null, new Constraints(Anchor.BEFORE, "HelpMenu"));
		}
	}

	private void registerProjectPopupMenu() {
		OpenCmsProjectPopupMenu openCmsProjectPopupMenu = (OpenCmsProjectPopupMenu) actionManager.getAction(PROJECT_POPUP_MENU_ID);
		if (openCmsProjectPopupMenu == null) {
			DefaultActionGroup projectPopup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
			openCmsProjectPopupMenu = new OpenCmsProjectPopupMenu(this);
			addAction(projectPopup, PROJECT_POPUP_MENU_ID, openCmsProjectPopupMenu, "_OpenCms", MENU_ICON, new Constraints(Anchor.BEFORE, "RevealIn"));
			projectPopup.add(Separator.getInstance(), new Constraints(Anchor.AFTER, PROJECT_POPUP_MENU_ID));
		}
	}

	private void registerEditorPopupActions() {
		OpenCmsEditorPopupMenu openCmsEditorPopupMenu = (OpenCmsEditorPopupMenu) actionManager.getAction(EDITOR_POPUP_MENU_ID);
		if (openCmsEditorPopupMenu == null) {
			DefaultActionGroup editorPopup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_POPUP);
			openCmsEditorPopupMenu = new OpenCmsEditorPopupMenu(this);
			addAction(editorPopup, EDITOR_POPUP_MENU_ID, openCmsEditorPopupMenu, "_OpenCms", MENU_ICON, new Constraints(Anchor.BEFORE, "IDEtalk.SendCodePointer"));
			editorPopup.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, EDITOR_POPUP_MENU_ID));
		}
	}

	private void registerEditorTabPopupActions() {
		OpenCmsEditorTabPopupMenu openCmsEditorTabPopupMenu = (OpenCmsEditorTabPopupMenu) actionManager.getAction(TAB_POPUP_MENU_ID);
		if (openCmsEditorTabPopupMenu == null) {
			DefaultActionGroup editorTabPopup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_TAB_POPUP);
			openCmsEditorTabPopupMenu = new OpenCmsEditorTabPopupMenu(this);
			editorTabPopup.addAction(Separator.getInstance());
			addAction(editorTabPopup, TAB_POPUP_MENU_ID, openCmsEditorTabPopupMenu, "_OpenCms", MENU_ICON);
		}
	}

	private void unregisterActions() {
		unregisterMenuAction(OPENCMS_MENU_ID);
		unregisterMenuAction(PROJECT_POPUP_MENU_ID);
		unregisterMenuAction(EDITOR_POPUP_MENU_ID);
		unregisterMenuAction(TAB_POPUP_MENU_ID);
	}

	private void unregisterMenuAction(String menuId) {
		OpenCmsMenu menu = (OpenCmsMenu)actionManager.getAction(menuId);
		if (menu != null) {
			menu.unregisterActions();
			actionManager.unregisterAction(menuId);
		}
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
