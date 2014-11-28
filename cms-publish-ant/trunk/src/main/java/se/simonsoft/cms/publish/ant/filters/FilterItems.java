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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * @author joakimdurehed
 *
 */
public interface FilterItems {
	
	public enum FilterOrder {
		PRE, POST
	}
	/**
	 * Initialize the filter with a necessary properties and tools.
	 * Might be used for simply filter out items while running additional queries and so on.
	 * RestClientReportRequest to perform additional queries
	 * List<CmsItem> the actual list to filter
	 * RepoRevision the head rev according to index, used for baseline
	 * Project the current ANT project
	 * 
	 * @param restReportClient
	 * @param itemList
	 * @param headRev
	 * @param project
	 */
	public void initFilter(RestClientReportRequest restReportClient, List<CmsItem> itemList, RepoRevision headRev, Project project);
	
	/**
	 * Run the filter
	 */
	public void runFilter();
	
}

// TODO support types of filter? 
// query filter: for when you need to perform two or more queries to get build the final query used for getting items to publish
// items filter for when you need to filter the result