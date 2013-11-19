package com.mediaworx.intellij.opencmsplugin.connector;


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

public class OpenCmsPluginConnector {

	private static final String ACTION_MODULEMANIFESTS = "moduleManifests";
	private static final String ACTION_RESOURCEINFOS = "resourceInfos";

	private String connectorUrl;
	private String user;
	private String password;
	private CloseableHttpClient httpClient;
	JSONParser jsonParser;

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

	public HashMap<String, String> getResourceInfos(List<String> resourcePaths) throws IOException {
		return getActionResponseMap(resourcePaths, ACTION_RESOURCEINFOS);
	}

	public HashMap<String, String> getModuleManifests(List<String> moduleNames) throws IOException {
		return getActionResponseMap(moduleNames, ACTION_MODULEMANIFESTS);
	}


	public HashMap<String, String> getActionResponseMap(List<String> identifiers, String action) throws IOException {

		HashMap<String, String> resourceInfos = new HashMap<String, String>();

		HttpPost httpPost = new HttpPost(connectorUrl);

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("user", user));
		postParams.add(new BasicNameValuePair("password", password));
		postParams.add(new BasicNameValuePair("action", action));
		postParams.add(new BasicNameValuePair("params", getJSONArrayForStringList(identifiers)));

		httpPost.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);

		try {
			System.out.println(response.getStatusLine());
			HttpEntity entity = response.getEntity();
			int status = response.getStatusLine().getStatusCode();
			if (entity != null && status >= 200 && status < 300) {
				String jsonString = EntityUtils.toString(entity);

				JSONArray jsonArray = (JSONArray)jsonParser.parse(jsonString);
				for (Object o : jsonArray) {
					JSONObject metaJson = (JSONObject)o;
					String id = (String)metaJson.get("id");
					String xml = (String)metaJson.get("xml");
					resourceInfos.put(id, xml);
				}
			}
		}
		catch (ParseException e) {
			System.out.println("There was an exception parsing the JSON response for the action " + action);
			e.printStackTrace(System.out);
		}
		finally {
			response.close();
		}

		return resourceInfos;
	}


	@SuppressWarnings("unchecked")
	private String getJSONArrayForStringList(List<String> list) {
		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll(list);
		return jsonArray.toJSONString();
	}

}
