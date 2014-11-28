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
package se.simonsoft.cms.publish.ant.filters;

import java.util.List;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

public interface FilterResult {
	
	
	/**
	 * Initializes the filter with a RestClientReportRequest and list of items to filter. Optinally we can add a query and fields if
	 * we need to perform additional queries or we actually perform the extra query in the filter.
	 * This filter is intended for when you have to fetch some special data from a query for use in another query
	 * 
	 * @param restReportClient
	 * @param query
	 * @param fields
	 * @param itemList
	 */
	public void initFilter(RestClientReportRequest restReportClient, String query, String fields, List<CmsItem> itemList);
	
	/**
	 * Runs the filter and returns the result as String
	 * @return the filter result
	 */
	public String runFilter();
}
