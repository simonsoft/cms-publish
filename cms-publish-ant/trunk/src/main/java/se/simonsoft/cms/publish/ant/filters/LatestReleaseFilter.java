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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.publish.ant.helper.RequestHelper;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

public class LatestReleaseFilter implements FilterItems {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private List<CmsItem> itemList;
	private RestClientReportRequest restReportClient;
	private RepoRevision headRev;
	private Project project;
	private String highestReleaseLabel;
	private String propertyName = "releaselabel";
	
	public LatestReleaseFilter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initFilter(RestClientReportRequest restReportClient,
			List<CmsItem> itemList, RepoRevision headRev, Project project) {
		logger.debug("enter");
		this.restReportClient = restReportClient;
		this.itemList = itemList;
		this.headRev = headRev;
		this.project = project;
	}

	@Override
	public void runFilter() {
		logger.debug("enter");
		Iterator<CmsItem> itemListIterator = this.itemList.iterator();
		ArrayList<CmsItem> itemsToRemove = new ArrayList<CmsItem>();
		
		//List<CmsItem> itemListCopy = (List<CmsItem>) this.itemList;
		logger.debug("LatestReleaseFilter");
		String releaseLabel = "";
		this.highestReleaseLabel = "";
		while (itemListIterator.hasNext()) {
			CmsItem item = itemListIterator.next();
			releaseLabel = RequestHelper.getItemProperty("prop_abx.ReleaseLabel", item.getProperties());
			
			if(releaseLabel.compareToIgnoreCase(this.highestReleaseLabel) > 0) {
				this.highestReleaseLabel = releaseLabel;
			}
			
		}
		
		while (itemListIterator.hasNext()) {
			CmsItem item = itemListIterator.next();
			
			releaseLabel = RequestHelper.getItemProperty("prop_abx.ReleaseLabel", item.getProperties());
			if(!releaseLabel.equals(this.highestReleaseLabel)) {
				logger.debug("Remove item with label {}", releaseLabel);
				itemsToRemove.add(item);
			} 
		}
		
		this.itemList.removeAll(itemsToRemove);
	}
	
	
	// TODO add method that check what kind of releaselabel it is: numeric, alphanumeric, alpha
}
