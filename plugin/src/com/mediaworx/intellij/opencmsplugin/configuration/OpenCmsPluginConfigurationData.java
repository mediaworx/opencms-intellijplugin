package com.mediaworx.intellij.opencmsplugin.configuration;

import com.mediaworx.intellij.opencmsplugin.entities.AutoPublishMode;
import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;

public class OpenCmsPluginConfigurationData {

	private boolean openCmsPluginEnabled = false;
    private String repository;
    private String username;
    private String password;
    private String webappRoot;
	private String defaultLocalVfsRoot;
	private SyncMode defaultSyncMode;
	private boolean pluginConnectorEnabled;
	private String connectorUrl;
	private boolean pullMetadataEnabled;
	private String manifestRoot;
	private AutoPublishMode autoPublishMode;

	public boolean isOpenCmsPluginEnabled() {
		return openCmsPluginEnabled;
	}

	public void setOpenCmsPluginEnabled(boolean openCmsPluginEnabled) {
		this.openCmsPluginEnabled = openCmsPluginEnabled;
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

	public String getDefaultLocalVfsRoot() {
		return defaultLocalVfsRoot;
	}

	public void setDefaultLocalVfsRoot(String defaultLocalVfsRoot) {
		this.defaultLocalVfsRoot = defaultLocalVfsRoot;
	}

	public SyncMode getDefaultSyncMode() {
		return defaultSyncMode != null ? defaultSyncMode : SyncMode.SYNC;
	}

	public void setDefaultSyncMode(SyncMode defaultSyncMode) {
		this.defaultSyncMode = defaultSyncMode;
	}

	public boolean isPluginConnectorEnabled() {
		return pluginConnectorEnabled;
	}

	public void setPluginConnectorEnabled(boolean pluginConnectorEnabled) {
		this.pluginConnectorEnabled = pluginConnectorEnabled;
	}

	public String getConnectorUrl() {
		return connectorUrl;
	}

	public void setConnectorUrl(String connectorUrl) {
		this.connectorUrl = connectorUrl;
	}

	public boolean isPullMetadataEnabled() {
		return pullMetadataEnabled;
	}

	public void setPullMetadataEnabled(boolean pullMetadataEnabled) {
		this.pullMetadataEnabled = pullMetadataEnabled;
	}

	public String getManifestRoot() {
		return manifestRoot;
	}

	public void setManifestRoot(String manifestRoot) {
		this.manifestRoot = manifestRoot;
	}

	private String stripTrailingSeparator(String s) {
		if (s != null && (s.endsWith("\\") || s.endsWith("/"))) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}

	public AutoPublishMode getAutoPublishMode() {
		return autoPublishMode != null ? autoPublishMode : AutoPublishMode.FILECHANGE;
	}

	public void setAutoPublishMode(AutoPublishMode autoPublishMode) {
		this.autoPublishMode = autoPublishMode;
	}
}
