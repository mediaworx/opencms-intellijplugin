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

public class OpenCmsModulePackager {

	private static final String RESOURCE_TYPE_FOLDER = "folder";

	private static final String MANIFEST_FILENAME = "manifest.xml";
	private XmlHelper xmlHelper;
	private Document manifestXml;
	private String manifestString;
	private Zipper zipper;
	private String vfsRootPath;

	public OpenCmsModulePackager() {
	}

	public String packageModule(String manifestRootPath, String vfsRootPath, String moduleZipTargetFolder) throws OpenCmsModulePackagerException {
		return packageModule(manifestRootPath, vfsRootPath, moduleZipTargetFolder, null);
	}

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

	private String ensureZipCompatibleFileSeparator(String path) {
		return !File.separator.equals("/") ? path : path.replaceAll(File.separator, "/");
	}

	private void initializeZipper(String targetFolderPath, String packageName) throws OpenCmsModulePackagerException {
		try {
			zipper = new Zipper(packageName, targetFolderPath);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException("com.mediaworx.ziputils.Zipper could not be initialized, abort", e);
		}
	}

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

	private String readModuleNameFromManifest(Document manifestXml) throws OpenCmsModulePackagerException {
		try {
			return xmlHelper.getStringValueForXpath(manifestXml, "/export/module/name");
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsModulePackagerException("the module name could not be read from the manifest, abort", e);
		}
	}

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

	private String ensureValidModuleVersionString(String version) {
		if (!version.matches("[0-9]+(\\.[0-9]+)?")) {
			version = "1.0";

		}
		return version;
	}

	private void packageZip() throws OpenCmsModulePackagerException {
		// first add the manifest to the zip
		try {
			zipper.addStringAsFile(MANIFEST_FILENAME, manifestString);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException(MANIFEST_FILENAME + " could not be added to the zip file, abort", e);
		}
		handleFiles();

	}

	private void handleFiles() throws OpenCmsModulePackagerException {
		// then find all file nodes in the manifest xml
		NodeList fileNodes = null;
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

	private void addResourceToZip(Node fileNode) throws OpenCmsModulePackagerException {
		String destination = getStringValueForXpath(fileNode, "destination");
		String type = getStringValueForXpath(fileNode, "type");
		if (type.equals(RESOURCE_TYPE_FOLDER)) {
			addDirectoryToZip(ensureZipCompatibleFileSeparator(destination));
		}
		else {
			String source = null;
			try {
				source = getStringValueForXpath(fileNode, "source");
			}
			catch (OpenCmsModulePackagerException e) {
				// do nothing
			}

			if (StringUtils.isNotBlank(source)) {
				addFileToZip(source, destination);
			}
		}
	}

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

	private void addDirectoryToZip(String directoryPath) throws OpenCmsModulePackagerException {
		try {
			zipper.addDirectory(directoryPath);
		}
		catch (IOException e) {
			throw new OpenCmsModulePackagerException("Directory " + directoryPath + " could not be added to module zip, abort", e);
		}
	}

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
