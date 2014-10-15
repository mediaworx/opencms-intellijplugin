/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.connector.AutoPublishMode;
import com.mediaworx.intellij.opencmsplugin.sync.SyncMode;
import org.apache.commons.lang.StringUtils;

/**
 * Configuration data container for the project level configuration of the OpenCms plugin for IntelliJ.
 */
public class OpenCmsPluginConfigurationData {

	private boolean openCmsPluginEnabled = false;
    private String repository;
    private String username;
    private String password;
    private String webappRoot;
	private String defaultLocalVfsRoot;
	private String moduleNamingScheme;
	private SyncMode defaultSyncMode;
	private String ignoredFiles;
	private String[] ignoredFilesArray;
	private String ignoredPaths;
	private String[] ignoredPathsArray;
	private boolean pluginConnectorEnabled;
	private String connectorUrl;
	private AutoPublishMode autoPublishMode;
	private boolean pullMetadataEnabled;
	private String manifestRoot;
	private boolean useMetaVariablesEnabled;

	/**
	 * Flag denoting if the plugin is enabled for the project.
	 * @return  <code>true</code> if the plugin is enabled for the project, <code>false</code> otherwise
	 */
	public boolean isOpenCmsPluginEnabled() {
		return openCmsPluginEnabled;
	}

	/**
	 * Sets the flag denoting if the plugin is enabled for the project.
	 * @param openCmsPluginEnabled  <code>true</code> if the plugin is to be enabled for the project,
	 *                              <code>false</code> otherwise
	 */
	public void setOpenCmsPluginEnabled(boolean openCmsPluginEnabled) {
		this.openCmsPluginEnabled = openCmsPluginEnabled;
	}

	/**
	 * Returns the configured CMIS-Repository used to sync files and/or folders to and from the OpenCms VFS.
	 * @return the configured CMIS-Repository, e.g. "http://localhost:8080/opencms/cmisatom/cmis-offline/"
	 */
	public String getRepository() {
        return repository;
    }

	/**
	 * Sets the CMIS-Repository to be used to sync files and/or folders to and from the OpenCms VFS.
	 * @param repository the CMIS-Repository, e.g. "http://localhost:8080/opencms/cmisatom/cmis-offline/"
	 */
    public void setRepository(String repository) {
        this.repository = repository;
    }

	/**
	 * Returns the configured OpenCms username
	 * @return the configured OpenCms username (e.g. "Admin")
	 */
    public String getUsername() {
        return username;
    }

	/**
	 * Sets the OpenCms username to be used to login to OpenCms
	 * @param username an OpenCms username (e.g. "Admin")
	 */
    public void setUsername(String username) {
        this.username = username;
    }

	/**
	 * Returns the configured OpenCms password
	 * @return the configured OpenCms password (e.g. "admin")
	 */
   public String getPassword() {
        return password;
    }

	/**
	 * Sets the OpenCms password to be used to login to OpenCms
	 * @param password the OpenCms user's password (e.g. "admin")
	 */
    public void setPassword(String password) {
        this.password = password;
    }

	/**
	 * Returns the configured webapp root
	 * @return the configured webapp root
	 */
    public String getWebappRoot() {
	    return stripTrailingSeparator(webappRoot);
    }

	/**
	 * Sets the webapp root
	 * @param webappRoot the root of the local OpenCms webapp
	 */
    public void setWebappRoot(String webappRoot) {
        this.webappRoot = webappRoot;
    }

	/**
	 * Returns the configured default local VFS root. That's where VFS files are stored under the module root. Modules
	 * can configure differing VFS roots.
	 * @return the configured default local VFS root (e.g. "src/main/vfs")
	 * @see OpenCmsModuleConfigurationData#getLocalVfsRoot()
	 */
	public String getDefaultLocalVfsRoot() {
		return defaultLocalVfsRoot;
	}

	/**
	 * Sets the default local VFS root. That's where VFS files are stored under the module root. Modules can configure
	 * differing VFS roots.
	 * @param defaultLocalVfsRoot the default local VFS root (e.g. "src/main/vfs")
	 * @see OpenCmsModuleConfigurationData#setLocalVfsRoot(String)
	 */
	public void setDefaultLocalVfsRoot(String defaultLocalVfsRoot) {
		this.defaultLocalVfsRoot = defaultLocalVfsRoot;
	}

