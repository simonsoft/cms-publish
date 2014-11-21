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
import se.simonsoft.cms.publish.ant.MissingPropertiesException;
import se.simonsoft.cms.publish.ant.filters.FilterItems;
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
	protected String target; // The target name of the target in charge of
								// publishing an item
	private ArrayList<CmsItem> itemList;
	protected String publishtime; // The mean time to publish in seconds. Used
									// for estimating total publishing time

	/**
	 * @return the publishtime
	 */
	public String getPublishtime() {
		return publishtime;
	}

	/**
	 * @param publishtime
	 *            the publishtime to set
	 */
	public void setPublishtime(String publishtime) {
		this.publishtime = publishtime;
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

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * @param filter
	 *            the filter to set
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
	 * @param target
	 *            the target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * Executes the task after checking that the ant target to use for
	 * publishing exists in ant project
	 */
	public void execute() {
		logger.debug("enter");
		// Make sure we have a publish task to use for publishing
		if (this.getProject().getTargets().get(this.getTarget()) == null
				|| this.getTarget().equals("")) {
			throw new BuildException(
					"This task requires a publush target like: PublishRequestPETask");
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

		this.addConfigsToRequest(this.request);
		this.addParamsToRequest(this.request);

		// Retrieve the CmsItemList with query (set in configs)
		CmsItemList cmsItemList = null;

		try {
			cmsItemList = this.request.getItemsWithQuery();
			// Create a itemlist we can work with, for instance the .get(index)
			// method is very useful
			this.itemList = this.createMutableItemList(cmsItemList);
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
	 * 
	 * @param itemList
	 * @return a ArrayList<CmsItem> copy of CmsItemList items
	 */
	private ArrayList<CmsItem> createMutableItemList(CmsItemList itemList) {
		logger.debug("enter");
		ArrayList<CmsItem> copyItemList = new ArrayList<CmsItem>();

		for (CmsItem item : itemList) {
			copyItemList.add(item); // Add item to our itemlist
		}
		logger.debug("ItemList created with size {}", copyItemList.size());
		return copyItemList;
	}

	/**
	 * Filters items using specified filter
	 */
	private void filterItems() {
		logger.debug("enter");

		if (this.filter == null || this.filter.equals("")) {
			logger.info("No filter to init");
			return;
		}

		// Dynamically instantiate correct filter. Do we need/want to be this
		// dynamic?
		// Could we settle for a switch/ if/else
		try {
			// TODO check that the filter syntax is a full qualified class path?
			// We will grab any problem
			// in the exceptions anyways of course.
			logger.info("Init filter {}", this.filter);
			// Filters are instantiated by the name of the filter and the suffix
			// Filter
			Class<?> filterClass = Class.forName(this.filter);
			FilterItems filterResponse = (FilterItems) filterClass
					.newInstance();

			// Todo should/would filter need some other stuff? I think the
			// request object, the list of items and a baseline
			// should suffice for almost whatever it is we need to do in the
			// filter.
			filterResponse.initFilter(this.request, this.itemList,
					this.headRevision);
			filterResponse.runFilter();

		} catch (InstantiationException e) {
			logger.warn("Filter Init resulted in InstantiationException {}",
					e.getMessage());
			return; // Do I need this?
		} catch (IllegalAccessException e) {
			logger.warn("Filter Init resulted in IllegalAccessException {}",
					e.getMessage());
			return;
		} catch (ClassNotFoundException e) {
			logger.warn("Filter Init resulted in ClassNotFoundException {}",
					e.getMessage());
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

		// Publish items
		int count = 0;
		logger.info("Estimated total publish time {} for {} number of items",
				this.estimatedTimeLeft(this.itemList.size()),
				this.itemList.size());
		logger.debug("Start publishing items");
		for (CmsItem item : this.itemList) {
			count++;
			// Output info depending on if this is the first item or not
			if(count == 1) {
				logger.info(
						"\nPublish item nr {} with name {} \nEstimated time left: {}",
						count, item.getId().getRelPath().getName(),
						this.estimatedTimeLeft(this.itemList.size()));
			} else {
				logger.info(
						"\nPublish item nr {} with name {} \nEstimated time left: {} with {}/{} items left",
						count, item.getId().getRelPath().getName(),
						this.estimatedTimeLeft(this.itemList.size() - count), this.itemList.size() - count, this.itemList.size());
			}
			
			this.publishItem(item, this.headRevision.getNumber(), null);
		}
		logger.debug("leave");
	}

	/**
	 * Calculates the estimated time left of publishing based on how long a
	 * "normal" publishing takes times the number of items to publish
	 * 
	 * @param numberOfItems
	 * @return the estimated time left as hours, minutes, seconds 
	 */
	private String estimatedTimeLeft(int numberOfItems) {
		logger.debug("enter");
		// Create a returnString
		StringBuffer returnString = new StringBuffer();
		
		if(this.getPublishtime() == null || this.getPublishtime().equals("")) {
			logger.debug("Value for publishtime is not set");
			returnString.append("Unknown");
		} else {
			// Calculate total time
			float totalTime = Float.parseFloat(this.getPublishtime())
					* (float) numberOfItems;
			
			// Calculate totaltime as hours, minutes and seconds
			int hours = (int) totalTime / 3600;
			int remainder = (int) totalTime - hours * 3600;
			int mins = remainder / 60;
			remainder = remainder - mins * 60;
			int secs = remainder;
			
			
			
			returnString.append(hours);
			returnString.append(" hour(s) ");
			returnString.append(mins);
			returnString.append(" minute(s) ");
			returnString.append(secs);
			returnString.append(" second(s)");
		}
		
		return  returnString.toString();
	}

	/**
	 * Calls the publish task for a CmsItem and sets properties needed for
	 * publishing to work. Adds a baseline pegrev which should be head at the
	 * time of the query to reporting framework ran.
	 * 
	 * @param CmsItem
	 *            item
	 * @param Long
	 *            baseLine
	 * @param ArrayList
	 *            <String> publishProperties
	 */
	private void publishItem(CmsItem item, Long baseLine,
			ArrayList<String> publishProperties) {
		logger.debug("enter");
		// TODO ability to set what "properties" should be passed to publish
		// target
		logger.debug("set Property param.file with {} adding peg {}",
				item.getId(), baseLine);

		this.getProject().setProperty("param.file",
				item.getId().withPegRev(baseLine).toString());

		this.getProject().setProperty("filename",
				item.getId().getRelPath().getNameBase());

		this.getProject().setProperty("lang",
				this.getItemProperty("abx:lang", item.getProperties()));

		// RepoRevision itemRepoRev = item.getRevisionChanged();
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

	/**
	 * Sets all params to RestClientReportRequest requests param map
	 * 
	 * @param request
	 *            RestClientReportRequest
	 */
	private void addParamsToRequest(RestClientReportRequest request) {
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				request.addParam(param.getName(), param.getValue());
			}
		}
	}

	/**
	 * Sets all configs to RestClientReportRequest configs map
	 * 
	 * @param request
	 */
	private void addConfigsToRequest(RestClientReportRequest request) {
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				request.addConfig(config.getName(), config.getValue());
			}
		}
	}
}
