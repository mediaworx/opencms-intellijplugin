package com.mediaworx.intellij.opencmsplugin.connector;


import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.entities.OpenCmsModuleResource;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenCmsPluginConnector {

	private static final Logger LOG = Logger.getInstance(OpenCmsPluginConnector.class);

	private static final String ACTION_MODULEMANIFESTS = "moduleManifests";
	private static final String ACTION_RESOURCEINFOS = "resourceInfos";
	private static final String ACTION_PUBLISH = "publishResources";

	private String connectorUrl;
	private String user;
	private String password;
	private CloseableHttpClient httpClient;
	private JSONParser jsonParser;

	private String message;

	public OpenCmsPluginConnector(String connectorUrl, String user, String password) {
		this.connectorUrl = connectorUrl;
		this.user = user;
		this.password = password;

		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		clientBuilder.setUserAgent("IntelliJ OpenCms plugin connector");
		httpClient = clientBuilder.build();

		jsonParser = new JSONParser();
	}

	private void resetClient() {
		// TODO: reset the client or the session or whatever, if necessary
	}

	public void setConnectorUrl(String connectorUrl) {
		if (this.connectorUrl == null || !this.connectorUrl.equals(connectorUrl)) {
			this.connectorUrl = connectorUrl;
		}
	}

	public void setUser(String user) {
		if (this.user == null || !this.user.equals(user)) {
			this.user = user;
			resetClient();
		}
	}

	public void setPassword(String password) {
		if (this.password == null || !this.password.equals(password)) {
			this.password = password;
			resetClient();
		}
	}

	public HashMap<String, String> getModuleResourceInfos(List<OpenCmsModuleResource> moduleResources) throws IOException {
		message = null;
		List<String> resourcePaths = new ArrayList<String>(moduleResources.size());
		for (OpenCmsModuleResource moduleResource : moduleResources) {
			resourcePaths.add(moduleResource.getResourcePath());
		}
		return getActionResponseMap(resourcePaths, ACTION_RESOURCEINFOS);
	}


	public HashMap<String, String> getResourceInfos(List<String> resourcePaths) throws IOException {
		message = null;
		return getActionResponseMap(resourcePaths, ACTION_RESOURCEINFOS);
	}

	public HashMap<String, String> getModuleManifests(List<String> moduleNames) throws IOException {
		message = null;
		return getActionResponseMap(moduleNames, ACTION_MODULEMANIFESTS);
	}

	public boolean publishResources(List<String> resourcePaths, boolean publishSubResources) throws IOException {
		message = null;

		Map<String, String> additionalParams = new HashMap<String, String>();
		additionalParams.put("publishSubResources", String.valueOf(publishSubResources));

		String response = getActionResponseString(resourcePaths, ACTION_PUBLISH, additionalParams);
		if (response == null) {
			return false;
		}
		response = response.trim();
		if (!response.equals("OK")) {
			message = "There were warnings during publish:\n" + response + "\nCheck the OpenCms log file.";
			return false;
		}
		else {
			return true;
		}
	}

	public String getActionResponseString(List<String> identifiers, String action, Map<String, String> additionalParameters) throws IOException {

		HttpPost httpPost = new HttpPost(connectorUrl);

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("user", user));
		postParams.add(new BasicNameValuePair("password", password));
		postParams.add(new BasicNameValuePair("action", action));
		if (additionalParameters != null) {
			for (String key : additionalParameters.keySet()) {
				postParams.add(new BasicNameValuePair(key, additionalParameters.get(key)));
			}
		}
		postParams.add(new BasicNameValuePair("json", getJSONArrayForStringList(identifiers)));

		httpPost.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);

		try {
			LOG.info("Status: " + response.getStatusLine().getStatusCode());
			HttpEntity entity = response.getEntity();
			int status = response.getStatusLine().getStatusCode();
			if (entity != null && status >= 200 && status < 300) {
				return EntityUtils.toString(entity);
			}
			else {
				message = "An invalid http status was returned: " + status;
			}
		}
		finally {
			response.close();
		}

		return null;
	}

	public HashMap<String, String> getActionResponseMap(List<String> identifiers, String action) throws IOException {

		HashMap<String, String> resourceInfos = new HashMap<String, String>();
		String jsonString = getActionResponseString(identifiers, action, null);

		try {
			JSONArray jsonArray = (JSONArray)jsonParser.parse(jsonString);
			for (Object o : jsonArray) {
				JSONObject metaJson = (JSONObject)o;
				String id = (String)metaJson.get("id");
				String xml = (String)metaJson.get("xml");
				resourceInfos.put(id, xml);
			}
		}
		catch (ParseException e) {
			LOG.warn("There was an exception parsing the JSON response for the action " + action, e);
		}
		return resourceInfos;
	}


	@SuppressWarnings("unchecked")
	private String getJSONArrayForStringList(List<String> list) {
		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll(list);
		return jsonArray.toJSONString();
	}

	public String getMessage() {
		return message;
	}
}