	/**
	 * Returns the configured naming scheme for modules. If no specific module name is provided in the module
	 * configuration, that's the scheme used to generate the OpenCms module name by replacing ${modulename} with the
	 * IntelliJ module name.
	 * @return the module naming scheme (e.g. "com.yourcompany.opencms.${modulename})
	 * @see com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModuleConfigurationData#getModuleName() ()
	 */
	public String getModuleNamingScheme() {
		return moduleNamingScheme;
	}

	/**
	 * Sets the naming scheme to be used to generate OpenCms module names. The variable ${modulename} is replaced by
	 * the IntelliJ module name.
	 * @param moduleNamingScheme the naming scheme for OpenCms module names
	 */
	public void setModuleNamingScheme(String moduleNamingScheme) {
		this.moduleNamingScheme = moduleNamingScheme;
	}

	/**
	 * Returns the configured default Sync Mode. The Sync Mode can be PUSH, SYNC or PULL. Modules can configure
	 * differing Sync Modes.
	 * @return the configured Sync Mode
	 * @see OpenCmsModuleConfigurationData#getSyncMode()
	 */
	public SyncMode getDefaultSyncMode() {
		return defaultSyncMode != null ? defaultSyncMode : SyncMode.SYNC;
	}

	/**
	 * Sets the default Sync Mode. The Sync Mode can be PUSH, SYNC or PULL. Modules can configure differing Sync Modes.
	 * @param defaultSyncMode the SyncMode (PUSH, SYNC or PULL)
	 * @see OpenCmsModuleConfigurationData#setSyncMode(SyncMode)
	 */
	public void setDefaultSyncMode(SyncMode defaultSyncMode) {
		this.defaultSyncMode = defaultSyncMode;
	}

	/**
	 * Gets the configured ignored file regex list (one String, separated by Newlines). Files matching one of these
	 * regular expressions are not synced to/from the OpenCms VFS.
	 * @return  the configured ignored file list
	 * @see #getIgnoredFilesArray()
	 */
	public String getIgnoredFiles() {
		return ignoredFiles;
	}

	/**
	 * Gets the configured ignored file regex list as a String Array. Files matching one of these regular expressions
	 * are not synced to/from the OpenCms VFS.
	 * @return  the configured ignored file list split into a String Array.
	 * @see #getIgnoredFiles()
	 */
	public String[] getIgnoredFilesArray() {
		return ignoredFilesArray;
	}

	/**
	 * Sets the ignored file regex list. Files matching one of these regular expressions are not synced to/from the
	 * OpenCms VFS.
	 * @param ignoredFiles the ignored file regex list (one String, separated by Newlines)
	 */
	public void setIgnoredFiles(String ignoredFiles) {
		this.ignoredFiles = ignoredFiles;
		if (ignoredFiles != null && ignoredFiles.length() > 0) {
			ignoredFilesArray = StringUtils.split(ignoredFiles.trim());
		}
		else {
			ignoredFilesArray = new String[0];
		}
	}

	/**
	 * Gets the configured ignored folders regex list (one String, separated by Newlines). Folders matching one of these
	 * regular expressions are not synced to/from the OpenCms VFS.
	 * @return  the configured ignored folders list
	 * @see #getIgnoredFilesArray()
	 */
	public String getIgnoredPaths() {
		return ignoredPaths;
	}

	/**
	 * Gets the configured ignored folders regex list as a String Array. Folders matching one of these regular
	 * expressions are not synced to/from the OpenCms VFS.
	 * @return  the configured ignored folders list split into a String Array.
	 * @see #getIgnoredFiles()
	 */
	public String[] getIgnoredPathsArray() {
		return ignoredPathsArray;
	}

	/**
	 * Sets the ignored folders regex list. Folders matching one of these regular expressions are not synced to/from the
	 * OpenCms VFS.
	 * @param ignoredPaths the ignored folders regex list (one String, separated by Newlines)
	 */
	public void setIgnoredPaths(String ignoredPaths) {
		this.ignoredPaths = ignoredPaths;
		if (ignoredPaths != null && ignoredPaths.length() > 0) {
			ignoredPathsArray = StringUtils.split(ignoredPaths.trim());
		}
		else {
			ignoredPathsArray = new String[0];
		}
	}

