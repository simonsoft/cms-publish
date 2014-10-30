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

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;
import se.simonsoft.publish.ant.helper.RestClientReportRequest.Reportversion;

/**
 * Sends request to CMS reporting 1.0 and sets the response as ant property
 * Uses JREs default jks for SSL
 * 
 * @author joakimdurehed
 *
 */

public class RequestReportTask extends Task {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private RestClientJavaNet httpClient;
	
	protected ConfigsNode configs;
	protected ParamsNode params;
	
	private RepoRevision headRevision; // Head according to Index
	
	private RestClientReportRequest request;


	/**
	 * @return the configs
	 */
	public ParamsNode getConfigs() {
		return params;
	}

	/**
	 * @param params the params to set
	 */
	public void addConfiguredParams(ParamsNode params) {
		this.params = params;
	}
	
	/**
	 * @param configs the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
	}

	/**
	 * executes the task
	 */
	public void execute()
	{
		this.request = new RestClientReportRequest();
		//this.request.setApiuri(this.getApiuri());
		//this.request.setBaseUrl(this.getUrl());
		
		this.addConfigsToRequest();
		this.addParamsToRequest();
		
		
		CmsItemList itemList = this.request.requestCMSItemReport();
		
		this.headRevision = this.request.getRevisionCompleted();
		
		this.publishItems(itemList);
		
		/*
		// If we require a CMS 3 report
		if(reportversion.equals(Reportversion.v32.toString())) {
			this.request.requestCMSItemReport();
		} else {
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
		//*/
	}
	
	private void publishItems(CmsItemList itemList) 
	{
		logger.debug("enter");
		Iterator<CmsItem> itemListIterator = itemList.iterator();
		while (itemListIterator.hasNext()) {
			CmsItem item = itemListIterator.next();
			logger.debug("id: {}, checksum {}", item.getId(),
					item.getChecksum());
			
			this.publishItem(item, this.headRevision.getNumber());
		}
	}
	private void publishItem(CmsItem item, Long baseLine) 
	{
		logger.debug("enter");
		this.getProject().setProperty("param.file", item.getId().withPegRev(baseLine).toString());
		this.getProject().setProperty("filename", item.getId().getRelPath().getNameBase());
		this.getProject().setProperty("lang", this.getItemProperty("abx:lang", item.getProperties()));
		RepoRevision itemRepoRev = item.getRevisionChanged();
		logger.debug("filename {}", item.getId().getRelPath().getNameBase());
		this.getProject().executeTarget("publish");
		/*
		 * <param name="param.file" value="${file}" />
					<param name="filename" value="${filename}" />
					<param name="lang" value="${lang}" />
		 */
	}
	/**
	 * Gets a property value by property name
	 * 
	 * @param propertyName the name of the property
	 * @param props the items propeties
	 * @return
	 */
	private String getItemProperty(String propertyName, CmsItemProperties props) 
	{
		 for (String name : props.getKeySet()) {
            // p.put(n, props.getString(n));
			 if(name.equals(propertyName)) {
				 logger.debug("Fond value {} for prop {}", props.getString(name), propertyName);
				 return props.getString(name);
			 }
		 }
		return "";
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
