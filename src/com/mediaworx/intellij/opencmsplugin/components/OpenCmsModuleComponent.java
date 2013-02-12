package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 25.01.13
 * Time: 18:15
 * To change this template use File | Settings | File Templates.
 */
public class OpenCmsModuleComponent implements ModuleComponent {

	public OpenCmsModuleComponent(Module module) {
	}

	public void initComponent() {
		// TODO: insert component initialization logic here
	}

	public void disposeComponent() {
		// TODO: insert component disposal logic here
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsModuleComponent";
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
}
