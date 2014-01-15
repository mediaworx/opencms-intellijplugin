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

/**
 * Class used to create helper objects that simplify parsing and modifying XML files. To retrieve single nodes or
 * multiple nodes from the XML XPath is used, see
 * <a href="http://docs.oracle.com/javase/6/docs/api/javax/xml/xpath/XPath.html">The Java API documentation on XPath</a>
 * for more information.
 */
public class XmlHelper {

	private static final Logger LOG = LoggerFactory.getLogger(XmlHelper.class);

	/** Default encoding that is used if no encoding is given */
	public static final String DEFAULT_ENCODING = "UTF-8";

	/** DocumentBuilder used to parse xml files */
	private DocumentBuilder builder;

	/** factory used to create new XPath objects */
	XPathFactory xPathfactory;


	/**
	 * Creates and initializes a new XmlHelper instance.
	 * @throws ParserConfigurationException if for some reason the DocumentBuilder used to parse the XML can't be
	 *                                      initialized
	 */
	public XmlHelper() throws ParserConfigurationException {
		builder = getNonValidatingDocumentBuilder();
		xPathfactory = XPathFactory.newInstance();
	}

	/**
	 * Creates and returns a document builder that is configured with the following options:
	 * <ul>
	 *     <li>don't validate</li>
	 *     <li>ignore comments</li>
	 *     <li>ignore content whitespace</li>
	 *     <li>convert CDATA nodes to text nodes</li>
	 *     <li>don't perform namespace processing</li>
	 *     <li>ignore DTDs</li>
	 * </ul>
	 * @return the DocumentBuilder
	 * @throws ParserConfigurationException if for some reason the DocumentBuilder used to parse the XML can't be
	 *                                      initialized
	 */
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

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text noes containing whitespace only are removed.
	 * @param path the XML file's path
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(String path) throws IOException, SAXException {
		return parseFile(path, DEFAULT_ENCODING);
	}

	/**
	 * Parses the XML content of the file at the given path using the given encoding. Empty text nodes or
	 * text noes containing whitespace only are removed.
	 * @param path the XML file's path
	 * @param encoding  the encoding to be used to parse the file (must be a valid encoding like "UTF-8")
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(String path, String encoding) throws IOException, SAXException {
		return parseFile(new File(path), encoding);
	}

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text noes containing whitespace only are removed.
	 * @param file the file containing the XML
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(File file) throws IOException, SAXException {
		return parseFile(file, DEFAULT_ENCODING);
	}

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text noes containing whitespace only are removed.
	 * @param file the file containing the XML
	 * @param encoding  the encoding to be used to parse the file (must be a valid encoding like "UTF-8")
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(File file, String encoding) throws IOException, SAXException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(file), encoding);
		BufferedReader reader = new BufferedReader(in);
		Document document = builder.parse(new InputSource(reader));
		cleanEmptyTextNodes(document);
		return document;
	}

	/**
	 * Retrieves the NodeList for the given XPath from the given ancestor Node.
	 * @param ancestorNode the node from which the NodeList is to be read
	 * @param xPath        the XPath (relative to the ancestor node)
	 * @return the NodeList for the given XPath
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist)
	 */
	public NodeList getNodeListForXPath(Node ancestorNode, String xPath) throws XPathExpressionException {
		XPath xpath = xPathfactory.newXPath();
		return (NodeList)xpath.evaluate(xPath, ancestorNode, XPathConstants.NODESET);
	}

	/**
	 * Retrieves a single node at a given XPath from the given ancestor node.
	 * @param ancestorNode the node from which the Node is to be read
	 * @param xPath        the XPath (relative to the ancestor node)
	 * @return the Node for the given XPath
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist or
	 *                                  because it does not point to a single node)
	 */
	public Node getSingleNodeForXPath(Node ancestorNode, String xPath) throws XPathExpressionException {
		XPath xpath = xPathfactory.newXPath();
		return (Node)xpath.evaluate(xPath, ancestorNode, XPathConstants.NODE);
	}

	/**
	 * Retrieves the String content of a node at the given XPath.
	 * @param ancestorNode  the parent node from which the Node content is to be read
	 * @param xPath         the XPath (relative to the ancestor node)
	 * @return the String content of the node at the given XPath
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist or
	 *                                  because it does not point to a single node)
	 */
	public String getStringValueForXpath(Node ancestorNode, String xPath) throws XPathExpressionException {
		return getSingleNodeForXPath(ancestorNode, xPath).getFirstChild().getNodeValue();
	}

