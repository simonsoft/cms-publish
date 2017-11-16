package se.simonsoft.cms.publish.config.filter;

import java.util.List;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class PublishConfigFilterStatus implements PublishConfigFilter {

	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		
		boolean accept = false;
		List<String> statusInclude = config.getStatusInclude();
		
		if (statusInclude == null || config.getStatusInclude().contains(item.getStatus())) {
			accept = true;
		}
		
		return accept;
	}

}
