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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

public class TechSpecFilter implements FilterItems {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private List<CmsItem> itemList;
	private RestClientReportRequest restReportClient;
	private RepoRevision headRev;
	
	public TechSpecFilter() {
		// Empty constructor
	}

	@Override
	public void initFilter(RestClientReportRequest restReportClient, List<CmsItem> itemList, RepoRevision headRev) {
		logger.debug("enter");
		this.restReportClient = restReportClient;
		this.itemList = itemList;
		this.headRev = headRev;
	}

	@Override
	public void runFilter() {
		logger.debug("enter");
		this.filterItemsToPublish();

	}
	
	/**
	 * Filters out any items with an underscore in it's name.
	 * For these, only publish it's parent.
	 * Filter also makes sure the parent exists.
	 * 
	 */
	private void filterItemsToPublish() 
	{
		
		logger.debug("enter");
		Iterator<CmsItem> itemListIterator = this.itemList.iterator();
		CmsItemList itemsParents = null;
		
		ArrayList<CmsItem> parentsToPublish = new ArrayList<CmsItem>();
		ArrayList<CmsItem> itemsToRemove = new ArrayList<CmsItem>();
		
		//List<CmsItem> itemListCopy = (List<CmsItem>) this.itemList;
		logger.debug("Filter ItemList");
		int counter = 0;
		while (itemListIterator.hasNext()) {
			CmsItem item = itemListIterator.next();
			if(item.getId().getRelPath().getNameBase().contains("_")) {
				logger.info("Found {} to to include an underscore.", item.getId().getRelPath().getName());
				// Get this items parent
				try {
					// Need to understand getParents...it wont work
					//itemsParents = this.restReportClient.getItemsParents(item.getId(), "", "", "","abx:Dependencies", "", true);
					
					String query = "prop_abx.Dependencies:*" + item.getId().getRelPath().getName() + "* AND head:true";
					itemsParents = this.restReportClient.getItemsWithQuery(query);
					
				} catch (FailedToInitializeException e) {
					logger.debug("Failed to init {}", e.getMessage());
				}
				
				ArrayList<CmsItem> parents = this.createMutableItemList(itemsParents);
				logger.debug("Size of parents list: {} (Expecting 1)", parents.size()); // Expected to be ONLY one
				
				if(parents.size() > 0) {
					counter++;
					CmsItem parentItem = parents.get(0); // We always fetch the first item and expect it to not have any more items
					logger.debug("Item contains underscore use parent {} instead of {}", parentItem.getId().getRelPath().getName(), item.getId().getRelPath().getName());

					// Both the parent and it's child item can be modified, make sure only ONE instance of the item exists in the list
					if(!this.itemList.contains(parentItem)) {
						logger.debug("Save parentitem to add it to the itemlist later in bulk");
						parentsToPublish.add(parentItem); // Save items to add to list later in bulk
					}
					
				} else {
					logger.info("No parent found. Might not be added to CMS yet or {} is not added as a dependency yet.", item.getId().getRelPath().getName());
				}
				// itemListIterator.remove(); // We could remove on the fly
				// No matter if we find a parent or not, we need to remove the item because we can't publish it on its own.
				itemsToRemove.add(item); // We store the items to be removed and remove them in bulk
				
				// getParents(CmsItemId itemId, String target, String base, String rev, String type, String pathArea, boolean head)
			}  
		}
		logger.info("{} items exchanged for their parents", counter);
		
		logger.debug("Size before removing items {}", this.itemList.size());
		// Remove all items that should not be published.
		this.itemList.removeAll(itemsToRemove);
		logger.debug("Size after removing items {}", this.itemList.size());
		logger.debug("Add items (parents) to itemList");
		// Add all parents that we need to publish to our modified itemlist
		this.itemList.addAll(parentsToPublish);
		logger.debug("Size after adding parent items {}", this.itemList.size());
		logger.debug("leave");
	}
	
	/**
	 * Helper method that creates a mutable copy of a CmsItemList
	 * @param CmsItemList itemList
	 */
	private ArrayList<CmsItem> createMutableItemList(CmsItemList itemList) 
	{
		logger.debug("enter");
		ArrayList<CmsItem> copyItemList = new ArrayList<CmsItem>();
		
 		for(CmsItem item: itemList ) {
 			copyItemList.add(item); // Add item to our itemlist
		}
 		return copyItemList;
	}
}
