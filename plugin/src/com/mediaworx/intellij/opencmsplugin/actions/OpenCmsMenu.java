package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("ComponentNotRegistered")
public class OpenCmsMenu extends DefaultActionGroup {

	private static final Logger LOG = Logger.getInstance(OpenCmsMenu.class);

	public static final String SYNC_MODULE_ID_PREFIX = "OpenCmsPlugin.SyncModule.";
	private ArrayList<String> menuSyncModuleActionIds;
	private ActionManager actionManager;
	private Keymap keymap;

	Project currentProject;

	public OpenCmsMenu(String shortName, boolean popup) {
		super(shortName, popup);
		actionManager = ActionManager.getInstance();
		keymap = KeymapManager.getInstance().getActiveKeymap();
		menuSyncModuleActionIds = new ArrayList<String>();
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);

		Project eventProject = e.getProject();

		if (eventProject == null) {
			return;
		}

		if (eventProject != currentProject) {

			LOG.info("project switched, reinitializing sync module actions");

			if (menuSyncModuleActionIds.size() > 0) {
				unregisterCurrentSyncModuleActions();
			}

			OpenCmsPlugin plugin = eventProject.getComponent(OpenCmsPlugin.class);
			Collection<OpenCmsModule> ocmsModules = plugin.getOpenCmsModules().getAllModules();
			for (OpenCmsModule ocmsModule : ocmsModules) {
				registerSyncModuleAction(plugin, ocmsModule);
			}

			currentProject = eventProject;
		}
	}

	private void unregisterCurrentSyncModuleActions() {
		for (String actionId : menuSyncModuleActionIds) {
			keymap.removeAllActionShortcuts(actionId);
			remove(actionManager.getAction(actionId));
			actionManager.unregisterAction(actionId);
		}
		menuSyncModuleActionIds.clear();
	}

	public void registerSyncModuleAction(OpenCmsPlugin plugin, OpenCmsModule ocmsModule) {
		int moduleNo = menuSyncModuleActionIds.size() + 1;
		boolean addShortcut = moduleNo < 10;

		String actionId = SYNC_MODULE_ID_PREFIX + ocmsModule.getIntelliJModuleRoot();
		if (addShortcut) {
			Shortcut shortcut = new KeyboardShortcut(OpenCmsPlugin.COMMON_FIRST_KEYSTROKE, KeyStroke.getKeyStroke(String.valueOf(moduleNo)));
			keymap.addShortcut(actionId, shortcut);
		}
		String text = "Sync Module " + (addShortcut ? "_" : "") + moduleNo + " " + ocmsModule.getModuleName();
		plugin.addAction(this, actionId, new OpenCmsSyncAction(), text);
		menuSyncModuleActionIds.add(actionId);
	}

}
