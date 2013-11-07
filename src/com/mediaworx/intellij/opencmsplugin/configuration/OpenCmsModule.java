package com.mediaworx.intellij.opencmsplugin.configuration;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OpenCmsModule {

	private String moduleConfigPath;
	private String name;

	private String localVfsRoot;

	private List<ModuleExportPoint> exportPoints = new ArrayList<ModuleExportPoint>();

	private XPathFactory xPathfactory;


	public OpenCmsModule(String name) {
		this.name = name;
	}

	public String getLocalVfsRoot() {
		return localVfsRoot;
	}

	public void setLocalVfsRoot(String localVfsRoot) {
		this.localVfsRoot = localVfsRoot;
	}

	public void addExportPoint(String vfsSource, String rfsTarget) {
		exportPoints.add(new ModuleExportPoint(vfsSource, rfsTarget));
	}

	public List<ModuleExportPoint> getExportPoints() {
		return exportPoints;
	}

	public void initModuleConfig(Document moduleConfiguration) {
		xPathfactory = XPathFactory.newInstance();

		initExportPoints(moduleConfiguration);
		// TODO: resource-Paths verarbeiten
	}

	private void initExportPoints(Document moduleConfiguration) {

         System.out.println("Initializing export points for module "+name);

         try {
             XPath xpath = xPathfactory.newXPath();
             XPathExpression expr = xpath.compile(String.format("/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../exportpoints/exportpoint", name));

             NodeList nl = (NodeList) expr.evaluate(moduleConfiguration, XPathConstants.NODESET);
             int numExportPoints = nl.getLength();

             for (int i = 0; i < numExportPoints; i++) {
                 Node n = nl.item(i);
                 NamedNodeMap attr = n.getAttributes();
                 String uri = attr.getNamedItem("uri").getNodeValue();
                 String destination = attr.getNamedItem("destination").getNodeValue();
                 System.out.println("Exportpoint "+(i+1)+": uri="+uri+" - destination="+destination);
                 addExportPoint(uri, destination);
             }
         }
         catch (Exception e) {
             System.out.println("There was an Exception initializing export points for module " + name + " : "+e+"\n"+e.getMessage());
         }
    }
}
