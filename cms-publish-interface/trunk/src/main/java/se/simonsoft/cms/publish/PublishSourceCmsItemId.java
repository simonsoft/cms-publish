package se.simonsoft.cms.publish;

import se.simonsoft.cms.item.CmsItemId;

/**
 * Logical ID without hostname (or should we require hostname at construction and return full logical id?).
 */
public class PublishSourceCmsItemId implements PublishSource {

	private CmsItemId id;

	public PublishSourceCmsItemId(CmsItemId id) {
		this.id = id;
	}
	
	@Override
	public String getURI() {
		return id.getLogicalId();
	}

}
