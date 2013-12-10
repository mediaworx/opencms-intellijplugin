package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.actions.OpenCmsPluginAction;
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

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsMainMenu extends OpenCmsMenu {

	private static final Logger LOG = Logger.getInstance(OpenCmsMainMenu.class);

	private static final String SYNC_SELECTED_ID = "OpenCmsPlugin.SyncAction";
	private static final String SYNC_OPEN_TABS_ID = "OpenCmsPlugin.SyncOpenTabsAction";
	private static final String SYNC_ALL_MODULES_ID = "OpenCmsPlugin.SyncAllAction";
	private static final String PULL_MODULE_METADATA_ID = "OpenCmsPlugin.PullModuleMetaDataAction";
	private static final String PULL_ALL_METADATA_ID = "OpenCmsPlugin.PullAllMetaDataAction";
	private static final String PUBLISH_SELECTED_ID = "OpenCmsPlugin.PublishAction";
	private static final String PUBLISH_OPEN_TABS_ID = "OpenCmsPlugin.PublishOpenTabsAction";
	public static final String SYNC_MODULE_ID_PREFIX = "OpenCmsPlugin.SyncModule.";
	private static final String PUBLSH_ALL_MODULES_ID = "OpenCmsPlugin.PublishAllModules";
	public static final String PUBLISH_MODULE_ID_PREFIX = "OpenCmsPlugin.PublishModule.";

	private static final Shortcut SYNC_SHORTCUT = new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK), null);

	private Keymap keymap;
	private DefaultActionGroup syncModuleActions;
	private DefaultActionGroup publishModuleActions;

	Project currentProject;

	public OpenCmsMainMenu(OpenCmsPlugin plugin) {
		super(plugin, false);
		currentProject = plugin.getProject();
		registerModuleActions();
	}

	private void registerKeyboardShortcuts() {
		if (keymap.getShortcuts(SYNC_SELECTED_ID).length == 0) {
			keymap.addShortcut(SYNC_SELECTED_ID, SYNC_SHORTCUT);
		}
	}

	@Override
	protected void registerActions() {
		keymap = KeymapManager.getInstance().getActiveKeymap();
		syncModuleActions = new DefaultActionGroup();
		publishModuleActions = new DefaultActionGroup();

		registerKeyboardShortcuts();

		plugin.addAction(this, SYNC_SELECTED_ID, new OpenCmsSyncSelectedAction(), "_Sync selected Modules/Folders/Files");
		plugin.addAction(this, SYNC_OPEN_TABS_ID, new OpenCmsSyncOpenEditorTabsAction(), "Sync all open Editor _Tabs");
		plugin.addAction(this, SYNC_ALL_MODULES_ID, new OpenCmsSyncAllModulesAction(), "Sync _all Modules");

		add(Separator.getInstance());

		add(syncModuleActions);

		add(Separator.getInstance());

		plugin.addAction(this, PULL_MODULE_METADATA_ID, new OpenCmsPullSelectedModuleMetaDataAction(), "_Pull Meta Data for selected Modules");
		plugin.addAction(this, PULL_ALL_METADATA_ID, new OpenCmsPullAllMetaDataAction(), "Pull all _Meta Data");

		add(Separator.getInstance());

		plugin.addAction(this, PUBLISH_SELECTED_ID, new OpenCmsPublishSelectedAction(), "_Publish selected Modules/Folders/Files");
		plugin.addAction(this, PUBLISH_OPEN_TABS_ID, new OpenCmsPublishOpenEditorTabsAction(), "Publish all open Editor Tabs");
		plugin.addAction(this, PUBLSH_ALL_MODULES_ID, new OpenCmsPublishAllModulesAction(), "Publish all Modules");

		add(Separator.getInstance());

		add(publishModuleActions);
	}

	@Override
	public void update(AnActionEvent event) {
		super.update(event);

		event.getPresentation().setEnabled(isPluginEnabled());

		if (isPluginEnabled()) {
			Project eventProject = event.getProject();

			if (eventProject == null) {
				return;
			}

			if (eventProject != currentProject) {
				LOG.info("project switched, reinitializing module actions");
				registerModuleActions();
				currentProject = eventProject;
				registerModuleActions();
			}
		}
	}

	@Override
	public boolean disableIfNoVisibleChildren() {
		return false;
	}

	public void registerModuleActions() {
		if (syncModuleActions.getChildrenCount() > 0) {
			unregisterCurrentSyncModuleActions();
		}
		if (publishModuleActions.getChildrenCount() > 0) {
			unregisterCurrentPublishModuleActions();
		}

		Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
		for (OpenCmsModule ocmsModule : ocmsModules) {
			registerSyncModuleAction(ocmsModule);
		}

		for (OpenCmsModule ocmsModule : ocmsModules) {
			registerPublishModuleAction(ocmsModule);
		}
	}

	public void registerSyncModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, syncModuleActions, new OpenCmsSyncModuleAction(), SYNC_MODULE_ID_PREFIX, "Sync", syncModuleActions.getChildrenCount() < 10);
	}

	public void registerPublishModuleAction(OpenCmsModule ocmsModule) {
		registerModuleAction(ocmsModule, publishModuleActions, new OpenCmsPublishModuleAction(), PUBLISH_MODULE_ID_PREFIX, "Publish", false);
	}

	public void registerModuleAction(OpenCmsModule ocmsModule, DefaultActionGroup group, OpenCmsPluginAction action, String idPrefix, String textPrefix, boolean addShortcut) {
		int moduleNo = group.getChildrenCount() + 1;
		String actionId = idPrefix + ocmsModule.getIntelliJModuleRoot();
		String text = textPrefix + " Module " + (addShortcut ? "_" : "") + moduleNo + " " + ocmsModule.getModuleName();
		plugin.addAction(group, actionId, action, text);
	}

	private void unregisterCurrentSyncModuleActions() {
		AnAction[] allActions = syncModuleActions.getChildActionsOrStubs();
		for (AnAction action : allActions) {
			String actionId = actionManager.getId(action);
			keymap.removeAllActionShortcuts(actionId);
			actionManager.unregisterAction(actionId);
		}
		syncModuleActions.removeAll();
	}

	private void unregisterCurrentPublishModuleActions() {
		AnAction[] allActions = publishModuleActions.getChildActionsOrStubs();
		for (AnAction action : allActions) {
			String actionId = actionManager.getId(action);
			keymap.removeAllActionShortcuts(actionId);
			actionManager.unregisterAction(actionId);
		}
		publishModuleActions.removeAll();
	}

}
