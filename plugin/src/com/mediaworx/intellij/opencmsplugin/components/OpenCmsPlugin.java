package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
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
import java.awt.event.KeyEvent;

public class OpenCmsPlugin implements ProjectComponent {

	// private static final Logger LOG = Logger.getInstance(OpenCmsPlugin.class);

	public static final String TOOLWINDOW_ID = "OpenCms";

	private static final String OPENCMS_MENU_ID = "OpenCmsPlugin.ActionMenu";

	private static final String MENU_SYNC_ID = "OpenCmsPlugin.SyncAction";
	private static final String MENU_SYNC_OPEN_TABS_ID = "OpenCmsPlugin.SyncOpenTabsAction";
	private static final String MENU_SYNC_ALL_ID = "OpenCmsPlugin.SyncAllAction";
	private static final String MENU_PULL_MODULE_METADATA_ID = "OpenCmsPlugin.PullModuleMetaDataAction";
	private static final String MENU_PULL_ALL_METADATA_ID = "OpenCmsPlugin.PullAllMetaDataAction";

	private static final String PROJECT_POPUP_SYNC_ID = "OpenCmsPlugin.ProjectPopupSyncAction";
	private static final String PROJECT_POPUP_PULL_METADATA_ID = "OpenCmsPlugin.ProjectPopupPullModuleMetaDataAction";

	private static final String EDITOR_POPUP_SYNC_ID = "OpenCmsPlugin.EditorPopupSyncAction";

	private static final String TABS_POPUP_SYNC_ID = "OpenCmsPlugin.TabsPopupSyncAction";
	private static final String TABS_POPUP_SYNC_OPEN_TABS_ID = "OpenCmsPlugin.TabsPopupSyncOpenTabsAction";

