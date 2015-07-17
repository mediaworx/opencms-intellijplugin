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

package com.mediaworx.intellij.opencmsplugin;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsEditorPopupMenu;
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsEditorTabPopupMenu;
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMainMenu;
import com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsProjectPopupMenu;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationComponent;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.connector.OpenCmsPluginConnector;
import com.mediaworx.intellij.opencmsplugin.listeners.OpenCmsModuleFileChangeListener;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsConfiguration;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsPluginToolWindowFactory;
import com.mediaworx.intellij.opencmsplugin.toolwindow.OpenCmsToolWindowConsole;
import com.mediaworx.opencms.ideconnector.client.IDEConnectorClient;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

/**
 * OpenCms plugin for IntelliJ providing IntelliJ menu actions to sync resources to and from the OpenCms VFS, to publish
 * resources right from your IDE (using an additional OpenCms module), to create module manifest files and to package
 * OpenCms modules into zips.
 * OpenCms menu actions are provided in four different locations:
 * <ul>
 *     <li>At the main menu</li>
 *     <li>In the project window (left)</li>
 *     <li>In the main editor window (center)</li>
 *     <li>In the file tabs (usually on top of the editor)</li>
 * </ul>
 *
 * The configuration of the plugin is done on two levels:
 * <br /><br />
 * <strong>Project level</strong>
 * <br />
 * Some general options like the path to your local OpenCms webapp, your OpenCms user credentials or generic settings
 * are done on the project level. These options can be found under File > Settings > OpenCms Plugin.
 * <br /><br />
 * <strong>Module level</strong>
 * <br />
 * Module specific settings are configured for each separate module under File > Project Structure > Modules > [name of
 * the module] > Tab "OpenCms Module".
 *
 * @author Kai Widmann, 2007-2014 mediaworx berlin AG
 */
// TODO: make new service configurable
// TODO: introduce Import menu actions only if the service is configured (should be displayed but greyed out or should display message)

public class OpenCmsPlugin implements ProjectComponent {

	private static final Logger LOG = Logger.getInstance(OpenCmsPlugin.class);

	public static final String TOOLWINDOW_ID = "OpenCms";

	private static final String OPENCMS_MENU_ID = "OpenCmsPlugin.ActionMenu";
	private static final String PROJECT_POPUP_MENU_ID = "OpenCmsPlugin.ProjectPopupMenu";
	private static final String EDITOR_POPUP_MENU_ID = "OpenCmsPlugin.EditorPopupMenu";
	private static final String TAB_POPUP_MENU_ID = "OpenCmsPlugin.TabsPopupGroup";

	private static final Icon MENU_ICON = new ImageIcon(OpenCmsPlugin.class.getResource("/icons/opencms_menu.png"));

	/** The IntelliJ project */
	private Project project;

	/** Helper object to retrieve configuration data from the OpenCms configuration */
	private OpenCmsConfiguration openCmsConfiguration;

	/** Container for all OpenCms modules configured in the project */
	private OpenCmsModules openCmsModules;

	/** Adapter used to sync the RFS with the OpenCms VFS */
	private VfsAdapter vfsAdapter;

	/**
	 * Connector used to retrieve module or resource information from OpenCms and execute actions in OpenCms
	 * (e.g. publishing), backed by the OpenCms module "com.mediaworx.opencms.ideconnector"
	 */
	private OpenCmsPluginConnector pluginConnector;

	/**
	 * Client used to execute actions in OpenCms (
	 */
	private IDEConnectorClient connectorClient;

	/**
	 * ToolWindow for the OpenCms plugin
	 */
	private ToolWindow toolWindow;

	/**
	 * Console used to log OpenCms actions like sync or publish
	 */
	private OpenCmsToolWindowConsole console;

	/** IntelliJ's action manager */
	private ActionManager actionManager;

	/**
	 * Set to <code>true</code> the first time the OpenCms plugin is enabled
	 */
	private boolean wasInitialized = false;

	/**
	 * Creates a new plugin instance, called by IntelliJ
	 * @param project   the IntelliJ project
	 */
	public OpenCmsPlugin(Project project) {
		this.project = project;
		openCmsModules = new OpenCmsModules(this);
	}

	/**
	 * Initializes the plugin, called by IntelliJ
	 */
	public void initComponent() {
		LOG.warn("initComponent called, project: " + project.getName());
		actionManager = ActionManager.getInstance();
		
	}
	
	/**
	 * Enables the plugin (if it is enabled in the project level configuration), called by IntelliJ whenever a project
	 * is opened.
	 */
	public void projectOpened() {
		OpenCmsPluginConfigurationData config = getPluginConfiguration();
		if (config != null && config.isOpenCmsPluginEnabled()) {
			enable();
		}
	}

