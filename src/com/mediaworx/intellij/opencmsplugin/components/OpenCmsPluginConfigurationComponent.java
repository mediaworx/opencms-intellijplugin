package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@State(
    name = "OpenCmsPluginConfigurationData",
    storages = {
        @Storage( file = "$WORKSPACE_FILE$"),
        @Storage( file = "$PROJECT_CONFIG_DIR$/opencms.xml", scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class OpenCmsPluginConfigurationComponent implements ProjectComponent, Configurable, PersistentStateComponent<OpenCmsPluginConfigurationData> {

   	private OpenCmsPluginConfigurationForm form;
    private OpenCmsPluginConfigurationData configurationData;

    public void projectOpened() {
        // Do nothing
    }

    public void projectClosed() {
        // Do nothing
    }

    public void initComponent() {
        // Do nothing
    }

    public void disposeComponent() {
        // Do nothing
    }

    @NotNull
    public String getComponentName() {
        return "OpenCmsPluginConfigurationComponent";
    }

    @Nls
    public String getDisplayName() {
        return "OpenCms Plugin";
    }

    public Icon getIcon() {
        return IconLoader.getIcon("/icons/opencms.png");
    }

    public String getHelpTopic() {
        return null;  // Do nothing
    }

    public JComponent createComponent() {
		if (configurationData == null) {
			configurationData = new OpenCmsPluginConfigurationData();
		}
		if (form == null) {
			form = new OpenCmsPluginConfigurationForm();
		}
		return form.getRootComponent();
    }

    public boolean isModified() {
		return form != null && form.isModified(configurationData);
    }

    public void apply() throws ConfigurationException {
		if (form != null) {
			// Get data from editor to component
			form.getData(configurationData);
		}
    }

    public void reset() {
		if (form != null) {
			// Reset form data from component
			form.setData(configurationData);
		}
    }

    public void disposeUIResources() {
		form = null;
    }

    public OpenCmsPluginConfigurationData getState() {
        return this.configurationData;
    }

    public void loadState(OpenCmsPluginConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsPluginConfigurationData();
		}
        XmlSerializerUtil.copyBean(configurationData, this.configurationData);
	    this.configurationData.initModuleConfiguration();
    }

    public OpenCmsPluginConfigurationData getConfigurationData() {
        return configurationData;
    }
}
