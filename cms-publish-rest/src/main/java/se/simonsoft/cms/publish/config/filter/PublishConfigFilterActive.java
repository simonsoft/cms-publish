package se.simonsoft.cms.publish.config.filter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class PublishConfigFilterActive implements PublishConfigFilter {

	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		return false;
	}

}
