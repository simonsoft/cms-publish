package se.simonsoft.cms.publish.rest;

import java.util.Set;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.release.ReleaseLabel;

public class PublishPackage {

	private final String publication;
	private final PublishConfig publishConfig;
	private final Set<PublishProfilingRecipe> profilingSet;
	private final Set<CmsItem> publishedItems;
	
	// Additional information when describing a Release.
	private final CmsItemId releaseItemId;
	private final ReleaseLabel releaseLabel;

	
	public PublishPackage(String publication, PublishConfig publishConfig, Set<PublishProfilingRecipe> profilingSet, Set<CmsItem> publishedItems, CmsItemId releaseItemId, ReleaseLabel releaseLabel) {
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


	public Set<CmsItem> getPublishedItems() {
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
