package com.mediaworx.opencms.ideconnector;

import org.apache.chemistry.opencmis.commons.impl.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencms.db.CmsPublishList;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.flex.CmsFlexController;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.publish.CmsPublishManager;
import org.opencms.report.CmsLogReport;
import org.opencms.report.I_CmsReport;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class OpenCmsIDEConnector {

	private static final Log LOG = LogFactory.getLog(OpenCmsIDEConnector.class);

	private static final String ACTION_MODULEMANIFESTS = "moduleManifests";
	private static final String ACTION_RESOURCEINFOS = "resourceInfos";
	private static final String ACTION_PUBLISH = "publishResources";

	private ServletRequest request;
	private ServletOutputStream out;
	private CmsObject cmsObject;
	private MetaXmlHelper xmlHelper;

	private JSONParser jsonParser;

	String action;
	String json;

	public OpenCmsIDEConnector(PageContext pageContext) throws IOException {
		request = pageContext.getRequest();
		out = pageContext.getResponse().getOutputStream();

		CmsFlexController flexController = CmsFlexController.getController(pageContext.getRequest());
		cmsObject = flexController.getCmsObject();
		xmlHelper = new MetaXmlHelper(cmsObject);
		jsonParser = new JSONParser();

		action = request.getParameter("action");
		json = request.getParameter("json");

		String user = request.getParameter("user");
		String password = request.getParameter("password");

		login(user, password);
	}

	private void login(String user, String password) {
		boolean isUserLoggedIn = !cmsObject.getRequestContext().getCurrentUser().isGuestUser();

		if (!isUserLoggedIn) {
			try {
				cmsObject.loginUser(user, password);
				CmsProject cmsproject = cmsObject.readProject("Offline");
				cmsObject.getRequestContext().setCurrentProject(cmsproject);
				cmsObject.getRequestContext().setSiteRoot("/");
			}
			catch (CmsException e) {
				LOG.error("the user " + user + " can't be logged in", e);
			}
		}
	}


	public void streamResponse() {
		if (ACTION_MODULEMANIFESTS.equals(action)) {
			streamModuleManifestsOrResourceInfos(true);
		}
		else if (ACTION_RESOURCEINFOS.equals(action)) {
			streamModuleManifestsOrResourceInfos(false);
		}
		else if (ACTION_PUBLISH.equals(action)) {
			publishResources();
		}
	}

	@SuppressWarnings("unchecked")
	private void streamModuleManifestsOrResourceInfos(boolean isModuleManifest) {
		String[] ids = getStringArrayFromJSON(json);
		if (ids == null) {
			return;
		}

		JSONArray out = new JSONArray();
		for (String id : ids) {
			String xml;
			if (isModuleManifest) {
				try {
					xml = xmlHelper.getModuleManifestStub(id);
				}
				catch (IllegalArgumentException e) {
					LOG.error(id + " is not a valid module name");
					continue;
				}
			}
			else {
				if (!cmsObject.existsResource(id)) {
					continue;
				}
				xml = xmlHelper.getResourceInfo(id);
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", id);
			jsonObject.put("xml", xml);
			out.add(jsonObject);
		}
		println(out.toJSONString());
	}

	private void publishResources() {

		LOG.info("IntelliJ triggered publish. Publishing the following resources (if necessary):");

		String[] resourcePaths = getStringArrayFromJSON(json);
		boolean publishSubResources = "true".equals(request.getParameter("publishSubResources"));

		List<CmsResource> publishResources = new ArrayList<CmsResource>(resourcePaths.length);
		boolean hasWarnings = false;
		StringBuilder warnings = new StringBuilder();

		for (String resourcePath : resourcePaths) {
			if (cmsObject.existsResource(resourcePath, CmsResourceFilter.ALL)) {
				CmsResource resource;
				try {
					resource = cmsObject.readResource(resourcePath, CmsResourceFilter.ALL);
				}
				catch (CmsException e) {
					String message = resourcePath + " could not be read from the VFS";
					warnings.append(message).append("\n");
					LOG.warn(message, e);
					hasWarnings = true;
					continue;
				}
				LOG.info("    " + resourcePath);
				publishResources.add(resource);
			}
		}
		if (publishResources.size() > 0) {
			publish: {
				CmsPublishManager publishManager = OpenCms.getPublishManager();
				CmsPublishList publishList;
				try {
					publishList = publishManager.getPublishList(cmsObject, publishResources, false, publishSubResources);
				}
				catch (CmsException e) {
					String message = "Error retrieving CmsPublishList from OpenCms";
					warnings.append(message).append("\n");
					LOG.warn(message, e);
					hasWarnings = true;
					break publish;
				}
				I_CmsReport report = new CmsLogReport(Locale.ENGLISH, OpenCmsIDEConnector.class);
				try {
					List<CmsResource> resources = publishList.getAllResources();
					for (CmsResource resource : resources) {
						if (resource.getState().isDeleted()) {
							LOG.info("DELETED resource " + resource.getRootPath() + " will be published");
						}
						else {
							LOG.info("Resource " + resource.getRootPath() + " will be published");
						}
					}
					publishManager.publishProject(cmsObject, report, publishList);
				}
				catch (CmsException e) {
					String message = "Error publishing the resources";
					warnings.append(message).append("\n");
					LOG.warn(message, e);
					hasWarnings = true;
				}
			}
		}
		if (!hasWarnings) {
			println("OK");
		}
		else {
			println(warnings.toString());
		}
	}

	private String[] getStringArrayFromJSON(String json) {
		JSONArray jsonArray;
		try {
			jsonArray = (JSONArray)jsonParser.parse(json);
		}
		catch (ParseException e) {
			LOG.error("Exception parsing JSON parameters, aborting\nJSON: " + json, e);
			return null;
		}
		catch (ClassCastException e) {
			LOG.error("JSON can be parsed but cast to JSONArray throws Exception, aborting\nJSON: " + json, e);
			return null;
		}

		String[] arr = new String[jsonArray.size()];
		Iterator it = jsonArray.iterator();
		for (int i = 0; it.hasNext(); i++) {
			arr[i] = (String)it.next();
		}
		return arr;
	}

	private void println(String str) {
		try {
			out.println(str);
		}
		catch (IOException e) {
			LOG.error("printing to out is not possible", e);
		}
	}
}
