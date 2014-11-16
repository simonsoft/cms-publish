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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.restclient.HttpStatusError;
import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestAuthentication;
import se.repos.restclient.RestResponse;
import se.repos.restclient.auth.RestAuthenticationSimple;
import se.repos.restclient.base.Codecs;
import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
import se.simonsoft.cms.publish.ant.MissingPropertiesException;
import se.simonsoft.cms.reporting.client.CmsItemSearchREST;
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
	private CmsItemSearchREST itemSearchRest;
	public CmsItemList itemList;
	public static final String CONFIGMAP = "configs";
	public static final String PARAMMAP = "params";
	
	
	/**
	 * Reportversion enumeration (possible not to be used)
	 *
	 */
	public enum Reportversion {
		v094, v100, v320 // v MAJOR MINOR PATCH
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
	 * Adds param (key) with value and urlencode value
	 * Use for query, field list and such
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
	 * Constructor
	 */
	/*
	public RestClientReportRequest() {
		try {
			this.initItemSearchRest();
		} catch (MissingPropertiesException e) {

		}
	}
	//*/
	
	/**
	 * Helper method that initializes the CmsItemSearchREST object
	 * @return
	 * @throws MissingPropertiesException 
	 */
	private void initItemSearchRest() throws MissingPropertiesException 
	{
		logger.debug("enter");
		this.initRestGetClient();
		
		if(!this.validateRequired("repo", PARAMMAP)) {
			logger.error("No valid repo parameter set. Aborting");
			throw new MissingPropertiesException("Parameter repo is required");
		}
		
		if(!this.validateRequired("q", PARAMMAP)) {
			logger.error("No valid query parameter set. Aborting");
			throw new MissingPropertiesException("Parameter q is required");
		}
		
		this.itemSearchRest = new CmsItemSearchREST(this.httpClient);
		
	}

	/**
	 * Initializes a RestClientJavaNet with host and auth information from configs map
	 * 
	 * @throws MissingPropertiesException 
	 */
	private void initRestGetClient() throws MissingPropertiesException {
		logger.debug("enter");
		
		//this.httpClient = null; // Reset
		
		// Only initialize once
		if(this.httpClient != null) {
			logger.debug("RestClientJavaNet already initialized");
			return;
		}
		
		RestAuthentication restAuth = null;
		
		// RestAuthenticationClientCert
		if (this.validateRequired("username", CONFIGMAP) && this.validateRequired("password", CONFIGMAP)) {
			logger.debug("Instantiating RestAuthentication with username {}", this.getConfigs().get("username"));
			// Create RestAuthentication object
			restAuth = new RestAuthenticationSimple(this.getConfigs().get(
					"username"), this.getConfigs().get("password"));
			restAuth.getSSLContext(this.getConfigs().get("baseurl")); // Might no be needed. But no harm. // Not verified 
			
		} else {
			logger.error("Could not load authentication object because username or password is not set");
			throw new MissingPropertiesException("Parameters for username and password are required");
		}
		
		// Make sure we have some baseulr value to initialize with
		if(this.validateRequired("baseurl", CONFIGMAP)) {
			logger.debug("Instantiating RestClientJavaNet with baseurl {}", this.getConfigs().get("baseurl"));
			this.httpClient = new RestClientJavaNet(this.getConfigs()
					.get("baseurl"), restAuth);
			
		} else {
			logger.error("Could not initialize RestClientJavaNet beceause baseurl was not set");
			throw new MissingPropertiesException("Parameters for baseurl is required");
		}

	}
	
	/**
	 * Returns the highest revision in the index. Most often equel to head, but if the indexing lags 
	 * it might very well not be.
	 * 
	 * @return RepoRevision
	 * @throws FailedToInitializeException 
	 */
	public RepoRevision getRevisionCompleted() throws FailedToInitializeException 
	{
		logger.debug("enter");
		try {
			this.initItemSearchRest();
		} catch (MissingPropertiesException e) {
			throw new FailedToInitializeException(e.getMessage(), e);
		}
		
		RepoRevision repoRev = this.itemSearchRest.getRevisionCompleted( this.getParams().get("repo"), "");
		logger.debug("repoRev: {}", repoRev.getNumber());
		
		return repoRev;
	}
	
	/**
	 * Using CmsItemSearchREST to query solr for items
	 * Using config and param maps to build query
	 * @return CmsItemList
	 * @throws FailedToInitializeException 
	 */
	public CmsItemListJSONSimple getItemsWithQuery() throws FailedToInitializeException  {
		logger.debug("enter");
		
		try {
			this.initItemSearchRest();
		} catch (MissingPropertiesException e) {
			// TODO Auto-generated catch block
			throw new FailedToInitializeException(e.getMessage(), e);
		}
		
		// Validate empty to make sure we at least have empty strings
		this.validateEmpty("fl", PARAMMAP);
		this.validateEmpty("sort", PARAMMAP);
		this.validateEmpty("rows", PARAMMAP);
		
		logger.debug("q {}", this.getParams().get("q"));
		
		// Return as a CmsItemListJSONSimple list
		CmsItemListJSONSimple resultItemList =  (CmsItemListJSONSimple) this.itemSearchRest.getItems(
					this.getParams().get("q"), this.getParams().get("fl"), this.getParams().get("repo"),
					this.getParams().get("sort"), this.getParams().get("rows"));
		
		
		logger.debug("CMSItemList size: {}", resultItemList.sizeFound());
		return resultItemList;

	}
	// getParents(CmsItemId itemId, String target, String base, String rev, String type, String pathArea, boolean head)
	/**
	 * 
	 * @param itemId
	 * @param target
	 * @param base
	 * @param rev
	 * @param type
	 * @param pathArea
	 * @param head
	 * @return
	 * @throws FailedToInitializeException
	 */
	public CmsItemListJSONSimple getItemsParents(CmsItemId itemId,String target, String base, String rev, String type, String pathArea, boolean head) throws FailedToInitializeException
	{
		try {
			this.initItemSearchRest();
		} catch (MissingPropertiesException e) {
			// TODO Auto-generated catch block
			throw new FailedToInitializeException(e.getMessage(), e);
		}
		
		CmsItemListJSONSimple resultItemList = (CmsItemListJSONSimple) this.itemSearchRest.getParents(itemId, target, base, rev, type, pathArea, head);
		
		return resultItemList;
	}
	
	/**
	 * Helper method to get meta data (for dev right now)
	 * @param metaName
	 * @param meta
	 * @return
	 */
	private Object getItemMeta(String metaName, Map<String, Object> meta) 
	{
		logger.debug("enter");
		logger.debug("meta size {}", meta.size());
		 for (String name : meta.keySet()) {
			 logger.debug("Meta name {} with value {}",name, meta.get(name));
            // p.put(n, props.getString(n));
			 if(name.equals(metaName)) {
				 logger.debug("Fond value {} for meta {}",meta.get(name), metaName);
				 return meta.get(name);
			 }
		 }
		return null;
	}
	
	/**
	 * Helper method to get Property (for dev right now)
	 * @param propertyName
	 * @param props
	 * @return
	 */
	private String getItemProperty(String propertyName, CmsItemProperties props) 
	{
		logger.debug("enter");
		 for (String name : props.getKeySet()) {
			 logger.debug("Prop name {} with value {}",name, props.getString(name));
            // p.put(n, props.getString(n));
			 if(name.equals(propertyName)) {
				 logger.debug("Fond value {} for prop {}", props.getString(name), propertyName);
				 return props.getString(name);
			 }
		 }
		return "";
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
	public boolean validateRequired(String key, String type) 
	{
		logger.debug("enter");
		if(type.equals(CONFIGMAP)) {
			if(this.getConfigs().get(key) == null || this.getConfigs().get(key) == "" || this.getConfigs().get(key).length() == 0) {
				logger.debug("Config {} is not valid", key);
				return false;
			}
		} else if(type.equals(PARAMMAP)) {
			if(this.getParams().get(key) == null || this.getParams().get(key) == "" || this.getParams().get(key).length() == 0) {
				logger.debug("Param {} is not valid", key);
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Makes sure no values are null
	 * @param key
	 * @param type
	 */
	private void validateEmpty(String key, String type)
	{
		logger.debug("enter");
		if(!this.validateRequired(key, type)) {
			logger.debug("Set {} to empty String", key);
			if(type.equals(CONFIGMAP)) {
				this.getConfigs().put(key, "");
			}
			if(type.equals(PARAMMAP)) {
				this.getParams().put(key, "");
			}
		}
	}
	
	/**
	 * Creates basic auth headers and adds them to requestheaders HashMap
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

	/**
	 * Constructs URI as required by Reporting 0.9.x >
	 * For use with when requesting without cms-reporing Java API
	 * @return
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

	/**
	 * Constructs URL as required for Reporting 0.9.x >
	 * For use with when requesting without cms-reporing Java API
	 * @return
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

	/**
	 * URL Encode given value in UTF-8
	 * @param String value
	 * @return encoded String
	 */
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
