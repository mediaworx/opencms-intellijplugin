package com.mediaworx.opencms.moduleutils.packager;

import com.mediaworx.opencms.moduleutils.packager.exceptions.OpenCmsModulePackagerException;
import com.mediaworx.xmlutils.XmlHelper;
import com.mediaworx.ziputils.Zipper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

/**
 * Creates the zip file for OpenCms modules
 */
public class OpenCmsModulePackager {

	/** OpenCms resource type for folders */
	private static final String RESOURCE_TYPE_FOLDER = "folder";

	/** file name of the module manifest xml file */
	private static final String MANIFEST_FILENAME = "manifest.xml";

	/** xml helper used to parse the manifest xml file */
	private XmlHelper xmlHelper;

	/** xml contained in the manifest file as Document */
	private Document manifestXml;

	/** String contained in the manifest file */
	private String manifestString;

	/** zipper used to package the module zip */
	private Zipper zipper;

	/** root path to the VFS files / folders */
	private String vfsRootPath;

	/**
	 * Creates the zip file for the module described in the manifest file at the given <code>manifestRootPath</code>.
	 * The VFS files are retrieved from the given <code>vfsRootPath</code>. The resulting zip file is placed at the
	 * given <code>moduleZipTargetFolder</code>.
	 * @param manifestRootPath      path containing the module's manifest xml file (parent folder without the manifest's
	 *                              file name)
	 * @param vfsRootPath           root path under which the VFS files to be packaged are stored
	 * @param moduleZipTargetFolder target folder where the module zip is to be placed
	 * @return  the file name of the created module zip (containing the version), e.g.
	 *          com.mycompany.mypackege.mymodule_1.0.zip
	 * @throws  OpenCmsModulePackagerException  thrown in case of any error (e.g. xml parse exception, file
	 *                                          write exception). The exception's message contains detailed information
	 *                                          about what went wrong.
	 */
	public String packageModule(String manifestRootPath, String vfsRootPath, String moduleZipTargetFolder) throws OpenCmsModulePackagerException {
		return packageModule(manifestRootPath, vfsRootPath, moduleZipTargetFolder, null);
	}

	/**
	 * Creates the zip file for the module described in the manifest file at the given <code>manifestRootPath</code>.
	 * The VFS files are retrieved from the given <code>vfsRootPath</code>. The resulting zip file is placed at the
	 * given <code>moduleZipTargetFolder</code>.
	 * @param manifestRootPath      path containing the module's manifest xml file (parent folder without the manifest's
	 *                              file name)
	 * @param vfsRootPath           root path under which the VFS files to be packaged are stored
	 * @param moduleZipTargetFolder target folder where the module zip is to be placed
	 * @param moduleVersion         module version to be used (overrides the module version in the manifest file), may
	 *                              be <code>null</code> (then the module version from the manifest file is used). If
	 *                              provided, the syntax must match <code>[0-9]+(\.[0-9]+)?</code> (e.g. "1" or "1.2").
	 *                              Any String not matching this syntax will be replaced by "1.0".
	 * @return  the file name of the created module zip (containing the version), e.g.
	 *          com.mycompany.mypackege.mymodule_1.4.zip
	 * @throws  OpenCmsModulePackagerException  thrown in case of any error (e.g. xml parse exception, file
	 *                                          write exception). The exception's message contains detailed information
	 *                                          about what went wrong.
	 */
	public String packageModule(String manifestRootPath, String vfsRootPath, String moduleZipTargetFolder, String moduleVersion) throws OpenCmsModulePackagerException {

		try {
			xmlHelper = new XmlHelper();
		}
		catch (ParserConfigurationException e) {
			throw new OpenCmsModulePackagerException("The XML helper could not be initialized", e);
		}

		String manifestFilePath = manifestRootPath + "/" + MANIFEST_FILENAME;
		File manifestFile = new File(manifestFilePath);
		if (!manifestFile.exists()) {
			throw new OpenCmsModulePackagerException(MANIFEST_FILENAME + " not found in " + manifestRootPath + ", abort");
		}

		manifestXml = getManifestDocument(manifestFile);

		String moduleName = readModuleNameFromManifest(manifestXml);

		manifestString = getManifestString(manifestFile);

		// if the module version is not set, get it from the manifest
		if (moduleVersion == null) {
			moduleVersion = readModuleVersionFromManifest(manifestXml);
		}
		// if the module version is set, swap the existing module version in the manifest string
		else {
			moduleVersion = ensureValidModuleVersionString(moduleVersion);
			manifestString = manifestString.replaceFirst("<version>[^<]*</version>", "<version>" + moduleVersion + "</version>");
		}

		String packageName = moduleName + "_" + moduleVersion + ".zip";

		this.vfsRootPath = ensureZipCompatibleFileSeparator(vfsRootPath);
		if (!this.vfsRootPath.endsWith("/")) {
			this.vfsRootPath = this.vfsRootPath.concat("/");
		}

		try {
			initializeZipper(moduleZipTargetFolder, packageName);
			packageZip();
		}
		finally {
			if (zipper != null) {
				zipper.finish();
			}
		}
		return packageName;
	}

