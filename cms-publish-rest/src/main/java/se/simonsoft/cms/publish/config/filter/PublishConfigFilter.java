package se.simonsoft.cms.publish.config.filter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public interface PublishConfigFilter {
	
	boolean accept(PublishConfig config, CmsItem item);

}
