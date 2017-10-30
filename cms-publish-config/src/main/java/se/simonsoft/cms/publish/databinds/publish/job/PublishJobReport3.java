package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJobReport3 {
	private Map<String, String> meta;
	private List<PublishJobItem> items;
	
	public Map<String, String> getMeta() {
		return meta;
	}
	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}
	public List<PublishJobItem> getItems() {
		return items;
	}
	public void setItems(List<PublishJobItem> items) {
		this.items = items;
	}
}