	/**
	 * Converts all file separators to "/" (as needed for zip paths).
	 * @param path  the original path
	 * @return  the original path with all file separators replaced by "/"
	 */
	private String ensureZipCompatibleFileSeparator(String path) {
		return !File.separator.equals("/") ? path : path.replaceAll(File.separator, "/");
	}

	/**
	 * Initializes the Zipper-Tool.
	 * @param targetFolderPath  path to the folder under which the zip file is to be stored
	 * @param packageName       name of the zip file
	 * @throws OpenCmsModulePackagerException if the Zipper an not be initialized
	 */
	private void initializeZipper(String targetFolderPath, String packageName) throws OpenCmsModulePackagerException {
		try {
			zipper = new Zipper(packageName, targetFolderPath);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException("com.mediaworx.ziputils.Zipper could not be initialized, abort", e);
		}
	}

	/**
	 * Retrieves the XML Document from the content of the manifest file.
	 * @param manifestFile  the module manifest file
	 * @return  the manifest file's xml as a Document
	 * @throws OpenCmsModulePackagerException if the module manifest file can not be read or parsed
	 */
	private Document getManifestDocument(File manifestFile) throws OpenCmsModulePackagerException {
		Document manifestXml;
		try {
			manifestXml = xmlHelper.parseFile(manifestFile);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException(manifestFile.getPath() + " file could not be read, abort", e);
		}
		catch (SAXException e) {
			throw new OpenCmsModulePackagerException(manifestFile.getPath() + " file could not be parsed, abort", e);
		}
		return manifestXml;
	}

	/**
	 * Reads the module's name from the manifest xml.
	 * @param manifestXml   the manifest file's xml as a Document
	 * @return  the module name
	 * @throws OpenCmsModulePackagerException
	 */
	private String readModuleNameFromManifest(Document manifestXml) throws OpenCmsModulePackagerException {
		try {
			return xmlHelper.getStringValueForXpath(manifestXml, "/export/module/name");
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsModulePackagerException("the module name could not be read from the manifest, abort", e);
		}
	}

	/**
	 * Reads the String content of the manifest file.
	 * @param manifestFile  the manifest file
	 * @return  the content of the manifest file as String
	 * @throws OpenCmsModulePackagerException if the manifest file can not be read
	 */
	private String getManifestString(File manifestFile) throws OpenCmsModulePackagerException {
		String manifestString;
		try {
			manifestString = FileUtils.readFileToString(manifestFile);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException(manifestFile.getPath() + " file could not be read, abort", e);
		}
		return manifestString;
	}

	/**
	 * Reads the module's version from the manifest xml.
	 * @param manifestXml   the manifest file's xml as a Document
	 * @return  the module version
	 * @throws OpenCmsModulePackagerException
	 */
	private String readModuleVersionFromManifest(Document manifestXml) throws OpenCmsModulePackagerException {
		try {
			String version =  xmlHelper.getStringValueForXpath(manifestXml, "/export/module/version");
			version = ensureValidModuleVersionString(version);
			return version;
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsModulePackagerException("the module version could not be read from the manifest, abort", e);
		}
	}

	/**
	 * ensures that the given version String is valid
	 * @param version   the version String to check
	 * @return  the original version String or "1.0" if it was invalid (did not match the regex [0-9]+(\.[0-9]+)?)
	 */
	private String ensureValidModuleVersionString(String version) {
		if (!version.matches("[0-9]+(\\.[0-9]+)?")) {
			version = "1.0";

		}
		return version;
	}

