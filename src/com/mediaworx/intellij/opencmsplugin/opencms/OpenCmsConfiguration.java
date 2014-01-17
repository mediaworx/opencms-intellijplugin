package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.xmlutils.XmlHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class OpenCmsConfiguration {

	private static final Logger LOG = Logger.getInstance(OpenCmsConfiguration.class);

	private static final String CONFIGPATH = "/WEB-INF/config/";
	private static final String MODULECONFIGFILE = "opencms-modules.xml";
	private static final String EXPORTPOINT_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../exportpoints/exportpoint";
	private static final String MODULE_RESOURCE_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../resources/resource";

	private OpenCmsPluginConfigurationData config;
	private XmlHelper xmlHelper;

	private Document parsedModuleConfigurationFile;


	public OpenCmsConfiguration(OpenCmsPluginConfigurationData config) {
		this.config = config;

		try {
			xmlHelper = new XmlHelper();
		}
		catch (Exception e) {
			LOG.warn("Exception during initialization of the module configuration: " + e);
		}
	}

	private void parseConfiguration() {
		if (config.getWebappRoot() != null) {
			try {
				parsedModuleConfigurationFile = xmlHelper.parseFile(config.getWebappRoot() + CONFIGPATH + MODULECONFIGFILE);
			}
			catch (Exception e) {
				LOG.warn("Exception parsing the module configuration ", e);
			}
		}
	}

	private Document getParsedModuleConfigurationFile() {
		parseConfiguration();
		return parsedModuleConfigurationFile;
	}

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

	public List<String> getModuleResourcesForModule(String moduleName) {
		List<String> moduleResources = new ArrayList<String>();
		Document configDocument = getParsedModuleConfigurationFile();

		if (configDocument != null) {
			try {
				NodeList nl = xmlHelper.getNodeListForXPath(configDocument, String.format(MODULE_RESOURCE_XPATH, moduleName));
				int numExportPoints = nl.getLength();

				for (int i = 0; i < numExportPoints; i++) {
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

}
