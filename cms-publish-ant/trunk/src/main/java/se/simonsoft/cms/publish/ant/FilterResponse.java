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

import java.util.List;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * @author joakimdurehed
 *
 */
public interface FilterResponse {
	
	/**
	 * Initialize the filter with a necessary properties and tools.
	 * RestClientReportRequest to perform additional queries
	 * List<CmsItem> the actual list to filter
	 * RepoRevision the head rev according to index, used for baseline
	 * 
	 * @param RestClientReportRequest restReportClient
	 * @param List<CmsItem> itemList
	 * @param RepoRevision headRev
	 */
	public void initFilter(RestClientReportRequest restReportClient, List<CmsItem> itemList, RepoRevision headRev);
	
	/**
	 * Run the filter
	 */
	public void runFilter();
}
