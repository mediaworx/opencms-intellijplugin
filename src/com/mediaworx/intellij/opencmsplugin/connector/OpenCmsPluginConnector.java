/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.connector;


import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.exceptions.OpenCmsConnectorException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModuleResource;
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

/**
 * The connector to OpenCms used for publishing and pulling module and resource meta data. The Plugin Connector is
 * dependent on the OpenCms module "com.mediaworx.opencms.ideconnector" that must be installed on the local OpenCms
 * instance. The module contains a JSP handling requests.<br />
 * <br />
 * The Plugin Connector must be enabled and configured in the project level configuration
 * (see {@link com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData#isPluginConnectorEnabled()}).
 * The Plugin Connector supports three actions:
 * <ul>
 *     <li>moduleManifests: used to pull Module Manifest stubs from OpenCms</li>
 *     <li>resourceInfos: used to pull resource meta data from OpenCms</li>
 *     <li>publishResources: used to start a direct publish session in OpenCms</li>
 * </ul>
 *
 * Communication between the plugin and the connector JSP is done via http by sending JSON requests. Responses from
 * the connector module are in JSON format as well.
 */
public class OpenCmsPluginConnector {

	private static final Logger LOG = Logger.getInstance(OpenCmsPluginConnector.class);

	private static final String ACTION_MODULEMANIFESTS = "moduleManifests";
	private static final String ACTION_RESOURCEINFOS = "resourceInfos";
	private static final String ACTION_PUBLISH = "publishResources";

	private String connectorUrl;
	private String user;
	private String password;
	private boolean useMetaDateVariables;
	private boolean useMetaIdVariables;
	private CloseableHttpClient httpClient;
	private JSONParser jsonParser;

	/**
	 * Creates a new Plugin Connector
	 * @param connectorUrl          the Url under which the connector JSP cam be called
	 * @param user                  OpenCms user to be used for communication with the connector
	 * @param password              The OpenCms user's password
	 * @param useMetaDateVariables  <code>true</code> if date variables should be used in meta data, <code>false</code>
	 *                              otherwise
	 * @param useMetaIdVariables    <code>true</code> if UUID variables should be used in meta data, <code>false</code>
	 *                              otherwise
	 */
	public OpenCmsPluginConnector(String connectorUrl, String user, String password, boolean useMetaDateVariables, boolean useMetaIdVariables) {
		this.connectorUrl = connectorUrl;
		this.user = user;
		this.password = password;
		this.useMetaDateVariables = useMetaDateVariables;
		this.useMetaIdVariables = useMetaIdVariables;
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		clientBuilder.setUserAgent("IntelliJ OpenCms plugin connector");
		httpClient = clientBuilder.build();

		jsonParser = new JSONParser();
	}

	/**
	 * Internal method to reset the HttpClient.
	 */
	private void resetClient() {
		// TODO: reset the client or the session or whatever, if necessary
	}

	public void setConnectorUrl(String connectorUrl) {
		if (this.connectorUrl == null || !this.connectorUrl.equals(connectorUrl)) {
			this.connectorUrl = connectorUrl;
		}
	}

	/**
	 * Sets the OpenCms user to be used for communication with the connector
	 * @param user an OpenCms user
	 */
	public void setUser(String user) {
		if (this.user == null || !this.user.equals(user)) {
			this.user = user;
			resetClient();
		}
	}

	/**
	 * Sets the OpenCms users's password
	 * @param password the OpenCms users's password
	 */
	public void setPassword(String password) {
		if (this.password == null || !this.password.equals(password)) {
			this.password = password;
			resetClient();
		}
	}

	/**
	 * Sets the flag denoting if using placeholders instead of dates in resource meta data is enabled
	 * @param useMetaDateVariables  <code>true</code> if using date placeholders should be enabled, <code>false</code>
	 *                              otherwise
	 */
	public void setUseMetaDateVariables(boolean useMetaDateVariables) {
		this.useMetaDateVariables = useMetaDateVariables;
	}

	/**
	 * Sets the flag denoting if using placeholders instead of UUIDs in resource meta data is enabled
	 * @param useMetaIdVariables  <code>true</code> if using UUID placeholders should be enabled, <code>false</code>
	 *                            otherwise
	 */
	public void setUseMetaIdVariables(boolean useMetaIdVariables) {
		this.useMetaIdVariables = useMetaIdVariables;
	}

	/**
	 * Gets the resource meta data for the given module resources
	 * @param moduleResources   a list of module resources for which meta data is to be retrieved
	 * @return a map containing the resource path as key and the corresponding resource meta data (XML String) as value
	 * @throws IOException if something went wrong with the HttpClient
	 * @throws OpenCmsConnectorException if the connector was not found at the given Url or if the connector returned
	 *                                   an invalid http status
	 */
	public HashMap<String, String> getModuleResourceInfos(List<OpenCmsModuleResource> moduleResources) throws IOException, OpenCmsConnectorException {
		List<String> resourcePaths = new ArrayList<>(moduleResources.size());
		for (OpenCmsModuleResource moduleResource : moduleResources) {
			resourcePaths.add(moduleResource.getResourcePath());
		}
		return getActionResponseMap(resourcePaths, ACTION_RESOURCEINFOS);
	}


	/**
	 * Gets the resource meta data for the module resources at the given paths
	 * @param resourcePaths   a list of module resource paths for which meta data is to be retrieved
	 * @return a map containing the resource path as key and the corresponding resource meta data (XML String) as value
	 * @throws IOException if something went wrong with the HttpClient
	 * @throws OpenCmsConnectorException if the connector was not found at the given Url or if the connector returned
	 *                                   an invalid http status
	 */
	public HashMap<String, String> getResourceInfos(List<String> resourcePaths) throws IOException, OpenCmsConnectorException {
		return getActionResponseMap(resourcePaths, ACTION_RESOURCEINFOS);
	}

