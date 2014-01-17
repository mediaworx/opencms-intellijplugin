package com.mediaworx.opencms.ideconnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.opencms.file.*;
import org.opencms.i18n.CmsEncoder;
import org.opencms.importexport.CmsExport;
import org.opencms.importexport.CmsImportExportManager;
import org.opencms.importexport.CmsImportVersion7;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.module.CmsModuleXmlHandler;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsRole;
import org.opencms.util.CmsDateUtil;
import org.opencms.util.CmsUUID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

public class MetaXmlHelper extends CmsExport {

	private static final Log LOG = LogFactory.getLog(MetaXmlHelper.class);
	private static final String NODE_FILE_INFO = "fileinfo";
	private static final String NODE_SIBLING_COUNT = "siblingcount";

	private CmsObject cmsObject;

	public MetaXmlHelper(CmsObject cmsObject) {
		this.cmsObject = cmsObject;
	}

	public Element getExportInfoElement() {
		Element info = DocumentHelper.createElement(CmsImportExportManager.N_INFO);
		info.addElement(CmsImportExportManager.N_CREATOR).addText(OpenCms.getDefaultUsers().getUserAdmin());
		info.addElement(CmsImportExportManager.N_OC_VERSION).addText(OpenCms.getSystemInfo().getVersionNumber());
		info.addElement(CmsImportExportManager.N_DATE).addText(CmsDateUtil.getHeaderDate(System.currentTimeMillis()));
		info.addElement(CmsImportExportManager.N_INFO_PROJECT).addText("Offline");
		info.addElement(CmsImportExportManager.N_VERSION).addText(CmsImportExportManager.EXPORT_VERSION);
		return info;
	}

	public Element getModuleElement(String moduleName) throws IllegalArgumentException {
		CmsModule module = OpenCms.getModuleManager().getModule(moduleName);
		if (module == null) {
			throw new IllegalArgumentException(moduleName + " is not a valid OpenCms module");
		}
		return CmsModuleXmlHandler.generateXml(module);
	}

	public String getModuleManifestStub(String moduleName) throws IllegalArgumentException {
		Element exportElement = DocumentHelper.createElement(CmsImportExportManager.N_EXPORT);
		exportElement.add(getExportInfoElement());
		exportElement.add(getModuleElement(moduleName));
		exportElement.addElement(CmsImportVersion7.N_FILES);
		return getFormattedStringForDocument(DocumentHelper.createDocument(exportElement));
	}

