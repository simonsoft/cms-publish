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
package se.simonsoft.cms.publish.ant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.tools.ant.Task;

import se.repos.restclient.HttpStatusError;
import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestResponse;
import se.repos.restclient.auth.RestAuthenticationSimple;
import se.repos.restclient.javase.RestClientJavaNet;

public class RequestReportTask extends Task {

	private RestClientJavaNet httpClient;
	
	protected ConfigsNode configs;
	protected String url;
	protected String username;
	protected String password;
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
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
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	protected ParamsNode params;
	
	/**
	 * @return the configs
	 */
	public ParamsNode getConfigs() {
		return params;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @param jobs the jobs to set
	 */
	public void addConfiguredParams(ParamsNode params) {
		this.params = params;
	}

	public void execute()
	{
		
		String response = this.requestReport(this.constructURI());
		log("response: " + response);
		this.getProject().setProperty("reportresponse", response);
	}
	
	private String requestReport(String uri)
	{	
		log("Request report");
		RestAuthenticationSimple authentication = new RestAuthenticationSimple(username,password);
		this.httpClient = new RestClientJavaNet(this.url, authentication);
		
		
		HashMap<String,String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Accept", "application/json");
		requestHeaders.put("Authorization", "Basic amR1cmVoZWQ6amR1cmVoZWQ=");
		try {
			
			final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
			
			this.httpClient.get(new URL(uri), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
					log("Response from API: " + headers.getStatus());
					return byteOutputStream; // Returns to our outputstream
				}
			}, requestHeaders);
			
			// Return the JSON response as string
			return byteOutputStream.toString("UTF-8");
			
		} catch (HttpStatusError e) {
			// TODO Here we need to fail the build
			log("Communication error: " + e.getResponse());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	/*
	 * Supports report framework 1.0
	 */
	private String constructURI()
	{
		log("Construct URI");
		StringBuffer uri = new StringBuffer();
		uri.append(this.url);
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				// Adding the query (query is mandatory)
				if(param.name.equals("q")){
					uri.append("?q=" + urlencode(param.value));
				}
				// Adding rows
				if(param.name.equals("rows")){
					uri.append("&rows=" + param.value);
				}
				
				if(param.name.equals("start")){
					uri.append("&start=" + param.value);
				}
			}
		}
		
		log("URI:" + uri.toString());
		return uri.toString(); // Return as string
	}
	
	protected String urlencode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// encoding is a constant so we must consider this a fatal runtime environment issue
			throw new RuntimeException("Unexpected JVM behavior: failed to encode URL using UTF-8", e);
		}
	}
}
