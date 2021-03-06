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
package se.simonsoft.cms.publish.ant.filters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * Filters item based on release label. 
 * Only supports A, B, C etc system right now.
 * 
 * @author joakimdurehed
 *
 */
public class LatestReleaseFilter implements FilterItems {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private List<CmsItem> itemList;
	private RestClientReportRequest restReportClient;
	private RepoRevision headRev;
	private Project project;
	private String propRelease = "abx:ReleaseLabel";
	
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
		
		
		// Filter will just keep items with the highestreleaselabel. 
		// It does not check for any translations. But takes for granted that tye
		// publish query included translation area. 
		// Also this filter works on releaselabels system A, B, C and so on for  now
		
		
		// TODO add method that check what kind of releaselabel it is: numeric, alphanumeric, alpha
		String release = this.getHighestReleaseLabel();
		
		if(!release.equals("")) {
			this.filterItemsByRelease(release);
		}
		
		logger.debug("end");
	}
	
	/**
	 * Finds the highest (latest) release label in itemList or uses the one set
	 * in Jenkins/ANT with property param.releaselabel. 
	 * 
	 * @return String release label
	 */
	private String getHighestReleaseLabel()
	{
		Iterator<CmsItem> itemListIterator = this.itemList.iterator();
		
		logger.info("Running release filter. Supports param.releaselabel");
		
		String highestReleaseLabel = "";
		
		// If a releaselabel has not been set by Jenkins/ANT find the highest and use that, else use the one given
		if(this.project.getProperty("param.releaselabel").equals("") || this.project.getProperty("param.releaselabel") == null) {
			
			// Find higehst releaseLabel 
			while (itemListIterator.hasNext()) {
				String releaseLabel = "";
				CmsItem item = itemListIterator.next();
				releaseLabel = item.getProperties().getString("abx:ReleaseLabel");
				
				if(releaseLabel != null) {
					
					if(releaseLabel.compareToIgnoreCase(highestReleaseLabel) > 0 || highestReleaseLabel.equals("")) {
						logger.debug("Set highestReleaseLabel to {}", releaseLabel);
						highestReleaseLabel = releaseLabel;
					}
				}
			}
		} else {
			logger.debug("Set highestReleaseLabel to param.releaselabel: {}", this.project.getProperty("param.releaselabel"));
			highestReleaseLabel = this.project.getProperty("param.releaselabel"); 
			// Todo, find out if 
		}
				
		logger.info("Release label to use: {}", highestReleaseLabel);
		return highestReleaseLabel;
	}
	
	/**
	 * Filters the item list by release label. Any items that do not match the release label are removed.
	 * @param release
	 */
	private void filterItemsByRelease(String release)
	{
		
		Iterator<CmsItem> itemListIterator =  this.itemList.iterator(); 
		ArrayList<CmsItem> itemsToKeep = new ArrayList<CmsItem>(); // List of items to publish
		if(release != null) {
			// Filter out items that is not of highest release label
			while (itemListIterator.hasNext()) {
				String releaseLabel = "";
				CmsItem item = itemListIterator.next();
				
				releaseLabel = item.getProperties().getString("abx:ReleaseLabel").toUpperCase();
				
				if(releaseLabel != null) {
					logger.debug("releaseLabel: {} highestReleaseLabel: {}", releaseLabel, release);
					if(releaseLabel.equals(release.toUpperCase())) {
						logger.debug("Keep item with label {}", releaseLabel);
						itemsToKeep.add(item);
					}
				}
			}
		}
		logger.debug("itemList size bfr clear: {} itemsToKeep size {}", this.itemList.size(), itemsToKeep.size());
		// Make sure we don't clear list if we've not found anything to filter
		if(itemsToKeep.size() > 0) {
			this.itemList.clear(); // Remove all
			this.itemList.addAll(itemsToKeep); // Add items to publish
		}
	
	}
	

}