	/**
	 * Does nothing, called by IntelliJ whenever a project is closed.
	 */
	public void projectClosed() {
	}

	/**
	 * Enables the plugin for the current project. Initializes the plugin and its actions if initialization was not
	 * done before. If the plugin connector is activated in the project level configuration it gets initialized as well.
	 */
	public void enable() {
		if (!wasInitialized) {
			OpenCmsPluginConfigurationData config = getPluginConfiguration();
			if (config != null && config.isOpenCmsPluginEnabled()) {
				if (config.isPluginConnectorEnabled()) {
					pluginConnector = new OpenCmsPluginConnector(config.getConnectorUrl(), config.getUsername(), config.getPassword(), config.isUseMetaVariablesEnabled());
				}
			}
			registerMenus();
			registerListeners();
			wasInitialized = true;

			checkWebappRootConfiguration(true);
		}
		else {
			setToolWindowAvailable(true);
		}
	}

	/**
	 * Disables the plugin. The only thing done here is deactivating the plugin's tool window, menus are disabled
	 * automatically because all actions are hidden if the plugin is deactivated
	 */
	public void disable() {
		if (wasInitialized) {
			setToolWindowAvailable(false);
		}
	}

	/**
	 * Registers the OpenCms menus for MainMenu, ProjectPopup, EditorPopup and EditorTabPopup
	 */
	private void registerMenus() {
		registerMainMenu();
		registerProjectPopupMenu();
		registerEditorPopupMenu();
		registerEditorTabPopupMenu();
	}

	/**
	 * Registers listeners for IntelliJ events. The listeners only listen for file change events and only file
	 * deletions, renames and moves are handled (depending on the project level configuration these changes may
	 * be synced back to OpenCms).
	 */
	private void registerListeners() {
		MessageBus bus = ApplicationManager.getApplication().getMessageBus();
		MessageBusConnection connection = bus.connect();
		OpenCmsModuleFileChangeListener fileChangeListener = new OpenCmsModuleFileChangeListener(this);
		connection.subscribe(VirtualFileManager.VFS_CHANGES, fileChangeListener);
	}

	/**
	 * Adds a menu action to a given menu group.
	 * @param group     the menu group the action should be added to
	 * @param id        the action's identifier
	 * @param action    the action itself
	 * @param text      text to be displayed in the menu
	 */
	public void addAction(DefaultActionGroup group, String id, AnAction action, String text) {
		addAction(group, id, action, text, null, null);
	}

	/**
	 * Adds a menu action to a given menu group.
	 * @param group     the menu group the action should be added to
	 * @param id        the action's identifier
	 * @param action    the action itself
	 * @param text      text to be displayed in the menu
	 * @param icon      icon to be displayed with the menu action (usually displayed befor the text)
	 */
	private void addAction(DefaultActionGroup group, String id, AnAction action, String text, Icon icon) {
		addAction(group, id, action, text, icon, null);
	}

	/**
	 *
	 * Adds a menu action to a given menu group.
	 * @param group         the menu group the action should be added to
	 * @param id            the action's identifier
	 * @param action        the action itself
	 * @param text          text to be displayed in the menu
	 * @param icon          icon to be displayed with the menu action (usually displayed befor the text)
	 * @param constraints   constraints telling IntelliJ where to position the menu action
	 */
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

