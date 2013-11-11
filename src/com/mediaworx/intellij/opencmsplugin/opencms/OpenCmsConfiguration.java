package com.mediaworx.intellij.opencmsplugin.opencms;

import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OpenCmsConfiguration {

	private static final String CONFIGPATH = File.separator+"WEB-INF"+File.separator+"config"+File.separator;
	private static final String MODULECONFIGFILE = "opencms-modules.xml";
	private static final String EXPORTPOINT_XPATH = "/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../exportpoints/exportpoint";

	private String webappRoot;
	DocumentBuilder builder;
	private XPathFactory xPathfactory;

	private Document parsedModuleConfigurationFile;


	public OpenCmsConfiguration(String webappRoot) {
		this.webappRoot = webappRoot;

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
			System.out.println("Exception during initialization of the module configuration: " + e);
			e.printStackTrace(System.out);
		}

		xPathfactory = XPathFactory.newInstance();
	}

	private void parseConfiguration() {
		try {
			parsedModuleConfigurationFile = builder.parse(webappRoot + CONFIGPATH + MODULECONFIGFILE);
		}
		catch (Exception e) {
			System.out.println("Exception parsing the module configuration: " + e);
			e.printStackTrace(System.out);
		}
	}

	private Document getParsedModuleConfigurationFile() {
		parseConfiguration();
		return parsedModuleConfigurationFile;
	}

	public List<ModuleExportPoint> getExportPointsForModule(String moduleName) {
		List<ModuleExportPoint> exportPoints = new ArrayList<ModuleExportPoint>();
		try {
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile(String.format(EXPORTPOINT_XPATH, moduleName));

			NodeList nl = (NodeList) expr.evaluate(getParsedModuleConfigurationFile(), XPathConstants.NODESET);
			int numExportPoints = nl.getLength();

			for (int i = 0; i < numExportPoints; i++) {
				Node n = nl.item(i);
				NamedNodeMap attr = n.getAttributes();
				String uri = attr.getNamedItem("uri").getNodeValue();
				String destination = attr.getNamedItem("destination").getNodeValue();
				System.out.println("Exportpoint " + (i + 1) + ": uri=" + uri + " - destination=" + destination);
				exportPoints.add(new ModuleExportPoint(uri, destination));
			}
		}
		catch (Exception e) {
			System.out.println("There was an Exception initializing export points for module " + moduleName + " : "+e+"\n"+e.getMessage());
		}
		return exportPoints;
	}

}
