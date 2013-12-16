package com.mediaworx.opencms.moduleutils.manifestgenerator;

import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlFileWriteException;
import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlParseException;
import com.mediaworx.xmlutils.XmlHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

public class OpenCmsModuleManifestGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(OpenCmsModuleManifestGenerator.class);

	private static final String FILENAME_MANIFEST_STUB = "manifest_stub.xml";
	private static final String FILENAME_MANIFEST = "manifest.xml";
	private static final String FOLDER_META_SUFFIX = ".ocmsfolder.xml";
	private static final String FILE_META_SUFFIX = ".ocmsfile.xml";

	private static final String FILES_NODE_XPATH = "/export/files";
	private static final String FILE_NODE_XPATH = "/fileinfo/file";
	private static final String SIBLINGCOUNT_NODE_XPATH = "/fileinfo/siblingcount";
	private static final String RESOURCEID_NODE_XPATH = "/fileinfo/file/uuidresource";
	private static final String SOURCE_NODE_XPATH = "/fileinfo/file/source";

	private static final String[] CDATA_NODES = new String[] { "nicename", "description", "authorname", "authoremail" };

	private HashSet<String> handledSiblingResourceIds;

	private XmlHelper xmlHelper;

	public void generateManifest(File manifestRoot) throws OpenCmsMetaXmlParseException, OpenCmsMetaXmlFileWriteException {

		handledSiblingResourceIds = new HashSet<String>();

		String manifestStubPath = manifestRoot.getPath() + File.separator + FILENAME_MANIFEST_STUB;
		String manifestPath = manifestRoot.getPath() + File.separator + FILENAME_MANIFEST;
		LOG.info("manifestStubPath: {}", manifestStubPath);

		Node filesNode;
		Document manifest;
		try {
			xmlHelper = new XmlHelper();
			manifest = xmlHelper.parseFile(manifestStubPath);
			filesNode = xmlHelper.getSingleNodeForXPath(manifest, FILES_NODE_XPATH);
		}
		catch (ParserConfigurationException e) {
			throw new OpenCmsMetaXmlParseException("The XmlHelper could not be initialized", e);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlParseException("The manifest stub file could not be read", e);
		}
		catch (SAXException e) {
			throw new OpenCmsMetaXmlParseException("The manifest stub xml could not be parsed (parse error)", e);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("The manifest stub xml could not be parsed (xpath error)", e);
		}


		IOFileFilter excludeDirectoryInfoFilter = new RegexFileFilter("^(?:(?!" + Pattern.quote(FOLDER_META_SUFFIX) + "$).)*$");
		Collection<File> files = FileUtils.listFilesAndDirs(manifestRoot, excludeDirectoryInfoFilter, TrueFileFilter.INSTANCE);

		for (File file : files) {
			if (file.isDirectory()) {
				if (file.getPath().equals(manifestRoot.getPath())) {
					continue;
				}
				handleDirectory(filesNode, file);
			}
			else {
				if (file.getPath().equals(manifestPath) || file.getPath().equals(manifestStubPath)) {
					continue;
				}
				handleFile(filesNode, file);
			}
		}

		String manifestString = xmlHelper.getXmlStringFromDocument(manifest, CDATA_NODES);
		try {
			writeManifest(manifestPath, manifestString);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlFileWriteException("manifest.xml could not be written", e);
		}
	}

	public void handleDirectory(Node files, File directory) throws OpenCmsMetaXmlParseException {
		LOG.debug("directory:   {}", directory.getPath());
		String metaXmlFilePath = directory.getPath() + FOLDER_META_SUFFIX;
		LOG.debug("meta folder: {}", metaXmlFilePath);
		try {
			xmlHelper.appendFileAsNode(files, metaXmlFilePath);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlParseException("The file " + metaXmlFilePath + " could not be read", e);
		}
		catch (SAXException e) {
			throw new OpenCmsMetaXmlParseException("The xml from the file " + metaXmlFilePath + " could not be parsed", e);
		}
	}

	public void handleFile(Node filesNode, File metaFile) throws OpenCmsMetaXmlParseException {
		String metaXmlFilePath = metaFile.getPath();
		LOG.debug("meta file:   {}", metaXmlFilePath);

		Document fileMetaInfo = getFileMetaInfoFromXmlFile(metaXmlFilePath);
		Node fileNode = getFileNodeFromMetaInfo(metaXmlFilePath, fileMetaInfo);
		int numSiblings = getNumSiblingsForFile(fileMetaInfo, metaXmlFilePath);

		// sibling handling only has to be done if there are at least two siblings
		if (numSiblings >= 2) {
			String resourceId = getResourceIdForFile(fileMetaInfo, metaXmlFilePath);
			// if we encouter a resourceId that already has been handled ...
			if (handledSiblingResourceIds.contains(resourceId)) {
				// ... the &lt;source&gt; node is removed from the file's xml (that's how OpenCms treats siblings: only
				// the first gets a source node), so the resource is not imported a second time
				removeSourceNodeFromFile(fileNode, metaXmlFilePath);
			}
			else {
				handledSiblingResourceIds.add(resourceId);
			}
		}

		xmlHelper.appendNode(filesNode, fileNode);
	}

	private void writeManifest(String manifestPath, String manifestString) throws IOException {
		FileUtils.writeStringToFile(new File(manifestPath), manifestString);
	}

	private Document getFileMetaInfoFromXmlFile(String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		Document fileMetaInfo;

		try {
			fileMetaInfo = xmlHelper.parseFile(metaXmlFilePath);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlParseException("The file " + metaXmlFilePath + " could not be read", e);
		}
		catch (SAXException e) {
			throw new OpenCmsMetaXmlParseException("The xml from the file " + metaXmlFilePath + " could not be parsed (parse error)", e);
		}
		return fileMetaInfo;
	}

	private Node getFileNodeFromMetaInfo(String metaXmlFilePath, Document fileMetaInfo) throws OpenCmsMetaXmlParseException {
		Node fileNode;

		try {
			fileNode = xmlHelper.getSingleNodeForXPath(fileMetaInfo, FILE_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("The xml from the file " + metaXmlFilePath + " could not be parsed (xpath error)", e);
		}
		return fileNode;
	}

	private int getNumSiblingsForFile(Document metaInfo, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		int numSiblings;
		try {
			numSiblings = xmlHelper.getIntValueForXpath(metaInfo, SIBLINGCOUNT_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("Can't determine sibling count from " + metaXmlFilePath + " (xpath error)", e);
		}
		catch (NumberFormatException e) {
			throw new OpenCmsMetaXmlParseException("Can't determine sibling count from " + metaXmlFilePath + " (not a number)", e);
		}
		return numSiblings;
	}

	private String getResourceIdForFile(Document metaInfo, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		String resourceId;
		try {
			resourceId = xmlHelper.getStringValueForXpath(metaInfo, RESOURCEID_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("Can't determine resource id from " + metaXmlFilePath + " (xpath error)", e);
		}
		return resourceId;
	}

	private void removeSourceNodeFromFile(Node fileNode, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		Node sourceNode;
		try {
			sourceNode = xmlHelper.getSingleNodeForXPath(fileNode, SOURCE_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("Can't remove sibling's source node from " + metaXmlFilePath + " (xpath error)", e);
		}
		sourceNode.getParentNode().removeChild(sourceNode);
	}

	public static String getMetaInfoPath(String manifestRoot, String vfsPath, boolean isDirectory) {
		return manifestRoot + vfsPath + getMetaInfoSuffix(isDirectory);
	}

	private static String getMetaInfoSuffix(boolean isDirectory) {
		return isDirectory ? FOLDER_META_SUFFIX : FILE_META_SUFFIX;
	}
}