	/**
	 * Creates and registers the OpenCms menu for the main menu as an action group
	 */
	private void registerMainMenu() {
		OpenCmsMainMenu openCmsMainMenu = (OpenCmsMainMenu)actionManager.getAction(OPENCMS_MENU_ID);
		if (openCmsMainMenu == null) {
			DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_MAIN_MENU);
			openCmsMainMenu = OpenCmsMainMenu.getInstance(this);
			addAction(mainMenu, OPENCMS_MENU_ID, openCmsMainMenu, "_OpenCms", null, new Constraints(Anchor.BEFORE, "HelpMenu"));
		}
	}

	/**
	 * Creates and registers the OpenCms menu for the project popup as an action group
	 */
	private void registerProjectPopupMenu() {
		OpenCmsProjectPopupMenu openCmsProjectPopupMenu = (OpenCmsProjectPopupMenu)actionManager.getAction(PROJECT_POPUP_MENU_ID);
		if (openCmsProjectPopupMenu == null) {
			DefaultActionGroup projectPopup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
			openCmsProjectPopupMenu = new OpenCmsProjectPopupMenu(this);
			addAction(projectPopup, PROJECT_POPUP_MENU_ID, openCmsProjectPopupMenu, "_OpenCms", MENU_ICON, new Constraints(Anchor.BEFORE, "RevealIn"));
			projectPopup.add(Separator.getInstance(), new Constraints(Anchor.AFTER, PROJECT_POPUP_MENU_ID));
		}
	}

	/**
	 * Creates and registers the OpenCms menu for the editor popup as an action group
	 */
	private void registerEditorPopupMenu() {
		OpenCmsEditorPopupMenu openCmsEditorPopupMenu = (OpenCmsEditorPopupMenu)actionManager.getAction(EDITOR_POPUP_MENU_ID);
		if (openCmsEditorPopupMenu == null) {
			DefaultActionGroup editorPopup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_POPUP);
			openCmsEditorPopupMenu = new OpenCmsEditorPopupMenu(this);
			addAction(editorPopup, EDITOR_POPUP_MENU_ID, openCmsEditorPopupMenu, "_OpenCms", MENU_ICON, new Constraints(Anchor.BEFORE, "IDEtalk.SendCodePointer"));
			editorPopup.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, EDITOR_POPUP_MENU_ID));
		}
	}

	/**
	 * Creates and registers the OpenCms menu for the editor tab popup as an action group
	 */
	private void registerEditorTabPopupMenu() {
		OpenCmsEditorTabPopupMenu openCmsEditorTabPopupMenu = (OpenCmsEditorTabPopupMenu)actionManager.getAction(TAB_POPUP_MENU_ID);
		if (openCmsEditorTabPopupMenu == null) {
			DefaultActionGroup editorTabPopup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_EDITOR_TAB_POPUP);
			openCmsEditorTabPopupMenu = new OpenCmsEditorTabPopupMenu(this);
			editorTabPopup.addAction(Separator.getInstance());
			addAction(editorTabPopup, TAB_POPUP_MENU_ID, openCmsEditorTabPopupMenu, "_OpenCms", MENU_ICON);
		}
	}

	public boolean checkWebappRootConfiguration(boolean showDialog) {
		boolean configOK = true;
		OpenCmsPluginConfigurationData config = getPluginConfiguration();
		if (config != null && config.isOpenCmsPluginEnabled()) {
			File file = new File(config.getWebappRoot());
			// show an error message if the webapp root folder was not found
			if (!file.exists()) {
				configOK = false;
				if (showDialog) {
					Messages.showDialog("The Webapp Root was not found.\nPlease check the OpenCms Webapp Root in the OpenCms Plugin settings.", "OpenCms Plugin - Configuration Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
				}
			}
			else {
				// check if the OpenCms configuration path exists
				file = new File(config.getWebappRoot() + OpenCmsConfiguration.CONFIGPATH);
				if (!file.exists()) {
					configOK = false;
					if (showDialog) {
						Messages.showDialog("The OpenCms configuration was not found.\nPlease check the OpenCms Webapp Root in the OpenCms Plugin settings.", "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
					}
				}
			}
		}
		return configOK;
	}

	/**
	 * Does some cleanup, called by IntelliJ.
	 */
	public void disposeComponent() {
		project = null;
		if (openCmsConfiguration != null) {
			openCmsConfiguration.stopMonitoringConfigurationChanges();
			openCmsConfiguration = null;
		}
		openCmsModules = null;
		vfsAdapter = null;
		pluginConnector = null;
		toolWindow = null;
		console = null;
		actionManager = null;
	}

	/**
	 * Returns the component's name.
	 * @return  "OpenCmsPlugin.MainComponent"
	 */
	@NotNull
	public String getComponentName() {
		return "OpenCmsPlugin.MainComponent";
	}

	/**
	 * Returns the IntelliJ project
	 * @return  the IntelliJ project
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * Returns the helper object to retrieve configuration data from the OpenCms configuration
	 * @return  the OpenCms configuration helper object
	 */
	public OpenCmsConfiguration getOpenCmsConfiguration() {
		if (openCmsConfiguration == null) {
			OpenCmsPluginConfigurationData config = getPluginConfiguration();
			openCmsConfiguration = new OpenCmsConfiguration(config);
			openCmsConfiguration.startMonitoringConfigurationChanges();
		}
		return openCmsConfiguration;
	}

	/**
	 * Returns the plugin's project level configuration data
	 * @return  the project level configuration data
	 */
	public OpenCmsPluginConfigurationData getPluginConfiguration() {
		if (project != null && project.getComponent(OpenCmsPluginConfigurationComponent.class) != null) {
			return project.getComponent(OpenCmsPluginConfigurationComponent.class).getState();
		}
		else {
			return null;
		}
	}

	/**
	 * Returns the container for all OpenCms modules configured in the project
	 * @return  the OpenCms module container
	 */
	public OpenCmsModules getOpenCmsModules() {
		return openCmsModules;
	}

	public void refreshOpenCmsModules() {
		openCmsModules.refreshAllModules();
	}

	/**
	 * Returns the adapter used to sync the RFS with the OpenCms VFS
	 * @return  the VFS adapter for sync actions
	 */
	public VfsAdapter getVfsAdapter() {
		if (vfsAdapter == null) {
			OpenCmsPluginConfigurationData config = getPluginConfiguration();
			if (config != null && config.isOpenCmsPluginEnabled() && config.getPassword() != null && config.getPassword().length() > 0) {
				vfsAdapter = new VfsAdapter(config.getRepository(), config.getUsername(), config.getPassword());
			}
		}
		return vfsAdapter;
	}

	/**
	 * Returns the connector used to retrieve module or resource information from OpenCms and execute actions in
	 * OpenCms (e.g. publishing)
	 * @return the OpenCms plugin connector for information retrieval and publishing
	 * @deprecated the old connector (via JSP) will be removed once all functionality is moved to the new connector
	 *             service (via Servlet)
	 */
	@Deprecated
	public OpenCmsPluginConnector getPluginConnector() {
		return pluginConnector;
	}

	/**
	 * Sets the connector used to retrieve module or resource information from OpenCms and execute actions in
	 * OpenCms (e.g. publishing)
	 *
	 * @param pluginConnector the OpenCms plugin connector for information retrieval and publishing
	 * @deprecated the old connector (via JSP) will be removed once all functionality is moved to the new connector
	 *             service (via Servlet)
	 */
	@Deprecated
	public void setPluginConnector(OpenCmsPluginConnector pluginConnector) {
		this.pluginConnector = pluginConnector;
	}

	/**
	 * Returns the IDE connector client that may be used to execute actions on the local OpenCms instance (must be
	 * running)
	 * @return the IDE connector client
	 */
	public IDEConnectorClient getConnectorClient() {
		return connectorClient;
	}

	/**
	 * Sets the connector client used to access the new IDE Connector Service (e.g. for importing modules)
	 * @param connectorClient
	 */
	public void setConnectorClient(IDEConnectorClient connectorClient) {
		this.connectorClient = connectorClient;
	}

	/**
	 * Initializes the plugin's tool window
	 */
	private void initToolWindow() {
		if (project == null) {
			LOG.info("Unable to initialize the tool window since the project is null");
			return;
		}
		ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
		if (toolWindowManager == null) {
			LOG.info("ToolWindowManager could not be retrieved from the project, it may not be registered yet.");
			return;
		}
		toolWindow = toolWindowManager.getToolWindow(OpenCmsPlugin.TOOLWINDOW_ID);
		if (toolWindow == null) {
			OpenCmsPluginConfigurationData config = getPluginConfiguration();
			toolWindow = toolWindowManager.registerToolWindow(OpenCmsPlugin.TOOLWINDOW_ID, false, ToolWindowAnchor.BOTTOM);
			toolWindow.setIcon(new ImageIcon(this.getClass().getResource("/icons/opencms_13.png")));
			toolWindow.setAvailable(config != null && config.isOpenCmsPluginEnabled(), null);
			OpenCmsPluginToolWindowFactory toolWindowFactory = new OpenCmsPluginToolWindowFactory();
			toolWindowFactory.createToolWindowContent(project, toolWindow);
		}
	}

	/**
	 * Returns the plugin's tool window
	 * @return  the plugin's tool window
	 */
	public ToolWindow getToolWindow() {
		if (toolWindow == null) {
			initToolWindow();
		}
		return toolWindow;
	}

	/**
	 * Activates or deactivates the plugin's tool window
	 * @param available <code>true</code> to activate and <code>false</code> to deactivate the tool window
	 */
	public void setToolWindowAvailable(boolean available) {
		ToolWindow toolWindow = getToolWindow();
		toolWindow.setAvailable(available, null);
	}

	/**
	 * Activates the plugin's tool window to display the console
	 */
	public void showConsole() {
		if (toolWindow == null || !toolWindow.isActive()) {
			getToolWindow().activate(null);
		}
	}

	/**
	 * Returns the console used to log OpenCms actions like sync or publish
	 * @return  the console used to log OpenCms actions like sync or publish
	 */
	public OpenCmsToolWindowConsole getConsole() {
		// if the console has not been initialized ...
		if (console == null) {
			initToolWindow();  // ... initialize the tool window (containing the console)
		}
		return console;
	}

	/**
	 * Sets the console used to log OpenCms actions like sync or publish
	 * @param console  the console used to log OpenCms actions like sync or publish
	 */
	public void setConsole(OpenCmsToolWindowConsole console) {
		this.console = console;
	}

}
