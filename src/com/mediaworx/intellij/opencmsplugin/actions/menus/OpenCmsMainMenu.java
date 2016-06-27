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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
import com.mediaworx.intellij.opencmsplugin.actions.generatemanifest.OpenCmsGenerateAllManifestsAction;
import com.mediaworx.intellij.opencmsplugin.actions.generatemanifest.OpenCmsGenerateSelectedModuleManifestsAction;
import com.mediaworx.intellij.opencmsplugin.actions.importmodule.OpenCmsImportAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.importmodule.OpenCmsImportModuleAction;
import com.mediaworx.intellij.opencmsplugin.actions.importmodule.OpenCmsImportSelectedModuleAction;
import com.mediaworx.intellij.opencmsplugin.actions.packagemodule.OpenCmsPackageAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.packagemodule.OpenCmsPackageSelectedModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishModuleAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.publish.OpenCmsPublishSelectedAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullAllMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.pullmetadata.OpenCmsPullSelectedModuleMetaDataAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncAllModulesAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncModuleAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncOpenEditorTabsAction;
import com.mediaworx.intellij.opencmsplugin.actions.sync.OpenCmsSyncSelectedAction;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Collection;

/**
 * Used to create the OpenCms menu located in the main menu bar containing the following actions:
 * <ul>
 *     <li>Sync actions (sync selected, sync all open tabs, sync all modules, sync specific module)</li>
 *     <li>Pull Meta Data actions (Pull meta data for selected module, pull all meta data)</li>
 *     <li>Generate Manifest actions (Generate manifest for selected module, Generate manifest for all modules)</li>
 *     <li>Package Module Zip actions (Package module zip for selected module, Package Module Zip for all modules)</li>
 *     <li>Publish actions (publish selected, publish all open tabs, publish all modules, publish specific module)</li>
 * </ul>
 *
 * The actions are context aware, so a different text is displayed or an action is disabled depending on the selection
 * in the project tree.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsMainMenu extends OpenCmsMenu {

	private static final Logger LOG = Logger.getInstance(OpenCmsMainMenu.class);

	private static final String SYNC_SELECTED_ID = "OpenCmsPlugin.SyncAction";
	private static final String SYNC_OPEN_TABS_ID = "OpenCmsPlugin.SyncOpenTabsAction";
	private static final String SYNC_ALL_MODULES_ID = "OpenCmsPlugin.SyncAllAction";
	public static final String SYNC_MODULE_ID_PREFIX = "OpenCmsPlugin.SyncModule.";
	private static final String PULL_MODULE_METADATA_ID = "OpenCmsPlugin.PullModuleMetaDataAction";
	private static final String PULL_ALL_METADATA_ID = "OpenCmsPlugin.PullAllMetaDataAction";
	private static final String GENERATE_SELECTED_MODULE_MANIFEST_ID = "OpenCmsPlugin.GenerateManifestAction";
	private static final String GENERATE_ALL_MANIFESTS_ID = "OpenCmsPlugin.GenerateAllManifestsAction";
	private static final String PACKAGE_SELECTED_MODULE_ID = "OpenCmsPlugin.PackageModuleAction";
	private static final String PACKAGE_ALL_MODULES_ID = "OpenCmsPlugin.PackageAllModulesAction";
	private static final String IMPORT_SELECTED_MODULE_ID = "OpenCmsPlugin.ImportModuleAction";
	private static final String IMPORT_ALL_MODULES_ID = "OpenCmsPlugin.ImportAllModulesAction";
	public static final String IMPORT_MODULE_ID_PREFIX = "OpenCmsPlugin.ImportModule.";
	private static final String PUBLISH_SELECTED_ID = "OpenCmsPlugin.PublishAction";
	private static final String PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.PublishOpenTabsAction";
	private static final String PUBLSH_ALL_MODULES_ID = "OpenCmsPlugin.PublishAllModules";
	public static final String PUBLISH_MODULE_ID_PREFIX = "OpenCmsPlugin.PublishModule.";

	private static final Shortcut SYNC_SHORTCUT = new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK), null);

	private Keymap keymap;
	private DefaultActionGroup syncModuleActions;
	private DefaultActionGroup importModuleActions;
	private DefaultActionGroup publishModuleActions;

	private static OpenCmsMainMenu instance;

	Project currentProject;

	/**
	 * Creates the OpenCms menu for the main menu and registers the sync and publish actions for all OpenCms modules in
	 * the project
	 * @param plugin the OpenCms plugin instance
	 */
	private OpenCmsMainMenu(OpenCmsPlugin plugin) {
		super(plugin, "All OpenCms actions", false);
		currentProject = plugin.getProject();
		registerModuleActions();
	}

	/**
	 * Returns the OpenCmsMainMenu instance, creating a new one if necessary
	 * @param plugin the OpenCms plugin instance
	 * @return  the OpenCmsMainMenu instance
	 */
	public static OpenCmsMainMenu getInstance(OpenCmsPlugin plugin) {
		if (instance == null) {
			instance = new OpenCmsMainMenu(plugin);
		}
		return instance;
	}

	/**
	 * Registers the keyboard shortcut for "Sync selected file/folder/module" (Ctrl+Shift+D)
	 */
	private void registerKeyboardShortcuts() {
		if (keymap.getShortcuts(SYNC_SELECTED_ID).length == 0) {
			keymap.addShortcut(SYNC_SELECTED_ID, SYNC_SHORTCUT);
		}
	}

	/**
	 * Registers the main menu's actions
	 */
	@Override
	protected void registerActions() {
		keymap = KeymapManager.getInstance().getActiveKeymap();
		syncModuleActions = new DefaultActionGroup();
		syncModuleActions.getTemplatePresentation().setText("Sync &Module");
		syncModuleActions.setPopup(true);
		importModuleActions = new DefaultActionGroup();
		importModuleActions.getTemplatePresentation().setText("Impor&t Module");
		importModuleActions.setPopup(true);
		publishModuleActions = new DefaultActionGroup();
		publishModuleActions.getTemplatePresentation().setText("Pu&blish Module");
		publishModuleActions.setPopup(true);

		registerKeyboardShortcuts();

		plugin.addAction(this, SYNC_SELECTED_ID, new OpenCmsSyncSelectedAction(), "_Sync selected Modules/Folders/Files");
		plugin.addAction(this, SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenEditorTabsAction(), "Sync all open Editor _Tabs");
		plugin.addAction(this, SYNC_ALL_MODULES_ID, new OpenCmsSyncAllModulesAction(), "Sync _all Modules");
		add(syncModuleActions);

		add(Separator.getInstance());

		plugin.addAction(this, PULL_MODULE_METADATA_ID, new OpenCmsPullSelectedModuleMetaDataAction(), "P_ull Meta Data for selected Modules");
		plugin.addAction(this, PULL_ALL_METADATA_ID, new OpenCmsPullAllMetaDataAction(), "Pu_ll all Meta Data");

		add(Separator.getInstance());

		plugin.addAction(this, GENERATE_SELECTED_MODULE_MANIFEST_ID, new OpenCmsGenerateSelectedModuleManifestsAction(), "_Generate manifest.xml for selected Modules");
		plugin.addAction(this, GENERATE_ALL_MANIFESTS_ID, new OpenCmsGenerateAllManifestsAction(), "Generate manifest._xml for all Modules");

		add(Separator.getInstance());

		plugin.addAction(this, PACKAGE_SELECTED_MODULE_ID, new OpenCmsPackageSelectedModulesAction(), "Package Module _Zip for selected Modules");
		plugin.addAction(this, PACKAGE_ALL_MODULES_ID, new OpenCmsPackageAllModulesAction(), "Package Module Z_ip for all Modules");

		add(Separator.getInstance());

		plugin.addAction(this, IMPORT_SELECTED_MODULE_ID, new OpenCmsImportSelectedModuleAction(), "_Import selected Modules");
		plugin.addAction(this, IMPORT_ALL_MODULES_ID, new OpenCmsImportAllModulesAction(), "Impo_rt all Modules");
		add(importModuleActions);

		add(Separator.getInstance());

		plugin.addAction(this, PUBLISH_SELECTED_ID, new OpenCmsPublishSelectedAction(), "_Publish selected Modules/Folders/Files");
		plugin.addAction(this, PUBLISH_OPEN_TABS_ID, new OpenCmsPublishOpenEditorTabsAction(), "Publish all _open Editor Tabs");
		plugin.addAction(this, PUBLSH_ALL_MODULES_ID, new OpenCmsPublishAllModulesAction(), "Publis_h all Modules");
		add(publishModuleActions);
	}

	/**
	 * refreshes the list of module actions on project change (if two instances of IntelliJ are running and a
	 * switch from one instance to the other occurs). If the active project doesn't use the IntelliJ plugin, the
	 * OpenCms menu in the main menu is explicitly disabled because the disableIfNoVisibleChildren mechanism doesn't
	 * work for the main menu (this seems to be a bug in IntelliJ, see
	 * {@link com.mediaworx.intellij.opencmsplugin.actions.menus.OpenCmsMenu#disableIfNoVisibleChildren()})
	 * @param event the event (provided by IntelliJ)
	 */
	@Override
	public void update(AnActionEvent event) {
		super.update(event);
		if (currentProject != null && !DumbService.isDumb(currentProject)) {
			Presentation presentation = event.getPresentation();
	
			if (presentation.isEnabled() != isPluginEnabled()) {
				presentation.setEnabled(isPluginEnabled());
			}
	
			Project eventProject = event.getProject();
	
			if (eventProject == null) {
				return;
			}
	
			if (eventProject != currentProject) {
	
				if (isPluginEnabled()) {
					LOG.info("project switched, reinitializing module actions");
					currentProject = eventProject;
					registerModuleActions();
				}
				else {
					unregisterModuleActions();
				}
				plugin.setToolWindowAvailable(isPluginEnabled());
			}
		}
	}

	/**
	 * the disableIfNoVisibleChildren mechanism doesn't work for the main menu (this seems to be a bug in IntelliJ),
	 * so it is explicitly disabled.
	 * @return always returns <code>false</code>
	 */
	@Override
	public boolean disableIfNoVisibleChildren() {
		return false;
	}

	/**
	 * Unregisters the current sync and publish module actions. Used to cleanup module actions on project switch before
	 * re-initialization
	 */
	public void unregisterModuleActions() {
		if (syncModuleActions.getChildrenCount() > 0) {
			unregisterCurrentSyncModuleActions();
		}
		if (importModuleActions.getChildrenCount() > 0) {
			unregisterCurrentImportModuleActions();
		}
		if (publishModuleActions.getChildrenCount() > 0) {
			unregisterCurrentPublishModuleActions();
		}
	}

	/**
	 * Registers sync and publish actions for all OpenCms modules in the current project
	 */
	public void registerModuleActions() {

		unregisterModuleActions();

		if (plugin.getPluginConfiguration() != null && plugin.getPluginConfiguration().isOpenCmsPluginEnabled()) {
			try {
				Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
				for (OpenCmsModule ocmsModule : ocmsModules) {
					registerSyncModuleAction(ocmsModule);
					registerImportModuleAction(ocmsModule);
					registerPublishModuleAction(ocmsModule);
				}
			}
			catch (NullPointerException e) {
				LOG.warn("NullPointerException during OpenCms module registration in the main menu.", e);
				LOG.warn("plugin: " + plugin);
				LOG.warn("plugin.getOpenCmsModules(): " + plugin.getOpenCmsModules());
				LOG.warn("plugin.getOpenCmsModules().getAllModules(): " + plugin.getOpenCmsModules().getAllModules());
			}
		}
	}

	/**
	 * Registers a sync action for the given module
	 * @param ocmsModule    an OpenCms module
	 */
	private void registerSyncModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, syncModuleActions, new OpenCmsSyncModuleAction(), SYNC_MODULE_ID_PREFIX);
	}

	/**
	 * Registers an import action for the given module
	 * @param ocmsModule    an OpenCms module
	 */
	private void registerImportModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, importModuleActions, new OpenCmsImportModuleAction(), IMPORT_MODULE_ID_PREFIX);
	}

	/**
	 * Registers a publish action for the given module
	 * @param ocmsModule    an OpenCms module
	 */
	private void registerPublishModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, publishModuleActions, new OpenCmsPublishModuleAction(), PUBLISH_MODULE_ID_PREFIX);
	}

	/**
	 * Internal method to register sync or publish actions for specific modules
	 * @param ocmsModule    an OpenCms module
	 * @param group         the parent action group the new action should be contained in
	 * @param action        the action to be registered
	 * @param idPrefix      the prefix to be used for the action identifier
	 */
	private void registerModuleAction(OpenCmsModule ocmsModule, DefaultActionGroup group, OpenCmsPluginAction action, String idPrefix) {
		int moduleNo = group.getChildrenCount() + 1;
		String actionId = idPrefix + ocmsModule.getModuleBasePath();
		String text = (moduleNo < 10 ? "_" : "") + moduleNo + " " + ocmsModule.getModuleName();
		plugin.addAction(group, actionId, action, text);
	}

	/**
	 * Unregisters all current sync module actions
	 */
	private void unregisterCurrentSyncModuleActions() {
		unregisterChildActions(syncModuleActions);
	}

	/**
	 * Unregisters all current import module actions
	 */
	private void unregisterCurrentImportModuleActions() {
		unregisterChildActions(importModuleActions);
	}

	/**
	 * Unregisters all current publish module actions
	 */
	private void unregisterCurrentPublishModuleActions() {
		unregisterChildActions(publishModuleActions);
	}

	private void unregisterChildActions(DefaultActionGroup parentGroup) {
		AnAction[] allActions = parentGroup.getChildActionsOrStubs();
		for (AnAction action : allActions) {
			String actionId = actionManager.getId(action);
			keymap.removeAllActionShortcuts(actionId);
			actionManager.unregisterAction(actionId);
		}
		parentGroup.removeAll();
	}

}
