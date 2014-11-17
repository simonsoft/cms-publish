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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
import se.simonsoft.cms.publish.ant.FilterItems;
import se.simonsoft.cms.publish.ant.MissingPropertiesException;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * Publishes the result of query to Reporting framework (1.2.2)
 * 
 * @author joakimdurehed
 *
 */

public class PublishReportTask extends Task {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private RestClientReportRequest request;
	private RepoRevision headRevision; // Head according to Index
	protected ConfigsNode configs;
	protected ParamsNode params;
	protected String filter; // Filter to use Should perhaps be a list
	protected String target; // The target name of the target in charge of publishing an item
	private ArrayList<CmsItem> itemList;

	/**
	 * @return the configs
	 */
	public ParamsNode getConfigs() {
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
	
	/**
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(String filter) {
		this.filter = filter;
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

	/**
	 * executes the task
	 */
	public void execute() {
		logger.debug("enter");
		// Make sure we have a publish task to use for publishing
		if (this.getProject().getTargets().get(this.getTarget()) == null || this.getTarget().equals("")) {
			throw new BuildException("This task requires a publush target like: PublishRequestPETask");
		}
		// Lets start it all
		this.initPublishing();
	}

	/**
	 * Method responsible initing RestClientReportRequest and for passing on
	 * configuration and parameters to it. Then also to init the report request
	 * and publishing of the resulting items
	 */
	private void initPublishing() {
		logger.debug("enter");
		// Init the ReportRequest Helper
		this.request = new RestClientReportRequest();

		this.addConfigsToRequest();
		this.addParamsToRequest();

		// Retrieve the CmsItemList with query (set in configs)
		CmsItemList cmsItemList = null;

		try {
			cmsItemList = this.request.getItemsWithQuery();
			// Create a itemlist we can work with
			this.createMutableItemList(cmsItemList);
		} catch (FailedToInitializeException ex) {
			throw new BuildException(ex.getMessage());
		}

		// Get the "head according to index"
		try {
			this.headRevision = this.request.getRevisionCompleted();
		} catch (FailedToInitializeException e) {
			throw new BuildException(e.getMessage());
		}
		
		// First check if we need to filter our itemList
		this.filterItems();
		// Publish items using our mutable item list
		this.publishItems();
	}
	
	/**
	 * Creates a mutable copy of a CmsItemList
	 * @param CmsItemList itemList
	 */
	private void createMutableItemList(CmsItemList itemList) 
	{
		logger.debug("enter");
		if(this.itemList == null) {
			this.itemList = new ArrayList<CmsItem>();
		}
		
 		for(CmsItem item: itemList ) {
			this.itemList.add(item); // Add item to our itemlist
		}
	}

	/**
	 * Filters items using specified filter
	 */
	private void filterItems() 
	{
		logger.debug("enter");
		
		if(this.filter == null || this.filter.equals("")) {
			logger.info("No filter to init");
			return;
		}
		
		// Dynamically instantiate correct filter. Do we need/want to be this dynamic? 
		// Could we settle for a switch/ if/else 
		try {
			
			logger.info("Init filter {}", this.filter);
			// Filters are instantiated by the name of the filter and the suffix Filter
			Class<?> filterClass = Class.forName(this.filter);
			FilterItems filterResponse = (FilterItems) filterClass.newInstance();
			
			filterResponse.initFilter(this.request, this.itemList, this.headRevision);
			filterResponse.runFilter();
			
		} catch (InstantiationException e) {
			logger.warn("Filter Init resulted in InstantiationException {}", e.getMessage());
			return;
		} catch (IllegalAccessException e) {
			logger.warn("Filter Init resulted in IllegalAccessException {}", e.getMessage());
			return;
		} catch (ClassNotFoundException e) {
			logger.warn("Filter Init resulted in ClassNotFoundException {}", e.getMessage());
			return;
		}
	}
	
	/**
	 * Iterates CmsItemList and passes each item to publishItem method
	 * 
	 * @param itemList
	 */
	private void publishItems() {
		logger.debug("enter");
		
		Iterator<CmsItem> itemListIterator = this.itemList.iterator();
		
		// Count number of items.
		long count = 0L;
		while (itemListIterator.hasNext()) {
			count++;
		}
		
		logger.debug("Counted {} and sizeFound {} number of items to publish", count, this.itemList.size());
		// TODO Add some filter method that will filter the list of items to publish
		// based on some criteria.
		
		// Publish items
		count = 0L;
		while (itemListIterator.hasNext()) {
			CmsItem item = itemListIterator.next();
			count++;
			logger.debug("Item nr {} file: {}", count, item.getId().getRelPath().getName());
			
			this.publishItem(item, this.headRevision.getNumber(), null);
		}
		logger.debug("leave");
	}

	/**
	 * Calls the publish task for a CmsItem and sets properties needed for
	 * publishing to work. Adds a baseline pegrev which should be head at the
	 * time of the query to reporting framework ran.
	 * 
	 * @param CmsItem item
	 * @param Long baseLine
	 * @param ArrayList<String> publishProperties
	 */
	private void publishItem(CmsItem item, Long baseLine, ArrayList<String> publishProperties) {
		logger.debug("enter");
		// TODO ability to set what "properties" should be passed to publish target
		logger.debug("set Property param.file with {} adding peg {}", item.getId(), baseLine);
		
		this.getProject().setProperty("param.file",
				item.getId().withPegRev(baseLine).toString());
		
		this.getProject().setProperty("filename",
				item.getId().getRelPath().getNameBase());
		
		this.getProject().setProperty("lang",
				this.getItemProperty("abx:lang", item.getProperties()));
		
		RepoRevision itemRepoRev = item.getRevisionChanged();
		logger.debug("filename {}", item.getId().getRelPath().getNameBase());

		this.getProject().executeTarget("publish");
		logger.debug("leave");
	}

	/**
	 * Gets a property value by property name
	 * 
	 * @param propertyName
	 *            the name of the property
	 * @param props
	 *            the items propeties
	 * @return
	 */
	private String getItemProperty(String propertyName, CmsItemProperties props) {
		logger.debug("enter");
		for (String name : props.getKeySet()) {
			if (name.equals(propertyName)) {
				logger.debug("Found value {} for prop {}",
						props.getString(name), propertyName);
				return props.getString(name);
			}
		}
		return "";
	}

	private String requestReport() {
		log("Request report");
		String response = this.request.sendRequest();

		if (response == null) {
			throw new BuildException("Could not get report response!");
		}
		return response;
	}

	private void addParamsToRequest() {
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				this.request.addParam(param.getName(), param.getValue());
			}
		}
	}

	private void addConfigsToRequest() {
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				this.request.addConfig(config.getName(), config.getValue());
			}
		}
	}
}