	public Element getFileElement(CmsResource resource) {

		String rootPath = resource.getRootPath();

		try {
			String fileName = trimResourceName(rootPath);

			// it is not allowed to export organizational unit resources
			if (fileName.startsWith("system/orgunits")) {
				return null;
			}

			// define the file node
			Element fileElement = DocumentHelper.createElement(CmsImportVersion7.N_FILE);

			// only write <source> if resource is a file
			if (resource.isFile()) {
				fileElement.addElement(CmsImportVersion7.N_SOURCE).addText(fileName);
			}
			fileElement.addElement(CmsImportVersion7.N_DESTINATION).addText(fileName);
			fileElement.addElement(CmsImportVersion7.N_TYPE).addText(OpenCms.getResourceManager().getResourceType(resource.getTypeId()).getTypeName());
			fileElement.addElement(CmsImportVersion7.N_UUIDSTRUCTURE).addText(resource.getStructureId().toString());
			if (resource.isFile()) {
				fileElement.addElement(CmsImportVersion7.N_UUIDRESOURCE).addText(resource.getResourceId().toString());
			}
			fileElement.addElement(CmsImportVersion7.N_DATELASTMODIFIED).addText(CmsDateUtil.getHeaderDate(resource.getDateLastModified()));
			String userNameLastModified;
			try {
				userNameLastModified = cmsObject.readUser(resource.getUserLastModified()).getName();
			}
			catch (CmsException e) {
				userNameLastModified = OpenCms.getDefaultUsers().getUserAdmin();
			}
			fileElement.addElement(CmsImportVersion7.N_USERLASTMODIFIED).addText(userNameLastModified);
			fileElement.addElement(CmsImportVersion7.N_DATECREATED).addText(CmsDateUtil.getHeaderDate(resource.getDateCreated()));
			String userNameCreated;
			try {
				userNameCreated = cmsObject.readUser(resource.getUserCreated()).getName();
			}
			catch (CmsException e) {
				userNameCreated = OpenCms.getDefaultUsers().getUserAdmin();
			}
			fileElement.addElement(CmsImportVersion7.N_USERCREATED).addText(userNameCreated);
			if (resource.getDateReleased() != CmsResource.DATE_RELEASED_DEFAULT) {
				fileElement.addElement(CmsImportVersion7.N_DATERELEASED).addText(CmsDateUtil.getHeaderDate(resource.getDateReleased()));
			}
			if (resource.getDateExpired() != CmsResource.DATE_EXPIRED_DEFAULT) {
				fileElement.addElement(CmsImportVersion7.N_DATEEXPIRED).addText(CmsDateUtil.getHeaderDate(resource.getDateExpired()));
			}
			int resFlags = resource.getFlags();
			resFlags &= ~CmsResource.FLAG_LABELED;
			fileElement.addElement(CmsImportVersion7.N_FLAGS).addText(Integer.toString(resFlags));

			// properties
			Element propertiesElement = fileElement.addElement(CmsImportVersion7.N_PROPERTIES);
			List<CmsProperty> properties = cmsObject.readPropertyObjects(cmsObject.getSitePath(resource), false);
			Collections.sort(properties);
			for (CmsProperty property : properties) {
				if (property == null) {
					continue;
				}
				addPropertyNode(propertiesElement, property.getName(), property.getStructureValue(), false);
				addPropertyNode(propertiesElement, property.getName(), property.getResourceValue(), true);
			}

			// relations
			List<CmsRelation> relations = cmsObject.getRelationsForResource(resource, CmsRelationFilter.TARGETS.filterNotDefinedInContent());
			Element relationsElement = fileElement.addElement(CmsImportVersion7.N_RELATIONS);

			for (CmsRelation relation : relations) {
				CmsResource target;
				try {
					target = relation.getTarget(cmsObject, CmsResourceFilter.ALL);
				}
				// if the relation's target is not found, LOG it and skip
				catch (CmsVfsResourceNotFoundException e) {
					if (LOG.isWarnEnabled()) {
						LOG.warn("relation target " + relation.getTargetPath() + " not found for " + rootPath, e);
					}
					continue;
				}
				addRelationNode(relationsElement, target.getStructureId().toString(), target.getRootPath(), relation.getType().getName());
			}

			// access control
			Element acl = fileElement.addElement(CmsImportVersion7.N_ACCESSCONTROL_ENTRIES);

			// read the access control entries
			List<CmsAccessControlEntry> fileAcEntries = cmsObject.getAccessControlEntries(rootPath, false);

			// create xml elements for each access control entry
			for (CmsAccessControlEntry ace : fileAcEntries) {
				Element accessentry = acl.addElement(CmsImportVersion7.N_ACCESSCONTROL_ENTRY);

				// now check if the principal is a group or a user
				int flags = ace.getFlags();
				String acePrincipalName;
				CmsUUID acePrincipal = ace.getPrincipal();
				if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_ALLOTHERS) > 0) {
					acePrincipalName = CmsAccessControlEntry.PRINCIPAL_ALL_OTHERS_NAME;
				}
				else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_OVERWRITE_ALL) > 0) {
					acePrincipalName = CmsAccessControlEntry.PRINCIPAL_OVERWRITE_ALL_NAME;
				}
				else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_GROUP) > 0) {
					// the principal is a group
					acePrincipalName = cmsObject.readGroup(acePrincipal).getPrefixedName();
				}
				else if ((flags & CmsAccessControlEntry.ACCESS_FLAGS_USER) > 0) {
					// the principal is a user
					acePrincipalName = cmsObject.readUser(acePrincipal).getPrefixedName();
				}
				else {
					// the principal is a role
					acePrincipalName = CmsRole.PRINCIPAL_ROLE + "." + CmsRole.valueOfId(acePrincipal).getRoleName();
				}

				accessentry.addElement(CmsImportVersion7.N_ACCESSCONTROL_PRINCIPAL).addText(acePrincipalName);
				accessentry.addElement(CmsImportVersion7.N_FLAGS).addText(Integer.toString(flags));

				Element permissionset = accessentry.addElement(CmsImportVersion7.N_ACCESSCONTROL_PERMISSIONSET);
				permissionset.addElement(CmsImportVersion7.N_ACCESSCONTROL_ALLOWEDPERMISSIONS).addText(Integer.toString(ace.getAllowedPermissions()));
				permissionset.addElement(CmsImportVersion7.N_ACCESSCONTROL_DENIEDPERMISSIONS).addText(Integer.toString(ace.getDeniedPermissions()));
			}

			return fileElement;
		}
		catch (CmsException e) {
			LOG.error("There was a CmsException while trying to genereate the XML info for the resource " + rootPath, e);
			return null;
		}
	}


	public String getResourceInfo(String resourcePath) {
		try {
			CmsResource resource = cmsObject.readResource(resourcePath);
			if (!resource.isFolder()) {
				Element resourceInfo = DocumentHelper.createElement(NODE_FILE_INFO);
				resourceInfo.add(getFileElement(resource));
				Element siblingCount = resourceInfo.addElement(NODE_SIBLING_COUNT);
				siblingCount.setText(String.valueOf(resource.getSiblingCount()));
				return getFormattedStringForDocument(DocumentHelper.createDocument(resourceInfo));
			}
			else {
				return getFormattedStringForDocument(DocumentHelper.createDocument(getFileElement(resource)));
			}
		}
		catch (CmsException e) {
			LOG.error("Resource " + resourcePath + " can't be read", e);
			return null;
		}
	}

	public static String getFormattedStringForDocument(Document document) {
		XMLWriter writer = null;
		ByteArrayOutputStream outputStream = null;
		String xmlString = null;
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setIndentSize(4);
		format.setTrimText(false);
		format.setEncoding(CmsEncoder.ENCODING_UTF_8);

		try {
			outputStream = new ByteArrayOutputStream();
			writer = new XMLWriter(outputStream, format);
			writer.write(document.getRootElement());
			writer.flush();
			xmlString = outputStream.toString(CmsEncoder.ENCODING_UTF_8).trim();
		}
		catch (UnsupportedEncodingException e) {
			// This doesn't happen since UTF-8 is known to be supported
			LOG.error("UTF-8 is not supported", e);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				}
				catch (IOException e) {
					// do nothing
				}
			}
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException e) {
					// do nothing
				}
			}
		}
		return xmlString;
	}

}
