package se.simonsoft.cms.publish.rest.config.filter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishConfigFilterProfiling implements PublishConfigFilter {

	
	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		
		if (!(item instanceof CmsItemPublish)) {
			return false;
		}
		
		CmsItemPublish itemPublish = (CmsItemPublish) item;
		
		if (config.getProfilingInclude() == null) {
			return true;
		} else {
			return config.getProfilingInclude().equals(itemPublish.hasProfiles());
		}
	}

}
