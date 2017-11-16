package se.simonsoft.cms.publish.config.filter;

import java.util.List;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class PublishConfigFilterStatus implements PublishConfigFilter {

	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		
		List<String> statusInclude = config.getStatusInclude();
		if (statusInclude == null) {
			return true;
		} else if (statusInclude.isEmpty()) {
			return false;
		} else if (config.getStatusInclude().contains(item.getStatus())) {
			return true;
		} else {
			return false;
		}
	}

}
