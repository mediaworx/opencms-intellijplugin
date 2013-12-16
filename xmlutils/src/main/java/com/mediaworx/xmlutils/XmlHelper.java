package com.mediaworx.xmlutils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class XmlHelper {

	private static final Logger LOG = LoggerFactory.getLogger(XmlHelper.class);

	public static final String DEFAULT_ENCODING = "UTF-8";

	private DocumentBuilder builder;
	private final XPath xpath;


	public XmlHelper() throws ParserConfigurationException {
		builder = getNonValidatingDocumentBuilder();
		XPathFactory xPathfactory = XPathFactory.newInstance();
		xpath = xPathfactory.newXPath();
	}

	private DocumentBuilder getNonValidatingDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setValidating(false);
		documentBuilderFactory.setIgnoringComments(true);
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilderFactory.setCoalescing(true);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return documentBuilderFactory.newDocumentBuilder();
	}

	public Document parseFile(String path) throws IOException, SAXException {
		return parseFile(path, DEFAULT_ENCODING);
	}

	public Document parseFile(String path, String encoding) throws IOException, SAXException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(path), encoding);
		BufferedReader reader = new BufferedReader(in);
		Document document = builder.parse(new InputSource(reader));
		cleanEmptyTextNodes(document);
		return document;
	}

	public NodeList getNodeListForXPath(Node ancestorNode, String xPath) throws XPathExpressionException {
		return (NodeList)xpath.evaluate(xPath, ancestorNode, XPathConstants.NODESET);
	}

	public Node getSingleNodeForXPath(Node ancestorNode, String xPath) throws XPathExpressionException {
		return (Node)xpath.evaluate(xPath, ancestorNode, XPathConstants.NODE);
	}

	public String getStringValueForXpath(Node ancestorNode, String xPath) throws XPathExpressionException {
		return getSingleNodeForXPath(ancestorNode, xPath).getFirstChild().getNodeValue();
	}

	public int getIntValueForXpath(Node ancestorNode, String xPath) throws XPathExpressionException, NumberFormatException {
		return Integer.parseInt(getStringValueForXpath(ancestorNode, xPath));
	}

	public void appendNode(Node parent, Node newChild) {
		Node toBeImported = newChild instanceof Document ? ((Document) newChild).getDocumentElement() : newChild;
		Node importedNode = parent.getOwnerDocument().importNode(toBeImported, true);
		parent.appendChild(importedNode);
	}

	public void appendFileAsNode(Node parent, String newChildFilePath) throws IOException, SAXException {
		Document newChild = parseFile(newChildFilePath);
		appendNode(parent, newChild);
	}

	public String getXmlStringFromDocument(Document document, String[] cdataElements) {
		return getXmlStringFromDocument(document, cdataElements, DEFAULT_ENCODING);
	}

	public String getXmlStringFromDocument(Document document, String[] cdataElements, String encoding) {
		cleanEmptyTextNodes(document);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		}
		catch (TransformerConfigurationException e) {
			LOG.error("Exception configuring the XML transformer", e);
			return "";
		}
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		if (cdataElements != null && cdataElements.length > 0) {
			String cdataElementsJoined = StringUtils.join(cdataElements, ' ');
			transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, cdataElementsJoined);
		}
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		StringWriter writer = new StringWriter();
		try {
			transformer.transform(new DOMSource(document), new StreamResult(writer));
		}
		catch (TransformerException e) {
			LOG.error("Exception transforming the XML document to String", e);
		}
		finally {
			try {
				writer.close();
			}
			catch (IOException e) {
				// it seems the writer was closed already
				LOG.warn("Exception closing the writer", e);
			}
		}
		return writer.getBuffer().toString();
	}

	/**
	 * Removes text nodes that only contains whitespace. The conditions for
	 * removing text nodes, besides only containing whitespace, are: If the
	 * parent node has at least one child of any of the following types, all
	 * whitespace-only text-node children will be removed: - ELEMENT child -
	 * CDATA child - COMMENT child
	 *
	 * The purpose of this is to make the format() method (that use a
	 * Transformer for formatting) more consistent regarding indenting and line
	 * breaks.
	 */
	private static void cleanEmptyTextNodes(Node parentNode) {
		boolean removeEmptyTextNodes = false;
		Node childNode = parentNode.getFirstChild();
		while (childNode != null) {
			removeEmptyTextNodes |= checkNodeTypes(childNode);
			childNode = childNode.getNextSibling();
		}

		if (removeEmptyTextNodes) {
			removeEmptyTextNodes(parentNode);
		}
	}

	private static void removeEmptyTextNodes(Node parentNode) {
		Node childNode = parentNode.getFirstChild();
		while (childNode != null) {
			// grab the "nextSibling" before the child node is removed
			Node nextChild = childNode.getNextSibling();

			short nodeType = childNode.getNodeType();
			if (nodeType == Node.TEXT_NODE) {
				boolean containsOnlyWhitespace = childNode.getNodeValue()
						.trim().isEmpty();
				if (containsOnlyWhitespace) {
					parentNode.removeChild(childNode);
				}
			}
			childNode = nextChild;
		}
	}

	private static boolean checkNodeTypes(Node childNode) {
		short nodeType = childNode.getNodeType();

		if (nodeType == Node.ELEMENT_NODE) {
			cleanEmptyTextNodes(childNode); // recurse into subtree
		}

		if (nodeType == Node.ELEMENT_NODE
				|| nodeType == Node.CDATA_SECTION_NODE
				|| nodeType == Node.COMMENT_NODE) {
			return true;
		}
		else {
			return false;
		}
	}

}
