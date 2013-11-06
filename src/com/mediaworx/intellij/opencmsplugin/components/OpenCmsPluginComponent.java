package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import org.jetbrains.annotations.NotNull;

public class OpenCmsPluginComponent implements ProjectComponent {

	private static final String OPENCMS_MENU_ID = "OpenCmsPlugin.ActionMenu";
	private static final String EDITOR_POPUP_ID = "OpenCmsEditorPopupAction";
	private static final String PROJECT_POPUP_ID = "OpenCmsProjectViewPopupAction";

    Project project;
    VfsAdapter vfsAdapter;
	OpenCmsPluginConfigurationData config;

    public OpenCmsPluginComponent(Project project) {
        this.project = project;
    }

    public void projectOpened() {
	}

	public void projectClosed() {
	}

    public void initComponent() {
	    config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();
	    ActionManager actionManager = ActionManager.getInstance();
	    if (config != null && config.isOpenCmsPluginActive()) {
		    AnAction openCmsMenu = actionManager.getAction(OPENCMS_MENU_ID);
		    DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction("MainMenu");
		    mainMenu.addAction(openCmsMenu, new Constraints(Anchor.BEFORE, "WindowMenu"));

		    AnAction editorPopupAction = actionManager.getAction(EDITOR_POPUP_ID);
		    DefaultActionGroup editorPopupMenu = (DefaultActionGroup)actionManager.getAction("EditorPopupMenu");
		    editorPopupMenu.addAction(editorPopupAction, new Constraints(Anchor.BEFORE, "IDEtalk.SendCodePointer"));
		    editorPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, EDITOR_POPUP_ID));

		    DefaultActionGroup editorTabPopupMenu = (DefaultActionGroup)actionManager.getAction("EditorTabPopupMenu");
		    editorTabPopupMenu.addAction(editorPopupAction);
		    editorTabPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.BEFORE, EDITOR_POPUP_ID));

		    AnAction projectPopupAction = actionManager.getAction(PROJECT_POPUP_ID);
		    DefaultActionGroup projectPopupMenu = (DefaultActionGroup)actionManager.getAction("ProjectViewPopupMenu");
		    projectPopupMenu.addAction(projectPopupAction, new Constraints(Anchor.BEFORE, "RevealIn"));
		    projectPopupMenu.addAction(Separator.getInstance(), new Constraints(Anchor.AFTER, PROJECT_POPUP_ID));
	    }
	    else {
		    actionManager.unregisterAction(OPENCMS_MENU_ID);
		    actionManager.unregisterAction(EDITOR_POPUP_ID);
		    actionManager.unregisterAction(PROJECT_POPUP_ID);
	    }
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPluginComponent";
	}

    public VfsAdapter getVfsAdapter() {
        if (vfsAdapter == null) {
            if (config != null && config.isOpenCmsPluginActive() && config.getPassword() != null && config.getPassword().length() > 0) {
                this.vfsAdapter = new VfsAdapter(config);
            }
        }
        return vfsAdapter;
    }
}
