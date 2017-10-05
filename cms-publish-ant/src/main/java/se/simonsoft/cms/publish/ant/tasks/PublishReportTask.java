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
import se.simonsoft.cms.publish.ant.filters.FilterItems.FilterOrder;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.FilterNode;
import se.simonsoft.cms.publish.ant.nodes.FiltersNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.publish.ant.helper.RequestHelper;
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
	protected FiltersNode filters;
	protected Long baseline;
	protected String target; // The target name of the target in charge of
	protected String publishtime; // The mean time to publish in seconds. Used
	// for estimating total publishing time
	// publishing an item
	private ArrayList<CmsItem> itemList;
	private CmsItem currentItem; // Current cmsitem being published

	/**
	 * @return the publishtime
	 */
	public String getPublishtime() {
		if(publishtime == null) {
			publishtime = "";
		}
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
	 * @return the filters
	 */
	public FiltersNode getFilters() {
		return filters;
	}

	/**
	 * @param filters
	 *            the filters to set
	 */
	public void addConfiguredFilters(FiltersNode filters) {
		this.filters = filters;
	}

	/**
	 * @param configs
	 *            the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
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
	 * @return the baseline
	 */
	public Long getBaseline() {
		return baseline;
	}

	/**
	 * @param baseline the baseline to set
	 */
	public void setBaseline(Long baseline) {
		this.baseline = baseline;
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
					"This task requires a publish task target like: PublishRequestPETask");
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

		RequestHelper.copyConfigsToRequest(this.getConfigs(), this.request);
		RequestHelper.copyParamsToRequest(this.getParams(), this.request);

		// Retrieve the CmsItemList with query (set in configs)
		CmsItemList cmsItemList = null;

		this.runFilters(FilterOrder.PREQUERY);

		// 1 Get the "head according to index" or not
		if(this.getBaseline() == null) {
			try {
				logger.debug("No beseline set, find head according to index");
				this.headRevision = this.request.getRevisionCompleted();
			} catch (FailedToInitializeException e) {
				throw new BuildException(e.getMessage());
			}
		}
		logger.debug("Value of baseline getter {}, value of get project property {}", this.getBaseline(), this.getProject().getProperty("baseline"));
		
		// 2 Perform the query for publishable items
		try {
			cmsItemList = this.request.getItemsWithQuery();
			// Create a itemlist we can work with, for instance the .get(index)
			// method is very useful
			// this.itemList = this.createMutableItemList(cmsItemList);
			this.itemList = RequestHelper.createMutableItemList(cmsItemList);
		} catch (FailedToInitializeException ex) {
			throw new BuildException(ex.getMessage());
		}
		this.runFilters(FilterOrder.POSTQUERY);
		// Publish items using our mutable item list
		this.publishItems();
	}

	/**
	 * Runs filters with specified order. Returns true if filter has run, false if not
	 *  
	 * @param order
	 * @return true if any filter has run with specified order
	 */
	private boolean runFilters(FilterOrder order) {
		logger.debug("Find fitlers to run with order {}", order.toString());
		boolean filterHasRun = false;
		if (null != this.getFilters() && this.filters.isValid()) {
			for (final FilterNode filter : this.filters.getFilters()) {

				if (filter.getOrder().toUpperCase().equals(order.toString())) {
					logger.debug("Filter {} with order {}",
							filter.getClasspath(), filter.getOrder());
					// Is it better just giving publish filter a baseline and not a complete headRev object?
					// more options of course with a complete object
					// Publish filter run
					if (order.equals(FilterOrder.PUBLISH)) {
						if (RequestHelper.runPublishFilterWithClassPath(
								filter.getClasspath(), this.currentItem,
								this.headRevision, this.getProject(),
								this.getTarget())) {
							filterHasRun = true;
						}
					}
					// Items filters run
					if (order.equals(FilterOrder.POSTQUERY)
							|| order.equals(FilterOrder.PREQUERY)) {
						if (RequestHelper.runItemsFilterWithClassPath(
								filter.getClasspath(), this.itemList,
								this.request, this.headRevision,
								this.getProject())) {
							filterHasRun = true;
						}
					}

				}
			}
		}
		
		return filterHasRun;
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
			if (count == 1) {
				logger.info(
						"\nPublish item nr {} with name {} \nEstimated time left: {}",
						count, item.getId().getRelPath().getName(),
						this.estimatedTimeLeft(this.itemList.size()));
			} 
			else if (count == this.itemList.size()) {
				logger.info(
						"\nPublish item nr {} with name {} \nEstimated time left: {} with {}/{} items left",
						count, item.getId().getRelPath().getName(),
						this.estimatedTimeLeft(this.itemList.size() - count),
						this.itemList.size() - count, this.itemList.size());
			}
			else {
				logger.info(
						"\nPublish item nr {} with name {} \nEstimated time left: {} with {}/{} items left",
						count, item.getId().getRelPath().getName(),
						this.estimatedTimeLeft(this.itemList.size() - count - 1),
						this.itemList.size() - count, this.itemList.size());
			}

			this.publishItem(item);
		}
		logger.debug("leave");
	}

	/**
	 * Calls the publish task for a CmsItem and sets properties needed for
	 * publishing to work. Adds a baseline pegrev which should be head at the
	 * time of the query to reporting framework ran.
	 * 
	 * @param CmsItem
	 *            item
	 * @param ArrayList
	 *            <String> publishProperties
	 */
	private void publishItem(CmsItem item) {
		logger.debug("enter");

		this.currentItem = item; // Set current item
		
		
		Long currentBaseline = null;
		if(this.getBaseline() == null) {
			currentBaseline = this.headRevision.getNumber();
		} else {
			currentBaseline = this.getBaseline();
			this.getProject().setProperty("baseline", this.getBaseline().toString());
		}
		
		// Run publish filter if any exists
		boolean filtersDidRun = this.runFilters(FilterOrder.PUBLISH);
		
	
		// If no filter ran, use default properties 
		// param.file = path to file (logicalid)
		// filename = the filename to use 
		// lang = abx:lang (if it is present)
		if (!filtersDidRun) {
			logger.info("No {} filter to run", FilterOrder.PUBLISH.toString());
			
			logger.debug(
					"Passing logicalid ({}), filename ({}) to publish target ({}) ",
					item.getId().withPegRev(currentBaseline).toString(), item.getId()
							.getRelPath().getNameBase(), item.getProperties()
							.getString("abx:lang"), this.getTarget());

			// Defaults to use file and filename as properties to pass on to publish target
			this.getProject().setProperty("param.file",
					item.getId().withPegRev(currentBaseline).toString());

			this.getProject().setProperty("filename",
					item.getId().getRelPath().getNameBase());
			
			// If we find a lang, lets set it to lang property. 
			if(!"".equals(item.getProperties().getString("abx:lang"))) {
				logger.debug("Found lang: {}, passing it to {}",item.getProperties().getString("abx:lang"), this.getTarget());
				this.getProject().setProperty("lang",
						item.getProperties().getString("abx:lang"));
			}
			// A test:
			/* DID NOT WORK
			this.getProject().getProperties().put("CMSITEM", item);
			//*/

			this.getProject().executeTarget(this.getTarget());
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

		if (this.getPublishtime() == null || this.getPublishtime().equals("")) {
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

		return returnString.toString();
	}
}
