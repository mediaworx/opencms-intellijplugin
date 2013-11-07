package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class OpenCmsPluginConfigurationData {

    public static final String NEW_LINE = System.getProperty("line.separator");

	private boolean openCmsPluginActive;
    private String repository;
    private String username;
    private String password;
    private String webappRoot;
	private HashMap<String, OpenCmsModule> modules;
	private SyncMode syncMode;

	public boolean isOpenCmsPluginActive() {
		return openCmsPluginActive;
	}

	public void setOpenCmsPluginActive(boolean openCmsPluginActive) {
		this.openCmsPluginActive = openCmsPluginActive;
	}

	public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWebappRoot() {
	    return stripTrailingSeparator(webappRoot);
    }

    public void setWebappRoot(String webappRoot) {
        this.webappRoot = webappRoot;
    }

    public String getLocalModuleVfsRoots() {
        StringBuilder localModuleVfsRoots = new StringBuilder();
        if (this.modules != null) {
            for (String moduleName : this.modules.keySet()) {
                String moduleFolder = this.modules.get(moduleName).getLocalVfsRoot();
                localModuleVfsRoots.append(moduleName).append("=").append(moduleFolder).append(NEW_LINE);
            }
        }
        return localModuleVfsRoots.toString();
    }

    public void setLocalModuleVfsRoots(String localModuleVfsRoots) {
        if (localModuleVfsRoots != null && localModuleVfsRoots.length() > 0) {
            localModuleVfsRoots = localModuleVfsRoots.trim();

	        // make sure that the newlines are consistent
	        localModuleVfsRoots = localModuleVfsRoots.replaceAll("[\r\n]+", NEW_LINE);

            String[] moduleAssignmentStrings = localModuleVfsRoots.split(NEW_LINE);

            this.modules = new LinkedHashMap<String,OpenCmsModule>(moduleAssignmentStrings.length);

            for (String moduleAssignmentString : moduleAssignmentStrings) {

                String[] moduleToPathMapping = moduleAssignmentString.split("=");

	            if (moduleToPathMapping.length == 2) {
		            // strip the trailing path separator if there is one
		            if (moduleToPathMapping[1].endsWith("/") || moduleToPathMapping[1].endsWith("\\")) {
			            moduleToPathMapping[1] = moduleToPathMapping[1].substring(0, moduleToPathMapping[1].length() - 1);
		            }

		            String moduleName = moduleToPathMapping[0];
		            String localModuleVfsRoot = moduleToPathMapping[1];

		            OpenCmsModule module = new OpenCmsModule(moduleName);
		            module.setLocalVfsRoot(localModuleVfsRoot);
					modules.put(moduleName, module);
	            }
	            else {
		            // TODO: Tell the user that there's a configuration error
	            }
            }
        }
    }

	public HashMap<String, OpenCmsModule> getModules() {
		return modules;
	}

    public OpenCmsModule getModule(String moduleName) {
        return modules.get(moduleName);
    }

    public String getLocalModuleVfsRoot(String moduleName) {
        return modules.get(moduleName).getLocalVfsRoot();
    }

	public SyncMode getSyncMode() {
		return syncMode != null ? syncMode : SyncMode.PUSH;
	}

	public void setSyncMode(String syncMode) {
		this.syncMode = SyncMode.fromString(syncMode);
	}

	public void setSyncMode(SyncMode syncMode) {
		this.syncMode = syncMode;
	}

	private String stripTrailingSeparator(String s) {
		if (s != null && s.endsWith(File.separator)) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}
}
