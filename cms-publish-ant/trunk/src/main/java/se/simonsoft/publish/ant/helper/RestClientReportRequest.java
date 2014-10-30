/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.publish.ant.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.restclient.HttpStatusError;
import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestAuthentication;
import se.repos.restclient.RestGetClient;
import se.repos.restclient.RestResponse;
import se.repos.restclient.RestResponseAccept;
import se.repos.restclient.auth.RestAuthenticationSimple;
import se.repos.restclient.base.Codecs;
import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.reporting.client.CmsItemSearchREST;
import se.simonsoft.cms.reporting.repositem.CmsItemSearchRepositem;
import se.simonsoft.cms.reporting.rest.itemlist.CmsItemListJSON;
import se.simonsoft.cms.reporting.rest.itemlist.CmsItemListJSONSimple;

/**
 * A restclient request class, creates the request, sends and retrieve it. Might
 * benefit from implementing an interface
 * 
 * @author joakimdurehed
 *
 */
public class RestClientReportRequest {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private HashMap<String, String> configs; 
	private HashMap<String, String> params;
	private HashMap<String, String> requestHeaders;
	private RestClientJavaNet httpClient;
	public CmsItemList itemList;
	private static final String CONFIGMAP = "configs";
	private static final String PARAMMAP = "params";
	
	/**
	 * Reportversion enumeration (possible not to be used)
	 *
	 */
	public enum Reportversion {
		v094, v1, v32
	}

	/**
	 * Map of parameters
	 * @return map
	 */
	public Map<String, String> getParams() {
		return this.params; // TODO: return copy
	}
	
	/**
	 * Map of configurations
	 * @return map
	 */
	public Map<String, String> getConfigs() {
		return this.configs; // TODO: return copy
	}

	/**
	 * Adds config (key) with value.
	 * Use for username, password, baseurl and such
	 * 
	 * @param key
	 * @param value
	 */
	public void addConfig(String key, String value) {

		if (this.configs == null) {
			this.configs = new HashMap<String, String>();
		}
		logger.debug("Adding key {} with value {}", key, value);
		this.configs.put(key, value);
	}

	/**
	 * Adds param (key) with value
	 * Use query, field list and such
	 * 
	 * @param key
	 * @param value
	 */
	public void addParam(String key, String value) {

		if (this.params == null) {
			logger.debug("init params map");
			this.params = new HashMap<String, String>();
		}
		logger.debug("Adding key {} with value {}", key, urlencode(value));
		this.params.put(key, value);
	}

	// http://appdev1.pdsvision.net/cms/rest/report3/items?q=prop_cms.status:In_Translation&repo=demo1
	
	/**
	 * Initializes a RestClientJavaNet 
	 * @return
	 */
	private boolean initRestGetClient() {
		logger.debug("enter");
		
		// Only initialize once
		if(this.httpClient != null) {
			logger.debug("RestClientJavaNet already initialized");
			return true;
		}
		RestAuthentication restAuth = null;
		
		// RestAuthenticationClientCert
		if (this.validateRequired("username", CONFIGMAP) && this.validateRequired("password", CONFIGMAP)) {
			logger.debug("Instantiating RestAuthentication with username {}", this.getConfigs().get("username"));
			// Create RestAuthentication object
			restAuth = new RestAuthenticationSimple(this.getConfigs().get(
					"username"), this.getConfigs().get("password"));
		} else {
			logger.debug("Could not load authentication object because username or password is not set");
			return false;
		}
		
		// Make sure we have some baseulr value to initialize with
		if(this.validateRequired("baseurl", CONFIGMAP)) {
			logger.debug("Instantiating RestClientJavaNet with baseurl {}", this.getConfigs().get("baseurl"));
			this.httpClient = new RestClientJavaNet(this.getConfigs()
					.get("baseurl"), restAuth);
			return true;
		} else {
			logger.debug("Could not initialize RestClientJavaNet beceause baseurl was not set");
			return false;
		}
	
	}

