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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author joakimdurehed
 *
 */
public class FilterResponseFlirImpl implements FilterResponse {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private ArrayList<SimpleCmsItemV1Impl> items;
	
	@Override
	public String filterField(String field) {
		// TODO Auto-generated method stub
	
		return null;
	}

	@Override
	public void setParsedItems(List items) {
		logger.debug("enter");
		// Casting away
		if(items instanceof ArrayList<?>) {
			this.items = (ArrayList<SimpleCmsItemV1Impl>)items;
		}
		logger.debug("leave");
	}

	@Override
	public void initFilter(String filter) {
		logger.debug("enter");
		this.filterDependents();
		this.filterName();
		logger.debug("leave");
	}
	
	
	private boolean haveDuplicate(String id)
	{
		logger.debug("enter");
		for(SimpleCmsItemV1Impl item : this.items) {
			
			if(item.getId().equals(id)) {
				return true;
			}
		}
		logger.debug("leave");
		return false;
	}
	
	
	private void filterFindHighestRev() 
	{
		Long highestRev = 0L;
		for(SimpleCmsItemV1Impl item : this.items) {
			
			if(highestRev < item.getRev()){
				highestRev = item.getRev();
			}
		}
	}
	
	private void filterName() 
	{
		logger.debug("enter");
		for(SimpleCmsItemV1Impl item : this.items) {
			
			if(item.getName().contains(".")) {
				String slices[] = item.getName().split("\\.");
				item.setName(slices[0]);
			}
		}
		logger.debug("leave");
	}
	
	private void filterDependents() 
	{
		logger.debug("enter");
		for(SimpleCmsItemV1Impl item : this.items) {
			// If the filename contains an underscore it is a dependency. 
			// Then we want to publish it's parent, so update the id accordingly
			if(item.getName().contains("_")) {
				String slices[] = item.getName().split("_");
				item.setName(slices[0]);
				
				// Modify logical id to match parent
				int index = item.getId().lastIndexOf("/");
				String updatedId = item.getId();
				updatedId = updatedId.substring(0, index);
				//log("Substring: " + updatedId);
				logger.debug("Updated id to: " + updatedId + " from " + item.getId());
				updatedId = updatedId + "/" + item.getName() + ".xml";
				
				// If the id does not already exist, change it.
				if(!this.haveDuplicate(updatedId)) {
					item.setId(updatedId);
				} else {
					this.items.remove(item);
				}
			}
			
		}
		logger.debug("leave");
	}
	

}