	/**
	 * Creates the zip file, adds the manifest and all VFS resources.
	 * @throws OpenCmsModulePackagerException
	 */
	private void packageZip() throws OpenCmsModulePackagerException {
		// add the manifest to the zip
		try {
			zipper.addStringAsFile(MANIFEST_FILENAME, manifestString);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException(MANIFEST_FILENAME + " could not be added to the zip file, abort", e);
		}
		addVfsResourcesToZip();
	}

	/**
	 * Adds all VFS resources (files and folders) to the zip file.
	 * @throws OpenCmsModulePackagerException if the file nodes can not be read from the XML
	 */
	private void addVfsResourcesToZip() throws OpenCmsModulePackagerException {
		// find all file nodes in the manifest xml
		NodeList fileNodes;
		try {
			fileNodes = xmlHelper.getNodeListForXPath(manifestXml, "/export/files/file");
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsModulePackagerException("could not read the file nodes from the manifest, abort", e);
		}

		int numFileNodes = fileNodes.getLength();

		for (int i = 0; i < numFileNodes; i++) {
			Node fileNode = fileNodes.item(i);
			addResourceToZip(fileNode);
		}
	}

	/**
	 * adds the resource (file or folder) for the given file node to the module zip
	 * @param fileNode  the file describing the resource to be added to the zip file
	 * @throws OpenCmsModulePackagerException
	 */
	private void addResourceToZip(Node fileNode) throws OpenCmsModulePackagerException {
		String destination = getStringValueForXpath(fileNode, "destination");
		String type = getStringValueForXpath(fileNode, "type");
		if (type.equals(RESOURCE_TYPE_FOLDER)) {
			addFolderToZip(ensureZipCompatibleFileSeparator(destination));
		}
		else {
			String source = null;
			try {
				source = getStringValueForXpath(fileNode, "source");
			}
			catch (OpenCmsModulePackagerException e) {
				// do nothing; if there is no source node, source stays null and the following if will be false
			}

			if (StringUtils.isNotBlank(source)) {
				addFileToZip(source, destination);
			}
		}
	}

	/**
	 * Reads the String value from the sub node at the given XPath.
	 * @param fileNode  the file node containing the requested sub node
	 * @param xpath xpath for the sub node
	 * @return  the String value from the sub node at the given XPath
	 * @throws OpenCmsModulePackagerException if the xpath can not be read from the file node
	 */
	private String getStringValueForXpath(Node fileNode, String xpath) throws OpenCmsModulePackagerException {
		String value;
		try {
			value = xmlHelper.getStringValueForXpath(fileNode, xpath);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsModulePackagerException("xpath " + xpath + " could not be read from file node, abort", e);
		}
		catch (NullPointerException e) {
			throw new OpenCmsModulePackagerException("xpath " + xpath + " could not be read from file node, abort", e);
		}
		return value;
	}

	/**
	 * Adds the VFS folder at the given <code>folderPath</code> to the zip file.
	 * @param folderPath path of the folder to be added (relative to the zip root)
	 * @throws OpenCmsModulePackagerException in case the directory can not be added to the zip file
	 */
	private void addFolderToZip(String folderPath) throws OpenCmsModulePackagerException {
		try {
			zipper.addDirectory(folderPath);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException("Directory " + folderPath + " could not be added to module zip, abort", e);
		}
	}

	/**
	 * Adds a VFS from the given source path to the zip file (at the given destination). Source and destination are
	 * taken from the manifest file and are usually equal.
	 * @param source        source VFS path
	 * @param destination   target VFS path
	 * @throws OpenCmsModulePackagerException in case the source file is not found or the file can not be added to the zip
	 */
	private void addFileToZip(String source, String destination) throws OpenCmsModulePackagerException {
		String sourceFilePath = vfsRootPath + source;
		File sourceFile = new File(sourceFilePath);
		if (!sourceFile.exists()) {
			throw new OpenCmsModulePackagerException("the source file " + sourceFilePath + " does not exist");
		}
		try {
			zipper.addFile(destination, sourceFile);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException("could not add file " + sourceFilePath + " to module zip", e);
		}
	}

}
