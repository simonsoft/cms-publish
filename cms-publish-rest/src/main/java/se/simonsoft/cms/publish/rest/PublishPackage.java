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
package se.simonsoft.cms.publish.rest;

import java.util.LinkedHashSet;
import java.util.Set;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.ReleaseLabel;

public class PublishPackage {

	private final String publication;
	private final PublishConfig publishConfig;
	private final Set<PublishProfilingRecipe> profilingSet;
	private final LinkedHashSet<CmsItemPublish> publishedItems;
	
	// Additional information when describing a Release.
	private final CmsItemId releaseItemId;
	private final ReleaseLabel releaseLabel;

	
	public PublishPackage(String publication, PublishConfig publishConfig, Set<PublishProfilingRecipe> profilingSet, LinkedHashSet<CmsItemPublish> publishedItems, CmsItemId releaseItemId, ReleaseLabel releaseLabel) {
		this.publication = publication;
		this.publishConfig = publishConfig;
		this.profilingSet = profilingSet;
		this.publishedItems = publishedItems;
		
		this.releaseItemId = releaseItemId;
		this.releaseLabel = releaseLabel;
	}


	public String getPublication() {
		return this.publication;
	}


	public PublishConfig getPublishConfig() {
		return this.publishConfig;
	}


	public Set<PublishProfilingRecipe> getProfilingSet() {
		return this.profilingSet;
	}


	public Set<CmsItemPublish> getPublishedItems() {
		return this.publishedItems;
	}


	public CmsItemId getReleaseItemId() {
		return this.releaseItemId;
	}


	public ReleaseLabel getReleaseLabel() {
		return this.releaseLabel;
	}

	
	public long getRevisionLatest() {
		
		long rev = 0;
		for (CmsItem item: publishedItems) {
			long number = item.getId().getPegRev();
			if (rev < number) {
				rev = number;
			}
		}
		return rev;
	}
}