	/**
	 * Flag denoting if the Plugin Connector is enabled
	 * @return  <code>true</code> if the Plugin Connector is enabled, <code>false</code> otherwise
	 */
	public boolean isPluginConnectorEnabled() {
		return pluginConnectorEnabled;
	}

	/**
	 * Sets the flag denoting if the Plugin Connector is enabled
	 * @param pluginConnectorEnabled <code>true</code> if the Plugin Connector should be enabled, <code>false</code>
	 *                               otherwise
	 */
	public void setPluginConnectorEnabled(boolean pluginConnectorEnabled) {
		this.pluginConnectorEnabled = pluginConnectorEnabled;
	}

	/**
	 * Gets the configured Url under which the Plugin Connector can be called.
	 * @return the configured Url under which the Plugin Connector can be called
	 */
	public String getConnectorUrl() {
		return connectorUrl;
	}

	/**
	 * Sets the Url under which the Plugin Connector can be called.
	 * @param connectorUrl the Plugin Connector's Url (e.g.
	 *                     http://localhost:8080/opencms/opencms/system/modules/com.mediaworx.opencms.ideconnector/connector.jsp
	 */
	public void setConnectorUrl(String connectorUrl) {
		this.connectorUrl = connectorUrl;
	}

	/**
	 * Gets the configured Auto Publish Mode
	 * @return the configured Auto Publish Mode (OFF, FILECHANGE, ALL)
	 */
	public AutoPublishMode getAutoPublishMode() {
		return autoPublishMode != null ? autoPublishMode : AutoPublishMode.FILECHANGE;
	}

	/**
	 * Sets the Auto Publish Mode
	 * @param autoPublishMode the Auto Publish Mode to be used by the plugin (OFF, FILECHANGE, ALL)
	 */
	public void setAutoPublishMode(AutoPublishMode autoPublishMode) {
		this.autoPublishMode = autoPublishMode;
	}


	/**
	 * Flag denoting if pulling OpenCms module and resource meta data is enabled
	 * @return  <code>true</code> if pulling meta data is enabled, <code>false</code> otherwise
	 */
	public boolean isPullMetadataEnabled() {
		return pullMetadataEnabled;
	}

	/**
	 * Sets the flag denoting if pulling OpenCms module and resource meta data is enabled
	 * @param pullMetadataEnabled <code>true</code> if pulling meta data should be enabled, <code>false</code> otherwise
	 */
	public void setPullMetadataEnabled(boolean pullMetadataEnabled) {
		this.pullMetadataEnabled = pullMetadataEnabled;
	}

	/**
	 * Gets the configured path under which OpenCms module manifest files (<code>manifest.xml</code>) and resource
	 * meta data are to be stored (relative to the module's root path).
	 * @return  the configured manifest root path
	 */
	public String getManifestRoot() {
		return manifestRoot;
	}

	/**
	 * Sets the path under which OpenCms module manifest files (<code>manifest.xml</code>) and resource meta data are
	 * to be stored (relative to the module's root path).
	 * @param manifestRoot the manifest root path to be used
	 */
	public void setManifestRoot(String manifestRoot) {
		this.manifestRoot = manifestRoot;
	}

	/**
	 * Gets the flag denoting if using placeholders instead of UUIDs and dates in resource meta data is enabled
	 * @return <code>true</code> if using placeholders is enabled, <code>false</code> otherwise
	 */
	public boolean isUseMetaVariablesEnabled() {
		return useMetaVariablesEnabled;
	}

	/**
	 * Sets the flag denoting if using placeholders instead of UUIDs and dates in resource meta data is enabled
	 * @param useMetaVariablesEnabled  <code>true</code> if using placeholders should be enabled, <code>false</code> otherwise
	 */
	public void setUseMetaVariablesEnabled(boolean useMetaVariablesEnabled) {
		this.useMetaVariablesEnabled = useMetaVariablesEnabled;
	}

	/**
	 * Internal utility method to strip trailing path separators from path Strings.
	 * @param s the path String
	 * @return  the path String with trailing path separators stripped.
	 */
	private String stripTrailingSeparator(String s) {
		if (s != null && (s.endsWith("\\") || s.endsWith("/"))) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}

}