	private static final KeyStroke COMMON_FIRST_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK);
	private static final Shortcut MENU_SYNC_SHORTCUT = new KeyboardShortcut(COMMON_FIRST_KEYSTROKE, KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
	private static final Shortcut MENU_SYNC_SHORTCUT2 = new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK), null);
	private static final Shortcut MENU_SYNC_OPEN_TABS_SHORTCUT = new KeyboardShortcut(COMMON_FIRST_KEYSTROKE, KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
	private static final Shortcut MENU_SYNC_ALL_SHORTCUT = new KeyboardShortcut(COMMON_FIRST_KEYSTROKE, KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
	private static final Shortcut MENU_PULL_MODULE_METADATA_SHORTCUT = new KeyboardShortcut(COMMON_FIRST_KEYSTROKE, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
	private static final Shortcut MENU_PULL_ALL_METADATA_SHORTCUT = new KeyboardShortcut(COMMON_FIRST_KEYSTROKE, KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));

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
		config = project.getComponent(OpenCmsProjectConfigurationComponent.class).getConfigurationData();
		if (config != null && config.isOpenCmsPluginEnabled()) {
			openCmsConfiguration = new OpenCmsConfiguration(config.getWebappRoot());

			if (config.isPluginConnectorEnabled()) {
				pluginConnector = new OpenCmsPluginConnector(config.getConnectorUrl(), config.getUsername(), config.getPassword());
			}
			registerKeyboardShortcuts();
			registerActions();
		}
		else {
			unregisterActions();
		}
	}

	private void registerKeyboardShortcuts() {
		Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
		if (keymap.getShortcuts(MENU_SYNC_ID).length == 0) {
			keymap.addShortcut(MENU_SYNC_ID, MENU_SYNC_SHORTCUT);
			keymap.addShortcut(MENU_SYNC_ID, MENU_SYNC_SHORTCUT2);
			keymap.addShortcut(MENU_SYNC_OPEN_TABS_ID, MENU_SYNC_OPEN_TABS_SHORTCUT);
			keymap.addShortcut(MENU_SYNC_ALL_ID, MENU_SYNC_ALL_SHORTCUT);
		}
		if (config.isPluginConnectorEnabled() && keymap.getShortcuts(MENU_PULL_MODULE_METADATA_ID).length == 0) {
			keymap.addShortcut(MENU_PULL_MODULE_METADATA_ID, MENU_PULL_MODULE_METADATA_SHORTCUT);
			keymap.addShortcut(MENU_PULL_ALL_METADATA_ID, MENU_PULL_ALL_METADATA_SHORTCUT);
		}
	}

	private void registerActions() {
		actionManager = ActionManager.getInstance();
		registerMainMenuActions();
		registerProjectPopupActions();
		registerEditorPopupActions();
		registerEditorTabPopupActions();
	}

	private void addAction(DefaultActionGroup group, String id, AnAction action, String text) {
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
		DefaultActionGroup openCmsMenu = (DefaultActionGroup)actionManager.getAction(OPENCMS_MENU_ID);

		if (openCmsMenu == null) {
			DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_MAIN_MENU);
			openCmsMenu = new DefaultActionGroup("_OpenCms", false);
			actionManager.registerAction(OPENCMS_MENU_ID, openCmsMenu);
			mainMenu.addAction(openCmsMenu, new Constraints(Anchor.BEFORE, "HelpMenu"));

			addAction(openCmsMenu, MENU_SYNC_ID, new OpenCmsSyncAction(), "_Sync selected Modules/Folders/Files");
			addAction(openCmsMenu, MENU_SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenTabsAction(), "Sync all open Editor _Tabs");
			addAction(openCmsMenu, MENU_SYNC_ALL_ID, new OpenCmsSyncAllAction(), "Sync _all Modules");

			if (config.isPluginConnectorEnabled()) {
				openCmsMenu.add(Separator.getInstance());
				addAction(openCmsMenu, MENU_PULL_MODULE_METADATA_ID, new OpenCmsPullModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
				addAction(openCmsMenu, MENU_PULL_ALL_METADATA_ID, new OpenCmsPullAllMetaDataAction(), "Pull all _Meta Data");
			}
		}
	}

	private void registerProjectPopupActions() {
		DefaultActionGroup projectPopupMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);

		AnAction projectPopupSyncAction = actionManager.getAction(PROJECT_POPUP_SYNC_ID);
		if (projectPopupSyncAction == null) {
			addAction(projectPopupMenu, PROJECT_POPUP_SYNC_ID, new OpenCmsSyncAction(), "OpenCms: Sync selected Modules/Folders/Files", MENU_ICON, new Constraints(Anchor.BEFORE, "RevealIn"));
			if (config.isPluginConnectorEnabled()) {
				addAction(projectPopupMenu, PROJECT_POPUP_PULL_METADATA_ID, new OpenCmsPullModuleMetaDataAction(), "OpenCms: Pull meta data for selected modules", MENU_ICON, new Constraints(Anchor.AFTER, PROJECT_POPUP_SYNC_ID));
			}
			projectPopupMenu.add(Separator.getInstance(), new Constraints(Anchor.AFTER, PROJECT_POPUP_PULL_METADATA_ID));
		}
	}

	private void registerEditorPopupActions() {
		DefaultActionGroup editorPopupMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_POPUP);
		AnAction editorPopupSyncAction = actionManager.getAction(EDITOR_POPUP_SYNC_ID);
		if (editorPopupSyncAction == null) {
			addAction(editorPopupMenu, EDITOR_POPUP_SYNC_ID, new OpenCmsEditorPopupSyncAction(), "OpenCms: Sync File", MENU_ICON, new Constraints(Anchor.BEFORE, "IDEtalk.SendCodePointer"));
			editorPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, EDITOR_POPUP_SYNC_ID));
		}
	}

	private void registerEditorTabPopupActions() {
		DefaultActionGroup tabPopupMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_TAB_POPUP);

		AnAction editorTabsSyncAction = actionManager.getAction(TABS_POPUP_SYNC_ID);
		if (editorTabsSyncAction == null) {
			addAction(tabPopupMenu, TABS_POPUP_SYNC_ID, new OpenCmsSyncAction(), "OpenCms: Sync File", MENU_ICON);
			addAction(tabPopupMenu, TABS_POPUP_SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenTabsAction(), "OpenCms: Sync all open Tabs", MENU_ICON);
			tabPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.BEFORE, TABS_POPUP_SYNC_ID));
		}
	}

	private void unregisterActions() {
		ActionManager actionManager = ActionManager.getInstance();
		actionManager.unregisterAction(MENU_SYNC_ID);
		actionManager.unregisterAction(MENU_SYNC_OPEN_TABS_ID);
		actionManager.unregisterAction(MENU_SYNC_ALL_ID);
		actionManager.unregisterAction(MENU_PULL_MODULE_METADATA_ID);
		actionManager.unregisterAction(MENU_PULL_ALL_METADATA_ID);
		actionManager.unregisterAction(OPENCMS_MENU_ID);

		actionManager.unregisterAction(PROJECT_POPUP_SYNC_ID);
		actionManager.unregisterAction(PROJECT_POPUP_PULL_METADATA_ID);

		actionManager.unregisterAction(EDITOR_POPUP_SYNC_ID);

		actionManager.unregisterAction(TABS_POPUP_SYNC_ID);
		actionManager.unregisterAction(TABS_POPUP_SYNC_OPEN_TABS_ID);
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

	public ToolWindow getToolWindow() {
		if (toolWindow == null) {
			toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(OpenCmsPlugin.TOOLWINDOW_ID, false, ToolWindowAnchor.BOTTOM);
			toolWindow.setIcon(new ImageIcon(this.getClass().getResource("/icons/opencms_13.png")));
			OpenCmsPluginToolWindowFactory toolWindowFactory = new OpenCmsPluginToolWindowFactory();
			toolWindowFactory.createToolWindowContent(project, toolWindow);
		}
		return toolWindow;
	}

	public OpenCmsToolWindowConsole getConsole() {
		return console;
	}

	public void setConsole(OpenCmsToolWindowConsole console) {
		this.console = console;
	}
}
