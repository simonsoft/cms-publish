package se.simonsoft.cms.publish.ant.filters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.list.CmsItemList;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
import se.simonsoft.cms.publish.ant.FilterResponse;
import se.simonsoft.cms.reporting.rest.itemlist.CmsItemListJSONSimple;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

public class FlirFilter implements FilterResponse {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private List<CmsItem> itemList;
	private RestClientReportRequest restReportClient;
	private RepoRevision headRev;
	
	public FlirFilter() {
		// TODO Auto-generated constructor stub
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
		// Return new ItemList?
		
		logger.debug("enter");
		//Iterator<CmsItem> itemListIterator = this.itemList.iterator();
		List<CmsItem> itemsParents = null;
		
		//List<CmsItem> itemListCopy = (List<CmsItem>) this.itemList;
		
		for(CmsItem item : this.itemList) {
			
			if(item.getId().getRelPath().getNameBase().contains("_")) {
				logger.debug("Found {} to be a of file name with _", item.getId().getRelPath().getName());
				// Get this items parent
				try {
					itemsParents = (List<CmsItem>) this.restReportClient.getItemsParents(item.getId(), "", "", headRev.toString(), "abx:Dependencies", item.getId().getRelPath().getPath(), true);
				} catch (FailedToInitializeException e) {
					logger.debug("Failed to init {}", e.getMessage());
				}
				
				
				if(itemsParents.size() > 1) {
					logger.debug("More than one parent to {}", item.getId().getRelPath().getName() );
				}
				Iterator<CmsItem> itemListIterator = itemsParents.iterator();
				itemsParents.get(0);
				/*
				for(CmsItem parentItem : itemsParents) {
					
				}
				*/
				// Propeties?
				//CmsItem parentItem = (CmsItem) new CmsItemIdArg(itemsParents.iterator().next().getId().getLogicalId());
				CmsItem parentItem = itemsParents.get(0);
				this.itemList.remove(item); // Remove now unuseful item
				this.itemList.add(parentItem); // Add parentitem
				
				// Remove THIS item from list
				// Add parent to list
				// getParents(CmsItemId itemId, String target, String base, String rev, String type, String pathArea, boolean head)
				logger.debug("Item contains _ use parent instead {} file: {}", item.getId().getRelPath().getName(), parentItem.getId().getRelPath().getName());
			}  
		}
	}
}
