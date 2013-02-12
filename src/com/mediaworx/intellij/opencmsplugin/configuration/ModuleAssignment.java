package com.mediaworx.intellij.opencmsplugin.configuration;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 25.01.13
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class ModuleAssignment {
    private String intelliJModule;
    private String openCmsModule;

    public ModuleAssignment() {

    }

    public ModuleAssignment(String intelliJModule, String openCmsModule) {
        this.intelliJModule = intelliJModule;
        this.openCmsModule = openCmsModule;
    }

    public String getIntelliJModule() {
        return intelliJModule;
    }

    public void setIntelliJModule(String intelliJModule) {
        this.intelliJModule = intelliJModule;
    }

    public String getOpenCmsModule() {
        return openCmsModule;
    }

    public void setOpenCmsModule(String openCmsModule) {
        this.openCmsModule = openCmsModule;
    }
}
