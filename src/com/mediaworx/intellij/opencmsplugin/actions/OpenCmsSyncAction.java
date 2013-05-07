package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPluginComponent;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPluginConfigurationComponent;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;

// TODO: prüfen und überlegen, ob es möglich und sinnvoll ist, Auto-Sync bei File change events zu realisieren
public class OpenCmsSyncAction extends AnAction {

    Project project;
    OpenCmsPluginConfigurationData config;
    VfsAdapter vfsAdapter;

	@Override
	public void actionPerformed(AnActionEvent event) {

	    this.project = DataKeys.PROJECT.getData(event.getDataContext());
		this.config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();

		System.out.println("Event: " + event);
		System.out.println("Config: " + config);
		System.out.println("Plugin active? " + config.isOpenCmsPluginActive());

		if (config.isOpenCmsPluginActive()) {

	        this.vfsAdapter = project.getComponent(OpenCmsPluginComponent.class).getVfsAdapter();

	        if (vfsAdapter != null) {

		        FileSyncer fileSyncer = new FileSyncer(project, config, vfsAdapter);

		        try {
			        fileSyncer.syncFiles(event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY));
		        }
		        catch (Exception e) {
			        System.out.println("Exception in OpenCmsPlugin.actionPerformed: " + e.getMessage());
		        }
	        }
		}
	}

}