	/**
	 * Gets the manifest stub files for the given modules
	 * @param moduleNames List of module names
	 * @return a map containing the module name as key and the corresponding module manifest data (XML String) as value
	 * @throws IOException if something went wrong with the HttpClient
	 * @throws OpenCmsConnectorException if the connector was not found at the given Url or if the connector returned
	 *                                   an invalid http status
	 */
	public HashMap<String, String> getModuleManifests(List<String> moduleNames) throws IOException, OpenCmsConnectorException {
		return getActionResponseMap(moduleNames, ACTION_MODULEMANIFESTS);
	}

	/**
	 * Starts a direct publish session for the given resources
	 * @param resourcePaths List containing the paths of the resources to be published
	 * @param publishSubResources <code>true</code> if sub resources should be published (e.g. a folder and all files
	 *                            and folders contained therein), <code>false</code> if only the resources in the list
	 *                            should be published and sub resources should be left alone.
	 * @throws IOException if something went wrong with the HttpClient
	 * @throws OpenCmsConnectorException if the connector was not found at the given Url or if the connector returned
	 *                                   an invalid http status
	 */
	public void publishResources(List<String> resourcePaths, boolean publishSubResources) throws IOException, OpenCmsConnectorException {
		Map<String, String> additionalParams = new HashMap<String, String>();
		additionalParams.put("publishSubResources", String.valueOf(publishSubResources));

		String response = getActionResponseString(resourcePaths, ACTION_PUBLISH, additionalParams);
		response = response.trim();
		if (!response.equals("OK")) {
			throw new OpenCmsConnectorException("There were warnings during publish:\n" + response + "\nCheck the OpenCms log file.");
		}
	}

	/**
	 * Internal method to get the http response String for a connector action
	 * @param identifiers List of identifiers (e.g. resource paths or module names)
	 * @param action the connector action to be executed
	 * @param additionalParameters Map of key/value parameters to be passed to the connector
	 * @return the http response of the connector JSP (usually JSON, plaintext for publish actions)
	 * @throws IOException if something went wrong with the HttpClient
	 * @throws OpenCmsConnectorException if the connector was not found at the given Url or if the connector returned
	 *                                   an invalid http status
	 */
	private String getActionResponseString(List<String> identifiers, String action, Map<String, String> additionalParameters) throws IOException, OpenCmsConnectorException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("requesting connector response");
			LOG.debug("connectorUrl: " + connectorUrl);
			LOG.debug("user: " + user);
			LOG.debug("password: ********");
			LOG.debug("action: " + action);
		}

		HttpPost httpPost = new HttpPost(connectorUrl);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("user", user));
		postParams.add(new BasicNameValuePair("password", password));
		postParams.add(new BasicNameValuePair("action", action));
		if (additionalParameters != null) {
			for (String key : additionalParameters.keySet()) {
				postParams.add(new BasicNameValuePair(key, additionalParameters.get(key)));
				if (LOG.isDebugEnabled()) {
					LOG.debug(key + ": " + additionalParameters.get(key));
				}
			}
		}
		String json = getJSONArrayForStringList(identifiers);
		postParams.add(new BasicNameValuePair("json", json));
		if (LOG.isDebugEnabled()) {
			LOG.debug("json: " + json);
		}

		httpPost.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

		CloseableHttpResponse response = httpClient.execute(httpPost);

		try {
			LOG.info("Status: " + response.getStatusLine().getStatusCode());
			HttpEntity entity = response.getEntity();
			int status = response.getStatusLine().getStatusCode();
			if (entity != null && status >= 200 && status < 300) {
				return EntityUtils.toString(entity, "UTF-8");
			}
			else if (status == 404) {
				throw new OpenCmsConnectorException("The connector was not found.\nIs the connector module installed in OpenCms?");
			}
			else {
				throw new OpenCmsConnectorException("An invalid http status was returned: " + status);
			}
		}
		finally {
			response.close();
		}
	}

	/**
	 * Internal method to get the http response JSON-String for a connector action and parse it into a Map
	 * (key: identifier, value: meta data in XML format)
	 * @param identifiers List of identifiers (e.g. resource paths or module names)
	 * @param action the connector action to be executed
	 * @return the http response of the connector JSP parsed into a Map (key: identifier, value: meta data in XML format)
	 * @throws IOException
	 * @throws OpenCmsConnectorException
	 */
	private HashMap<String, String> getActionResponseMap(List<String> identifiers, String action) throws IOException, OpenCmsConnectorException {

		Map<String, String> additionalParams = null;

		if ((action.equals(ACTION_RESOURCEINFOS) || action.equals(ACTION_MODULEMANIFESTS)) && (useMetaDateVariables || useMetaIdVariables)) {
			additionalParams = new HashMap<>();
			if (useMetaDateVariables) {
				additionalParams.put("useDateVariables", "true");
			}
			if (useMetaIdVariables) {
				additionalParams.put("useIdVariables", "true");
			}
		}

		HashMap<String, String> resourceInfos = new HashMap<String, String>();
		String jsonString = getActionResponseString(identifiers, action, additionalParams);
		if (jsonString != null) {
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
				LOG.warn("JSON:\n" + jsonString);
			}
		}
		return resourceInfos;
	}


	/**
	 * Internal utility method to convert a List of Strings into a JSON array
	 * @param list The List of Strings to be converted
	 * @return the list of Strings converted into a JSON array
	 */
	@SuppressWarnings("unchecked")
	private String getJSONArrayForStringList(List<String> list) {
		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll(list);
		return jsonArray.toJSONString();
	}
}
