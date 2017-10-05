/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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

import java.util.ArrayList;

import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.cms.reporting.rest.itemlist.CmsItemListJSONSimple;
import se.simonsoft.publish.ant.helper.RequestHelper;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * Perfoms a request to reporting and returns the result as JSON or as String
 * Used for 
 * 
 * @author joakimdurehed
 *
 */
public class RequestReportTask extends Task {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected String propertyName; // Name of the parameter to set with result
	private RestClientReportRequest request;
	protected ConfigsNode configs;
	protected ParamsNode params;
	private ArrayList<CmsItem> itemList;
	
	/**
	 * @return the propertyName
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @param propertyName the propertyName to set
	 */
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * @return the configs
	 */
	public ConfigsNode getConfigs() {
		return configs;
	}

	/**
	 * @return the params
	 */
	public ParamsNode getParams() {
		return params;
	}

	/**
	 * @param params
	 *            the params to set
	 */
	public void addConfiguredParams(ParamsNode params) {
		this.params = params;
	}

	/**
	 * @param configs
	 *            the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
	}
	
	
	public RequestReportTask() {
		// TODO Auto-generated constructor stub
	}

	
	public void execute() 
	{
		
	}
	
	private void initRequest()
	{
		// Init the ReportRequest Helper
		this.request = new RestClientReportRequest();
		RequestHelper.copyConfigsToRequest(this.getConfigs(), this.request);
		RequestHelper.copyParamsToRequest(this.getParams(), this.request);
		
		this.performQuery();
	}
	
	private String performQuery() 
	{
		
		CmsItemListJSONSimple cmsItemList = null;
		
		try {
			
			cmsItemList = this.request.getItemsWithQuery();
			
		} catch (FailedToInitializeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return cmsItemList.toJSONString();
	}
	
	private void setResultToPropery() 
	{
		
	}
}
