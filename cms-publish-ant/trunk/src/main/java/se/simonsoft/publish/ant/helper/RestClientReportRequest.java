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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.restclient.HttpStatusError;
import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestResponse;
import se.repos.restclient.RestResponseAccept;
import se.repos.restclient.auth.RestAuthenticationSimple;
import se.repos.restclient.base.Codecs;
import se.repos.restclient.javase.RestClientJavaNet;

/**
 * A restclient request class, creates the request, sends and retrieve it.
 * Might benefit from implementing an interface
 * @author joakimdurehed
 *
 */
public class RestClientReportRequest {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private HashMap<String, String> configs; // not used at the moment
	private HashMap<String, String> params;
	private String baseUrl;
	private String apiuri;
	private String username;
	private String password;
	private HashMap<String, String> requestHeaders;
	private RestClientJavaNet httpClient;
	
	public enum Reportversion {
		v1, v3
	}

	/**
	 * @return the baseUrl
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * @param baseUrl the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * @return the apiuri
	 */
	public String getApiuri() {
		return apiuri;
	}

	/**
	 * @param apiuri
	 *            the apiuri to set
	 */
	public void setApiuri(String apiuri) {
		this.apiuri = apiuri;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public Map<String, String> getParams() {
		return this.params; // TODO: return copy
	}

	public Map<String, String> getConfigs() {
		return this.configs; // TODO: return copy
	}

	// Add k,v config to config map
	public void addConfig(String key, String value) {
		this.configs.put(key, value);
	}

	// Add k,v param to param map
	public void addParam(String key, String value) {
		this.params.put(key, value);
	}

	// http://appdev1.pdsvision.net/cms/rest/report3/items?q=prop_cms.status:In_Translation&repo=demo1

	
	/**
	 * @return String with request result
	 */
	public String sendRequest()
	{
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
		requestHeaders.put("Authorization",
				"Basic " + Codecs.base64encode(username + ":" + password));
	}

	/*
	 * Constructs URI that should support both reporting 1 and newer
	 */
	private String constructURI() {
		logger.debug("enter");
		StringBuffer uri = new StringBuffer();
		uri.append(this.getApiuri());

		if (this.getParams().size() > 0) {
			for (Map.Entry<String, String> entry : this.getParams().entrySet()) {
				// We start with the query
				if (entry.getKey().equals("q")) {
					uri.append("?q=" + urlencode(entry.getValue()));
				}
				uri.append("&" + entry.getKey() + "="
						+ urlencode(entry.getValue()));
			}
		}

		logger.debug("Constructed report request-uri: " + uri.toString());
		return uri.toString(); // Return as string
	}

	/*
	 * Constructs URL that supports report framework 1.0
	 */
	private URL constructURL() {
		logger.debug("enter");
		StringBuffer url = new StringBuffer();
		url.append(this.getBaseUrl());
		url.append(this.constructURI()); // Adds both the
		URL returnURL = null;
		try {
			returnURL = new URL(url.toString());
		} catch (MalformedURLException e) {
			logger.debug("The URL is malformed: " + e.getMessage());
		}
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
					"Unexpected JVM behavior: failed to encode URL using UTF-8",e);
		}
	}
}
