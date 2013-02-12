package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleConfigurable;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginModuleConfigurationData;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginModuleConfigurationForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

// @State(
// 		name = "OpenCmsPluginModuleConfigurationData",
// 		storages = {
// 				@Storage(file = "$MODULE_FILE$"),
// 				@Storage(file = "$MODULE_CONFIG_DIR$/opencms.xml", scheme = StorageScheme.DIRECTORY_BASED)
// 		}
// )
//public class OpenCmsPluginModuleConfigurationComponent implements ModuleComponent, Configurable, PersistentStateComponent<OpenCmsPluginModuleConfigurationData> {
public class OpenCmsPluginModuleConfigurationComponent implements ModuleComponent, Configurable {

	private OpenCmsPluginModuleConfigurationForm form;
    private OpenCmsPluginModuleConfigurationData configurationData;

	public void projectOpened() {
        // Do nothing
	}

	public void projectClosed() {
        // Do nothing
	}

	public void moduleAdded() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void initComponent() {
        // Do nothing
	}

	public void disposeComponent() {
        // Do nothing
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPluginModuleConfigurationComponent";
	}

	@Nls
	public String getDisplayName() {
		return "OpenCms Plugin";
	}

	@Nullable
	public String getHelpTopic() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Nullable
	public JComponent createComponent() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Nullable
	// public JComponent createComponent() {
	// 	return null;  //To change body of implemented methods use File | Settings | File Templates.
	// }

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

    public OpenCmsPluginModuleConfigurationData getState() {
        return this.configurationData;
    }

    public void loadState(OpenCmsPluginModuleConfigurationData configurationData) {
		if (this.configurationData == null) {
			this.configurationData = new OpenCmsPluginModuleConfigurationData();
		}
        XmlSerializerUtil.copyBean(configurationData, this.configurationData);
    }

    public OpenCmsPluginModuleConfigurationData getConfigurationData() {
        return configurationData;
	}

	public void saveData() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void moduleStateChanged() {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
