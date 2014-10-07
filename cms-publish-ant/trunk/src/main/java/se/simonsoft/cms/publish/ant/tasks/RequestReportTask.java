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
package se.simonsoft.cms.publish.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * Sends request to CMS reporting 1.0 and sets the response as ant property
 * Uses JREs default jks for SSL
 * 
 * @author joakimdurehed
 *
 */

public class RequestReportTask extends Task {

	private RestClientJavaNet httpClient;
	
	protected ConfigsNode configs;
	protected ParamsNode params;
	protected String url;  
	protected String apiuri;
	protected String username;
	protected String password;
	protected String target;
	protected String reportversion;
	
	private RestClientReportRequest request;
	
	/**
	 * @return the reportversion
	 */
	public String getReportversion() {
		return reportversion;
	}

	/**
	 * @param reportversion the reportversion to set
	 */
	public void setReportversion(String reportversion) {
		this.reportversion = reportversion;
	}
	
	
	/**
	 * @return the uri
	 */
	public String getApiuri() {
		return apiuri;
	}

	/**
	 * @param uri the uri to set
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

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	
	public void execute()
	{
		this.request = new RestClientReportRequest();
		this.request.setApiuri(this.getApiuri());
		this.request.setBaseUrl(this.getUrl());
		
		this.addConfigsToRequest();
		this.addParamsToRequest();
		// We'll get a json string
		String response = this.requestReport();
		
		// We set a property with the response for somebody to parse
		this.getProject().setProperty("requestresponse", response);
		if(this.getTarget() != null) {
			log("Call target " + this.getTarget());
			// Call a target to deal with the response
			this.getProject().executeTarget(this.getTarget());
		}
		
	}
	
	private String requestReport()
	{	
		log("Request report");
		String response = this.request.sendRequest();
		if(response == null) {
			throw new BuildException("Could not get report response!");
		}
		return response;
	}
	
	
	private void addParamsToRequest()
	{
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				this.request.addParam(param.getName(), param.getValue());
			}
		}
	}
	
	private void addConfigsToRequest()
	{
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				this.request.addConfig(config.getName(), config.getValue());
			}
		}
	}
}
