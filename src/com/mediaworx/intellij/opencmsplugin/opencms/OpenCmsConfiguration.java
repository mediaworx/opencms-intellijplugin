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

package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.xmlutils.XmlHelper;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for OpenCms XML configuration files, right now only handles the module configuration file
 * <code>/WEB-INF/config/opencms-modules.xml</code>. Used to extract export points and module resources. Also provides
 * Notifications if the module configuration file is changed. Listener for module configuration changes can register
 * using {@link #registerConfigurationChangeListener(com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsConfiguration.ConfigurationChangeListener)}.
 * Listeners must implement the Interface {@link ConfigurationChangeListener} that is also contained in this class.
 */
public class OpenCmsConfiguration {

	private static final Logger LOG = Logger.getInstance(OpenCmsConfiguration.class);

	/** Relative path to the OpenCms configuration files, static String "/WEB-INF/config/" */
	public static final String CONFIGPATH = "/WEB-INF/config/";

	private static final String MODULECONFIGFILE = "opencms-modules.xml";
	private static final String EXPORTPOINT_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../exportpoints/exportpoint";
	private static final String MODULE_RESOURCE_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../resources/resource";

	private OpenCmsPluginConfigurationData config;
	private XmlHelper xmlHelper;
	private File moduleConfigurationFile;
	private FileAlterationMonitor configurationChangeMonitor;
	private List<ConfigurationChangeListener> configurationChageListeners = new ArrayList<ConfigurationChangeListener>();

	private Document parsedModuleConfigurationFile;

	/**
	 * enumeration for configuration changes, containing only MODULECONFIGURATION right now, may be extended as needed
	 * at a later time
	 */
	public enum  ConfigurationChangeType {
		MODULECONFIGURATION
	}

	/**
	 * Creates a new OpenCmsConfiguration instance, prepares the XML helper for parsing configuration files, starts
	 * a FileAlterationObserver used to handle changes to configuration files.
	 * @param config the project level plugin configuration data
	 */
	public OpenCmsConfiguration(OpenCmsPluginConfigurationData config) {
		this.config = config;

		File configurationFolder = new File(config.getWebappRoot() + CONFIGPATH);
		this.moduleConfigurationFile = new File(config.getWebappRoot() + CONFIGPATH + MODULECONFIGFILE);

		// Create an Observer for configuration changes
		FileAlterationObserver configurationChangeObserver = new FileAlterationObserver(configurationFolder);
		configurationChangeObserver.addListener(new ConfigurationAlterationListener());
		configurationChangeMonitor = new FileAlterationMonitor(5000, configurationChangeObserver);

		try {
			xmlHelper = new XmlHelper();
		}
		catch (Exception e) {
			LOG.warn("Exception during initialization of the module configuration: " + e);
		}
	}


	/** Internal method used to parse the OpenCms configuration (right now only the odule configuration is parsed) */
	private void parseConfiguration() {
		if (config.getWebappRoot() != null) {
			try {
				parsedModuleConfigurationFile = xmlHelper.parseFile(moduleConfigurationFile, null);
			}
			catch (Exception e) {
				LOG.warn("Exception parsing the module configuration ", e);
			}
		}
	}

	/**
	 * Starts the configuration change monitor that is used to observe configuration file changes and to notify
	 * listeners after such changes occurred
	 * @see #stopMonitoringConfigurationChanges()
	 */
	public void startMonitoringConfigurationChanges() {
		try {
			LOG.info("Starting OpenCms configuration change monitor");
			configurationChangeMonitor.start();
		}
		catch (Exception e) {
			LOG.error("There was an error starting the OpenCms configuration change monitor", e);
		}
	}

	/**
	 * Stops the configuration change monitor
	 * @see #startMonitoringConfigurationChanges()
	 */
	public void stopMonitoringConfigurationChanges() {
		try {
			configurationChangeMonitor.stop();
		}
		catch (Exception e) {
			LOG.error("There was an error stopping the OpenCms configuration change monitor");
		}
	}

	/**
	 * Internal method, parses the module configuration XML file and returns the resulting Document
	 * @return the parsed module configuration XML file as a Document
	 */
	private Document getParsedModuleConfigurationFile() {
		parseConfiguration();
		return parsedModuleConfigurationFile;
	}

	/**
	 * Reads the export points from the module configuration
	 * @param moduleName    the module whose export points should be returned
	 * @return  A List of export points for the given module
	 */
	public List<OpenCmsModuleExportPoint> getExportPointsForModule(String moduleName) {
		List<OpenCmsModuleExportPoint> exportPoints = new ArrayList<OpenCmsModuleExportPoint>();
		Document configDocument = getParsedModuleConfigurationFile();
		if (configDocument != null) {
			try {
				NodeList nl = xmlHelper.getNodeListForXPath(configDocument, String.format(EXPORTPOINT_XPATH, moduleName));
				int numExportPoints = nl.getLength();

				for (int i = 0; i < numExportPoints; i++) {
					Node n = nl.item(i);
					NamedNodeMap attr = n.getAttributes();
					String uri = attr.getNamedItem("uri").getNodeValue();
					String destination = attr.getNamedItem("destination").getNodeValue();
					LOG.info("Exportpoint " + (i + 1) + ": uri=" + uri + " - destination=" + destination);
					exportPoints.add(new OpenCmsModuleExportPoint(uri, destination));
				}
			}
			catch (Exception e) {
				LOG.warn("There was an Exception initializing export points for module " + moduleName, e);
			}
		}
		return exportPoints;
	}

	/**
	 * Reads the module resource from the module configuration
	 * @param moduleName    the module whose resources should be returned
	 * @return A List of module resources for the given module
	 */
	public List<String> getModuleResourcesForModule(String moduleName) {
		List<String> moduleResources = new ArrayList<String>();
		Document configDocument = getParsedModuleConfigurationFile();

		if (configDocument != null) {
			try {
				NodeList nl = xmlHelper.getNodeListForXPath(configDocument, String.format(MODULE_RESOURCE_XPATH, moduleName));
				int numResources = nl.getLength();

				for (int i = 0; i < numResources; i++) {
					Node n = nl.item(i);
					NamedNodeMap attr = n.getAttributes();
					String uri = attr.getNamedItem("uri").getNodeValue();
					LOG.info("Module Resource " + (i + 1) + ": uri=" + uri);
					moduleResources.add(uri);
				}
			}
			catch (Exception e) {
				LOG.warn("There was an Exception initializing export points for module " + moduleName, e);
			}
		}
		return moduleResources;
	}


	/**
	 * Adds a configuration change listener. If a configuration change occurs, the listener's
	 * <code>handleOpenCmsConfigurationChange</code> method is called.
	 * @param listener  the listener to add
	 */
	public void registerConfigurationChangeListener(ConfigurationChangeListener listener) {
		configurationChageListeners.add(listener);
	}

	/**
	 * The internal FileAlterationListener handling file changes of OpenCms configuration files
	 */
	private class ConfigurationAlterationListener implements FileAlterationListener {

		/** does nothing */
		@Override
		public void onStart(FileAlterationObserver fileAlterationObserver) {
			// LOG.debug("Checking for OpenCms configuration changes");
		}

		/** does nothing */
		@Override
		public void onDirectoryCreate(File file) {
			// Do nothing
		}

		/** does nothing */
		@Override
		public void onDirectoryChange(File file) {
			// Do nothing
		}

		/** does nothing */
		@Override
		public void onDirectoryDelete(File file) {
			// Do nothing
		}

		/** does nothing */
		@Override
		public void onFileCreate(File file) {
			// Do nothing
		}

		/**
		 * Handles changes to OpenCms configuration files (right now only changes to the module configuration are
		 * handled). Parses the changed configuration file and calls <code>handleOpenCmsConfigurationChange</code> on
		 * all registered listeners.
		 * @param file  the changed file
		 */
		@Override
		public void onFileChange(File file) {
			// ignore changes in the configuration backup directory
			if (file.getPath().contains(File.pathSeparator + "backup")) {
				return;
			}

			// if the module configuration was changed ...
			if (file.getName().equals(MODULECONFIGFILE)) {
				LOG.info("The OpenCms module configuration has been changed, refreshing modules");
				parseConfiguration();

				// notify the listeners that the module configuration was changed
				for (ConfigurationChangeListener listener : configurationChageListeners) {
					listener.handleOpenCmsConfigurationChange(ConfigurationChangeType.MODULECONFIGURATION);
				}
			}
		}

		/** does nothing */
		@Override
		public void onFileDelete(File file) {
			// Do nothing
		}

		/** does nothing */
		@Override
		public void onStop(FileAlterationObserver fileAlterationObserver) {
			// LOG.debug("Checking for OpenCms configuration changes finished");
		}
	}

	/**
	 * Interface to be implemented by classes registering as listener for configuration changes
	 */
	public interface ConfigurationChangeListener {

		/**
		 * Method that is called whenever an OpenCms configuration file is changed (right now only changes to the
		 * module configuration are handled).
		 * @param changeType    the type of the changed OpenCms configuration (right now only MODULECONFIGURATION)
		 */
		public void handleOpenCmsConfigurationChange(ConfigurationChangeType changeType);
	}
}
