package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import org.jetbrains.annotations.NotNull;

public class OpenCmsPluginComponent implements ProjectComponent {

    Project project;
    VfsAdapter vfsAdapter;

    public OpenCmsPluginComponent(Project project) {
        this.project = project;
    }

    public void projectOpened() {
	}

	public void projectClosed() {
	}

    public void initComponent() {
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPluginComponent";
	}

    public VfsAdapter getVfsAdapter() {
        if (vfsAdapter == null) {
            OpenCmsPluginConfigurationData config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();
            if (config != null && config.isOpenCmsPluginActive() && config.getPassword() != null && config.getPassword().length() > 0) {
                this.vfsAdapter = new VfsAdapter(config);
            }
        }
        return vfsAdapter;
    }
}
