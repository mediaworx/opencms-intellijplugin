package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsConfiguration;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import org.jetbrains.annotations.NotNull;

public class OpenCmsPlugin implements ProjectComponent {

	private static final String OPENCMS_MENU_ID = "OpenCmsPlugin.ActionMenu";
	private static final String EDITOR_POPUP_ID = "OpenCmsEditorPopupAction";
	private static final String PROJECT_POPUP_ID = "OpenCmsProjectViewPopupAction";
	private static final String SYNC_ALL_ID = "OpenCmsSyncAllAction";
	private static final String SYNC_ID = "OpenCmsSyncAction";

	Project project;
	OpenCmsConfiguration openCmsConfiguration;
	OpenCmsModules openCmsModules;
	VfsAdapter vfsAdapter;
	OpenCmsPluginConfigurationData config;

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
		ActionManager actionManager = ActionManager.getInstance();
		if (config != null && config.isOpenCmsPluginActive()) {
			openCmsConfiguration = new OpenCmsConfiguration(config.getWebappRoot());

			AnAction openCmsMenu = actionManager.getAction(OPENCMS_MENU_ID);
			DefaultActionGroup mainMenu = (DefaultActionGroup) actionManager.getAction("MainMenu");
			try {
				mainMenu.addAction(openCmsMenu, new Constraints(Anchor.BEFORE, "WindowMenu"));
			}
			catch (IllegalArgumentException e) {
				System.out.println(OPENCMS_MENU_ID + " has already been added to MainMenu. " + e.getMessage());
			}

			AnAction editorPopupAction = actionManager.getAction(EDITOR_POPUP_ID);
			DefaultActionGroup editorPopupMenu = (DefaultActionGroup) actionManager.getAction("EditorPopupMenu");
			try {
				editorPopupMenu.addAction(editorPopupAction, new Constraints(Anchor.BEFORE, "IDEtalk.SendCodePointer"));
				editorPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, EDITOR_POPUP_ID));
			}
			catch (IllegalArgumentException e) {
				System.out.println(EDITOR_POPUP_ID + " has already been added to EditorPopupMenu. " + e.getMessage());
			}

			DefaultActionGroup editorTabPopupMenu = (DefaultActionGroup) actionManager.getAction("EditorTabPopupMenu");
			try {
				editorTabPopupMenu.addAction(editorPopupAction);
				editorTabPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.BEFORE, EDITOR_POPUP_ID));
			}
			catch (IllegalArgumentException e) {
				System.out.println(EDITOR_POPUP_ID + " has already been added to EditorPopupMenu. " + e.getMessage());
			}

			AnAction projectPopupAction = actionManager.getAction(PROJECT_POPUP_ID);
			DefaultActionGroup projectPopupMenu = (DefaultActionGroup) actionManager.getAction("ProjectViewPopupMenu");
			try {
				projectPopupMenu.addAction(projectPopupAction, new Constraints(Anchor.BEFORE, "RevealIn"));
				projectPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, PROJECT_POPUP_ID));
			}
			catch(IllegalArgumentException e) {
				System.out.println(PROJECT_POPUP_ID + " has already been added to ProjectViewPopupMenu. " + e.getMessage());
			}
		}
		else {
			unregisterActions();
		}
	}

	private void unregisterActions() {
		ActionManager actionManager = ActionManager.getInstance();
		actionManager.unregisterAction(OPENCMS_MENU_ID);
		actionManager.unregisterAction(EDITOR_POPUP_ID);
		actionManager.unregisterAction(PROJECT_POPUP_ID);
		actionManager.unregisterAction(SYNC_ID);
		actionManager.unregisterAction(SYNC_ALL_ID);
	}

	public void disposeComponent() {
		project = null;
		vfsAdapter = null;
		config = null;
		openCmsConfiguration = null;
		openCmsModules = null;
		unregisterActions();
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPluginComponent";
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
			if (config != null && config.isOpenCmsPluginActive() && config.getPassword() != null && config.getPassword().length() > 0) {
				vfsAdapter = new VfsAdapter(config.getRepository(), config.getUsername(), config.getPassword());
				vfsAdapter.startSession();
			}
		}
		return vfsAdapter;
	}
}
