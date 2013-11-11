package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.mediaworx.intellij.opencmsplugin.configuration.module.OpenCmsModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.module.OpenCmsModuleConfigurationForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@State(
    name = "OpenCmsModuleConfigurationData",
    storages = {
        @Storage(id = "OpenCmsPluginModuleConfiguration", file = "$MODULE_FILE$")
    }
)
public class OpenCmsModuleConfigurationComponent implements ModuleComponent, Configurable, PersistentStateComponent<OpenCmsModuleConfigurationData> {

	private OpenCmsPlugin plugin;
	private Module module;
	private OpenCmsModuleConfigurationForm form;
	private OpenCmsModuleConfigurationData configurationData;

	public OpenCmsModuleConfigurationComponent(Module module) {
		this.module = module;
	}

	public void initComponent() {
		plugin = module.getProject().getComponent(OpenCmsPlugin.class);
	}

	public void disposeComponent() {
		plugin = null;
		module = null;
		form = null;
		configurationData = null;
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsModuleConfigurationComponent";
	}

	public void projectOpened() {
		// called when project is opened
	}

	public void projectClosed() {
		// called when project is being closed
	}

	public void moduleAdded() {
		// Invoked when the module corresponding to this component instance has been completely
		// loaded and added to the project.
	}

	@Nls
	public String getDisplayName() {
		return "OpenCms Module";
	}

	public Icon getIcon() {
        return IconLoader.getIcon("/icons/opencms.png");
	}

	@Nullable
	public String getHelpTopic() {
		return null;  // Do nothing
	}

	@Nullable
	public JComponent createComponent() {
		if (configurationData == null) {
			configurationData = new OpenCmsModuleConfigurationData();
		}
		if (form == null) {
			form = new OpenCmsModuleConfigurationForm();
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
			handleOcmsModuleRegistration();
		}
	}

	private void handleOcmsModuleRegistration() {
		if (configurationData.isOpenCmsModuleEnabled()) {
			plugin.getOpenCmsModules().registerModule(module, configurationData);
		}
		else {
			plugin.getOpenCmsModules().unregisterModule(module);
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

	@Nullable
	public OpenCmsModuleConfigurationData getState() {
		return this.configurationData;
	}

	public void loadState(OpenCmsModuleConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsModuleConfigurationData();
		}
        XmlSerializerUtil.copyBean(configurationData, this.configurationData);
		handleOcmsModuleRegistration();
	}
}
