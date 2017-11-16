package se.simonsoft.cms.publish.config.filter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class PublishConfigFilterType implements PublishConfigFilter {
	
	private final String typeInclude = "embd_xml_a_type"; //TODO: Unsure if this is the correct name: type-include embd_xml_a_type
	
	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		String type = (String) item.getMeta().get(typeInclude);
		return config.getOptions().getType().equals(type);
	}

}
