package com.mediaworx.intellij.opencmsplugin.configuration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class OpenCmsPluginConfigurationData {

    public static final String NEW_LINE = System.getProperty("line.separator");

	private boolean openCmsPluginActive;
    private String repository;
    private String username;
    private String password;
    private String webappRoot;
    private HashMap<String,String> localModuleVfsRootMap;
    private HashSet<String> moduleFolders;

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
        if (this.localModuleVfsRootMap != null) {
            for (String moduleName : this.localModuleVfsRootMap.keySet()) {
                String moduleFolder = this.localModuleVfsRootMap.get(moduleName);
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

            this.localModuleVfsRootMap = new LinkedHashMap<String,String>(moduleAssignmentStrings.length);
            moduleFolders = new HashSet<String>(moduleAssignmentStrings.length);

            for (String moduleAssignmentString : moduleAssignmentStrings) {
                String[] module = moduleAssignmentString.split("=");

	            if (module.length == 2) {
		            // strip the trailing path separator if there is one
		            if (module[1].endsWith("/") || module[1].endsWith("\\")) {
			            module[1] = module[1].substring(0, module[1].length() - 1);
		            }

		            this.localModuleVfsRootMap.put(module[0], module[1]);
	                moduleFolders.add(module[1]);
	            }
	            else {
		            // TODO: Tell the user that there's a configuration error
	            }
            }
        }
    }

    public HashMap<String, String> getLocalModuleVfsRootMap() {
        return localModuleVfsRootMap;
    }

    public HashSet<String> getModuleParentPaths() {
        return moduleFolders;
    }

    public String getLocalModuleVfsRoot(String moduleName) {
        return localModuleVfsRootMap.get(moduleName);
    }

    private String stripTrailingSeparator(String s) {
		if (s != null && s.endsWith(File.separator)) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}
}
