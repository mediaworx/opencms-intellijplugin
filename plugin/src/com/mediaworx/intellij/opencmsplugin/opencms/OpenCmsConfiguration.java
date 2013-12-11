package com.mediaworx.intellij.opencmsplugin.opencms;

import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

public class OpenCmsConfiguration {

	private static final Logger LOG = Logger.getInstance(OpenCmsConfiguration.class);

	private static final String CONFIGPATH = "/WEB-INF/config/";
	private static final String MODULECONFIGFILE = "opencms-modules.xml";
	private static final String EXPORTPOINT_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../exportpoints/exportpoint";
	private static final String MODULE_RESOURCE_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../resources/resource";

	private OpenCmsPluginConfigurationData config;
	DocumentBuilder builder;
	private XPathFactory xPathfactory;

	private Document parsedModuleConfigurationFile;


	public OpenCmsConfiguration(OpenCmsPluginConfigurationData config) {
		this.config = config;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setValidating(false);
		try {
			documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
			documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
			documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			builder = documentBuilderFactory.newDocumentBuilder();
		}
		catch (Exception e) {
			LOG.warn("Exception during initialization of the module configuration: " + e);
		}

		xPathfactory = XPathFactory.newInstance();
	}

	private void parseConfiguration() {
		if (config.getWebappRoot() != null) {
			try {
				parsedModuleConfigurationFile = builder.parse(config.getWebappRoot() + CONFIGPATH + MODULECONFIGFILE);
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
				XPath xpath = xPathfactory.newXPath();
				XPathExpression expr = xpath.compile(String.format(EXPORTPOINT_XPATH, moduleName));

				NodeList nl = (NodeList) expr.evaluate(configDocument, XPathConstants.NODESET);
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
				XPath xpath = xPathfactory.newXPath();
				XPathExpression expr = xpath.compile(String.format(MODULE_RESOURCE_XPATH, moduleName));

				NodeList nl = (NodeList) expr.evaluate(configDocument, XPathConstants.NODESET);
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
