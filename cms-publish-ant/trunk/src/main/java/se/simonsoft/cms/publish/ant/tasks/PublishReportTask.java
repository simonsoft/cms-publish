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

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
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
	 * executes the task
	 */
	public void execute() {

		// Make sure we have a publish task to use for publishing
		if (this.getProject().getTargets().get("publish") == null) {
			throw new BuildException("This task requires PublishRequestPETask");
		}
		// Lets start it all
		this.initPublishingWithQuery();
	}

	/**
	 * Method responsible initing RestClientReportRequest and for passing on
	 * configuration and parameters to it. Then also to init the report request
	 * and publishing of the resulting items
	 */
	private void initPublishingWithQuery() {
		// Init the ReportRequest Helper
		this.request = new RestClientReportRequest();

		this.addConfigsToRequest();
		this.addParamsToRequest();

		// Retrieve the CmsItemList with query (set in configs)
		CmsItemList itemList = null;

		try {
			itemList = this.request.getItemsWithQuery();
		} catch (FailedToInitializeException ex) {
			throw new BuildException(ex.getMessage());
		}

		// Get the "head according to index"
		try {
			this.headRevision = this.request.getRevisionCompleted();
		} catch (FailedToInitializeException e) {
			throw new BuildException(e.getMessage());
		}

		this.publishItems(itemList);
	}

	/**
	 * Iterates CmsItemList and passes each item to publishItem method
	 * 
	 * @param itemList
	 */
	private void publishItems(CmsItemList itemList) {
		logger.debug("enter");
		Iterator<CmsItem> itemListIterator = itemList.iterator();
		
		
		// Count number of items.
		long count = 0L;
		while (itemListIterator.hasNext()) {
			count++;
		}
		
		logger.debug("Counted {} and sizeFound {} number of items to publish", count, itemList.sizeFound());
		// TODO Add some filter method that will filter the list of items to publish
		// based on some criteria.
		
		// Publish items
		count = 0L;
		while (itemListIterator.hasNext()) {
			CmsItem item = itemListIterator.next();
			count++;
			logger.debug("Item nr {} file: {}", count, item.getId().getRelPath().getName());
			
			this.publishItem(item, this.headRevision.getNumber());
		}
		logger.debug("leave");
	}

	/**
	 * Calls the publish task for a CmsItem and sets properties needed for
	 * publishing to work. Adds a baseline pegrev which should be head at the
	 * time of the query to reporting framework ran.
	 * 
	 * @param item the CmsItem to publish
	 * @param baseLine the Long baseline to use
	 */
	private void publishItem(CmsItem item, Long baseLine) {
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
		for (String name : props.getKeySet()) {
			// p.put(n, props.getString(n));
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
