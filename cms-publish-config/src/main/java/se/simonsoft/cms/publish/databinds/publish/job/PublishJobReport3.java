package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJobReport3 {
	private PublishJobMeta meta;
	private List<PublishJobItem> items;
	
	public PublishJobMeta getMeta() {
		return meta;
	}
	public void setMeta(PublishJobMeta meta) {
		this.meta = meta;
	}
	public List<PublishJobItem> getItems() {
		return items;
	}
	public void setItems(List<PublishJobItem> items) {
		this.items = items;
	}
}