	/**
	 * Using CmsItemSearchREST to query solr for items
	 * Using config and param maps 
	 * @return CmsItemList
	 */
	public CmsItemListJSONSimple requestCMSItemReport() {
		logger.debug("enter");
		if (!this.initRestGetClient()) {
			logger.error("Could not initialize RestGetClient");

		}
		
		if(!this.validateRequired("q", PARAMMAP)) {
			logger.error("No valid query parameter set. Aborting");
			return null;
		}
		
		logger.debug("q {}", this.getParams().get("q"));
		
		if(this.initRestGetClient()) {
			logger.error("Could not init Rest Client!");
			return null;
		}
		
		CmsItemSearchREST rest = new CmsItemSearchREST(this.httpClient);
		// CmsItemListJSON result = new CmsItemListJSONSimpl	e();
		// result.fromJSONString(this.sendRequest());

		// TODO add validation of params.String q, String fl, String repo,
		// String sort, String rows
		CmsItemListJSONSimple result = (CmsItemListJSONSimple) rest.getItems(
				this.getParams().get("q"), this.getParams().get("fl"), this.getParams().get("repo"),
				this.getParams().get("sort"), this.getParams().get("rows"));
		itemList = result;
		Iterator<CmsItem> itemIterator = result.iterator();
		while (itemIterator.hasNext()) {
			CmsItem item = itemIterator.next();
			logger.debug("id: {}, checksum {}", item.getId(),
					item.getChecksum());
		}
		logger.debug("CMSItemList size: {}", result.sizeFound());
		return result;

	}

	/**
	 * @return String with request result
	 */
	public String sendRequest() {
		logger.debug("enter");
		this.addAuthHeader();
		// Make sure we request JSON
		this.requestHeaders.put("Accept", "application/json");

		try {
			final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
			this.httpClient.get(this.constructURL(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(ResponseHeaders headers) {
					logger.debug("Response from API: " + headers.getStatus());
					return byteOutputStream; // Returns to our outputstream
				}
			}, this.requestHeaders);

			// Return the JSON response as string
			return byteOutputStream.toString("UTF-8");

		} catch (HttpStatusError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * Helper method that will validate that a config or param is set.
	 * Not null or not empty strings.
	 * 
	 * @param key
	 * @return true if valid
	 */
	private boolean validateRequired(String key, String type) 
	{
		logger.debug("enter");
		if(type.equals(CONFIGMAP)) {
			if(this.getConfigs().get(key) != null || this.getConfigs().get(key) != "") {
				logger.debug("Config {} is valid", key);
				return true;
			}
		} else if(type.equals(PARAMMAP)) {
			if(this.getParams().get(key) != null || this.getParams().get(key) != "") {
				logger.debug("Param {} is valid", key);
				return true;
			}
		} else {
			logger.debug("Did we get a proper type?");
			return false;
		}
		
		return false;
	}

	/*
	 * Will create the necessary authentication
	 */
	private void addAuthHeader() {
		logger.debug("enter");
		// RestAuthenticationSimple authentication = new
		// RestAuthenticationSimple(this.getUsername(),this.getPassword());
		// authentication.getSSLContext(this.getBaseUrl()); // Trying to
		// configure SSL before hand

		this.requestHeaders = new HashMap<String, String>();
		requestHeaders.put(
				"Authorization",
				"Basic "
						+ Codecs.base64encode(this.configs.get("username")
								+ ":" + this.configs.get("password")));
		logger.debug("Auth header: {}", requestHeaders.get("Authorization"));
		logger.debug("leave");
	}

	/*
	 * Constructs URI that should support both reporting 1 and newer
	 */
	private String constructURI() {
		logger.debug("enter");
		StringBuffer uri = new StringBuffer();
		uri.append(this.configs.get("apiuri"));

		// First get the q
		uri.append("?q=" + urlencode(this.params.get("q")));

		if (this.getParams().size() > 0) {
			for (Map.Entry<String, String> entry : this.getParams().entrySet()) {
				// Add the rest of the params
				if (!entry.getKey().equals("q")) {
					uri.append("&" + entry.getKey() + "="
							+ urlencode(entry.getValue()));
				}

			}
		}

		logger.debug("Constructed uri: {}", uri.toString());
		return uri.toString(); // Return as string
	}

	/*
	 * Constructs URL that supports report framework 1.0
	 */
	private URL constructURL() {
		logger.debug("enter");
		StringBuffer url = new StringBuffer();
		url.append(this.configs.get("baseurl"));
		url.append(this.constructURI()); // Adds both the
		URL returnURL = null;
		try {
			returnURL = new URL(url.toString());
		} catch (MalformedURLException e) {
			logger.debug("The URL is malformed: " + e.getMessage());
		}
		logger.debug("Constructed URL: {}", returnURL);
		return returnURL;
	}

	protected String urlencode(String value) {
		logger.debug("enter");
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// encoding is a constant so we must consider this a fatal runtime
			// environment issue
			throw new RuntimeException(
					"Unexpected JVM behavior: failed to encode URL using UTF-8",
					e);
		}
	}
}
