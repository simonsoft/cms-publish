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
package se.simonsoft.publish.ant.helper;

import java.util.ArrayList;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.publish.ant.filters.FilterItems;
import se.simonsoft.cms.publish.ant.filters.FilterPublishProperties;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;

/**
 * Helper class associated to RestClientReportRequest class. These helpers could
 * perhaps be a part of RestClientReportRequest class instead?
 * 
 * @author joakimdurehed
 *
 */
public final class RequestHelper {
	private static final Logger logger = LoggerFactory
			.getLogger(RequestHelper.class);

	public RequestHelper() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a mutable copy of a CmsItemList
	 * 
	 * @param itemList
	 * @return a ArrayList<CmsItem> copy of CmsItemList items
	 */
	public static ArrayList<CmsItem> createMutableItemList(CmsItemList itemList) {
		logger.debug("enter");
		ArrayList<CmsItem> copyItemList = new ArrayList<CmsItem>();

		for (CmsItem item : itemList) {
			copyItemList.add(item); // Add item to our itemlist
		}
		logger.debug("ItemList created with size {}", copyItemList.size());
		return copyItemList;
	}

	// TODO We could perhaps make the follwing two methods into one in the
	// future.

	/**
	 * Sets all ParamsNode params to given RestClientReportRequest requests
	 * param map
	 * 
	 * @param request
	 * @param params
	 */
	public static void copyParamsToRequest(ParamsNode params,
			RestClientReportRequest request) {
		logger.debug("enter");
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				request.addParam(param.getName(), param.getValue());
			}
		}
		logger.debug("leave");
	}

	/**
	 * Sets all ConfigsNode configs to given RestClientReportRequest configs map
	 * 
	 * @param request
	 * @param configs
	 */
	public static void copyConfigsToRequest(ConfigsNode configs,
			RestClientReportRequest request) {
		logger.debug("enter");
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				request.addConfig(config.getName(), config.getValue());
			}
		}
		logger.debug("leave");
	}

	/**
	 * Filters items using specified filter
	 */
	public static boolean runItemsFilterWithClassPath(String filterClassPath,
			ArrayList<CmsItem> itemList, RestClientReportRequest request,
			RepoRevision headRevision, Project project) {
		logger.debug("enter");

		// If class exists continue
		if (filterExistsWithClassName(filterClassPath)) {

			try {
				// TODO check that the filter syntax is a full qualified class
				// path?
				// We will grab any problem
				// in the exceptions anyways of course.
				logger.info("Init filter {}", filterClassPath);
				// Filters are instantiated by the name of the filter and the
				// suffix
				// Filter
				Class<?> filterClass = Class.forName(filterClassPath);
				FilterItems filterResponse = (FilterItems) filterClass
						.newInstance();

				// Todo should/would filter need some other stuff? I think the
				// request object, the list of items and a baseline
				// should suffice for almost whatever it is we need to do in the
				// filter.
				filterResponse.initFilter(request, itemList, headRevision,
						project);
				filterResponse.runFilter();

			} catch (InstantiationException e) {
				logger.warn(
						"Filter Init resulted in InstantiationException {}",
						e.getMessage());

			} catch (IllegalAccessException e) {
				logger.warn(
						"Filter Init resulted in IllegalAccessException {}",
						e.getMessage());

			} catch (ClassNotFoundException e) {
				logger.warn(
						"Filter Init resulted in ClassNotFoundException {}",
						e.getMessage());
			}
			return true;
		}
		return false;
	}

	/**
	 * Runs a PublishProps filter
	 * @param filterClassPath
	 * @param item
	 * @param headRev
	 * @param project
	 * @param publishTarget
	 */
	public static boolean runPublishFilterWithClassPath(String filterClassPath,
			CmsItem item, RepoRevision headRev, Project project,
			String publishTarget) {

		// If class exists continue
		if (filterExistsWithClassName(filterClassPath)) {

			try {
				// TODO check that the filter syntax is a full qualified class
				// path?
				// We will grab any problem
				// in the exceptions anyways of course.
				logger.info("Init filter {}", filterClassPath);
				// Filters are instantiated by the name of the filter and the
				// suffix
				// Filter
				Class<?> filterClass = Class.forName(filterClassPath);
				FilterPublishProperties filterPublishingProps = (FilterPublishProperties) filterClass
						.newInstance();

				// Todo should/would filter need some other stuff? I think the
				// request object, the list of items and a baseline
				// should suffice for almost whatever it is we need to do in the
				// filter.
				filterPublishingProps.initFilter(item, headRev, project,
						publishTarget);
				filterPublishingProps.runFilter();

			} catch (InstantiationException e) {
				logger.warn(
						"Filter Init resulted in InstantiationException {}",
						e.getMessage());

			} catch (IllegalAccessException e) {
				logger.warn(
						"Filter Init resulted in IllegalAccessException {}",
						e.getMessage());

			} catch (ClassNotFoundException e) {
				logger.warn(
						"Filter Init resulted in ClassNotFoundException {}",
						e.getMessage());
			}
			return true;
		}
		return false;
		
	}
	
	/**
	 * Validates that a filter exists and returns true if that is the case,
	 * false if not
	 * 
	 * @param filterClassPath
	 * @return
	 */
	private static boolean filterExistsWithClassName(String filterClassPath) {
		boolean classExists = true;

		if (filterClassPath == null || filterClassPath.equals("")) {
			logger.info("No filter class name set");
			classExists = false;
		}
		/*
		try {
			Class.forName(filterClassPath, false,null );
		} catch (ClassNotFoundException e) {
			logger.warn("Filter Init resulted in ClassNotFoundException {}",
					e.getMessage());
			classExists = false;
		}
		//*/
		// Avoid this method for now
		return classExists;
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
	public static String getItemProperty(String propertyName,
			CmsItemProperties props) {
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
}