	/**
	 * Retrieves the content of the node at the given XPath as int.
	 * @param ancestorNode  the parent node from which the Node content is to be read
	 * @param xPath         the XPath (relative to the ancestor node)
	 * @return the content of the node at the given XPath as int
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist or
	 *                                  because it does not point to a single node)
	 * @throws NumberFormatException    if the content of the node at the XPath can't be converted to int
	 */
	public int getIntValueForXpath(Node ancestorNode, String xPath) throws XPathExpressionException, NumberFormatException {
		return Integer.parseInt(getStringValueForXpath(ancestorNode, xPath));
	}

	/**
	 * Appends a new child node to a parent node.
	 * @param parent    the parent node
	 * @param newChild  the child node to be appended
	 */
	public void appendNode(Node parent, Node newChild) {
		Node toBeImported = newChild instanceof Document ? ((Document) newChild).getDocumentElement() : newChild;
		Node importedNode = parent.getOwnerDocument().importNode(toBeImported, true);
		parent.appendChild(importedNode);
	}

	/**
	 * parses and appends the content of a file as a child node to the given parent node
	 * @param parent            the parent node
	 * @param newChildFilePath  the path to the file whose content is to be added as a child node
	 * @throws IOException      if there's a problem accessing the file
	 * @throws SAXException     if the file can't be parsed
	 */
	public void appendFileAsNode(Node parent, String newChildFilePath) throws IOException, SAXException {
		Document newChild = parseFile(newChildFilePath);
		appendNode(parent, newChild);
	}

	/**
	 * Converts the document to a formatted XML String (indentation level is 4) using default encoding (UTF-8).
	 * @param document      The document to be converted to String
	 * @param cdataElements String array containing the names of all elements that are to be added within CDATA sections
	 * @return  the String representation of the given Document
	 */
	public String getXmlStringFromDocument(Document document, String[] cdataElements) {
		return getXmlStringFromDocument(document, cdataElements, DEFAULT_ENCODING);
	}

	/**
	 * Converts the document to a formatted XML String (indentation level is 4) using the given encoding.
	 * @param document      The document to be converted to String
	 * @param cdataElements String array containing the names of all elements that are to be added within CDATA sections
	 * @param encoding      encoding to be used (added in the XML declaration)
	 * @return  the String representation of the given Document
	 */
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
	 * Removes text nodes that are empty or contain whitespace only if the parent node has at least one child of any
	 * of the following types: ELEMENT, CDATA, COMMENT. This is used to improve the XML format when using a transformer
	 * to do the formatting (whitespace nodes are interfering with indentation and line breaks).
	 * This method was modeled after a method by "user2401669" found on
	 * <a href="http://stackoverflow.com/questions/16641835/strange-xml-indentation">StackOverflow</a>.
	 */
	public static void cleanEmptyTextNodes(Node parentNode) {
		boolean removeEmptyTextNodes = false;

		Node childNode = parentNode.getFirstChild();
		while (childNode != null) {
			short nodeType = childNode.getNodeType();

			if (nodeType == Node.ELEMENT_NODE || nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.COMMENT_NODE) {
				removeEmptyTextNodes = true;
			}
			else {
				continue;
			}
			if (nodeType == Node.ELEMENT_NODE) {
				cleanEmptyTextNodes(childNode); // recurse into subtree
			}
			childNode = childNode.getNextSibling();
		}

		if (removeEmptyTextNodes) {
			removeEmptyTextNodes(parentNode);
		}
	}

	/**
	 * Removes all empty or whitespace only text nodes from the given parent node.
	 * @param parentNode    the parent node to be cleared of empty or whitespace only text nodes
	 */
	private static void removeEmptyTextNodes(Node parentNode) {
		Node childNode = parentNode.getFirstChild();
		while (childNode != null) {
			// grab the "nextSibling" before the child node is removed
			Node nextChild = childNode.getNextSibling();

			short nodeType = childNode.getNodeType();
			if (nodeType == Node.TEXT_NODE) {
				boolean containsOnlyWhitespace = childNode.getNodeValue().trim().isEmpty();
				if (containsOnlyWhitespace) {
					parentNode.removeChild(childNode);
				}
			}
			childNode = nextChild;
		}
	}

}
